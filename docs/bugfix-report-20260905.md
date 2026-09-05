# BBPMS 六项问题修复与回归测试报告

日期：2026-09-05

## 1. 项目分析

- 管理端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus。
- 客户端：独立 `bbpms-customer-h5`，Vue 3、Vant、Hash Router；客户业务使用 `/api/customer-portal/**` 自有数据接口。
- 后端：Spring Boot 3.2.5、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Redisson。
- 认证：登录签发 access/refresh token；JWT 携带用户、角色、权限和数据范围；后端使用 `@PreAuthorize` 做接口鉴权。
- RBAC：`sys_user -> sys_user_role -> sys_role -> sys_role_menu -> sys_menu`。`sys_menu` 同时承载目录、页面和按钮权限点。
- 订单状态机：`CREATED -> REJECTED -> CREATED`（驳回后重提）；`CREATED/REJECTED -> CANCELLED`；审核通过后进入派单、安装和完结流程。
- 工单状态机：`PENDING -> DISPATCHED -> ACCEPTED -> IN_PROGRESS -> COMPLETED`；转单回到 `PENDING`；改派经 `REASSIGNING` 回到 `DISPATCHED`。
- 数据库：本次涉及 `sys_user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu`、`installer_profile`、`broadband_order`、`work_order`、`work_order_timeline`、`dispatch_record`。

## 2. 复现与根因

六项问题均通过代码、初始化数据和前后端调用链静态复现。由于本机项目后端和 Redis 未运行，且现有 MySQL 不接受项目配置凭据，无法进行浏览器端写库复现。

| 编号 | 问题 | 静态复现 | 根因 | 涉及模块 |
| --- | --- | --- | --- | --- |
| 1 | 角色菜单为空 | 是 | `/menus/tree` 只查当前用户已绑定的目录/页面并排除了按钮；角色列表响应不含已选菜单，前端又从 `row.menus` 取值 | RBAC、角色页、菜单 API |
| 2 | 改派无法选人 | 是 | 候选人完全依赖易失的 Redis 在线集合；Redis 重启/未同步时结果为空；返回字段缺用户名、电话和状态；改派请求使用无校验 Map，负载未同步 | 派单、装维档案、工单 |
| 3 | cs1 登录后 403 | 是 | 登录页无条件使用陈旧 `redirect` 或 `/dashboard`，没有在用户权限加载后验证落地页；RBAC 变更缓存也可能未及时失效 | 登录、路由、RBAC 缓存 |
| 4 | customer1 登录后 403 | 是 | CUSTOMER 按最小权限被正确移除后台菜单并使用独立 H5，但管理端仍将其送往后台 `/dashboard` | 登录、客户门户、路由 |
| 5 | 重提/取消无响应 | 是 | 页面按状态展示按钮但不按权限展示；客服初始化数据缺 `order:cancel`；HTTP 403 在请求层静默拒绝；操作无 loading/catch；取消按钮状态范围还与状态机不一致 | 订单页、订单状态机、RBAC |
| 6 | 列表有待派而工作台为空 | 是 | 工单列表查询 `work_order.status=PENDING`，工作台却查询 `broadband_order.status=WAIT_DISPATCH`；自动/手动派单还把已存在的 PENDING 工单误判为已完成派单的幂等命中 | 工作台、派单、工单状态 |

## 3. 修复总结

| 编号 | 修复结果 | 自动化状态 | 实机 E2E |
| --- | --- | --- | --- |
| 1 | 管理接口返回完整菜单/按钮树；新增角色已选菜单 API；前端正确回显、保存多层树 | 菜单树单测通过，管理端构建通过 | 待可用后端/数据库复测 |
| 2 | 候选人以 MySQL 在岗、启用、容量和请假状态为准；返回姓名、用户名、电话、状态；改派校验负责人并同步时间线和双方负载 | 候选 DTO、改派服务单测通过 | 待可用后端/数据库复测 |
| 3 | 登录后先加载权限，再验证 redirect；无效落地页按当前权限选择；无权限 URL 仍进入 403 | 管理端类型检查/构建通过 | 待启动环境验证 cs1 |
| 4 | CUSTOMER 登录管理端时进入隔离的客户门户入口，不授予任何后台权限；客户 H5 保持自有 JWT/数据归属 API | 客户 H5 构建及客户门户既有单测通过 | 待启动环境验证 customer1 |
| 5 | 按钮同时按状态和权限显示；补客服取消权限；异步操作有 loading/success/error/finally；重提原子清空旧审核信息并写日志 | 订单状态机及 Service 单测通过 | 待可用数据库验证持久化 |
| 6 | 工作台与列表统一查询 `work_order`；PENDING 工单可被原地分配；订单、工单、时间线、负载、派单记录同步更新 | 待派分配 Service 单测通过 | 待可用数据库验证工作台 |

