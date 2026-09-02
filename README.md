# BBPMS — 宽带业务流程管理系统

> **Broadband Business Process Management System**
> 参考国内三大运营商（中国移动 / 中国联通 / 中国电信）宽带装机业务流程的企业级 **模块化单体（Modular Monolith）**

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

---

## 📖 项目简介

BBPMS 是一个面向运营商宽带装维业务的**全生命周期管理平台**，实现：

> 客户申请 → 订单创建 → 资料审核 → 资源校验 → 自动派单 → 装维接单 → 上门安装 → 销单归档

涵盖 **1 个 Spring Boot 单体应用 + 1 个 MySQL + 1 个 Redis + 2 个 Vue 前端**，代码组织按业务域分包（`com.bbpms.auth` / `user` / `order` / `workorder` / `dispatch` / `install` / `notify` / `log` / `file`）。

### 核心特性（简化后）

- ✅ **Modular Monolith** — 1 个 Spring Boot 应用，包结构按业务域隔离
- ✅ **6 类用户角色** + RBAC + 数据范围（ALL / DEPT / SELF / CUSTOM）
- ✅ **自动派单算法**（距离 / 负载 / 技能 / 评分加权 + Redis ZSET + Redisson 锁）
- ✅ **状态机 + 时间线审计**（订单 8 态、工单 7 态）
- ✅ **本地事务** 替代 Seata AT/Saga
- ✅ **Spring `ApplicationEvent`** 替代 RabbitMQ + Outbox
- ✅ **JWT 鉴权**（Spring Interceptor 替代 Gateway 过滤）
- ✅ **Redis 单节点** 缓存 + 分布式锁（多实例部署仍可扩展）
- ✅ **MySQL 单库**（所有业务表合并为一个 schema）

---

## 🛠️ 技术栈

### 后端（Java 21 + Spring Boot 3.2.5）

| 类别 | 技术 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.2.5 |
| 安全 | Spring Security + JWT (jjwt 0.12.5) + BCrypt |
| ORM | MyBatis Plus 3.5.7 |
| 缓存 / 锁 | Redis 7 + Redisson 3.27.2 |
| 数据库 | MySQL 8.0（单库多表） |
| 事件 | Spring `ApplicationEventPublisher` + `@TransactionalEventListener` |
| 调度 | Spring `@Scheduled` |
| 国密 | BouncyCastle SM4/SM3 |
| API 文档 | Knife4j (OpenAPI 3) |

### 前端

| 类别 | 技术 |
|---|---|
| 管理后台 | Vue 3.4 + Vite 5 + TypeScript 5 + Element Plus 2.5 + Pinia 2 |
| 装维 H5 | Vue 3.4 + Vant 4 + 高德地图 JS API |
| HTTP | Axios + 请求/响应拦截器 |

### 运维

| 类别 | 技术 |
|---|---|
| 数据库 | MySQL 8.0（Docker 单容器） |
| 缓存 | Redis 7（Docker 单容器） |
| 容器化 | Docker + Docker Compose |
| 静态资源 | Nginx（开发模式可省） |

---

## 📁 项目结构

```
BBPMS/
├── docs/                                  # 项目文档（PM 视角）
│   ├── README.md                          # 文档导航
│   ├── PROJECT_TOUR.md                    # ★ 项目导览（推荐先读）
│   ├── requirements.md                    # 项目需求
│   ├── architecture.md                    # 系统架构
│   ├── database-design.md                 # 数据库设计
│   ├── api-document.md                    # 接口文档
│   ├── deployment.md                      # 部署说明
│   └── project-plan.md                    # 项目计划与状态
├── bbpms-parent/                          # 父 POM
├── bbpms-app/                             # ★ 唯一后端单体应用
│   └── src/main/java/com/bbpms/
│       ├── BbpmsApplication.java          # 启动类
│       ├── common/                        # 公共组件（result/entity/exception/enums/util/...）
│       ├── interceptor/                   # JWT 鉴权拦截器
│       ├── auth/                          # 认证模块
│       ├── user/                          # 用户/RBAC 模块
│       ├── order/                         # 订单模块
│       ├── workorder/                     # 工单模块
│       ├── dispatch/                      # 派单模块（含算法）
│       ├── install/                       # 安装模块（含 BSS mock）
│       ├── notify/                        # 通知模块（短信）
│       ├── log/                           # 日志模块
│       ├── file/                          # 文件模块
│       └── config/                        # Web/Jackson/CORS/MyBatis+/Redisson 等
├── bbpms-admin-web/                       # 管理后台前端（Vue3 + Element Plus）
├── bbpms-installer-h5/                    # 装维 H5（Vue3 + Vant）
├── middleware/
│   ├── mysql/init/                        # 合并后的 SQL init 脚本
│   └── redis/redis.conf
└── docker-compose.yml                     # 仅 MySQL + Redis
```

