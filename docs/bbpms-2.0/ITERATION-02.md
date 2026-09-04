# BBPMS 2.0 — ITERATION 2 实施报告（地址与网络资源管理）

> 产出时间：2026-09-03 · 前置：ITERATION 0（审计基线）→ ITERATION 1（P0-1/P0-2/P0-3，commit `dde3486`/`28f15f9`/`729817a`/`a474757`）
> 范围：区域/小区/楼栋/单元/房间台账 + OLT/PON/ONU 设备台账 + 下单资源核查三态
> 验证：资源模块端到端 19/19 通过 · 数据权限回归 9/9 通过 · 前端类型检查与生产构建通过

---

## 0. 开工前的五个问题

| # | 问题 | 结论 |
|---|------|------|
| Q1 | 当前系统是否已具备本轮功能的一部分？ | **极少，属新建模块**。地址字段仅以 `broadband_order.install_address` 自由文本存在，无结构化台账；无设备（OLT/PON/ONU）概念；无资源核查逻辑。本轮为全新领域模型。 |
| Q2 | 现有代码哪些可以复用？ | **基础设施全部复用**：MyBatis-Plus BaseMapper/BaseDO 模式、`R<T>` 统一响应、`BizException`/`GlobalExceptionHandler`、`@PreAuthorize` 权限体系、前端 `PermissionButton`/`api` 封装/动态路由。资源模块本身全部新建。 |
| Q3 | 数据库是否需要修改？ | **是，但向后兼容**。新建 8 张 `net_*` 资源表（幂等 `CREATE TABLE IF NOT EXISTS`）；`broadband_order` 加 3 列 `room_id`/`resource_status`/`check_remark`（`_r_add_col_if_missing` 存储过程，非破坏）。**过程中发现并修复 1 个 schema 缺口**：新表漏建 `version` 列（BaseDO 乐观锁字段），已补并幂等对齐（详见 §3）。 |
| Q4 | API 是否需要修改？ | **新增而非改语义**。新增 `/api/resources/*` 组（check + 8 类台账 list/create）；订单创建 `OrderCreateReq` 增加 3 个可选字段回写；`OrderVO` 增加 3 个只读展示字段。既有端点行为不变。 |
| Q5 | 是否会影响已有功能？ | **已做防护**：① 资源核查为独立端点，不侵入订单创建主流程（带 roomId/resourceStatus 才回写，缺省仍可下单）；② 新权限码 `resource:view`/`resource:edit` 独立挂菜单 400/401/402，不影响既有权限矩阵；③ 顺带修复 1 个既有 SQL 歧义 bug（详见 §3）。 |

## 1. 本轮交付

### 1.1 领域模型与 SQL（middleware/mysql/init/06-resource-schema.sql）

```
地址资源血缘：net_region 1─N net_community 1─N net_building 1─N net_unit 1─N net_room
设备资源血缘：net_region 1─N net_olt 1─N net_pon；net_room 1─N net_onu（安装后绑定 room_id）
```

- `net_region`（区域，如"北京市"）/ `net_community`（小区，含 address/lat/lng/grid_code）
- `net_building`（楼栋，含 total_floors）/ `net_unit`（单元）/ `net_room`（房间，含 `is_installed` 可装标记）
- `net_olt`（OLT，含 ip/vendor/model）/ `net_pon`（PON 口，含 total_ports/used_ports）/ `net_onu`（ONU，含 sn/status：IN_STOCK|INSTALLED|FAULT|RETIRED）
- `broadband_order` 补列：`room_id`(BIGINT)、`resource_status`(VARCHAR(16))、`check_remark`(VARCHAR(255))
- 演示数据：区域 1、小区 3（覆盖三种业务形态）、楼栋 3、单元 4、房间 4（101 可装 / 102 已装 / 201 可装 / 301 可装）、OLT 1、PON 2、ONU 2（1 库存 + 1 已装 102）

### 1.2 后端资源模块（bbpms-app …/resource/，全部新建）

