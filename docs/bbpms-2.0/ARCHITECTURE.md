# 当前架构（ITERATION 0 审计产出）

> 审计时间：2026-09-03 · 审计方式：源码阅读 + 运行库核对（只读）
> 基线提交：`776d257`

## 1. 后端技术栈与模块划分

- **语言/框架**：Java 21 + Spring Boot 3.2.5
- **形态**：模块化单体（Modular Monolith），单应用 `bbpms-app`，按业务域分包
- **存储**：MySQL 单库（`bbpms`，26 张表）+ Redis 单节点（在线池 ZSET / 分布式锁 Redisson）
- **鉴权**：JWT（RSA 公钥加密登录，BCrypt+SALT）+ Spring Interceptor；无网关
- **本地事务**替代 Seata；**Spring `ApplicationEvent`** 替代 RabbitMQ（无 Outbox）
- **域包清单**（13 个）：

```
attendance/  考勤（签到/汇总/日报）
auth/        认证（登录/公钥/me/刷新）
common/      实体/枚举/状态机/工具
dashboard/   数据看板（overview/trend/dispatch-duration）
dispatch/    派单（算法/规则/记录，Redis 在线池）
file/        文件上传（本地桶）
install/     装维施工（施工记录/图片/签名）
interceptor/ 拦截器（鉴权/日志/审计）
leave/       请假（多级审批/升级规则）
log/         登录日志/操作日志
notify/      消息模板/发送记录（SMS/WECHAT/INAPP）
order/       客户/订单/预约/审核日志
user/        用户/角色/菜单/部门/装维档案
workorder/   工单/时间线/SLA 调度器
```

## 2. 数据库模型（26 表）

### RBAC 域（8 表）
| 表 | 用途 | 关键字段 |
|---|---|---|
| `sys_dept` | 部门树 | parent_id / path(物化路径) / sort |
| `sys_user` | 用户 | dept_id / user_type(1超级管理员~6客户) / salt |
| `sys_role` | 角色 | code / **data_scope(1=ALL,2=DEPT,3=DEPT_AND_CHILD,4=SELF,5=CUSTOM)** |
| `sys_menu` | 菜单/权限 | type(1目录/2菜单/3按钮) / perms / path |
| `sys_user_role` | 用户-角色 | user_id / role_id |
| `sys_role_menu` | 角色-权限 | role_id / menu_id |
| `login_log` | 登录日志 | username / ip / status |
| `operation_log` | 操作日志 | module / action / uri / cost_ms |

### 核心业务域
| 表 | 用途 | 关键字段 |
|---|---|---|
| `customer` | 客户 | name / phone / address / lat/lng / grid_code |
| `broadband_order` | 宽带订单 | status / package_code / cs_id / auditor_id / dispatch_time / completed_time / cancel_reason |
| `order_audit_log` | 审核记录 | order_id / from_status / to_status / auditor_id |
| `appointment` | 安装预约 | order_id / appointment_time / confirmed / remark |
| `work_order` | 工单 | order_id / installer_id / dispatcher_id / status / accept_time / finish_time |
| `work_order_timeline` | 工单时间线 | work_order_id / from_status / to_status / operator_id / operator_role |
| `install_record` | 施工记录 | onu_mac / onu_sn / olt_port / signal_db / photos(JSON) / signature_url |
| `installer_profile` | 装维档案 | skill_tags(JSON) / service_area(JSON) / lat/lng / on_duty / workload / score |
| `dispatch_record` | 派单记录 | work_order_id / installer_id / strategy(AUTO/MANUAL/REASSIGN) / score / candidates_json |
| `dispatch_rule` | 派单规则 | weight_distance=40 / weight_load=25 / weight_skill=20 / weight_rating=15 / radius_km=30 |
| `wo_sla_policy` | SLA 策略 | business_type / accept_timeout_minutes=30 / progress_heartbeat_timeout_hours=4 / stalled_recover_hours=24 |
| `attachment` | 附件 | object_key / biz_type(ORDER/INSTALL/CUSTOMER) / biz_id |
| `idempotency_record` | 幂等 | idempotency_key / result_json |

### 考勤/请假域（4 表）
| 表 | 用途 |
|---|---|
| `att_attendance_record` | 签到记录（clock_in/out/work_minutes/break/status） |
| `att_attendance_summary` | 月度汇总（work_days/late/early/absent） |
| `lv_leave_request` | 请假申请（type/status/current_level/required_level 两段审批） |
| `lv_leave_approval_record` | 审批记录（APPROVED/REJECTED/ESCALATED） |

### 消息域（2 表）
| 表 | 用途 |
|---|---|
| `message` | 发送记录（SMS/WECHAT/INAPP / PENDING/SUCCESS/FAILED） |
| `message_template` | 模板（aliyun_template_id / wechat_template_id） |

### ER 关系（核心链路）
```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
  │  dept_id
  ▼
sys_dept
customer ──< broadband_order ──< work_order ──< work_order_timeline
             │  cs_id/auditor_id → sys_user      │  installer_id/dispatcher_id → sys_user
             ├─< order_audit_log                  ├─1:1 install_record
             └─< appointment                      ├─< dispatch_record
                                                  └─< attachment(biz_id)
```