---

## 🚀 快速开始

### 1. 启动中间件

```bash
cd G:\2.Study\BBPMS
docker compose up -d
# 启动 mysql + redis 两个容器
```

### 2. 启动后端（IDEA）

1. **IDEA** → `File` → `Open` → `bbpms-parent/pom.xml`
2. 等 Maven 依赖下载完成
3. 找到 `bbpms-app/src/main/java/com/bbpms/BbpmsApplication.java`
4. 右键 → Run 'BbpmsApplication'
5. 看到 `Started BbpmsApplication in X seconds` 即成功（默认端口 8080）

### 3. 启动前端（WebStorm）

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
# → http://localhost:9002
```

### 4. 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 超级管理员 |
| `cs1` | `admin123` | 客服 |
| `audit1` | `admin123` | 审核员 |
| `disp1` | `admin123` | 调度员 |
| `install1` | `admin123` | 装维工程师 |

---

## 🔄 核心业务流程

```
客服 CS  ─→  创建订单（@Transactional + ApplicationEvent OrderCreatedEvent）
              │
              ▼
审核员  ─→  审核通过（@Transactional + ApplicationEvent OrderAuditedEvent）
              │
              ▼
@EventListener @Async  ─→  自动派单算法（Redis ZSET + Redisson 锁）
              │              │
              │              ▼
              │         创建工单 + WorkOrderDispatchedEvent
              ▼
装维  ─────  接单 → 出发 → 到达现场 → 装机信息 → 照片（≥3）→ 签名 → 完工
              │                                            │
              ▼                                            ▼
        InstallCompletedEvent                    @Transactional + try/catch
              │                                            │
              ▼                                            ▼
        BSS 开通（Mock）                          Saga 补偿（失败回滚）
              │
              ▼
        订单 FINISHED → 销单归档 → NotifyEvent → 短信通知
```

详细流程见 `docs/architecture.md`。

---

## 🧮 自动派单算法（核心 IP）

**4 因素加权评分**（`DispatchScoringService`）：

| 因素 | 权重 | 数据来源 |
|---|---|---|
| 距离 | 40% | Haversine 计算 |
| 负载 | 25% | 当前进行中工单数 |
| 技能 | 20% | 装维技能 vs 订单要求 |
| 评分 | 15% | 装维历史评分 |

**Redis 数据结构**：
- `installers:active` ZSET — score=last_ping epoch ms
- `installers:workload:{date}` ZSET — score=current count

**锁**：Redisson `tryLock("lock:dispatch:{installerId}", 5s, 30s)`

**审计**：`dispatch_record.candidates_json` 存储所有候选 + 评分，便于回溯。

---

## 🔒 安全设计

| 层级 | 实现 |
|---|---|
| 认证 | RSA RS256 JWT（access 30min + refresh 7d 单次旋转） |
| 鉴权 | Spring Interceptor 校验 + `@PreAuthorize` 按钮级 |
| 数据范围 | `@DataScope` AOP 注入 SQL 条件 |
| 加密 | SM4 身份证/手机号 + 字段 mask |
| 审计 | `@OperationLog` AOP → ApplicationEvent → 异步落库 |

---

## 📝 文档导航

| 文档 | 内容 |
|---|---|
| [docs/PROJECT_TOUR.md](docs/PROJECT_TOUR.md) | 项目导览：模块/表/API/流程/FAQ/扩展 |
| [docs/requirements.md](docs/requirements.md) | 项目需求与功能范围 |
| [docs/architecture.md](docs/architecture.md) | 系统架构与模块划分 |
| [docs/database-design.md](docs/database-design.md) | 数据库设计 |
| [docs/api-document.md](docs/api-document.md) | 接口文档 |
| [docs/deployment.md](docs/deployment.md) | 部署与运维 |
| [docs/project-plan.md](docs/project-plan.md) | 项目计划与当前状态 |

---

## 🎯 面试亮点

1. **架构演进**：从 11 个微服务 → 1 个模块化单体（业务亮点保留）
2. **自动派单算法**：4 因素加权评分 + Redis ZSET + Redisson 锁
3. **状态机自研**：枚举 + 转换表 + 时间线审计
4. **Spring 事件机制**：`ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` 实现可靠事件
5. **JWT 双层防护**：RSA 签名 + Refresh Token 单次旋转 + Redis 黑名单
6. **数据范围 AOP**：`@DataScope` 拦截器 SQL 注入
7. **SM4 国密加密**：身份证、手机号加解密
8. **Saga 替代**：try-catch + 状态回滚实现分布式补偿

---

## 📜 许可

本项目为**学习/参考项目**，按 Apache 2.0 协议开源。
