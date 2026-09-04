# 后端行为审计（ITERATION 0）

> 审计时间：2026-09-03 · 只读审计 · 基线 `776d257`

## 1. API 清单与权限注解覆盖

共 **22 个 Controller、124 个业务端点**。逐 Controller 统计（端点=映射方法数，权限=@PreAuthorize 数）：

| Controller | 端点 | @PreAuthorize | 缺口 |
|---|---|---|---|
| WorkOrderController | 18 | 14 | 4 |
| AttendanceController | 10 | 10 | 0 |
| InstallController | 9 | 8 | 1 |
| OrderController | 8 | 8 | 0 |
| AuthController | 7 | 0 | **7（登录/公钥/me 属认证端点，除 me 外合理放行）** |
| LeaveController | 7 | 7 | 0 |
| SysUserController | 7 | 7 | 0 |
| SysRoleController | 7 | 7 | 0 |
| CustomerController | 6 | 6 | 0 |
| MessageTemplateController | 6 | 4 | 2 |
| DispatchController | 6 | 5 | 1 |
| FileController | 5 | 5 | 0 |
| InstallerController | 5 | 4 | 1 |
| SysMenuController | 5 | 3 | 2 |
| SysDeptController | 4 | 4 | 0 |
| DashboardController | 3 | 3 | 0 |
| NotifyController | 3 | 3 | 0 |
| AppointmentController | 3 | 3 | 0 |
| DispatchRuleController | 2 | 2 | 0 |
| LoginLogController | 1 | 1 | 0 |
| OperationLogController | 1 | 1 | 0 |
| WorkOrderTimelineController | 1 | **0** | 1 |

**结论**：前端菜单已按 RBAC 隐藏，大部分端点有注解；缺注解的具体端点
（WorkOrder 4 + Install 1 + Dispatch 1 + Installer 1 + Menu 2 + Template 2 + Timeline 1 ≈ 12 处）
需在迭代中逐个补齐，属 **P1 权限加固**而非 P0（菜单已挡住，且无注解端点多数仍走登录态拦截器）。

## 2. 订单状态机实现现状 ⭐

- **模型**：`common/statemachine/OrderStateMachine`（事件驱动 + **角色白名单**），已实现 10 条合法转换（见 ARCHITECTURE.md §3）
- **实现亮点**：每个 Transition 绑定 `List<Integer> roles`，不满足角色直接拒绝——**不是"谁都能改状态"**，符合 2.0 原则
- **时点字段**：audit_time / dispatch_time / completed_time / cancelled_time 齐全
- **审计**：order_audit_log 记录 from/to/operator/remark
- **缺口**：
  - 订单状态机**缺"审核驳回"分支的再提交路径**（AUDIT_REJECT 直入 CANCELLED，无"驳回后可修改重提"）——真实运营商会驳回后让客服改单重提
  - 无"履约中"投标的宽泛中间态概念。当前 map：CREATED→AUDITED→WAIT_DISPATCH→DISPATCHED→INSTALLING→FINISHED→CLOSED，**相对线性**
  - 无 resource 核查环节（2.0 要拆"审核通过→资源核查→预约→派单"，当前审核通过即 WAIT_DISPATCH）

## 3. 工单状态机实现现状

- **模型**：`WorkOrderStateMachine` + 10 态枚举（PENDING/DISPATCHED/ACCEPTED/IN_PROGRESS/STALLED/REASSIGNING/COMPLETED/FAILED/CANCELLED/AUTO_CANCELLED）
- **终态**：COMPLETED / CANCELLED / FAILED / AUTO_CANCELLED（`isTerminal`）
- **时间线**：work_order_timeline 全量记录 from/to/operator/role —— **2.0"工单时间线"已基本具备**（ITERATION 3/4 可直接复用）
- **改派**：REASSIGNING 态存在；改派弹窗/下拉/候选（/dispatch/candidates）已实现并通过 UI E2E
- **缺口**：
  - 无"预约中/前往现场/已到达/待验收/异常"细分（当前 IN_PROGRESS 一态包揽施工全过程）——ITERATION 4 需用 install_record 或子阶段补充
  - 无客户验收/评价动作（FINISHED→CLOSED 用 CONFIRM，但无评价数据）

