# BBPMS 2.0 — ITERATION 1 实施报告（核心状态机完善）

> 产出时间：2026-09-03 · 前置：ITERATION 0（ARCHITECTURE / BACKEND-AUDIT / FRONTEND-AUDIT / IMPROVEMENT-BACKLOG）
> 范围：P0-1 驳回重提闭环 · P0-2 权限注解补齐 · P0-3 数据权限 DEPT/DEPT_AND_CHILD 落地
> 提交：`dde3486`（P0-1）· `28f15f9`（P0-2）· `729817a`（P0-3）

---

## 0. 开工前的五个问题（基于 ITERATION 0 审计结论）

| # | 问题 | 结论 |
|---|------|------|
| Q1 | 当前系统是否已具备本轮功能的一部分？ | **是**。订单已有 8 态事件机 + 角色白名单（审核/派发/取消闭环完整）；权限体系双保险（前端路由 + 后端 @PreAuthorize）；数据权限已有 SELF 实现与拦截器框架。本轮=补缺口而非新建。 |
| Q2 | 现有代码哪些可以复用？ | **大量复用**。OrderStateMachine/OrderEvent 枚举扩即可；DataScopeInnerInterceptor 白名单机制复用，仅扩展 DEPT/DEPT_AND_CHILD 分支；JWT claim 扩 `dept` 字段贯通全链路；sys_dept.path 物化路径免除自关联递归。 |
| Q3 | 数据库是否需要修改？ | **不建新表、不加列**。业务表无 `dept_id`，采用拦截器内联子查询（create_by → sys_user.dept_id → sys_dept.path）实现部门过滤，零迁移、零回填。仅 05-seed-demo-data.sql 追加演示数据（新部门/测试账号/跨部门订单）不变更 schema。 |
| Q4 | API 是否需要修改？ | **极小**。仅新增 `POST /orders/{id}/resubmit`（P0-1）与 11 处 `@PreAuthorize` 注解（P0-2，不改变端点语义）。数据权限为 SQL 层透明注入，调用方无感知。 |
| Q5 | 是否会影响已有功能？ | **已做兼容性防护**：① `IS NULL` 逃生口保留——系统自动派单/异步监听/SLA 调度器无登录上下文，create_by 为 NULL 的存量数据仍对范围内用户可见，避免 SA-P2-004 回归；② 既有 SELF 逻辑零改动，回归通过；③ REJECTED 为新增状态，CANCELLED 流程原样保留；④ 新测试角色独立于现有角色，不污染 MAX(data_scope) 语义。 |

## 1. 本轮交付

### P0-1 订单审核驳回可修改重提（commit `dde3486`）

**背景**：AUDIT_REJECT 直入 CANCELLED（终态），客服无法修改重提，只能重新下单 → 数据重复、流程断裂。

**改动**：
- `OrderStatus` 新增 `REJECTED("REJECTED","已驳回")`；`CREATED` 释义改为"待审核"
- `OrderEvent` 新增 `RESUBMIT`
- `OrderStateMachine` 新增 3 条合法迁移：
  - `CREATED -- AUDIT_REJECT(审核员) --> REJECTED`
  - `REJECTED -- RESUBMIT(客服) --> CREATED`
  - `REJECTED -- CANCEL(客服/客户) --> CANCELLED`
- `OrderService.resubmit(orderId, operatorId)`：校验 REJECTED → 置 CREATED → 清空审核备注 → 追加时间线"驳回后重新提交，重新进入审核队列"
- `POST /api/orders/{id}/resubmit`（`@PreAuthorize("hasAuthority('order:create')")`）
- 前端：列表 statusOptions 中文 + REJECTED 标签页 + 详情页"重新提交"按钮（`canResubmit` 计算属性）+ 权限按钮包裹；类型/API 同步

**验证**（resubmit-e2e）：
```
创建→CREATED → 审核员驳回→REJECTED（此前为 CANCELLED，行为已修复）
→ 审核员再点重提→403（角色白名单拦截）→ 客服重提→CREATED → 时间线记录完整
```

### P0-2 权限注解补齐（commit `28f15f9`）

**背景**：审计发现 ~12 个端点无 `@PreAuthorize`，仅靠前端路由拦截（可绕过）。

**改动**：为以下端点补注解（均与现有权限码一致，不新增权限码）：
- 工单：getDetail/page/byOrder/rawById → `workorder:view`；heartbeat → `workorder:view-own`
- 时间线 → `workorder:view`；装维 /my → `install:view`；菜单树/perms → `system:menu:view`；装维位置 → `installer:view`；消息模板 getById/list → `notify:template:view`

