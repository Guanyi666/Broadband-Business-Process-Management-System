# BBPMS 项目计划与状态

> 面向项目经理。阶段规划、当前完成度、已知问题、路线图。**最后更新：2026-08-06**。

## 1. 项目里程碑

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 0-1 | 模块化单体骨架 + 订单/工单/派单/安装主链路 | ✅ 已完成 |
| Phase 1 | 核心 bug 修复（乐观锁、Redis 方法、缺失接口） | ✅ 已完成 |
| Phase 3 | 考勤（签到/休息/自动签出/清理） | ✅ 代码完成 |
| Phase 4 | 请假（多级审批 + 升级规则 + 派单联动） | ✅ 代码完成 |
| Phase 5 | 工单生命周期加固（10 状态 + SLA 引擎） | ✅ 代码完成 |
| Phase 6-7 | 派单排除请假、在线装维过滤 | ✅ 代码完成 |
| **质量修复轮** | 端到端可运行性修复（鉴权链路/DataScope/重派/DB init/实体对齐/前后端契约） | ✅ 2026-08-06 完成 |

## 2. 当前完成度（2026-08）

**代码完成度 ≈ 90%**，但**可运行性曾严重不足** —— 2026-08-06 质量修复轮前，应用存在以下致命问题：

### 已修复（质量修复轮）
1. **鉴权链路**：JWT 拦截器桥接 Spring Security，`@PreAuthorize` 生效；修复 JWT claim key 不匹配（`uid/usr/scope`）、`/auth/me`+`/auth/menus` 缺失、refresh 单次旋转、登出吊销 refresh token。
2. **DataScope**：移除 4 处 XML 中引用不存在方法的 OGNL 表达式；重写 `DataScopeInnerInterceptor`（白名单 + SELF 过滤）。
3. **重派**：`create()` 幂等检查排除终态工单，改派/超时自动重派真正生效。
4. **SQL init**：合并冲突的 03 文件、seed 改 04 顺序执行、补 `USE bbpms`，新环境可干净初始化。
5. **缺失 SQL**：`SysUserMapper`（登录）/`SysMenuMapper`/`InstallerProfileMapper`/日志分页等补全。
6. **实体↔表对齐**：`sys_role`/`sys_dept`/`sys_menu`/`sys_user` 字段与表结构对齐（此前 Unknown column）。
7. **前后端契约**：统一 `/api` 前缀、修正 vite 代理、`/system/*`→裸路径、登录响应字段（`token`/`user`）、补齐 menu/dept 更新删除端点、权限码 `sys:*`→`system:*` + seed 补权限。
8. **单测**：40 个全绿（含修正 1 个与规范矛盾的用例）。

### 验证状态
- ✅ 后端编译通过；40 个单元测试通过。
- ✅ admin-web 前端类型检查通过。
- ⚠️ **SQL 初始化 + 登录/接口冒烟尚未在本机跑通**（Docker Desktop 未运行），需按 `deployment.md` 执行端到端验证。

## 3. 已知问题（待办，按优先级）

| 优先级 | 问题 | 影响 | 建议 |
|---|---|---|---|
| P0 | Docker 环境未验证（SQL init + 冒烟） | 端到端可运行性未最终确认 | 启动 Docker 后按 deployment.md 跑一遍登录+列表+刷新 |
| P1 | H5 `npm run build` 有 7 个预存 TS 类型错误 | H5 无法通过 `vue-tsc` 构建 | 修 Vant API 用法与 amap 类型声明 |
| P1 | H5 功能契约：`/work-orders/my` 返回结构（`records` vs `list`）、工单完工传参与后端不一致 | H5 列表/完工流程可能错乱 | 对齐 H5 与后端响应/入参 |
| P2 | 考勤月度汇总表从不计算 | 月度报表恒空 | 调度器补汇总逻辑 |
| P2 | 请假 L2 可绕过 L1 直接审批 | 审批顺序未强校验 | `approve()` 加顺序校验 |
| P2 | 考勤休息时长写死 30 分钟 | 统计不准 | 追踪 `break_start_at` |
| P3 | DEPT/CUSTOM 数据范围未实现 | 业务表缺 dept 列 | 架构演进项 |
| P3 | `wo_sla_policy` 表未接入 | 按业务类型 SLA 未启用 | v2 功能 |

## 4. 后续路线图（建议）

- **Sprint A（质量收口）**：Docker 端到端验证 → H5 TS 修复 → H5 契约对齐 → 补集成测试（至少 1 个 `@SpringBootTest` 冒烟 + 鉴权用例）。
- **Sprint B（功能补全）**：考勤月度汇总、请假审批顺序校验、休息时长真实统计。
- **Sprint C（架构演进，可选）**：`wo_sla_policy` 接入、DEPT/CUSTOM 数据范围、事件总线平滑替换 MQ。

## 5. 版本与文档

- 构建：`bbpms-parent`（父 POM）+ `bbpms-app`（单体应用）；版本 `1.0.0-SNAPSHOT`。
- 文档入口：`docs/README.md`；项目导览 `docs/PROJECT_TOUR.md`。
- 历史（11 微服务）设计文档：`temporary_backup/docs-legacy/`（仅参考）。