## 4. SLA 定时任务 ⭐（可复用资产）

`WorkOrderSlaScheduler`（@Scheduled）：
- DISPATCHED → `accept_timeout_minutes`(默认30) 内未接单 → **AUTO_CANCELLED**
- IN_PROGRESS → `progress_heartbeat_timeout_hours`(默认4) 无心跳 → **STALLED**
- STALLED → `stalled_recover_hours`(默认24) 未恢复 → **AUTO_CANCELLED**
- 阈值来自 **`wo_sla_policy` 表（按 business_type 可配置）**，已观测运行时生效

**2.0 SLA 缺口**：现仅"工单接单/停滞"两类 SLA；2.0 要的是受理/审核/派单/接单/上门/施工/开通/故障修复 8 类 SLA + 实时预警 + 超时预测。`wo_sla_policy` 结构需扩展（加 stage 维度与预警阈值）。

## 5. 智能派单算法 ⭐（可复用资产）

`dispatch/algorithm/DispatchScoringService`：
- **4 因子加权**（0-100 分）：distance(默认权重40) + load(25) + skill(20) + rating(15)；半径过滤 radius_km=30
- 权重可配置：`dispatch_rule` 表（weight_distance/weight_load/weight_skill/weight_rating/radius_km）
- 在线池：Redis ZSET `installers:active`（DispatchServiceImpl:79）
- 策略：AUTO / MANUAL / REASSIGN 三类（dispatch_record.strategy）

**2.0 差距**：任务书建议 6 因子（区域30/距离20/负载20/技能15/完成率10/评价5）——现缺"区域匹配"显式权重与"历史完成率"。权重配置表已具备扩展条件。

## 6. 测试现状

`bbpms-app/src/test/java` 共 **8 个测试类**：
- 单元：WorkOrderStateMachineTest / SnowflakeIdGeneratorTest / LeaveEscalationRuleTest / WorkOrderSlaPropertiesTest
- 集成：AuthIntegrationTest / OrderIntegrationTest / WorkOrderIntegrationTest（TestClients 提供客户端）

**缺口**：无 Dispatch/Install/Notify/Dashboard 集成测试；无前端自动化测试框架；覆盖偏核心链路但不足。

## 7. 问题清单

| 级别 | 问题 |
|---|---|
| **P0** | 无（前端菜单 RBAC、后端大部 @PreAuthorize、核心链路可跑通） |
| **P1** | ①~12 个端点缺 @PreAuthorize（WorkOrder×4 等）；②订单审核驳回无"修改重提"路径；③数据权限仅 SELF 落地 |
| **P2** | ①测试覆盖不足（派单/安装/仪表盘/通知无测试）；②文档声明工单 7 态与实际 10 态不符；③dispatch_rule 无自动派单总开关字段 |
| **P3** | AI Agent 前置的数据审计均依赖后续迭代建模（资源/故障/投诉/评价） |

## 8. 可复用资产汇总（2.0 直接获益）

- ✅ 订单状态机（事件+角色白名单）→ 2.0 状态机扩展基座
- ✅ 工单状态机 + 时间线表 → 施工全过程/时间线迭代基座
- ✅ SLA 调度器 + wo_sla_policy 可配置 → SLA 2.0 扩展基座
- ✅ 派单评分算法 + dispatch_rule 权重表 → 智能派单 2.0 扩展基座
- ✅ appointment / install_record / attachment / dispatch_record 表已存在 → 预约/施工/附件迭代可复用
- ✅ 幂等表 / 操作日志 / 审计字段（create_by/update_by/deleted/version）→ 审计合规基座