## 3. 订单/工单状态机（现状）

### 订单 8 态（`OrderStatus` + `OrderStateMachine`，事件驱动 + 角色白名单）
| 状态 | 说明 |
|---|---|
| CREATED | 已创建 |
| AUDITED | 已审核 |
| WAIT_DISPATCH | 待派单 |
| DISPATCHED | 已派单 |
| INSTALLING | 安装中 |
| FINISHED | 已完成 |
| CLOSED | 已归档（终态） |
| CANCELLED | 已取消（终态） |

**已实现的状态转换（含角色约束，role: 2=客服 3=审核 4=调度 5=装维 6=客户）**：
```
CREATED --AUDIT_PASS(3)--> AUDITED
CREATED --AUDIT_REJECT(3)--> CANCELLED
CREATED --CANCEL(2,6)--> CANCELLED
AUDITED --START_DISPATCH(4)--> WAIT_DISPATCH
WAIT_DISPATCH --DISPATCH_OK(4)--> DISPATCHED
WAIT_DISPATCH --DISPATCH_TIMEOUT(4)--> CANCELLED
DISPATCHED --ACCEPT(5)--> INSTALLING
DISPATCHED --TRANSFER(4,5)--> WAIT_DISPATCH
INSTALLING --COMPLETE(5)--> FINISHED
FINISHED --CONFIRM(2,6)--> CLOSED
```

### 工单 10 态（`WorkOrderStatus` + `WorkOrderStateMachine`）
```
PENDING(待派发) → DISPATCHED(已派单) → ACCEPTED(已接单) → IN_PROGRESS(施工中)
→ COMPLETED(已完成)     [终态: COMPLETED/CANCELLED/FAILED/AUTO_CANCELLED]
另有: STALLED(停滞) / REASSIGNING(改派中) / CANCELLED / AUTO_CANCELLED
```

**运行库现状核对**：
```
broadband_order: CREATED×1, AUDITED×1, WAIT_DISPATCH×1, DISPATCHED×2,
                 INSTALLING×1, FINISHED×1, CLOSED×1, CANCELLED×1   （8 态全覆盖）
work_order:      PENDING×1, DISPATCHED×1, COMPLETED×2, STALLED×1, AUTO_CANCELLED×3
```

## 4. 定时任务与事件

| 组件 | 位置 | 逻辑 |
|---|---|---|
| `WorkOrderSlaScheduler` | workorder/scheduler | @Scheduled 扫描：DISPATCHED→超30min未接单→AUTO_CANCELLED；IN_PROGRESS→4h无心跳→STALLED；STALLED→24h未恢复→AUTO_CANCELLED。阈值来自 `wo_sla_policy` 可配置 |
| `AttendanceScheduleJob` | attendance/job | 考勤日报/月度汇总计算 |
| DispatchEvent（事件） | dispatch/event | 派单事件解耦（自动派单触发） |

> SLA 调度器已在运行时观测到真实生效（手动置 DISPATCHED 的老工单 ~30 秒被扫描自动取消）。

## 5. 数据权限现状

- **模型层**：`sys_role.data_scope` 已定义 5 档（1=ALL / 2=DEPT / 3=DEPT_AND_CHILD / 4=SELF / 5=CUSTOM）
- **实施层**：实际仅 SELF 生效（装维人员只能看自己的工单）；DEPT/CUSTOM 未落地到 SQL 过滤（SA-P3-001 延期项）
- **表结构支撑**：sys_user.dept_id、sys_dept.path（物化路径）已具备，实现 DEPT 范围查询无结构障碍

## 6. 架构现状与声明的差异/问题

✅ **与 README 声明一致**：订单 8 态、工单 10 态（README 说 7 态，实际枚举 10 个含 STALLED/FAILED/AUTO_CANCELLED，属超量实现）、数据范围 5 档定义、自动派单加权算法、SLA 调度器、事件驱动解耦

⚠️ **差异点**：
1. 工单"7 态"声明 vs 实际 10 态枚举（多出 STALLED / FAILED / AUTO_CANCELLED）——文档需更新
2. 数据范围仅 SELF 落地，DEPT/CUSTOM 停在模型层
3. 无 resource（地址资源/OLT/PON/端口）模型——**ITERATION 2 需要全新建模**
4. 无 fault（故障）/ complaint（投诉）/ evaluation（评价）/ bill（账单）模型——对应 ITERATION 7/8/9/10 均为空白
5. 设备/材料无实体（install_record 仅有 onu_sn/mac，无库存/领用/SN 台账）——ITERATION 5 需新建
6. 网络开通无实体（无宽带账号/VLAN/IP/激活记录）——ITERATION 6 需新建
7. appointment 仅为单时间点预约（无时间段/改约/状态机）——ITERATION 3 需扩展
8. 自动化派单事件存在，但"自动派单开关"可配置性待确认（dispatch_rule 仅有权重无开关字段）