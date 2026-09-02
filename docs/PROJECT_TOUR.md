# BBPMS 项目导览

> 一份写给"刚接手这个项目的人"的地图。读完你应该知道：
> - 整个系统**做什么**、**由哪些部分组成**
> - 每个目录、每个关键文件**管什么**
> - 一条业务订单**怎么从客户手里流转到装维完工**
> - 想加功能 / 改行为**从哪里下手**

如果你只想跑起来看效果，跳到 [§12 如何运行](#12-如何运行) 和 [§13 默认账号](#13-默认账号)。
其余章节按"读源码时遇到的疑问"反向组织，每一节都能独立查。

---

## 目录

1. [系统是什么](#1-系统是什么)
2. [技术栈与版本](#2-技术栈与版本)
3. [顶层目录结构](#3-顶层目录结构)
4. [业务全景图](#4-业务全景图)
5. [角色与数据范围](#5-角色与数据范围)
6. [后端模块详解](#6-后端模块详解)
   - 6.1 [common — 公共底座](#61-common--公共底座)
   - 6.2 [interceptor — JWT 拦截](#62-interceptor--jwt-拦截)
   - 6.3 [auth — 登录 / 注销 / 验证码](#63-auth--登录--注销--验证码)
   - 6.4 [user — RBAC 与装维档案](#64-user--rbac-与装维档案)
   - 6.5 [order — 订单生命周期](#65-order--订单生命周期)
   - 6.6 [workorder — 工单生命周期](#66-workorder--工单生命周期)
   - 6.7 [dispatch — 派单算法](#67-dispatch--派单算法)
   - 6.8 [install — 装机完工 + BSS 模拟](#68-install--装机完工--bss-模拟)
   - 6.9 [notify — 短信 / 微信 / 站内信](#69-notify--短信--微信--站内信)
   - 6.10 [log — 操作 / 登录日志](#610-log--操作--登录日志)
   - 6.11 [file — 文件存储](#611-file--文件存储)
7. [数据库表一览](#7-数据库表一览)
8. [横切机制](#8-横切机制)
9. [前端项目](#9-前端项目)
10. [REST API 速查表](#10-rest-api-速查表)
11. [关键业务流转](#11-关键业务流转)
12. [如何运行](#12-如何运行)
13. [默认账号](#13-默认账号)
14. [常见操作 FAQ](#14-常见操作-faq)
15. [如何扩展](#15-如何扩展)

---

## 1. 系统是什么

**BBPMS = Broadband Business Process Management System**，国内运营商宽带装维业务的全流程管理系统。

业务主链路：

```
客户申请 → 客服建单 → 审核员审核 → 自动派单 → 装维接单 → 上门装机 → 拍照 + 签名 → 完工归档 → 通知客户
```

技术定位：**模块化单体（Modular Monolith）**。一个 Spring Boot 应用 + 一个 MySQL + 一个 Redis，**不**是文档里规划过的 11 个微服务（详见 `docs/legacy/`）。

实现上的关键"妥协"（相比原微服务方案）：

| 原方案 | 现在 |
|---|---|
| RabbitMQ | Spring `ApplicationEventPublisher` |
| Seata AT/Saga | 本地 `@Transactional`（补偿逻辑手写） |
| Spring Cloud Gateway | Spring Interceptor |
| Nacos 配置中心 | `application.yml` + 环境变量 |
| OpenFeign | 直接 `@Autowired` 其他模块的 Service |
| MinIO（OSS） | 默认 local，可切 MinIO |

---

## 2. 技术栈与版本

### 后端
- **Java 21**（虚拟线程可用）
- **Spring Boot 3.2.5** + Spring Security 6 + Spring AOP
- **MyBatis Plus 3.5.7**（含分页、自动填充、逻辑删除）
- **Redisson 3.27.2**（分布式锁 + Redis ZSET）
- **jjwt 0.12.5**（JWT 签发 + 验签）
- **BouncyCastle**（SM4 国密 + 身份证/手机号加解密）
- **Knife4j 4.5.0**（OpenAPI 3 文档 → `http://localhost:8080/doc.html`）

### 前端
| 项目 | 技术 |
|---|---|
| `bbpms-admin-web` | Vue 3.4 + Vite 5 + TypeScript 5 + Element Plus 2.5 + Pinia 2 + ECharts 5 |
| `bbpms-installer-h5` | Vue 3.4 + Vite 5 + TypeScript 5 + Vant 4 + 高德地图 JS API + vue-signature-pad |

### 基础设施
- MySQL 8.0（单实例单库 `bbpms`）
- Redis 7（缓存 + 分布式锁 + 在线装维 ZSET）
- Docker Compose（只起 MySQL + Redis 两个容器）

---

## 3. 顶层目录结构

```
G:/2.Study/BBPMS/
├── CLAUDE.md                     # Claude Code 工作指南
├── README.md                     # 项目入口
├── docker-compose.yml            # MySQL + Redis
│
├── bbpms-parent/                 # Maven 父 POM
├── bbpms-app/                    # 唯一后端 Spring Boot 应用
├── bbpms-admin-web/              # PC 管理后台
├── bbpms-installer-h5/           # 装维移动端
│
├── middleware/
│   ├── mysql/init/               # 3 个 SQL（建库 / 建表 / 种子）
│   └── redis/redis.conf          # Redis 配置
│
├── docs/
│   ├── README.md                 # 文档导航
│   ├── PROJECT_TOUR.md           # ← 本文件
│   └── legacy/                   # 旧微服务方案文档（仅参考）
│
└── .gitignore
```

**后端内部**（`bbpms-app/src/main/java/com/bbpms/`）按业务域分包：

```
com.bbpms/
├── BbpmsApplication.java          # 启动类（@EnableAsync @EnableScheduling @EnableTransactionManagement @MapperScan）
├── common/                        # 公共底座（详见 §6.1）
├── interceptor/                   # JWT 拦截器（详见 §6.2）
├── auth/                          # 登录/注销/验证码（详见 §6.3）
├── user/                          # RBAC + 装维档案（详见 §6.4）
├── order/                         # 订单（详见 §6.5）
├── workorder/                     # 工单（详见 §6.6）
├── dispatch/                      # 派单（详见 §6.7）
├── install/                       # 安装（详见 §6.8）
├── notify/                        # 通知（详见 §6.9）
├── log/                           # 日志（详见 §6.10）
└── file/                          # 文件（详见 §6.11）
```

每个业务域内部统一结构：`controller / service(/impl) / mapper / entity / dto / vo / config`。

---

## 4. 业务全景图

```
┌────────────────────────────────────────────────────────────────────────┐
│  PC 管理后台 (bbpms-admin-web)        装维 H5 (bbpms-installer-h5)       │
│  - 6 角色: 管理员/客服/审核/调度/装维/客户                              │
└─────────────┬──────────────────────────────────────────┬───────────────┘
              │ HTTPS                                     │ HTTPS
              ▼                                           ▼
┌────────────────────────────────────────────────────────────────────────┐
│  Spring Interceptor  (JwtAuthInterceptor)                              │
│  - 解析 JWT → 注入 SecurityUser（userId / roles / dataScope）            │
│  - @PreAuthorize 校验权限码                                             │
└─────────────┬───────────────────────────────────────────┬───────────────┘
              │                                           │
              ▼                                           ▼
┌────────────────────────────────────────────────────────────────────────┐
│  REST API（按 Controller 分域）                                          │
│  /auth/*  /api/users/*  /api/orders/*  /api/work-orders/*               │
│  /api/dispatch/*  /api/install/*  /api/notify/*  /api/files/*  ...      │
└─────────────┬───────────────────────────────────────────┬───────────────┘
              │                                           │
              ▼                                           ▼
┌────────────────────────────────────────────────────────────────────────┐
│  Service 层（按业务域，AOP 横切）                                         │
│  - OrderService / WorkOrderService / DispatchService / InstallService    │
│  - 状态机校验 + 业务编排 + 事件发布 + 分布式锁                            │
└─────────────┬───────────────────────────────────────────┬───────────────┘
              │                                           │
              ▼                                           ▼
┌──────────────────────────────┐    ┌───────────────────────────────┐
│  MyBatis Plus Mapper → MySQL │    │  Redis (Redisson 客户端)       │
│  单库 `bbpms` 全部表          │    │  - 缓存 / 锁 / 在线装维 ZSET    │
└──────────────────────────────┘    └───────────────────────────────┘

      ApplicationEventPublisher（进程内事件总线，替代 RabbitMQ）
      - OrderCreatedEvent    → 日志模块记录
      - OrderAuditedEvent    → Dispatch 模块触发自动派单
      - WorkOrderDispatched  → 通知模块发短信
      - InstallCompleted     → 通知模块发完工短信
      - OperationLogEvent    → log 模块异步落库
```

---

## 5. 角色与数据范围

`sys_role.data_scope` 决定这个角色查询业务数据时能看多少行（在 `@DataScope` AOP 拦截器里强制注入 SQL WHERE）：

| code | name | data_scope | 含义 |
|---|---|---|---|
| `SUPER_ADMIN` | 超级管理员 | 1 = ALL | 所有数据 |
| `CUSTOMER_SERVICE` | 客服 | 4 = SELF | 自己创建的订单 |
| `AUDITOR` | 审核员 | 4 = SELF | 自己审核的 |
| `DISPATCHER` | 调度员 | 4 = SELF | 自己派的单 |
| `INSTALLER` | 装维工程师 | 4 = SELF | 自己接的工单 |
| `CUSTOMER` | 客户 | 4 = SELF | 自己的订单 |

具体账号见 [§13](#13-默认账号)。

---

## 6. 后端模块详解

### 6.1 common — 公共底座

整个项目复用率最高的部分，被其他域大量引用。

| 子包 / 文件 | 作用 |
|---|---|
| `result/R.java` | 统一响应 `{code, msg, data, traceId}` |
| `result/PageResp.java` | 分页响应（与 MyBatis-Plus `IPage` 互转） |
| `entity/BaseDO.java` | 所有 entity 的基类：`id` / `create_time` / `update_time` / `create_by` / `update_by` / `deleted` / `version`（前 4 个由 `AutoFillHandler` 自动填） |
| `entity/BaseDTO.java` | 请求 DTO 基类（带 `pageNum` / `pageSize`） |
| `exception/BizException.java` | 业务异常（带 `ResultCode`） |
| `exception/OptimisticLockException.java` | 乐观锁冲突 |
| `enums/OrderStatus.java` `OrderEvent.java` `WorkOrderStatus.java` `InstallStatus.java` `MessageChannel.java` `UserType.java` `ResultCode.java` | 状态/事件/角色/错误码枚举 |
| `util/SnowflakeIdGenerator.java` | Twitter Snowflake ID 生成器（12 位 seq + 5 worker + 5 dc） |
| `util/JwtUtils.java` `CryptoUtils.java` `RedisUtils.java` `JsonUtils.java` `IpUtils.java` `AssertUtils.java` `SecurityUtils.java` | 工具集 |
| `security/SecurityContextHolder.java` `SecurityUser.java` | 从 Interceptor 注入的当前用户信息 |
| `lock/DistributedLock.java` | 锁接口（基于 Redisson） |
| `lock/RedissonDistributedLock.java` | Redisson 实现 |
| `statemachine/OrderStateMachine.java` | 订单状态机（见 §6.5） |
| `statemachine/WorkOrderStateMachine.java` | 工单状态机（见 §6.6） |
| `statemachine/OrderTransition.java` | 转换表数据类（from / event / to / 允许角色） |
| `annotation/DistributedLock.java` | `@DistributedLock(key=SpEL)` — Redisson 锁 |
| `annotation/Idempotent.java` | `@Idempotent(key=...)` — 幂等防重 |
| `annotation/OperationLog.java` | `@OperationLog(value, module)` — 异步操作日志 |
| `annotation/DataScope.java` | `@DataScope` — 数据范围 SQL 注入 |
| `aspect/DistributedLockAspect.java` `IdempotentAspect.java` `OperationLogAspect.java` `DataScopeAspect.java` | 4 个 AOP 拦截器 |
| `event/BbpmsEvents.java` | **所有内部事件类的容器**：`OrderCreatedEvent` / `OrderAuditedEvent` / `OrderCancelledEvent` / `WorkOrderDispatchedEvent` / `WorkOrderAcceptedEvent` / `WorkOrderCompletedEvent` / `WorkOrderTransferEvent` / `WorkOrderDispatchFailedEvent` / `InstallCompletedEvent` / `NotifyEvent` / `OperationLogEvent` / `LoginLogEvent` |
| `config/MybatisPlusConfig.java` | MyBatis-Plus 配置（注册 `DataScopeInnerInterceptor`） |
| `config/AutoFillHandler.java` | `MetaObjectHandler` 实现，自动填 `create_time` / `create_by` 等 |
| `config/DataScopeInnerInterceptor.java` | MyBatis 拦截器：读 `DataScope` 注解并改写 SQL |
| `config/RedissonConfig.java` | Redisson 客户端 Bean |
| `config/JacksonConfig.java` | Jackson 全局配置（long → string 防精度丢失等） |
| `config/CorsConfig.java` | 跨域 |
| `config/OpenApiConfig.java` | Knife4j / OpenAPI 3 |
| `config/GlobalExceptionHandler.java` | `@RestControllerAdvice` 全局异常 → `R.fail()` |
| `config/TraceIdFilter.java` | 生成 MDC traceId |
| `config/WebMvcConfig.java` | 静态资源映射 |

### 6.2 interceptor — JWT 拦截

| 文件 | 作用 |
|---|---|
| `interceptor/JwtAuthInterceptor.java` | 解析 `Authorization: Bearer <jwt>` → 校验签名 + 黑名单 → 注入 `SecurityUser` 到 `ThreadLocal`。**所有 `/api/**` 都过这里**（白名单在 Controller 路径如 `/auth/*`） |
| `interceptor/WebMvcConfig.java` | 把拦截器注册进 Spring MVC |

### 6.3 auth — 登录 / 注销 / 验证码

**`/auth/*`（不需要 token）**

| 文件 | 作用 |
|---|---|
| `controller/AuthController.java` | `POST /auth/login` / `POST /auth/refresh` / `POST /auth/logout` / `GET /auth/captcha` / `GET /auth/public-key` |
| `service/AuthService.java` + `impl/AuthServiceImpl.java` | 登录全流程：验证码 → 用户名校验 → **RSA 解密前端加密的密码** → BCrypt 校验 → 写登录日志 → 颁发 access+refresh token |
| `service/CaptchaService.java` + `impl/CaptchaServiceImpl.java` | 图形验证码（5 分钟 TTL，Redis 存 key→code） |
| `service/RsaKeyService.java` + `impl/RsaKeyServiceImpl.java` | RSA 密钥对（开发期内存生成；生产应从配置读） |
| `service/TokenService.java` + `impl/TokenServiceImpl.java` | 颁发 / 解析 / 撤销 access + refresh token（**refresh 单次使用**，用过即失效） |
| `dto/LoginReq.java` `LoginRespVO.java` `CaptchaRespVO.java` | 入参出参 |
| `config/AuthProperties.java` | `bbpms.jwt.*` 配置绑定 |

**登录流程细节**：
1. 前端先用 RSA 公钥加密密码
2. `POST /auth/login` 携带 `username` + `password(密文)` + `captchaKey/Code`
3. 后端用私钥解密 → BCrypt 比对
4. `TokenService.issue()` 生成 access(30 min) + refresh(7 d)，把 `jti` 写入 `auth:active:{jti}` 和 `auth:refresh:{jti}`
5. 前端把 access 放到 `Authorization` 头，refresh 存本地
6. **refresh 单次旋转**：`POST /auth/refresh?refreshToken=...` 验证后删旧 jti，颁新一对

### 6.4 user — RBAC 与装维档案

| 文件 | 作用 |
|---|---|
| `controller/SysUserController.java` | `/api/users/*` — 用户 CRUD + 分配角色 + 改密 |
| `controller/SysRoleController.java` | `/api/roles/*` — 角色 CRUD |
| `controller/SysMenuController.java` | `/api/menus/*` — 菜单 CRUD（菜单即权限点） |
| `controller/SysDeptController.java` | `/api/depts/*` — 部门 CRUD |
| `controller/InstallerController.java` | `/api/installers/*` — 装维列表 / 地图 / 心跳上报 / 位置更新 |
| `service/impl/SysUserServiceImpl.java` | 用户相关：BCrypt 密码、登录日志、用户认证信息 DTO |
| `service/impl/SysRoleServiceImpl.java` | 角色增删 + 绑菜单 |
| `service/impl/SysMenuServiceImpl.java` | 菜单 + 权限码 |
| `service/impl/SysDeptServiceImpl.java` | 部门树（materialized path 实现） |
| `service/impl/InstallerProfileServiceImpl.java` | 装维档案：技能、服务区、当前位置、工作量、评分；`getOnline()` 返回 onDuty=1 |
| `service/impl/RbacServiceImpl.java` | `buildMenuTree()` 把扁平菜单构建为树；`getCurrentDataScope()` 查当前用户数据范围 |
| `entity/SysUser.java` `SysRole.java` `SysMenu.java` `SysUserRole.java` `SysRoleMenu.java` `SysDept.java` `InstallerProfile.java` | 7 张表对应的 entity |
| `dto/*` `vo/*` `mapper/*` | DTO / VO / MyBatis Mapper |

**权限模型**：

```
sys_user ──N:M── sys_user_role ──N:M── sys_role ──N:M── sys_role_menu ──N:M── sys_menu
                                                                                  │
                                                                                  └── perms: "order:create"
```

菜单 `type`：
- `1 = DIR`：目录（无页面）
- `2 = MENU`：菜单（页面）
- `3 = BUTTON`：按钮（仅 `perms` 字段）

### 6.5 order — 订单生命周期

| 文件 | 作用 |
|---|---|
| `controller/OrderController.java` | `/api/orders/*` — 创建 / 详情 / 分页 / 审核 / 取消 / 时间线 / 改预约 |
| `controller/CustomerController.java` | `/api/customers/*` — 增改查 + `/{id}/unmasked`（仅超管可看明文） |
| `controller/AppointmentController.java` | `/api/appointments/*` — 预约管理 |
| `service/OrderService.java` + `impl/OrderServiceImpl.java` | 订单核心：`create` / `audit` / `cancel` / `getDetail` / `page` / `updateStatus`（内部 API）/ `snapshot`（被工单模块调用拿地址/电话） |
| `service/CustomerService.java` + `impl/CustomerServiceImpl.java` | 客户 upsert + 脱敏（SM4 解密 → `maskPhone` / `maskIdCard`） |
| `service/AppointmentService.java` + `impl/AppointmentServiceImpl.java` | 预约 CRUD |
| `service/OrderTimelineService.java` + `impl/OrderTimelineServiceImpl.java` | 时间线读取（写由 `OrderServiceImpl.appendAuditLog()` 完成） |
| `entity/BroadbandOrder.java` `Customer.java` `Appointment.java` `OrderAuditLog.java` | 4 张表 |
| `dto/OrderCreateReq.java` `OrderAuditReq.java` `OrderCancelReq.java` `OrderQueryReq.java` `OrderDetailVO.java` `OrderTimelineVO.java` | 订单 DTO |
| `config/OrderProperties.java` `config/SecurityConfig.java` | 配置（SM4 key） + Security 放行配置 |

**订单状态机**（`OrderStateMachine`）：

```
CREATED ─audit_pass──▶ AUDITED ─start_dispatch──▶ WAIT_DISPATCH ─dispatch_ok──▶ DISPATCHED
   │                       │                          │                          │
   │ audit_reject /cancel  │ cancel                   │ dispatch_timeout         │ accept
   ▼                       ▼                          ▼                          ▼
CANCELLED              CANCELLED                  CANCELLED                  INSTALLING
                                                                                │
                                                                                │ complete
                                                                                ▼
                                              FINISHED ─confirm──▶ CLOSED（终态）
                                              CANCELLED（终态）
```

每次状态变化都过 `OrderStateMachine.assertTransition()` 校验：合法转换 + 角色匹配；并写一行 `order_audit_log`（同事务）。

**事件**（`BbpmsEvents`）：
- `OrderCreatedEvent` → 暂未订阅（可挂通知）
- `OrderAuditedEvent` → **`OrderAuditedListener` 自动派单**
- `OrderCancelledEvent` → 暂未订阅

### 6.6 workorder — 工单生命周期

| 文件 | 作用 |
|---|---|
| `controller/WorkOrderController.java` | `/api/work-orders/*` — 创建 / 详情 / 分页 / 接单 / 开工 / 完工 / 转单 / 取消 / 改状态（管理员） |
| `controller/WorkOrderTimelineController.java` | `/api/work-orders/{id}/timeline`（备用） |
| `service/WorkOrderService.java` + `impl/WorkOrderServiceImpl.java` | 工单核心；**`create` 会被派单模块和 OrderAuditedListener 两种入口调用**（用 `selectByOrderId` 做幂等） |
| `service/WorkOrderTimelineService.java` + `impl/WorkOrderTimelineServiceImpl.java` | 工单时间线 |
| `entity/WorkOrder.java` `WorkOrderTimeline.java` | 2 张表 |
| `dto/WorkOrderCreateReq.java` 等 | DTO |

**工单状态机**（`WorkOrderStateMachine`）：

```
PENDING ──(派单)──▶ DISPATCHED ─accept──▶ ACCEPTED ─start──▶ IN_PROGRESS ─complete──▶ COMPLETED
                       │                                       │                       │
                       │ transfer                              │ fail (≤3次)            ▼
                       ▼                                       ▼                    FAILED
                  (回 PENDING)                              FAILED
                       │                                       │
                       ▼                                       ▼
                (再次被派)                                 (重派)
```

终态：`COMPLETED` / `CANCELLED` / `FAILED`。

### 6.7 dispatch — 派单算法

**核心 IP**。详细的算法和分数权重见 [`CLAUDE.md`](../CLAUDE.md) 里的"派单算法"段。

| 文件 | 作用 |
|---|---|
| `controller/DispatchController.java` | `/api/dispatch/auto`（系统）/ `manual` / `/{id}/reassign` / `candidates?orderId=` / `records/page` / `stats` |
| `controller/DispatchRuleController.java` | `/api/dispatch/rule/*` — 派单规则 CRUD（数据库里只有 `default` 一条） |
| `service/DispatchService.java` + `impl/DispatchServiceImpl.java` | 编排：**auto / manual / reassign** 三个写路径都包 `@Transactional` + Redisson 锁 |
| `service/DispatchRuleService.java` + `impl/DispatchRuleServiceImpl.java` | 派单规则读 |
| `algorithm/DispatchScoringService.java` | 4 因素加权评分：距离(40%) / 负载(25%) / 技能(20%) / 评分(15%)；同分时按 `workload` / `rating` / `installerId` tie-break |
| `config/DispatchProperties.java` | 派单相关阈值：权重、半径、最大并发、锁等待/租约、改派冷却 |
| `event/OrderAuditedListener.java` | 监听 `OrderAuditedEvent` → 自动调 `dispatchService.autoDispatch(orderId)` |
| `event/WorkOrderTransferListener.java` | 监听转单事件 |
| `dto/CandidateDTO.java` `InstallerDTO.java` `OrderDTO.java` `ManualDispatchReq.java` `ReassignReq.java` `DispatchResultDTO.java` | 评分算法入参出参 |
| `entity/DispatchRecord.java` `DispatchRule.java` | 派单记录 + 规则 |
| `mapper/DispatchRecordMapper.java` | `selectPageWithScope` 应用 `@DataScope`，`selectRecent(days)` 查统计 |

**Redis 数据结构**：
- `installers:active` ZSET — `score=last_heartbeat_ms`，`member=installerId`（自动派单读这个找最近 5 分钟内有心跳的装维）
- `dispatch:cooldown:{workOrderId}` STRING — 改派后冷却 30 min（防频繁改派）

### 6.8 install — 装机完工 + BSS 模拟

| 文件 | 作用 |
|---|---|
| `controller/InstallController.java` | `/api/install/{workOrderId}/arrive` / `info` / `photos` / `signature` / `complete` / `by-work-order/{id}` / `page` / `my` / `progress/{id}` |
| `service/InstallService.java` + `impl/InstallServiceImpl.java` | 5 步装机流程，**`complete` 替代了原 Saga** |
| `bss/BssClient.java` | BSS（运营商开通系统）的客户端；默认 `MockBssClient` 实现 |
| `config/InstallProperties.java` | GPS 阈值 / 最少照片数 / 光衰告警值 |
| `event/InstallEventListener.java` | 监听订单/工单事件做统计 |
| `event/InstallNotifyListener.java` | 监听安装完成事件转发通知 |
| `entity/InstallRecord.java` | 装机记录（含 photos JSON、signature URL、GPS、ONU MAC/SN） |
| `dto/InstallArriveReq.java` 等 | 5 步对应的请求体 |

**装机 5 步**（H5 端走流程）：

```
1. 到达现场  →  POST /api/install/{woId}/arrive     （GPS 上报 + 写 start_lat/lng）
2. 装机信息  →  POST /api/install/{woId}/info       （ONU MAC / SN / OLT port / 光衰）
3. 现场照片  →  POST /api/install/{woId}/photos     （多次调用，累计到 JSON 数组）
4. 电子签名  →  POST /api/install/{woId}/signature  （base64 / objectKey 均可）
5. 完工提交  →  POST /api/install/{woId}/complete   （GPS + 关联 orderId）
```

**`complete` 做的事**（`InstallServiceImpl.complete()` 注释自己写明是 "saga replacement"）：

```
1. GPS 距离软校验（超阈值仅 warn，不阻断）
2. 校验照片数 ≥ photos-min-count（默认 3，否则报错）
3. 光衰软校验（< -27dBm 仅 warn）
4. 持久化完工字段
5. workOrderService.complete() → 工单 → COMPLETED
6. bssClient.activate(orderId)        ← 失败则回滚工单到 IN_PROGRESS 抛异常
7. orderService.markFinished(orderId) → 订单 → FINISHED
8. 发 InstallCompletedEvent
9. 发 NotifyEvent（SMS，模板 INSTALL_COMPLETED_SMS）
```

**BSS Mock**（`application.yml`）：
```yaml
bbpms:
  bss:
    mock-enabled: true          # prod 应为 false
    failure-rate: 0.05          # 5% 随机失败，用于压测补偿
    min-delay-ms: 200
    max-delay-ms: 2000
```

### 6.9 notify — 短信 / 微信 / 站内信

| 文件 | 作用 |
|---|---|
| `controller/NotifyController.java` | `/api/notify/sms` / `wechat/template` / `messages/page` |
| `controller/MessageTemplateController.java` | `/api/notify/templates/*` — 模板 CRUD |
| `service/NotifyService.java` + `impl/NotifyServiceImpl.java` | 发送编排 + 落 `message` 表 |
| `service/MessageTemplateService.java` + `impl/MessageTemplateServiceImpl.java` | 模板管理 |
| `service/sender/SmsSender.java` `WechatSender.java` `SendResult.java` | 抽象接口 |
| `service/sender/MockSmsSender.java` `MockWechatSender.java` | 默认 mock 实现（dev 用） |
| `event/NotifyEventListener.java` | 监听 `NotifyEvent`（所有域要发通知就发这个事件） |
| `entity/Message.java` `MessageTemplate.java` | 2 张表 |
| `dto/SmsSendReq.java` `WechatTemplateSendReq.java` `MessagePageReq.java` | 请求体 |

**用法**：业务代码里
```java
applicationEventPublisher.publishEvent(new BbpmsEvents.NotifyEvent(
    "SMS", phone, null, userId, "ORDER_CREATED_SMS",
    Map.of("orderNo", orderNo, "customerName", name)
));
```
`NotifyEventListener` 会查模板 → 替换 `${orderNo}` 等占位符 → 调 `SmsSender.send()` → 落 `message` 表。

### 6.10 log — 操作 / 登录日志

| 文件 | 作用 |
|---|---|
| `controller/OperationLogController.java` | `/api/logs/operation/page` |
| `controller/LoginLogController.java` | `/api/logs/login/page` |
| `service/OperationLogService.java` + `impl/OperationLogServiceImpl.java` | 查操作日志 |
| `service/LoginLogService.java` + `impl/LoginLogServiceImpl.java` | 查登录日志 |
| `event/OperationLogListener.java` | 监听 `OperationLogEvent` 异步落 `operation_log` |
| `event/LoginLogListener.java` | 监听 `LoginLogEvent` 异步落 `login_log` |
| `entity/OperationLog.java` `LoginLog.java` | 2 张表 |

**触发方式**：
- **操作日志**：方法上打 `@OperationLog(value="...", module="...")` → `OperationLogAspect` AOP 在方法返回时发 `OperationLogEvent` → listener 异步落库
- **登录日志**：`AuthServiceImpl.login()` 手动构造 `LoginLogEvent` 发出去

### 6.11 file — 文件存储

| 文件 | 作用 |
|---|---|
| `controller/FileController.java` | `/api/files/upload` / `/{id}/presign` / `page` / `by-biz` / `/{id}` (DELETE) |
| `service/FileService.java` + `impl/FileServiceImpl.java` | 上传（local 或 MinIO）+ 预签名 URL + 元数据落 `attachment` 表 |
| `entity/Attachment.java`（在 `bbpms-app/.../file/entity`）| 元数据：objectKey、bucket、bizType（ORDER/INSTALL/CUSTOMER/OTHER）、bizId、uploader |
| `dto/FileUploadResp.java` `PresignedUrlResp.java` `FilePageReq.java` | DTO |

**存储切换**：`application.yml` 里 `bbpms.file.storage-type`：
- `local`（默认）：写到 `./uploads/`，`/files/*` 静态映射提供访问
- `minio`：用 `io.minio` SDK 写到 MinIO，可调 `getPresignedUrl`

---

## 7. 数据库表一览

**所有表都在单库 `bbpms` 里**（init 脚本 `01-bbpms-schema.sql`）。所有 entity 继承 `BaseDO`，自动有 `id` / `create_time` / `update_time` / `create_by` / `update_by` / `deleted` / `version` 七列。

| 表 | 所属模块 | 关键字段 | 索引 |
|---|---|---|---|
| `sys_dept` | user | name / path（materialized） / sort | parent_id, status |
| `sys_user` | user | username / password(bcrypt) / phone / user_type / status | uk_username, uk_phone, idx_dept_status |
| `sys_role` | user | code / data_scope（1~5） | uk_code |
| `sys_menu` | user | parent_id / type(1=DIR 2=MENU 3=BUTTON) / perms / path / component | idx_perms |
| `sys_user_role` | user | user_id / role_id | PK 组合 |
| `sys_role_menu` | user | role_id / menu_id | PK 组合 |
| `installer_profile` | user | user_id(PK) / skill_tags(JSON) / service_area(JSON) / current_lat/lng / on_duty / workload / level / score | — |
| `customer` | order | name / phone(SM4) / id_card_no(SM4) / address / lat / lng / grid_code | idx_phone, idx_id_card_no, idx_grid |
| `broadband_order` | order | order_no / customer_id / package_code / status / cs_id / auditor_id / dispatch_time / completed_time / cancelled_time | uk_order_no, idx_status_create_time, idx_auditor_id |
| `appointment` | order | order_id / appointment_time / contact_phone / confirmed | idx_order_id |
| `order_audit_log` | order | order_id / from_status / to_status / auditor_id / remark | idx_order_create_time |
| `work_order` | workorder | work_no / order_id / installer_id / status(PENDING/DISPATCHED/ACCEPTED/IN_PROGRESS/COMPLETED/CANCELLED/FAILED) / dispatch_time / accept_time / start_time / finish_time | uk_work_no, idx_installer_status, idx_status |
| `work_order_timeline` | workorder | work_order_id / from_status / to_status / operator_id / operator_role / remark | idx_work_order_create_time |
| `dispatch_record` | dispatch | work_order_id / installer_id / strategy(AUTO/MANUAL/REASSIGN) / score / candidates_json / reason | idx_work_order_id, idx_installer_id, idx_strategy |
| `dispatch_rule` | dispatch | name / weight_distance/load/skill/rating / radius_km / enabled | uk_name |
| `install_record` | install | work_order_id(UK) / installer_id / onu_mac / onu_sn / olt_port / signal_db / start_lat/lng / complete_lat/lng / photos(JSON) / signature_url / status | uk_work_order_id, idx_installer_id, idx_status |
| `attachment` | file | object_key / bucket / original_name / size / biz_type / biz_id / uploader_id | uk_object_key, idx_biz |
| `message` | notify | user_id / channel(SMS/WECHAT/INAPP) / template_code / params(JSON) / content / status(PENDING/SUCCESS/FAILED) / error_msg | idx_user_create, idx_status |
| `message_template` | notify | code / channel / subject / content / aliyun_template_id / wechat_template_id / enabled | uk_code |
| `operation_log` | log | user_id / module / action / request_uri / method / params / result / ip / cost_ms / status / error | idx_user_time, idx_module |
| `login_log` | log | user_id / username / ip / user_agent / status(1=成功 0=失败) / message | idx_user_time |

---

## 8. 横切机制

### 8.1 鉴权流程

```
HTTP Request
   │
   ▼
JwtAuthInterceptor.preHandle()
   │  读 Authorization: Bearer <jwt>
   │  调 tokenService.parse() 校验签名 + 过期
   │  查 Redis auth:revoked 黑名单
   │  解析出 userId / roles / dataScope
   │  注入 SecurityUser → ThreadLocal
   ▼
Spring Security Filter（SecurityConfig 配的）
   │  静态路径放行（/auth/*, /doc.html, /static/*）
   │  其它走 SecurityContext（从 Interceptor 同步过来）
   ▼
@PreAuthorize("hasAuthority('order:create')")  ←  方法级权限
   │
   ▼
@OperationLog(...)  ← 异步操作日志
   │
   ▼
业务方法
```

### 8.2 数据范围 SQL 注入

带 `@DataScope` 注解的 Mapper 方法，`DataScopeInnerInterceptor` 会：

1. 从 `SecurityContextHolder` 拿当前用户
2. 查 `sys_role.data_scope`
3. 根据 `data_scope` 改写 SQL 的 WHERE：

| data_scope | 加什么条件 |
|---|---|
| 1 = ALL | 不加 |
| 2 = DEPT | `dept_id = ?` |
| 3 = DEPT_AND_CHILD | `dept_id IN (本部门 + 子部门)` |
| 4 = SELF | `create_by = ?` |
| 5 = CUSTOM | `dept_id IN (sys_role 自定义部门列表)` |

**用法**：
```java
@Mapper
public interface OrderMapper extends BaseMapper<BroadbandOrder> {
    @DataScope
    IPage<BroadbandOrder> selectPageWithScope(Page<BroadbandOrder> page, @Param("ew") Wrapper<?> w);
}
```

### 8.3 分布式锁

```java
@DistributedLock(key = "'dispatch:installer:' + #installerId", waitTime = 5, leaseTime = 30)
public DispatchResult autoDispatch(...) { ... }
```
底层 Redisson `tryLock`，key 支持 SpEL。

### 8.4 幂等

```java
@Idempotent(key = "'order:create:' + #req.customerId", expireSeconds = 60)
public Long create(...) { ... }
```
实现：Redis `SETNX` 兜底 + 数据库唯一索引（`broadband_order.uk_order_no` 等）。

### 8.5 操作日志

```java
@OperationLog(value = "审核订单", module = "订单")
public void audit(...) { ... }
```
AOP 拦截 → 记录 userId / uri / method / params / cost_ms / status / error → 发 `OperationLogEvent` → 异步落 `operation_log`。

---

## 9. 前端项目

### 9.1 bbpms-admin-web（管理后台）

技术：Vue 3 + Vite 5 + TS 5 + Element Plus 2.5 + Pinia 2 + Axios + ECharts

```
src/
├── main.ts                # 入口：注册 Pinia / Router / ElementPlus / i18n
├── App.vue
├── api/                   # axios 调用（按域分文件）
│   ├── http.ts            # Axios 实例 + 请求/响应拦截器（自动加 JWT）
│   ├── auth.ts            # /auth/* 
│   ├── user.ts role.ts menu.ts dept.ts installer.ts
│   ├── order.ts workorder.ts dispatch.ts install.ts
│   ├── notify.ts file.ts log.ts customer.ts
│   └── dashboard.ts monitor.ts
├── stores/                # Pinia
│   ├── auth.ts            # token / userInfo / permissions / hasPermission(p)
│   ├── user.ts order.ts workorder.ts dispatch.ts app.ts
├── router/index.ts        # 路由 + 路由守卫（roles / permission 校验）
├── layouts/
│   ├── AdminLayout.vue    # 整体框架（顶栏 + 侧边栏 + 标签）
│   └── components/SidebarItem.vue
├── components/            # 通用组件
│   ├── BBPMSTable.vue     # 标准分页表格
│   ├── BBPMSForm.vue      # 标准表单
│   ├── BBPMSUpload.vue    # 文件上传
│   ├── BBPMSMapPicker.vue # 高德地图选址
│   ├── BBPMSStatusTag.vue # 状态彩色标签
│   ├── OrderTimeline.vue  # 订单时间线
│   ├── PermissionButton.vue # 带 v-permission 的按钮
│   └── PageHeader.vue
├── views/                 # 页面（按路由对应）
│   ├── login/index.vue
│   ├── dashboard/index.vue
│   ├── customer/{list,detail}.vue
│   ├── order/{list,create,detail,audit}.vue
│   ├── workorder/{list,dispatch-board,detail}.vue
│   ├── installer/{list,map,profile}.vue
│   ├── user/list.vue  role/list.vue  menu/list.vue  dept/list.vue
│   ├── notify/{template,record}.vue
│   ├── log/{operation,login}.vue
│   ├── monitor/{metrics,health}.vue
│   ├── file/index.vue
│   ├── profile/index.vue
│   └── error/{403,404,500}.vue
├── directives/permission.ts  # v-permission 指令
├── locales/{zh-CN,en-US}.ts # i18n
├── types/                 # 类型定义
└── utils/                 # auth.ts（token 存取）/ request.ts（已合并到 api/http）/ format.ts / permission.ts / dict.ts
```

**路由守卫**（`router/index.ts`）：
1. 白名单 `/login /403 /404` 直接放行
2. 没 token → 跳 `/login`
3. 有 token 但 `userInfo` 空 → 调 `auth.fetchUserInfo()` 拉一次
4. 路由 meta `roles` 检查
5. 路由 meta `permission` 检查（`hasPermission` 来自 Pinia）

### 9.2 bbpms-installer-h5（装维移动端）

技术：Vue 3 + Vite 5 + TS 5 + Vant 4 + 高德地图 JS API + `vue-signature-pad`

```
src/
├── main.ts
├── App.vue
├── api/{http,auth,install,workorder,file}.ts
├── stores/{auth,workorder}.ts
├── router/index.ts        # hash 模式（兼容 H5 / 小程序场景）
├── types/{auth,workorder,install}.ts
├── utils/{format,geo}.ts
├── components/SignaturePad.vue  # 电子签名 canvas
├── views/
│   ├── login/index.vue
│   ├── workorders/
│   │   ├── list.vue       # 我的工单（tab：待接单 / 进行中 / 已完成）
│   │   ├── detail.vue     # 工单详情 + 地图导航
│   │   └── install.vue    # 5 步装机流程
│   ├── profile/index.vue  # 我的统计 + 上下班
│   └── not-found.vue
└── assets/styles/index.scss
```

**5 步装机流程**（`workorders/install.vue`，对应后端 5 个接口）：

```
1. 到达现场 → 调 AMap 定位 → POST /arrive
2. 装机信息 → 填表单（ONU MAC/SN/OLT/光衰） → POST /info
3. 现场照片 → 调摄像头 / 相册 → 多次 POST /photos
4. 电子签名 → vue-signature-pad → POST /signature
5. 完工提交 → 提交 GPS + 汇总 → POST /complete
```

---

## 10. REST API 速查表

完整 OpenAPI 见 `http://localhost:8080/doc.html`。下面是按场景组织的速查表。

### 认证（公开）
```
POST   /auth/login                    { username, password(RSA密文), captchaKey, captchaCode } → { accessToken, refreshToken, userInfo }
POST   /auth/refresh                  ?refreshToken=...     → { accessToken, refreshToken }
GET    /auth/captcha                  → { key, image(base64) }
GET    /auth/public-key               → RSA 公钥
POST   /auth/logout                   (需 token)            → 撤销 jti
```

### 客户
```
POST   /api/customers                 新建/upsert
GET    /api/customers/{id}            详情（脱敏）
GET    /api/customers/{id}/unmasked   明文（仅超管）
GET    /api/customers/page            分页
GET    /api/customers/by-phone/{p}    按手机号查 id
```

### 订单
```
POST   /api/orders                    创建              [order:create]
GET    /api/orders/{id}               详情 + 时间线
GET    /api/orders/page               分页
GET    /api/orders/by-no/{orderNo}    按订单号
POST   /api/orders/{id}/audit         审核              [order:audit]
POST   /api/orders/{id}/cancel        取消              [order:cancel]
PUT    /api/orders/{id}/appointment   改预约
GET    /api/orders/{id}/timeline      时间线
```

### 工单
```
POST   /api/work-orders               系统内部建单（派单调用）
GET    /api/work-orders/{id}          详情
GET    /api/work-orders/page          分页
GET    /api/work-orders/by-order/{oid}
GET    /api/work-orders/my            当前装维的工单队列
POST   /api/work-orders/{id}/accept   接单
POST   /api/work-orders/{id}/start    开始施工
POST   /api/work-orders/{id}/complete 完工
POST   /api/work-orders/{id}/transfer 转单
POST   /api/work-orders/{id}/cancel   取消
PUT    /api/work-orders/{id}/status   管理员改状态
```

### 派单
```
POST   /api/dispatch/auto             系统触发（OrderAuditedListener）
POST   /api/dispatch/manual           手动派单           [dispatch:manual]
POST   /api/dispatch/{id}/reassign    改派               [dispatch:reassign]
GET    /api/dispatch/candidates?orderId=  看候选评分
GET    /api/dispatch/records/page     派单记录分页
GET    /api/dispatch/stats?days=7     统计
```

### 安装
```
POST   /api/install/{woId}/arrive     到达现场
POST   /api/install/{woId}/info       装机信息
POST   /api/install/{woId}/photos     现场照片（多次）
POST   /api/install/{woId}/signature  签名
POST   /api/install/{woId}/complete   完工
GET    /api/install/by-work-order/{id}
GET    /api/install/page
GET    /api/install/my                我的安装记录
GET    /api/install/progress/{woId}   进度
```

### 用户 / 角色 / 菜单 / 部门
```
POST   /api/users        / GET /api/users/page / /api/users/{id} / ...
POST   /api/users/{id}/roles      分配角色
POST   /api/users/{id}/password   改密码
（role / menu / dept 类似 CRUD，省略）
```

### 装维
```
GET    /api/installers/online        当前在线装维
POST   /api/installers/{id}/heartbeat  心跳（更新 Redis ZSET）
POST   /api/installers/{id}/location   位置上报
```

### 通知
```
POST   /api/notify/sms               发短信
POST   /api/notify/wechat/template   发微信模板
GET    /api/notify/messages/page     发送记录
```

### 日志 / 文件 / 系统
```
GET    /api/logs/operation/page
GET    /api/logs/login/page
POST   /api/files/upload
GET    /api/files/{id}/presign
GET    /api/files/page
GET    /api/files/by-biz?bizType=&bizId=
DELETE /api/files/{id}
```

### 监控
```
GET    /actuator/health
GET    /actuator/metrics
```

---

## 11. 关键业务流转

### 11.1 订单全生命周期（最核心）

```
  CS              审核员           系统              装维              客户
   │  POST 创建     │                │                 │                │
   ├───────────────▶│                │                 │                │
   │  OrderCreatedEvent              │                 │                │
   │                │                │                 │                │
   │  POST 审核     │                │                 │                │
   ├───────────────▶│                │                 │                │
   │                │ 状态机: CREATED→AUDITED          │                │
   │                │ + 写 audit_log │                 │                │
   │                │  OrderAuditedEvent              │                │
   │                │                │ OrderAuditedListener 自动派单  │
   │                │                │ 评分+锁+建工单  │                │
   │                │                │ WorkOrderDispatchedEvent        │
   │                │                │                 │  SMS 通知       │
   │                │                │                 │                │
   │                │                │  POST 接单      │                │
   │                │                │ ◀───────────────┤                │
   │                │                │  ACCEPTED       │                │
   │                │                │                 │                │
   │                │                │  POST 到达      │                │
   │                │                │ ◀───────────────┤                │
   │                │                │  POST info/photos/signature     │
   │                │                │ ◀───────────────┤                │
   │                │                │                 │                │
   │                │                │  POST 完工      │                │
   │                │                │ ◀───────────────┤                │
   │                │                │  workOrder→COMPLETED            │
   │                │                │  bssClient.activate()           │
   │                │                │  orderService.markFinished()     │
   │                │                │  order→FINISHED  │               │
   │                │                │  InstallCompletedEvent           │
   │                │                │  NotifyEvent ──────►  完工短信   │
```

**自动派单触发**（`dispatch/event/OrderAuditedListener.java`）：
```java
@TransactionalEventListener(phase = AFTER_COMMIT)  // 关键：等订单事务提交完
public void onOrderAudited(OrderAuditedEvent ev) {
    dispatchService.autoDispatch(ev.getOrderId());
}
```

**改派**（`reassign`）：取消旧工单 + 重新 `autoDispatch` 源订单；30 分钟冷却防滥用。

**BSS 失败回滚**（`install/service/impl/InstallServiceImpl.complete()`）：
```
如果 bssClient.activate() 抛异常：
  1. install_record.status → IN_PROGRESS
  2. workOrderService.revertToInstalling(woId)  // 工单回 IN_PROGRESS
  3. 重新抛异常 → @Transactional 回滚本方法的字段修改
  4. H5 端捕获后提示重试
```

### 11.2 登录与会话

```
前端                          后端
 │  GET  /auth/public-key       │
 ├─────────────────────────────▶│
 │  ←──────────────────────────┤  { publicKey: "-----BEGIN..." }
 │                             │
 │  GET  /auth/captcha          │
 ├─────────────────────────────▶│
 │  ←──────────────────────────┤  { key, image: base64 }
 │                             │
 │  POST /auth/login            │
 │  { username, password(密文), captchaKey, captchaCode }
 ├─────────────────────────────▶│
 │  1. 验验证码 (Redis)         │
 │  2. 查用户 → 私钥解 password  │
 │  3. BCrypt 比对              │
 │  4. issue access + refresh   │
 │     - jti 写入 Redis (TTL)   │
 │  5. 发 LoginLogEvent         │
 │  ←──────────────────────────┤  { accessToken, refreshToken, userInfo }
 │                             │
 │  之后每次请求：              │
 │  Authorization: Bearer <access>
 ├─────────────────────────────▶│
 │  JwtAuthInterceptor 解析 → 注入 SecurityUser │
 │  → Controller → @PreAuthorize 校验权限码       │
```

### 11.3 文件上传（装机照片）

```
H5 选图 → POST /api/files/upload (multipart, bizType=INSTALL, bizId=woId)
   │  文件大小 ≤ 20MB
   │  storage-type=local → 写 ./uploads/{yyyyMM}/{uuid}.{ext} → 落 attachment 表
   │  storage-type=minio → MinioClient.putObject() → 落 attachment 表
   ◀ { id, objectKey, url, thumbnailUrl }

H5 拿到 url 后 → POST /api/install/{woId}/photos { url }
   │  装机 service 累加到 install_record.photos (JSON 数组)
```

---

## 12. 如何运行

### 12.1 启动中间件
```bash
cd G:/2.Study/BBPMS
docker compose up -d              # MySQL 3306 + Redis 6379
# 第一次会自动跑 middleware/mysql/init/ 里的 3 个 SQL，建库 + 表 + 种子数据
# 清理 SQL 改动：docker compose down -v
```

### 12.2 启动后端
```bash
# IDEA：
# 1. File → Open → 选 bbpms-parent/pom.xml
# 2. 等 Maven 下载依赖
# 3. 找到 bbpms-app/src/main/java/com/bbpms/BbpmsApplication.java
# 4. 右键 → Run
# 看到 "Started BbpmsApplication in X seconds" 即成功
# 端口 8080，Knife4j 文档 http://localhost:8080/doc.html

# 命令行（可选）：
cd bbpms-parent
mvn -pl bbpms-app spring-boot:run
# 切 prod profile：mvn -pl bbpms-app spring-boot:run -Dspring-boot.run.profiles=prod
```

### 12.3 启动前端
```bash
# 管理后台
cd bbpms-admin-web
npm install
npm run dev
# → http://localhost:5173

# 装维 H5
cd bbpms-installer-h5
npm install
npm run dev
# → http://localhost:9002（hash 路由，直接打开）
```

### 12.4 运行测试
```bash
cd bbpms-parent
mvn test                            # 全部单测
mvn -pl bbpms-app test -Dtest=SnowflakeIdGeneratorTest   # 跑单个
```

### 12.5 构建产物
```bash
cd bbpms-parent
mvn clean install -DskipTests       # 打 jar 到 bbpms-app/target/bbpms-app.jar
# 部署：java -jar bbpms-app.jar --spring.profiles.active=prod
```

---

## 13. 默认账号

所有账号密码都是 **`admin123`**（BCrypt 同 hash）。

| 账号 | 角色 | 能做的事 |
|---|---|---|
| `admin` | SUPER_ADMIN | 全部菜单 + 查看明文客户信息 |
| `cs1` | CUSTOMER_SERVICE | 客户管理 + 创建订单 |
| `cs2` | CUSTOMER_SERVICE | 同上 |
| `audit1` | AUDITOR | 审核订单 |
| `disp1` | DISPATCHER | 查看派单 + 手动派单 + 改派 |
| `install1` ~ `install5` | INSTALLER | H5 端：我的工单 + 装机流程 |

后端 MySQL 应用账号 `bbpms_app / bbpms_pwd_2026`（库名 `bbpms`）。

---

## 14. 常见操作 FAQ

### Q: 我想改派单权重
A: 改 `application.yml` 的 `bbpms.dispatch.weight-*`（默认 40/25/20/15），或去管理后台 → 系统 → 派单规则改数据库的 `dispatch_rule` 表（应用读的是 `name='default'` 那条）。

### Q: 我想加一个订单状态
A:
1. `common/enums/OrderStatus.java` 加枚举
2. `common/enums/OrderEvent.java` 加触发事件
3. `common/statemachine/OrderStateMachine.java` 加转换行（带角色码）
4. 业务代码用 `orderStateMachine.transit(...)` 而不是直接 `order.setStatus(...)`
5. SQL 加新枚举值（`VARCHAR` 不用动）
6. 前端 `OrderTimeline.vue` / `BBPMSStatusTag.vue` 加显示映射

### Q: 我想加一种新角色
A:
1. `common/enums/UserType.java` 加枚举（code 数字）
2. seed SQL（`02-seed-data.sql`）加 `sys_role` 行
3. 菜单 → 角色绑定（`sys_role_menu`）
4. 状态机里允许的 `allowedRoles` 用对应 code
5. `RbacServiceImpl` 的 dataScope 逻辑里加新规则（如果需要）

### Q: 我想加一个新的 API
A: 在对应域建 controller / service，按现有模式抄（参见 §6 各模块的 controller 列表）。需要权限校验就在方法上加 `@PreAuthorize("hasAuthority('xxx:yyy')")`。

### Q: 我想给某条业务完成后自动发短信
A: 业务代码里发事件，不要直接调 `notifyService`：
```java
publisher.publishEvent(new BbpmsEvents.NotifyEvent(
    "SMS", phone, null, userId, "TEMPLATE_CODE",
    Map.of("var1", value1, "var2", value2)
));
```
然后在 `message_template` 表加一行 `code='TEMPLATE_CODE'` 的模板（content 里用 `${var1}` 占位符）。

### Q: 我想从 local 切到 MinIO
A:
1. `application.yml` 设 `bbpms.file.storage-type: minio`
2. 补 MinIO 连接配置（endpoint / accessKey / secretKey / bucket）
3. `FileServiceImpl` 里 minio 分支已写好（如果有的话），否则需要扩展
4. 旧 local 文件不会迁移

### Q: 我想加一个跨模块的"订单状态变了"钩子
A: 找 `OrderServiceImpl` 的 `updateStatus()`，在事务里调完之后发一个 `ApplicationEvent`。或者更精确地，在 `audit()` / `cancel()` 末尾发事件。**别**用 `@Async` 直接调其他 Service（同进程内 `@Async` 容易把事务上下文弄丢）。

### Q: 我加了字段没生效
A: 看 entity 加了 `@TableField` 吗？字段名是否匹配（MyBatis-Plus 默认 snake_case 自动转）？逻辑删除字段 `deleted` 不要手动改，用 service 层方法。BaseDO 的 `create_time` 等是 `FieldFill.INSERT` 自动填的，不要重复 set。

---

## 15. 如何扩展

### 15.1 加一个新业务域（比如 "账单"）

1. 在 `bbpms-app/src/main/java/com/bbpms/` 下建 `bill/` 包
2. 复制 `order/` 的目录结构（controller / service / mapper / entity / dto / vo / config）
3. 加表 → 写到 `middleware/mysql/init/01-bbpms-schema.sql` 末尾（按字母序，新表会跟现有表一起 init）
4. 种子数据 → 加到 `02-seed-data.sql`
5. RBAC → 加 `sys_menu` 行 + `sys_role_menu` 绑定
6. 前端 → 在 `bbpms-admin-web/src/api/` 加 `bill.ts` + `views/bill/` + 在 `router/index.ts` 加路由

### 15.2 替换某个本地组件为外部服务

如果将来要把派单做成独立微服务：
1. 把 `dispatch/` 包单独抽成 Spring Boot app
2. `DispatchService` 改成 OpenFeign client
3. 订单模块的 `OrderAuditedListener` 改成 HTTP / MQ 调用
4. Redis 共享（用同一个 Redis 集群）
5. 灰度：双写 + 灰度切流

### 15.3 改事件为真正的 MQ

把 `publisher.publishEvent(...)` 替换成 RabbitTemplate.convertAndSend(...)；listener 类加 `@RabbitListener`。事件类本身不需要改（继承同样的字段）。这是最自然的"未来可扩展"路径 — 现在的事件总线就是 MQ 的内部等价物。

---

## 附录：常用文件路径速查

| 需求 | 路径 |
|---|---|
| 启动入口 | `bbpms-app/src/main/java/com/bbpms/BbpmsApplication.java` |
| 统一响应 | `bbpms-app/.../common/result/R.java` |
| 全局异常 | `bbpms-app/.../common/config/GlobalExceptionHandler.java` |
| 状态机 | `bbpms-app/.../common/statemachine/` |
| 事件定义 | `bbpms-app/.../common/event/BbpmsEvents.java` |
| 派单算法 | `bbpms-app/.../dispatch/algorithm/DispatchScoringService.java` |
| BSS 模拟 | `bbpms-app/.../install/bss/BssClient.java` |
| 鉴权拦截器 | `bbpms-app/.../interceptor/JwtAuthInterceptor.java` |
| 配置 | `bbpms-app/src/main/resources/application*.yml` |
| SQL 初始化 | `middleware/mysql/init/` |
| Docker | `docker-compose.yml` |
| 前端路由 | `bbpms-admin-web/src/router/index.ts` |
| H5 路由 | `bbpms-installer-h5/src/router/index.ts` |

---

> 文档随项目演进。如发现对不上代码的地方，以代码为准并提个 PR 更新本文。
