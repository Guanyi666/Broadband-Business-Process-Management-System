# BBPMS 数据库设计

> 单库 `bbpms`，所有表在 `middleware/mysql/init/` 下。本文档汇总表结构与设计约定。

## 1. 设计约定

- **库**：单库 `bbpms`，应用账号 `bbpms_app / bbpms_pwd_2026`。
- **通用列（BaseDO）**：所有表含 `id` / `create_time` / `update_time` / `create_by` / `update_by` / `deleted` / `version` 七列。
  - `deleted`：逻辑删除（1=已删），业务查询一律 `deleted = 0`。
  - `version`：MyBatis-Plus 乐观锁，更新时校验。
  - `create_by` / `update_by`：由 `AutoFillHandler` 自动填充。
- **命名**：表/列 snake_case；时间列 `*_time`；布尔用 `TINYINT`；金额/评分用 `DECIMAL`。
- **初始化**：`docker compose down -v && up` 重跑 `middleware/mysql/init/`（00 建库 → 01 建表 → 03 扩展表 → 04 种子）。

## 2. 表清单（按模块）

### 用户 / 权限（user）
| 表 | 关键列 | 说明 |
|---|---|---|
| `sys_user` | username(uk), password(bcrypt), phone(uk), phone_enc, real_name, nickname, dept_id, user_type, status | 系统用户 |
| `sys_role` | code(uk), name, data_scope(1~5), sort, remark | 角色 + 数据范围 |
| `sys_menu` | parent_id, type(1=DIR/2=MENU/3=BUTTON), perms, path, component, icon, sort | 菜单即权限点 |
| `sys_user_role` | user_id, role_id（复合主键） | 用户-角色 |
| `sys_role_menu` | role_id, menu_id（复合主键） | 角色-菜单 |
| `sys_dept` | parent_id, name, leader, phone, path(物化路径), sort | 部门 |
| `installer_profile` | user_id(PK), skill_tags(JSON), service_area(JSON), current_lat/lng, last_location_time, on_duty, workload, level, score | 装维档案 |

### 订单（order）
| 表 | 关键列 | 说明 |
|---|---|---|
| `customer` | name, phone(SM4), id_card_no(SM4), address, lat, lng, grid_code | 客户 |
| `broadband_order` | order_no(uk), customer_id, package_code, status, cs_id, auditor_id, audit_time, dispatch_time, completed_time, cancelled_time | 宽带订单（状态机驱动） |
| `appointment` | order_id, appointment_time, contact_phone, confirmed | 预约 |
| `order_audit_log` | order_id, from_status, to_status, auditor_id, remark | 订单状态审计 |

### 工单（workorder）
| 表 | 关键列 | 说明 |
|---|---|---|
| `work_order` | work_no(uk), order_id, installer_id, dispatcher_id, status, dispatch_time, accept_time, start_time, finish_time, install_address, customer_phone, package_name, **priority, expected_finish_time, last_active_at, stall_reason, cancel_type**（SLA 列） | 工单（10 状态） |
| `work_order_timeline` | work_order_id, from_status, to_status, operator_id, operator_role, remark | 工单状态时间线 |

### 派单（dispatch）
| 表 | 关键列 | 说明 |
|---|---|---|
| `dispatch_record` | work_order_id, installer_id, strategy(AUTO/MANUAL/REASSIGN), score, candidates_json, reason | 派单记录（含候选评分审计） |
| `dispatch_rule` | name(uk), weight_distance, weight_load, weight_skill, weight_rating, radius_km, enabled | 派单规则（默认 `default`） |

### 安装（install）
| 表 | 关键列 | 说明 |
|---|---|---|
| `install_record` | work_order_id(uk), installer_id, onu_mac/sn, olt_port, signal_db, start_lat/lng, complete_lat/lng, photos(JSON), signature_url, status | 装机记录 |

### 支撑（notify / file / log）
| 表 | 关键列 | 说明 |
|---|---|---|
| `message` | user_id, channel(SMS/WECHAT/INAPP), template_code, params(JSON), content, status | 消息记录 |
| `message_template` | code(uk), channel, subject, content | 消息模板 |
| `attachment` | object_key(uk), bucket, original_name, size, biz_type, biz_id, uploader_id | 附件元数据 |
| `operation_log` | user_id, username, module, action, request_uri, method, params, ip, cost_ms, status, error | 操作日志 |
| `login_log` | user_id, username, ip, user_agent, status, message | 登录日志 |

### 考勤 / 请假（attendance / leave）
| 表 | 关键列 | 说明 |
|---|---|---|
| `att_attendance_record` | installer_id, work_date, clock_in_at, clock_out_at, work_minutes, break_minutes, status, source | 每日考勤（`uk_installer_date`） |
| `att_attendance_summary` | installer_id, year_month(uk), total_work_minutes, work_days, late_count, early_leave_count, absent_count | 月度汇总（当前未自动计算，待办） |
| `lv_leave_request` | applicant_id, leave_type, start_at, end_at, total_hours, reason, attachment_url, status, current_level, required_level, level1/2_approver_id | 请假申请（多级审批） |
| `lv_leave_approval_record` | leave_id, approver_id, approval_level, action, comment | 审批留痕 |
| `wo_sla_policy` | business_type(uk), accept_timeout_minutes, progress_heartbeat_timeout_hours, stalled_recover_hours, enabled | SLA 策略（当前由 yml 配置驱动，表为 v2 预留） |

## 3. 核心索引

- `sys_user`: uk_username / uk_phone / uk_email / idx_dept_status
- `broadband_order`: uk_order_no / idx_status_create_time / idx_auditor_id
- `work_order`: uk_work_no / idx_installer_status / idx_status / idx_order_id / idx_status_last_active
- `dispatch_record`: idx_work_order_id / idx_installer_id / idx_strategy
- `install_record`: uk_work_order_id / idx_installer_id / idx_status
- `att_attendance_record`: uk_installer_date / idx_status / idx_clock_in_at
- `lv_leave_request`: idx_applicant_status / idx_status_start / idx_leave_type

## 4. 已知数据库待办

1. `att_attendance_summary` 月度汇总无计算逻辑（调度器未填充）——报表恒空。
2. DEPT / CUSTOM 数据范围需业务表补充 `dept_id` 列（当前仅支持 ALL / SELF）。
3. `wo_sla_policy` 表当前未被读取（SLA 阈值走 `bbpms.workorder.sla.*` 配置）。