## 4. 关键实现

### 4.1 RBAC 与登录

- 菜单管理树查询所有未删除节点，包含 DIR、MENU、BUTTON，递归排序任意层级。
- 新增角色菜单 ID 查询；分配完成后清除 `user:auth` 缓存。
- 用户换角色、角色状态/菜单变化、菜单修改或删除时主动失效认证缓存。
- 登录落地页必须同时满足路由角色和权限；CUSTOMER 使用 `/customer-entry`，其他角色选择第一个有权页面。
- 侧边栏仍按权限过滤，路由守卫和后端 `@PreAuthorize` 均未关闭。

### 4.2 派单与改派

- `installer_profile.on_duty=1`、负载低于容量、`sys_user.status=1`、`user_type=INSTALLER` 且不在已批准假期内，才进入候选集。
- Redis 只保留在线/位置用途，不再作为候选人的唯一数据源。
- 已存在 `PENDING` 工单时，自动/手动派单更新原工单，不再错误地幂等返回，也不创建重复工单。
- 改派使用强类型请求，拒绝相同负责人和不可用负责人；状态记录 `原状态 -> REASSIGNING -> DISPATCHED`，同步旧/新装维负载。
- 转单通过显式 SQL 把 `installer_id` 和 `dispatch_time` 置空并回到 PENDING，避免 MyBatis-Plus 跳过 null 字段。
- 完成、取消、自动取消、强制关闭会释放装维负载。

### 4.3 订单操作

- `REJECTED -> CREATED` 使用带状态条件的原子 SQL，同时清空 `auditor_id/audit_time/audit_remark` 并增加 version。
- 取消按钮只在状态机允许的 CREATED/REJECTED 展示。
- 客服种子权限新增 `order:cancel`，既有数据库提供幂等修复脚本。
- 所有重提、取消、手动/自动派单和改派操作都给出失败原因，不再静默无响应。

## 5. API 修改

| 接口 | 方法 | 参数/响应变化 | 原因 |
| --- | --- | --- | --- |
| `/api/menus/tree` | GET | 返回完整管理树，含按钮节点 | 角色菜单不能只看当前用户的页面菜单 |
| `/api/roles/{id}/menu-ids` | GET | 新增；返回 `List<Long>` | 正确回显角色现有菜单 |
| `/api/dispatch/candidates` | GET | `orderId` 必填；新增可选 `excludeInstallerId`、`limit`；响应新增 `username/phone/status` | 支持真实候选选择和排除当前负责人 |
| `/api/work-orders/{id}/reassign` | POST | 请求体改为 `{newInstallerId, reason}`，Bean Validation 校验 | 避免无类型 Map 和参数不一致 |

订单重提、取消 API 地址和 HTTP 方法未改变，只修复权限、状态更新、错误处理和数据一致性。

## 6. 数据库修改

- 没有新增表或字段。
- `04-seed-data.sql`：客服角色新增菜单/权限点 102（`order:cancel`）。
- `07-customer-portal-schema.sql`：SUPER_ADMIN 同步获得后续新增的全部客户门户权限点；CUSTOMER 仍只有自助门户权限。
- 新增 `middleware/mysql/fix/fix-buglist-20260905.sql`：幂等修复既有库中的 cs1/customer1 角色关联、客服取消权限和超级管理员新增权限。
- 修复脚本未在本机数据库执行：3306 端口可达，但 `bbpms_app/bbpms_pwd_2026` 和 compose root 凭据均被 MySQL 5.7 拒绝。执行脚本后应重启后端或清理 `user:auth` Redis 缓存。
- 数据迁移风险低：脚本只有 `INSERT IGNORE ... SELECT`，不删除业务数据；客户后台权限仍由 07 初始化脚本按最小权限删除。

## 7. 修改文件

### 管理端

- `bbpms-admin-web/src/api/role.ts`：新增角色菜单 ID API。
- `bbpms-admin-web/src/views/role/list.vue`：加载完整树和已选项，处理 loading/error/save。
- `bbpms-admin-web/src/api/dispatch.ts`：候选参数、字段适配。
- `bbpms-admin-web/src/types/order.ts`：候选用户名和状态类型。
- `bbpms-admin-web/src/views/workorder/detail.vue`：真实候选、排除当前负责人、权限按钮和明确错误。
- `bbpms-admin-web/src/views/workorder/dispatch-board.vue`：改查工单 PENDING 数据源并使用 orderId 派单。
- `bbpms-admin-web/src/views/order/list.vue`：权限/状态按钮、loading、成功刷新、失败提示。
- `bbpms-admin-web/src/views/login/index.vue`：权限感知的安全落地页。
- `bbpms-admin-web/src/router/index.ts`：新增 CUSTOMER 专属入口路由。
- `bbpms-admin-web/src/env.d.ts`：客户门户 URL 配置类型。
- `bbpms-admin-web/src/views/customer-entry/index.vue`：隔离的客户门户入口。

