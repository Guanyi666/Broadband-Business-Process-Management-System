# BBPMS 2.0 — ITERATION 3 实施报告（CUSTOMER 客户自助端）

> 产出时间：2026-09-04  
> 范围：客户账号密码登录、客户数据绑定、订单进度、自助报装、报障/投诉、评价、预约改期、资料变更审核与消息中心  
> 明确不包含：短信验证码登录、微信/小程序授权登录

## 1. 交付范围

### 1.1 客户身份与安全边界

- 复用 `sys_user`、`sys_role`、JWT 与 BCrypt，客户使用账号密码登录。
- 新增 `customer_user_binding`，显式绑定 `sys_user.id -> customer.id`，停用账号时同时停用绑定与系统账号。
- 客户端只调用 `/api/customer-portal/**`，每次读写先解析当前登录人的有效绑定。
- 订单、预约、服务单、评价和资料申请均校验 `customer_id` 归属；跨客户访问统一返回“资源不存在”，避免泄露资源是否存在。
- CUSTOMER 不再使用通用 `order:view`。原 `SELF` 数据范围按 `create_by` 过滤，不能表达“客户主数据归属”，因此不能作为客户隔离边界。

### 1.2 客户 H5（`bbpms-customer-h5`）

- 账号密码登录（密码沿用现有 RSA 传输加密），登录后校验必须包含 `CUSTOMER` 角色。
- 首页、订单列表、订单详情和业务时间线。
- 自助报装：服务端套餐校验、资源二次核查、生成 `PENDING_CS_CONFIRM` 订单。
- 客服退回后修改地址/预约时间并重新提交。
- 预约改期并记录 `appointment_change_log`。
- 报障与投诉服务单：提交、查看进度、解决确认。
- 完工评价：一个订单只能评价一次，重新计算装维人员综合评分；现有派单算法继续使用该评分的 15% 权重。
- 资料变更申请：姓名、手机号、证件号在待审核表中以 SM4 密文保存，审核通过后才写入客户主数据。
- 站内消息和登录密码修改。

### 1.3 客服管理端

- 客户详情页：开通客户账号、启停账号、重置密码、查看最近登录时间。
- “客户自助业务”页：审核自助报装、处理报障/投诉、审核资料变更，支持状态过滤与分页。

## 2. 数据库变更

迁移文件：`middleware/mysql/init/07-customer-portal-schema.sql`。

新增 6 张表：

- `customer_user_binding`
- `broadband_package`
- `customer_service_ticket`
- `service_evaluation`
- `customer_profile_change`
- `appointment_change_log`

向后兼容补列：

- `broadband_order.source`
- `appointment.status/reschedule_count/confirmed_by/confirmed_time`
- `work_order.business_type`

脚本使用 `CREATE TABLE IF NOT EXISTS`、`INSERT IGNORE` 与按列存在性判断的存储过程，可重复执行。新部署会随 MySQL init 自动执行；已有数据库需手工执行一次：

```bash
mysql -u root -p bbpms < middleware/mysql/init/07-customer-portal-schema.sql
```

脚本内置演示账号 `customer1 / admin123`，使用自增用户 ID，并绑定演示客户 ID 1。

## 3. 主要 API

客户接口前缀：`/api/customer-portal`

| 能力 | 方法与路径 |
|---|---|
| 资料/改密 | `GET /profile`、`POST /password` |
| 套餐/资源 | `GET /packages`、`POST /resources/check` |
| 订单 | `GET/POST /orders`、`GET /orders/{id}`、`PUT /orders/{id}/resubmit` |
| 预约 | `PUT /orders/{id}/appointment` |
| 报障投诉 | `GET/POST /tickets`、`POST /tickets/{id}/confirm` |
| 评价 | `GET/POST /orders/{id}/evaluation` |
| 资料变更 | `GET/POST /profile/change-requests` |
| 消息 | `GET /messages`、`PUT /messages/{id}/read` |

管理接口前缀：`/api/customer-portal/admin`，统一要求 `customer-portal:admin` 权限。

## 4. 状态流转

客户自助订单：

```text
PENDING_CS_CONFIRM ──客服确认──> CREATED ──原有审核/派单/施工流程──> FINISHED/CLOSED
        │
        └──客服退回──> CS_REJECTED ──客户修改重提──> PENDING_CS_CONFIRM
```

服务单：

```text
SUBMITTED -> ACCEPTED -> PROCESSING -> WAIT_CONFIRM -> CLOSED
                                      └──────────────> 客户确认解决
```

## 5. 验证结果

- 后端 Maven 测试：44/44 通过，其中新增 4 个客户绑定与越权隔离测试。
- 管理后台：`vue-tsc --noEmit` 与 `vite build` 通过。
- 客户 H5：`vue-tsc --noEmit` 与 `vite build` 通过。
- `git diff --check` 通过。

本机没有可用的项目 MySQL 账号，因此未直接改动或污染现有开发库；数据库脚本随新库初始化执行，已有库按 §2 手工迁移。

## 6. 后续建议

- P0：正式环境首次登录强制改初始密码；密码重置后吊销该用户现有会话。
- P1：接入对象存储上传报障图片；服务单与维修工单联动。
- P1：接入短信服务后再增加验证码登录或找回密码，本迭代不预留绕过密码的入口。
- P2：地址标准化、安装时段容量与并发资源预占。