**验证**（perm-check2）11/11 通过：audit1 访问工单/时间线/模板/菜单/装维/位置全 403；install1 访问工单 my/装维 my 200；admin 菜单树 200。

### P0-3 数据权限 DEPT / DEPT_AND_CHILD 落地（commit `729817a`）

**背景**：SysRole.data_scope 支持 1=ALL/2=DEPT/3=DEPT_AND_CHILD/4=SELF/5=CUSTOM，但拦截器仅实现 ALL/SELF，DEPT 分支直接跳过（业务表无 dept_id 列）。

**方案**：**零 schema 变更**。拦截器在内联子查询中实现：
```
DEPT(2)           : create_by IN (SELECT id FROM sys_user WHERE dept_id = {user.deptId} AND deleted=0)
DEPT_AND_CHILD(3) : 先取 sys_dept.path（物化路径, 如 /1/3/），再
                    dept_id IN (SELECT id FROM sys_dept WHERE path='{path}' OR path LIKE '{path}%' AND deleted=0)
                    → 递归覆盖整棵子树；path 查不到时降级为本部门（fail-closed）
SELF(4)           : create_by = {userId} OR create_by IS NULL（原样保留）
```
**JWT 链路补 `dept` claim**（SecurityUser / UserAuthInfoDTO / JwtUtils / TokenServiceImpl / JwtAuthInterceptor / SysUserServiceImpl.getAuthInfo），登录时把 `sys_user.dept_id` 带进安全上下文。

**演示数据**（05-seed-demo-data.sql）：
- 部门 3 Field Ops A（/1/3/）、4 Branch B（/1/4/，平行）、5 Sub Team 5（/1/3/5/，dept3 子部门）
- 测试账号：disp2(dept3, role7 scope=2)、audit2(dept3, role8 scope=3)、disp3(dept4)、disp4(dept5, scope=2)
- 跨部门订单/工单 2010(dept3)/2020(dept3)/2030(dept4)/2040(dept5)

**验证**（verify-datascope2.cjs）**9/9 通过**：

| 用例 | scope | 期望 | 结果 |
|---|---|---|---|
| 订单页 admin | ALL(1) | 全量 14 条 | ✅ |
| 订单页 audit2(dept3) | DEPT_AND_CHILD(3) | 见 2010/2020/2040，不见平行部门 2030 | ✅ 递归正确 |
| 订单页 cs1 | SELF(4) | 不见任何部门数据 | ✅ 回归 |
| 订单页 audit1 | SELF(4) | 同上 | ✅ 回归 |
| 工单页 admin | ALL(1) | 全量 20 条 | ✅ |
| 工单页 disp2(dept3) | DEPT(2) | 见 2010/2020，不见 2030/2040 | ✅ |
| 工单页 disp4(dept5) | DEPT(2) | 仅见 2040 | ✅ 子部门精确 |
| 工单页 audit2 | 权限隔离 | 403（AUDITOR 无 workorder:view） | ✅ 权限注解未被破坏 |
| 工单页 disp1(dept2) | SELF(4) | 不见任何部门数据 | ✅ 回归 |

## 2. 兼容性与风险说明

- **存量数据照常可见**：BBDEMO 老订单 create_by 为 NULL，属"系统创建"逃生口语义，任何 scope 下都可见——这是刻意保留，避免自动派单/调度器产物不可见（SA-P2-004 回归）。
- **实时性权衡**：DEPT 过滤依赖 JWT 中的 `dept_id`（30 分钟有效期）。若期间用户被调部门，需重新登录生效。后续 ITERATION 可改为拦截器直查 DB，换取实时性（代价是每查询多一次 DB 访问）。
- **CUSTOM(5) 未实现**：需 sys_role_data_scope 授权数据表，留待业务需求明确后补（当前无该表），遇 scope=5 仍走 warn-skip。
- **SQL 注入防护**：dept_id/path 均为后端强类型生成（Long/内部值），无用户输入拼接。

## 3. 复盘与下一步

**做得好的**：Q3 上坚持"零 migration"的副查询方案，避免了加列+回填两条动作，风险面最小；验证脚本真实登录+真实数据，DEPT/DEPT_AND_CHILD/SELF/ALL 四档语义全部有证据。

**可改进**：P0-3 的数据权限目前只覆盖白名单内的 5 条分页查询（订单/工单/派单记录/安装记录/用户列表）。后续轮次新增查询时需把 mapper 加进 `SCOPED_STATEMENTS`，否则新端点无数据过滤——建议 ITERATION 9（智能派单 2.0）一并盘点。

**下一步（ITERATION 2）**：地址与网络资源管理（区域/小区/楼栋/单元/房间 + OLT/PON/ONU 台账 + 下单资源核查）。开工前同样先答五个问题。