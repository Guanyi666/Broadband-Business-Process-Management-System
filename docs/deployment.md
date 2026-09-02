# BBPMS 部署与运维

> 面向运维与交付人员。环境要求、启动、构建、部署、配置项。

## 1. 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | 后端编译与运行 |
| Maven | 3.9+ | 构建后端 |
| Node.js | 18+ | 构建前端 |
| MySQL | 8.0 | 单库 `bbpms` |
| Redis | 7.x | 缓存 / 锁 / 在线装维 ZSET |
| Docker + Compose | 最新 | 本地一键起 MySQL + Redis |

## 2. 本地启动

### 2.1 中间件
```bash
cd G:/2.Study/BBPMS
docker compose up -d          # MySQL(3306, root/123456) + Redis(6379, 123456)
docker compose down -v        # 销毁并重建（改 SQL 后必须做，首次 up 才会重跑 init）
```
SQL 初始化（`middleware/mysql/init/`，按字母序）：
`00-create-database`（建库+应用账号）→ `01-bbpms-schema`（业务表）→ `03-schema-extensions`（考勤/请假/SLA 表+缺列，幂等）→ `04-seed-data`（角色/用户/菜单/模板/规则）。

### 2.2 后端（8080）
```bash
cd bbpms-parent
mvn clean install -DskipTests      # 编译打包
mvn spring-boot:run                # dev profile（默认）
# 或 IDEA 打开 bbpms-parent/pom.xml → 运行 BbpmsApplication
```
- 接口文档：http://localhost:8080/doc.html
- 切 profile：`-Dspring.profiles.active=prod`

### 2.3 前端
```bash
# 管理后台 (5173)
cd bbpms-admin-web
npm install && npm run dev
# 装维 H5 (9002)
cd bbpms-installer-h5
npm install && npm run dev
```
- dev 代理：两套前端 Vite 代理将 `/api` 透传到 `http://localhost:8080`。
- `VITE_API_BASE=/api`（相对路径）；生产可改为带 `/api` 的绝对地址。

### 2.4 测试
```bash
cd bbpms-parent
mvn test                          # 单元测试（无 Spring 上下文）
```

## 3. 生产部署

### 3.1 后端
```bash
cd bbpms-parent && mvn clean install -DskipTests
java -jar bbpms-app/target/bbpms-app.jar --spring.profiles.active=prod
```
prod profile 全部配置走环境变量，**无默认值**（缺配置启动即报错，便于提前发现）：

| 环境变量 | 用途 |
|---|---|
| `BBPMS_RSA_PRIVATE_KEY` / `BBPMS_RSA_PUBLIC_KEY` | JWT RSA 密钥对（Base64） |
| `BBPMS_REFRESH_SECRET` | JWT refresh 密钥（≥32B Base64） |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | MySQL 连接 |
| `SPRING_DATA_REDIS_HOST/PORT/PASSWORD` | Redis 连接 |
| `ALIYUN_ACCESS_KEY` / `ALIYUN_SECRET_KEY` | 短信（真实发送时） |
| `AMAP_API_KEY` | 高德地图（派单距离） |
| `BBPMS_BSS_MOCK_ENABLED` | BSS 开通是否 Mock（生产应 false） |

> 不要把这些密钥硬编码进 YAML；`application-prod.yml` 无默认值，故意报错提醒。

### 3.2 前端构建
```bash
cd bbpms-admin-web && npm run build     # dist/
cd bbpms-installer-h5 && npm run build  # dist/
```
静态资源由 Nginx 托管；`/api` 反向代理到后端。

### 3.3 数据库初始化
生产首次部署：用 `middleware/mysql/init/` 的 SQL 在目标库执行（顺序：00→01→03→04），并创建 `bbpms_app` 应用账号。

## 4. 运维说明

- **日志**：后端日志输出到控制台；本地运行时落 `logs/`（已 gitignore）。生产建议接入集中日志。
- **健康检查**：`/actuator/health`。
- **SLA 阈值**：`bbpms.workorder.sla.*`（scan-interval-ms / accept-timeout-minutes / progress-heartbeat-timeout-hours / stalled-recover-hours / enabled）。
- **考勤**：`bbpms.attendance.*`（auto-checkout-hours / history-retention-days 等）。
- **数据备份**：MySQL 单库，建议每日 mysqldump；Redis 为缓存可重建。
- **常见排障**：
  - 登录 401/403 → 检查 JWT 密钥配置、Redis 是否可达。
  - 列表 500 → 检查对应表 `deleted`/`version` 列存在、DataScope 白名单语句。
  - 改 SQL 不生效 → `docker compose down -v` 后重新 `up`。

## 5. 已知部署注意点

1. 前端 `H5` 的 `npm run build` 存在**预先存在的 TS 类型错误**（Vant API 用法、amap-js-api-loader 类型缺失），修复前无法通过 `vue-tsc` 构建。
2. 考勤月度汇总表 `att_attendance_summary` 当前无计算逻辑，报表为空（待办）。
3. DEPT/CUSTOM 数据范围未实现（业务表缺 `dept_id` 列）。