- **entity/mapper**：8 个实体（均 extends BaseDO）+ 8 个 BaseMapper 接口
- **dto/vo**：`ResourceCheckReq`（address 必填、roomNo 可选）、`ResourceCheckResp`（status/message/各级 ID 与名称/roomId/roomNo）、8 个台账 VO
- **service**：`ResourceAdminService`（8 类台账 list/create，create 带 @Transactional 与重名校验）、`ResourceCheckService`（地址解析核查）
- **controller**：`/api/resources/*`，`check` → `order:create`；台账 list → `resource:view`；台账 create → `resource:edit`

**资源核查规则**（ResourceCheckServiceImpl）：
```
逆向逐级匹配：小区（名称/注册地址最长命中）→ 楼栋（likeRight）→ 单元（likeRight，可缺省按首单元）→ 房号（精确）
判定：
  房号存在且未安装      → RESOURCE_OK（可下单安装）         [例：…1号楼1单元101]
  房号已安装/无此房号    → RESOURCE_INSUFFICIENT（资源不足）  [例：…1号楼1单元102 / …2号楼1单元999]
  小区/楼栋不在册        → NO_COVERAGE（暂无覆盖）           [例：上海市未知路9号…]
```

### 1.3 订单回写（订单模块小幅扩展）

- `OrderCreateReq` / `BroadbandOrder` / `OrderServiceImpl.create()`：下单时回写 `roomId`/`resourceStatus`/`checkRemark`
- `OrderVO`：补 3 个只读字段（详情/列表页展示核查结果）

### 1.4 前端（bbpms-admin-web）

- `src/api/resource.ts`：checkResource + 8 类台账 list/create API 与 TS 类型
- `src/router/index.ts`：新增 `/resource` 路由（权限 `resource:view`）
- `src/views/resource/index.vue`：双面板台账页 —— ① 地址资源（区域→小区→楼栋→单元→房间四级联动 + 新增对话框）② 设备台账（OLT→PON→ONU 联动 + 新增对话框），编辑按钮用 `PermissionButton` 按 `resource:edit` 门控
- `src/views/order/create.vue`：地址表单项新增"资源核查"按钮 → 调用 `checkResource` → 三态结果以彩色 el-tag 展示 → 已通过时回写 `roomId/resourceStatus/checkRemark` 到创建请求

### 1.5 菜单与权限（middleware/mysql/init/04-seed-data.sql）

- 菜单 400 Resource（`/resource`，`resource:view`）、401 Resource List（`resource/index`）、402 Resource Edit Button（button 级，`resource:edit`，独立 7 列 INSERT 与目录/页面级区分）

## 2. 验证矩阵

### 2.1 资源模块端到端（docs/bbpms-2.0/verify-resource.cjs）— **19/19 通过**

| 分组 | 用例 | 期望 | 结果 |
|---|---|---|---|
| [A] 核查三态 | 建国路1号 1号楼1单元101 | RESOURCE_OK（可安装） | ✅ |
| | 建国路1号 1号楼1单元102 | RESOURCE_INSUFFICIENT（已安装） | ✅ |
| | 建国路1号 2号楼1单元999 | RESOURCE_INSUFFICIENT（无此房号） | ✅ |
| | 上海市未知路9号 1号楼1单元101 | NO_COVERAGE（小区不在册） | ✅ |
| [B] 台账列表 | 区域/小区/楼栋/单元/房间/OLT/PON/ONU 8 组 | 200 且数据符合 seed | ✅ |
| [B] 新增 | 新增房间 105 | 200 创建成功 | ✅（多轮运行幂等，无重复） |
| [C] 订单回写 | 创建订单（带 roomId=1/RESOURCE_OK） | 200 | ✅ |
| | GET 详情 order.roomId | =1 | ✅ |
| | GET 详情 order.resourceStatus | =RESOURCE_OK | ✅ |
| | GET 详情 order.checkRemark | 含"资源核查通过" | ✅ |
| [D] 权限隔离 | cs1 访问 /api/resources/regions | 403（无 resource:view） | ✅ |
| | 未登录访问 /api/resources/regions | 401 | ✅ |