### 后端

- `user/mapper/SysMenuMapper.java`、`user/service/SysMenuService.java`、`user/service/impl/SysMenuServiceImpl.java`、`user/controller/SysMenuController.java`：完整管理菜单树和缓存失效。
- `user/service/impl/RbacServiceImpl.java`：任意层级递归排序和字段映射。
- `user/service/SysRoleService.java`、`user/service/impl/SysRoleServiceImpl.java`、`user/controller/SysRoleController.java`：角色菜单读取、保存及缓存失效。
- `user/service/impl/SysUserServiceImpl.java`：用户换角色后失效认证缓存。
- `user/service/InstallerProfileService.java`、`user/service/impl/InstallerProfileServiceImpl.java`：MySQL 可接单装维查询。
- `dispatch/dto/InstallerDTO.java`、`dispatch/dto/CandidateDTO.java`、`dispatch/algorithm/DispatchScoringService.java`：候选身份和状态字段。
- `dispatch/service/DispatchService.java`、`dispatch/controller/DispatchController.java`、`dispatch/service/impl/DispatchServiceImpl.java`：候选过滤、PENDING 原地派单、记录和锁生命周期。
- `order/mapper/BroadbandOrderMapper.java`、`order/service/impl/OrderServiceImpl.java`：重提原子更新和审核字段清理。
- `common/statemachine/WorkOrderStateMachine.java`：补充 ACCEPTED 回待派池状态迁移。
- `workorder/service/WorkOrderService.java`、`workorder/service/impl/WorkOrderServiceImpl.java`：待派分配、改派、转单、状态和负载一致性。
- `workorder/mapper/WorkOrderMapper.java`：显式返回待派池 SQL。
- `workorder/controller/WorkOrderController.java`、`workorder/dto/WorkOrderReassignReq.java`：强类型改派请求。
- `workorder/dto/WorkOrderCreateReq.java`：更新 PENDING 语义文档。

### 测试与数据

- `common/statemachine/OrderStateMachineTest.java`：重提/取消合法和非法状态。
- `common/statemachine/WorkOrderStateMachineTest.java`：ACCEPTED 返回待派池。
- `dispatch/algorithm/DispatchScoringServiceTest.java`：候选身份/状态字段。
- `order/service/impl/OrderServiceImplTest.java`：重提原子更新、取消持久化和事件。
- `user/service/impl/RbacServiceImplTest.java`：三层菜单和按钮排序。
- `workorder/service/impl/WorkOrderServiceImplTest.java`：待派分配、改派负责人、时间线和负载。
- `middleware/mysql/init/04-seed-data.sql`、`middleware/mysql/init/07-customer-portal-schema.sql`、`middleware/mysql/fix/fix-buglist-20260905.sql`：权限初始化与既有库修复。

## 8. 测试结果

| 测试 | 结果 | 状态 |
| --- | --- | --- |
| 后端 `mvn test` | 54 tests，0 failures，0 errors，0 skipped | 已验证 |
| 后端 `mvn -DskipTests compile` | BUILD SUCCESS | 已验证 |
| 管理端 `npm run build` | `vue-tsc --noEmit` + Vite BUILD SUCCESS | 已验证 |
| 客户 H5 `npm run build` | `vue-tsc --noEmit` + Vite BUILD SUCCESS | 已验证 |
| `git diff --check` | 无 whitespace error；只有工作区既有 LF/CRLF 提示 | 已验证 |
| MySQL/Redis/后端联调 | MySQL 端口可达但项目凭据被拒；Redis 6379 和后端 8080 未启动 | 无法验证 |
| cs1/customer1 浏览器登录、角色菜单写库、派单/改派/取消端到端 | 依赖上述完整运行环境 | 部分验证（代码、单测、构建已通过） |

## 9. 部署后必须执行的最终 E2E 清单

1. 对既有数据库执行 `middleware/mysql/fix/fix-buglist-20260905.sql`，启动 Redis 和后端。
2. admin：检查角色菜单树和原有勾选；修改角色后重新登录验证缓存失效。
3. cs1：确认落到有权限页面，只显示客服菜单；验证驳回、重提、取消及刷新后状态。
4. customer1：从管理端登录时进入客户门户入口；在 9003 客户 H5 登录，只能读取绑定客户数据；直接访问后台 URL 仍为 403。
5. disp1：工单列表与派单工作台的 PENDING 数量和行数据一致；选择在岗装维后变为 DISPATCHED。
6. 改派：下拉显示姓名、用户名/工号、状态；排除当前负责人；提交后负责人、两条时间线、双方 workload 和派单记录一致。
