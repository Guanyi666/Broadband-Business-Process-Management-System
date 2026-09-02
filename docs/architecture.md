# BBPMS 系统架构

> 面向架构师与后续开发者。详细到文件级的导览见 [`PROJECT_TOUR.md`](PROJECT_TOUR.md)。

## 1. 总体架构

**模块化单体（Modular Monolith）**：单个 Spring Boot 3.2.5 应用按业务域分包，替代原规划的 11 个微服务。

```
┌──────────────── PC 管理后台 (5173) ────────┐  ┌────── 装维 H5 (9002) ──────┐
│  Vue3 + Element Plus + Pinia + ECharts      │  │  Vue3 + Vant4 + 高德地图   │
└────────────────────┬────────────────────────┘  └────────────┬──────────────┘
                     │ HTTPS (Authorization: Bearer JWT)      │
                     ▼                                         ▼
        ┌──────────────────────────────────────────────────────────┐
        │                 Spring Boot 3.2.5 (8080)                 │
        │  JwtAuthInterceptor → Spring Security 上下文桥接          │
        │  Controller（@PreAuthorize 权限码）→ Service → Mapper      │
        │  AOP 横切：@OperationLog / @DistributedLock / @Idempotent │
        └──────────────┬───────────────────────────────┬────────────┘
                       ▼                               ▼
              MySQL 8（单库 bbpms）              Redis 7（缓存/锁/ZSET）
                       │
              ApplicationEventPublisher（进程内事件总线）
              OrderAudited → 自动派单；InstallCompleted → 短信；OperationLog → 落库
```

## 2. 技术栈

| 层 | 选型 |
|---|---|
| 语言/框架 | Java 21 + Spring Boot 3.2.5 |
| 安全 | Spring Security 6 + JWT (jjwt 0.12.5) + BCrypt + SM4 (BouncyCastle) |
| 持久化 | MyBatis-Plus 3.5.7（分页/逻辑删除/乐观锁/自动填充） |
| 缓存/锁 | Redis 7 + Redisson 3.27.2 |
| 事件 | Spring `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` |
| 调度 | Spring `@Scheduled`（考勤自动签出、SLA 扫描、历史清理） |
| 文档 | Knife4j 4.5.0（OpenAPI 3）→ `http://localhost:8080/doc.html` |
| 前端 | Vue 3.4 + Vite 5 + TS 5；Element Plus / Vant 4 |

## 3. 模块划分（后端 `com.bbpms.*`）

| 模块 | 职责 |
|---|---|
| `common` | 公共底座：统一响应 `R<T>`、BaseDO、枚举、状态机、事件、锁、AOP 注解、安全、工具 |
| `interceptor` | JWT 鉴权拦截器 + 桥接 Spring Security |
| `auth` | 登录 / 刷新 / 登出 / 验证码 / RSA 密钥 |
| `user` | 用户 / 角色 / 菜单 / 部门 / 装维档案 / RBAC |
| `order` | 客户 / 订单 / 预约 / 订单时间线 |
| `workorder` | 工单 + 状态机 + SLA 调度 |
| `dispatch` | 自动/手动派单 + 4 因素评分算法 + 派单规则/记录 |
| `install` | 五步装机 + BSS（Mock） |
| `notify` | 短信/微信发送 + 模板 + 消息记录 |
| `log` | 操作 / 登录日志 |
| `file` | 附件上传（本地/MinIO） |
| `attendance` | 考勤（签到/休息/自动签出/清理） |
| `leave` | 请假（多级审批 + 升级规则） |

## 4. 核心流程

### 4.1 订单 → 工单主链路
```
客服建单 ──► 审核员审核 ──► OrderAuditedEvent(AFTER_COMMIT)
                                └─► 自动派单（评分 + Redisson 锁）──► 建工单(DISPATCHED)
装维接单 → 开工 → 到现场 → 装机信息/照片/签名 → 完工
    └─► workOrder COMPLETED ──► BSS activate ──► order FINISHED ──► 短信通知
```

### 4.2 状态机（自研，非 Spring Statemachine）
- `OrderStateMachine` / `WorkOrderStateMachine`：`Map<FromStatus, Map<Event, ToStatus>>` 转换表。
- 每次状态变更：乐观锁更新 + 同事务写 `order_audit_log` / `work_order_timeline`。
- 工单 10 状态：`PENDING/DISPATCHED/ACCEPTED/IN_PROGRESS/STALLED/REASSIGNING/COMPLETED/CANCELLED/AUTO_CANCELLED/FAILED`。

### 4.3 派单算法（核心 IP）
4 因素加权评分：**距离 40% / 负载 25% / 技能 20% / 评分 15%**（`bbpms.dispatch.weight-*` 可调）。
- 数据源：Haversine/高德距离、`installers:workload:{date}` ZSET、技能匹配、历史评分。
- 并发：Redisson `tryLock("lock:dispatch:{installerId}")` 覆盖整个写段。
- 过滤：排除请假装维；候选 + 评分序列化入 `dispatch_record.candidates_json` 审计回溯。

### 4.4 SLA 引擎（`WorkOrderSlaScheduler`）
| 扫描 | 规则（默认，可配 `bbpms.workorder.sla.*`） |
|---|---|
| 派单超时 | DISPATCHED 超 30min 未接 → AUTO_CANCELLED + 重派 |
| 进度超时 | IN_PROGRESS 超 4h 无心跳 → STALLED + 通知调度员 |
| 停滞恢复 | STALLED 超 24h 未恢复 → AUTO_CANCELLED + 重派 |

## 5. 横切机制

| 机制 | 实现 |
|---|---|
| 鉴权 | `JwtAuthInterceptor` 解析 JWT → 写自定义 ThreadLocal → **桥接 Spring Security 上下文**（权限码 + `ROLE_` 角色为 authorities）→ `@PreAuthorize` 生效 |
| 行级数据范围 | `DataScopeInnerInterceptor`（MyBatis 全局拦截器）按白名单语句注入 `create_by = ?`（SELF）；替代 XML 中不存在的 OGNL 引用 |
| 分布式锁 | `@DistributedLock(key=SpEL)` → Redisson |
| 幂等 | `@Idempotent(key=SpEL)` → Redis SETNX |
| 操作日志 | `@OperationLog` AOP → `OperationLogEvent` → 异步落库 |
| 事件总线 | `BbpmsEvents` 定义全部内部事件；跨域副作用一律发事件，不用 `@Async` 直调 |
| 乐观锁 | `BaseDO.version` + MyBatis-Plus `@Version`；所有更新 POJO 复制 `current.getVersion()` |

## 6. 安全设计

- 认证：RSA RS256 JWT；access 30min / refresh 7d **单次旋转**；`auth:revoked` 黑名单 + `auth:active` 白名单在 Redis。
- 授权：`@PreAuthorize("hasAuthority('xxx:yyy')")`，权限码来自 `sys_menu.perms`（随 JWT `perms` claim 下发）。
- 加密：身份证/手机号 SM4；密码 BCrypt。
- 数据范围：SELF（`create_by`）行级过滤。

## 7. 关键决策记录

| 决策 | 理由 |
|---|---|
| 11 微服务 → 模块化单体 | 降低部署/运维复杂度，保留业务域隔离；跨域用事件/本地事务替代 MQ/Seata |
| 自研状态机 | 避免引入 Spring Statemachine，转换表清晰可控 |
| 事件总线 | 进程内 `ApplicationEvent` 是 MQ 的内部等价物，未来可平滑替换 |
| 单库 `bbpms` | 全表一个 schema，事务边界简单 |