### 2.2 回归（verify-datascope2.cjs）— **9/9 通过**

P0-1 审核状态机、P0-2 权限注解、P0-3 数据权限 ALL/DEPT/DEPT_AND_CHILD/SELF 全部保持通过，无回归。

### 2.3 前端构建

- `vue-tsc --noEmit`：**通过（exit 0）**
- `vite build`：**通过（✓ built in 13.75s）**（注：沙箱对 dist 清空的批量删除有保护，构建前需先移走旧 dist，属环境限制非代码问题）

## 3. 过程中发现并修复的既有问题（2 个，均非本轮需求引入）

1. **新表缺 `version` 列（500 Unknown column 'version'）**：`BaseDO` 声明 `@Version` 乐观锁字段，MyBatis-Plus `selectList` 自动 SELECT `version`，而 8 张新表建表时漏了该列 → 台账列表全 500。已修复：建表语句补列 + `_r_add_col_if_missing` 对运行库幂等补列（与既有业务表 `sys_user`/`broadband_order` 的 `version` 列对齐）。
2. **`WorkOrderTimelineMapper.xml` selectByOrderId 列名歧义（订单详情 500）**：`SELECT t.<include …/>` 仅 `id` 带前缀，JOIN `work_order` 后 `create_by` 等列歧义 → `Column 'create_by' in field list is ambiguous`。属既有死代码缺陷（此前订单详情页功能正常未被触发，本次验证脚本调 `GET /api/orders/{id}` 暴露）。已修复：展开字段列表全部加 `t.` 前缀。

## 4. 兼容性与风险说明

- **核查不阻塞下单**：`/check` 是前置建议，订单创建接口不强制 `resourceStatus`；无 roomId 时订单照常创建（向后兼容既有流程）。
- **地址解析是演示级规则**：正则匹配 `数字(号楼|栋|座|幢)` / `数字(单元|门)` / `\d{2,4}` 房号，面向演示数据形态；真实地址门牌号（"建国路1号院"）需在后续迭代扩展。`matchCommunity` 采用"名称/注册地址最长命中"，命中度为 0 时返回 null（**已修复初版 `len > bestLen` 在全部未命中时误选第一个小区的边界 bug**）。
- **新权限码独立**：`resource:view`/`resource:edit` 仅挂给 admin 与后续需授权的角色（seed 中 `sys_role_menu` 绑定 admin）；不影响既有权限矩阵。
- **`net_*` 表未纳入数据权限拦截器白名单**：台账为系统管理员维护型数据，不与订单/工单同权限域（create_by=seed NULL 全可见）。如后续要求区域/小区维度的数据权限，需将对应 mapper 加入 `SCOPED_STATEMENTS` 并设计注入列。

## 5. 复盘与下一步

**做得好的**：① 新建模块零侵入——只扩了订单 DTO/实体/VO 三个文件的三个字段，其余全部新文件；② 验证先于报告，19 个用例真实登录 + 真实 DB，三态、CRUD、回写、权限隔离全有证据；③ 借验证脚本顺手挖出并修复了 2 个存量缺陷（version 列缺失、XML 列歧义），并固化进 schema 脚本防止复现。

**可改进**：地址解析目前是"匹配制"而非"标准化"——用户输入任何不在册格式都会落到 NO_COVERAGE 或 INSUFFICIENT，真实项目应引入地址标准化服务或前端反查小区后回填结构化 ID。另外 `check` 的幂等仅靠唯一房号约束，后续可加"资源预占"（check 通过后冻结房间 N 分钟，超时释放）防止同一房间并发下单，建议排入 ITERATION 3+。

**下一步（ITERATION 3）**：按 IMPROVEMENT-BACKLOG 推进（待用户确认优先级；候选：资源预占/并发防重、CUSTOM 数据权限、装维 App 端资源联动等）。