# PROJECT_CLEANUP_REPORT

> 项目清理报告 —— 2026-08-06
> 原则：保留项目运行/开发/部署/维护必需文件；删除无价值/重复/过时/临时文件；文档聚焦 PM 核心文档。

## 1. 清理前项目结构（摘要）

```
BBPMS/
├── 源码       bbpms-parent/ bbpms-app/ bbpms-admin-web/ bbpms-installer-h5/
├── 配置       docker-compose.yml .env.* vite.config.ts application*.yml .gitignore
├── 数据       middleware/mysql/init/*.sql middleware/redis/redis.conf
├── 文档       README.md CLAUDE.md docs/{README,PROJECT_TOUR,legacy/}
├── 垃圾/可再生 logs/ .idea/ target/ node_modules×2 4×花括号空目录
```

## 2. 删除文件列表

### A. 意外生成的空目录（直接删除）
| 路径 | 原因 |
|---|---|
| `bbpms-app/.../attendance/{entity,mapper,dto,vo,service/` | 空目录，`mkdir` brace-expansion 失败产物 |
| `bbpms-app/.../attendance/{entity,mapper,dto,vo,service/impl,...}/` | 空目录 |
| `bbpms-app/.../leave/{entity,mapper,dto,vo,service/` | 空目录 |
| `bbpms-app/.../leave/{entity,mapper,dto,vo,service/impl,...}/` | 空目录 |

### B. 可再生构建产物 / 运行日志（直接删除）
| 路径 | 大小 | 重建方式 |
|---|---|---|
| `bbpms-app/target/` | 2.3 MB | `mvn compile` |
| `bbpms-admin-web/node_modules/` | 253 MB | `npm install` |
| `bbpms-installer-h5/node_modules/` | 126 MB | `npm install` |
| `logs/bbpms-app.log` | 1.2 MB | 运行时自动生成 |

### C. 移至临时归档 `temporary_backup/`（确认后删除）
| 路径 | 原因 |
|---|---|
| `docs/legacy/` → `temporary_backup/docs-legacy/` | 重构前 11 微服务设计文档，已废弃（CLAUDE.md 注明不再适用） |
| `.idea/` → `temporary_backup/.idea/` | IDE 工作区配置，可重建 |

## 3. 保留文件列表（核心）

- **源码**：后端 `bbpms-app/src/`（13 业务域 + common）、`bbpms-parent/pom.xml`、`bbpms-admin-web/src/`、`bbpms-installer-h5/src/`
- **构建/配置**：全部 `pom.xml`/`package.json`/`package-lock.json`/`vite.config.ts`/`tsconfig*`/`.eslintrc*`/`.env.*`/`application*.yml`/`docker-compose.yml`/`.gitignore`
- **数据脚本**：`middleware/mysql/init/`（00/01/03/04 四个 SQL）、`middleware/redis/redis.conf`
- **核心文档**：根 `README.md`、`CLAUDE.md`、`docs/PROJECT_TOUR.md`、前端各自 `README.md`
- **本次新增 PM 文档**：`docs/requirements.md`、`docs/architecture.md`、`docs/database-design.md`、`docs/api-document.md`、`docs/deployment.md`、`docs/project-plan.md`

## 4. 当前项目结构说明

```
BBPMS/
├── README.md                      # 项目简介 / 快速开始 / 账号
├── CLAUDE.md                      # AI/开发工作指南
├── PROJECT_CLEANUP_REPORT.md      # 本报告
├── docker-compose.yml             # MySQL + Redis
├── .gitignore
├── bbpms-parent/                  # Maven 父 POM
├── bbpms-app/                     # 唯一后端单体应用（src + pom.xml）
├── bbpms-admin-web/               # PC 管理后台（src + 配置 + README）
├── bbpms-installer-h5/            # 装维 H5（src + 配置 + README）
├── middleware/
│   ├── mysql/init/                # 00/01/03/04 四个 SQL
│   └── redis/redis.conf
├── docs/                          # 文档（PM 视角）
│   ├── README.md                  # 文档导航
│   ├── PROJECT_TOUR.md            # ★ 项目导览
│   ├── requirements.md            # 需求
│   ├── architecture.md            # 架构
│   ├── database-design.md         # 数据库设计
│   ├── api-document.md            # 接口文档
│   ├── deployment.md              # 部署
│   └── project-plan.md            # 项目计划/状态
└── temporary_backup/              # 待确认删除（docs-legacy / .idea）
```

## 5. 伴随修正（非删除）

- 统一前端 `VITE_API_BASE=/api` + Vite 代理目标固定 `http://localhost:8080`（修复 dev 下登录/接口 404）。
- 修正根 `README.md` 失效文档链接，指向新 docs 结构。

## 6. 后续维护建议

1. **确认归档删除**：核对 `temporary_backup/` 无需要内容后，执行 `rm -rf temporary_backup/`。
2. **恢复运行**：`docker compose up -d`（重建 DB）+ `cd bbpms-parent && mvn spring-boot:run`；前端 `npm install` 后 `npm run dev`。
3. **文档维护**：`PROJECT_TOUR.md` 与 `CLAUDE.md` 是"活的"文档，代码变更时应同步；`project-plan.md` 随迭代更新状态。
4. **验证闭环**：按 `docs/deployment.md` 完成端到端冒烟（登录 → 列表 → 刷新），补 1 个集成测试防止回归。
5. **H5 待办**：`npm run build` 的预存 TS 错误需修复后才能真正交付。
