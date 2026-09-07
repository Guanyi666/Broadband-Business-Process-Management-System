# BBPMS 当前 UI/UX 与信息架构问题分析

> 阶段：Phase 1（全局代码审查，**本阶段未修改任何代码**）
> 审查日期：2026-09-06
> 审查对象：`bbpms-admin-web`（管理端，Vue 3 + TS + Element Plus + Pinia）
> 审查方法：通读布局/路由/状态/组件/全部 35 个页面 + 后端菜单与看板接口 + `sys_menu` 种子数据

---

## 0. 审查前提：先看清「已具备什么」

在挑问题之前，必须先确认哪些能力**已经存在但没接上**——这决定了后续改造是「新建」还是「接线」。

| 能力 | 现状 | 证据 |
|---|---|---|
| 后端按角色返回菜单树 | ✅ 已实现 | `AuthController.java:100-104` → `menuService.getMenuTreeByUserId(userId)` |
| 角色↔菜单关联数据 | ✅ 已配置（超管/客服/审计/调度员等） | `04-seed-data.sql:185-204` `sys_role_menu` |
| 前端获取菜单的封装 | ✅ 已实现但**无人调用** | `stores/auth.ts:47-51` `fetchMenus()`；`api/auth.ts:48` `/auth/menus` |
| 菜单权限前端过滤 | ✅ 已实现（基于硬编码 `allMenus`） | `AdminLayout.vue:39-65` |
| 路由级权限守卫 | ✅ 已实现（role + permission） | `router/index.ts:409-419` |
| Tab「关闭其他/全部」 | ✅ 已实现但**未暴露 UI** | `stores/app.ts:33-39` |
| 状态中文+颜色唯一真源 | ✅ 已实现 | `BBPMSStatusTag.vue:28-95` |
| 看板模块独立降级+重试 | ✅ 已实现 | `dashboard/index.vue:185-189, 268-271, 298` |
| 请求层统一错误提示 | ✅ 已实现（401/403/500/网络） | `utils/request.ts:51-80` |

**核心结论**：系统底座比预估的好——权限、降级、状态真源都已就位。真正的短板不在「没有」，而在 **「有但没接、有但双份、有但没统一」**。

---

## 1. 全局布局问题

**现状**：`Sidebar(220px) + Header(56px) + TagsView(34px) + Main`，结构本身合理。

| 问题 | 位置 | 说明 |
|---|---|---|
| 侧边栏配色硬编码 | `AdminLayout.vue:133` `background-color="#001529"` | 已有 `$sidebar-bg` 变量（`variables.scss:6`）却未使用，暗色侧栏 + 白色 Header + 灰底内容区缺乏统一关系 |
| 通知角标硬编码假数据 | `AdminLayout.vue:166` `:value="3"` | 数字写死，与真实未读数无关——属于「假数据」红线 |
| Header 无全局搜索 | `AdminLayout.vue:159-182` | 只有全屏/通知/用户，缺少「搜订单号/工单号/客户」的全局入口 |
| 无回到顶部 / 无页脚 | `AdminLayout.vue:199-207` | 长页面（看板）滚动后无快速回顶 |
| 未见暗色主题变量 | `variables.scss` 全文 | 仅一套颜色 token，而 `profile/index.vue` 中存在 `data-theme` 切换写入（`onThemeChange`），主题切换实际无样式支撑 |

---

## 2. 导航问题（**最高优先级**）

### 2.1 菜单与路由「双源」——信息架构的根因

菜单**不是**从路由生成的，而是 `AdminLayout.vue:226-330` 里一份**硬编码数组 `allMenus`**，与 `router/index.ts` 完全独立维护。

- 已在 `209a73b` 提交踩过一次坑（套餐资源路由已注册但菜单无入口）；
- 任何新增页面都要同时改两处，漏改即「路由能访问但菜单看不见」或反之。

### 2.2 后端动态菜单「有能力、没接线」

后端 `/api/auth/menus` + `sys_role_menu`（按角色配置）已完备，前端 `auth.fetchMenus()` 也封装好了，但 `AdminLayout` **从未调用**，仍用硬编码数组做前端过滤。

这意味着：菜单权限实际由**前端硬编码的 `perms`** 决定，与数据库里 `sys_role_menu` 的真实配置**可能不一致**——管理员在「菜单管理」里改了配置，前端菜单不会变。

### 2.3 切动态菜单的三道前置障碍（已核实）

`sys_menu` 数据（`04-seed-data.sql:30-108`）存在三个必须先行解决的问题：

1. **菜单名是英文**：`(1, 0, 'Dashboard', ...)`、`'Customer List'`、`'Create Order'` —— 直接切换会导致侧边栏变英文；
2. **含前端不存在的页面**：`attendance/My`、`attendance/ClockIn`、`leave/MyApplications`、`leave/Apply`、`leave/Calendar`，而前端实际只有 `attendance/Report.vue`、`leave/Approval.vue`、`sla/Expiring.vue` —— 直接切换会产生死链（点开 404/空白）；
3. **图标命名体系不同**：DB 用 `'dashboard'/'peoples'/'documentation'`（vue-element-admin 风格），前端用 `'DataLine'/'Avatar'/'Document'`（Element Plus）—— 需要映射表。

### 2.4 菜单分组未按业务域组织

当前一级菜单平铺 12 个（数据看板/客户/订单/工单/资源/装维/系统/通知/文件/日志/考勤/请假/SLA），**未体现「客户 → 订单 → 工单 → 装维」的业务链路**，用户感知是「功能集合」而非「业务系统」。

---

## 3. 页面层级问题

- **面包屑无业务标识**：`AdminLayout.vue:24-29` 由 `route.matched` 生成，订单详情只显示「订单管理 / 订单详情」，不显示单号；
- **`PageHeader` 的 `breadcrumb` prop 不存在**：`attendance/Report.vue:3`、`leave/Approval.vue:3`、`sla/Expiring.vue:3` 都传了 `:breadcrumb`，但 `PageHeader.vue:1-8` 只接收 `title/description/icon` → 静默失效；
- **页面容器不统一**：多数页面 `PageHeader + app-card + page-toolbar`，但 `attendance/Report.vue`、`leave/Approval.vue`、`sla/Expiring.vue` 自写 `padding:16px`；
- **详情页无返回入口**：`PageHeader` 无 `back` 能力，只能依赖浏览器返回。

---

## 4. 交互逻辑问题

| 问题 | 位置 | 说明 |
|---|---|---|
| Tab 标题重复 | `AdminLayout.vue:96-107` + `stores/app.ts:20-23` | 按 `path` 去重（正确），但 title 取静态 `meta.title`，打开 3 个订单详情就是 3 个「订单详情」 |
| Tab 无右键菜单 | `AdminLayout.vue:185-197` | `removeOtherViews/removeAllViews` 已实现却没接到 UI；缺「关闭左侧/右侧」 |
| `keep-alive` 全量缓存 | `AdminLayout.vue:200-206` `:key="r.fullPath"` | 路由里定义了 `meta.keepAlive`（`router/index.ts:107`）但 `keep-alive` 没用 `include`，实际缓存所有页面 |
| 操作后状态不联动 | `order/list.vue:135-144` | 审核/取消后调用了 `fetchData()`（✅ 正确）；但 `create.vue:124` 提交后直接跳详情，**无成功提示** |
| 危险确认不统一 | `order/list.vue:133` vs `workorder/detail.vue:68` | 订单取消只有一句「确定取消订单 XXX 吗？」，无后果说明；而工单取消**强制填原因**、改派**强制原因+候选面板**（✅ 好范例，应推广到订单侧） |
| 订单驳回无二次确认 | `order/audit.vue:36-46, 83` | 点「确认」即生效，无 `ElMessageBox` |
| 列表页无批量操作 | 全部列表页 | 无批量审核/批量派单/批量取消 |

---

## 5. 信息密度问题

- **列表页缺「统计摘要」**：仅 `dispatch-board.vue:139-144` 有统计卡、`sla/Expiring.vue:5` 用 alert 显示计数，其余列表（订单/工单/客户/装维/系统）**均无「全部 128 / 待审核 5 / 处理中 30」这类摘要带**；
- **看板密度合理**（`dashboard/index.vue` 三段式，✅ 好）；
- 详情页信息层级不统一：有的先概览后流程，有的直接堆字段。

---

## 6. 状态展示问题（**系统性问题**）

### 6.1 唯一真源已存在，但被绕开

`BBPMSStatusTag.vue:28-95` 设计良好：`STATUS_TEXT` 中文映射 + `map` 颜色映射 + 兜底链 `props.type || typeMap?.[status] || map[status] || 'info'`。

### 6.2 两套重复 STATUS_MAP 且**颜色冲突**

| 状态 | 真源颜色 | `sla/Expiring.vue:85-95` | 冲突 |
|---|---|---|---|
| `ACCEPTED` | primary | warning | ⚠️ |
| `IN_PROGRESS` | warning | success | ⚠️ |
| `STALLED` | danger | warning | ⚠️ |
| `AUTO_CANCELLED` | info | danger | ⚠️ |

另有 `attendance/Report.vue:107-112` 一套（ON_DUTY/ON_BREAK/OFF_DUTY/AUTO_OFF）。

→ 同一状态在不同页面颜色不同，正是需求中明令禁止的情况。

### 6.3 大量页面硬编码 `:type` 不走组件

`user/list.vue:172`、`role/list.vue:151`、`dept/list.vue:88`、`notify/template.vue:106`、`resource/package.vue:187`、`customer/detail.vue:94`、`installer/map.vue:123`、`notify/record.vue:98`、`log/login.vue:67`、`log/operation.vue:66`、`order/create.vue:192`。

### 6.4 真源覆盖不全

缺失：资源 `IN_STOCK`、客户自助 `SUBMITTED/PROCESSING/RESOLVED`、考勤 `ON_DUTY/OFF_DUTY/AUTO_OFF`、请假状态、通知发送状态 → 这些地方只能显示原始英文。

### 6.5 已确认的功能性 Bug

`log/operation.vue:66`：`row.status === 1` 但后端返回 status 为字符串 → **状态标签恒显示「失败」**（真 Bug，非样式问题）。

---

## 7. 权限展示问题

- ✅ 路由守卫已做（`router/index.ts:409-419`），无权限跳 `/403`；
- ✅ 菜单已做前端过滤（`AdminLayout.vue:39-65`）；
- ✅ `PermissionButton` 按钮级权限已存在；
- ❌ **但菜单权限源是硬编码 `perms` 而非后端 `sys_role_menu`**（见 2.2），存在双源不一致风险；
- ❌ `/system` 菜单用 `roles: ['SUPER_ADMIN']` 硬编码角色（`AdminLayout.vue:279`、`router/index.ts:205`），而其它模块用 permission——**权限模型混用**（role-based 与 permission-based 并存）。

---

## 8. 订单业务流程问题

| 问题 | 位置 |
|---|---|
| 详情页工单号**不可跳转**到工单 | `order/detail.vue:151` |
| 空态英文 `Not dispatched yet` | `order/detail.vue:149` |
| 审核页状态显示原始枚举 `{{ order.status }}`，未走中文映射 | `order/audit.vue:59` |
| 驳回无二次确认 | `order/audit.vue:36-46` |
| 列表无统计摘要、无批量操作 | `order/list.vue` |
| 取消仅简单 confirm，无后果说明 | `order/list.vue:133` |
| `fetchData` 无 catch，失败静默 | `order/list.vue:70` |
| 创建订单提交后无成功提示直接跳详情 | `order/create.vue:124` |

✅ 做得好：客户手机号脱敏（`order/detail.vue:140`）、双轨时间线（`OrderTimeline`）、操作后自动刷新（`order/list.vue:135-144`）、权限门控。

---

## 9. 工单业务流程问题

| 问题 | 位置 |
|---|---|
| 详情页订单 ID **不可跳转**到关联订单 | `workorder/detail.vue:142` |
| 列表 `placeholder="Installer ID"` 英文 | `workorder/list.vue:100` |
| 列表无操作列，改派/取消必须进详情 | `workorder/list.vue` |
| 订单 ID 纯文本不可跳转 | `workorder/list.vue:109` |
| 完成仅简单 confirm 无后果说明 | `workorder/detail.vue:59` |

✅ 做得好：**`dispatch-board.vue` 是全系统最「流程化」的页面**——统计卡（L139-144）+ 看板 + 空态（L162/187）+ 派单确认带装维姓名（L79/98）+ 30s 轮询（L123）+ 订单转派深链自动开弹窗（L117）。**建议以它为模板统一其余页面**。

✅ 工单取消强制填原因（`workorder/detail.vue:68`）、改派强制原因 + 候选决策面板（L75-120）——应推广为全系统危险操作标准。

---

## 10. 数据看板问题

✅ **已做得好**（超出预期）：三段式布局（今日态势 / 流程健康 / 待办与风险）、每模块独立 loading/error/empty + 重试、权限门控（无权限不发请求）、空态文案场景化、流程节点可下钻（`openProcessNode` L147-157 带 `status` query）、洞察文案自动指出堵点（`BusinessProcessMetro.vue:insight`）。

| 问题 | 位置 | 说明 |
|---|---|---|
| **KPI 卡片不可点击** | `BBPMSKpiCard.vue:13-23` | 组件无 `click`/`to` 事件，8 张 KPI 卡全部不可钻取——需求第 27 条「所有数字可点击」未满足 |
| **无「待办中心」** | `dashboard/index.vue:367-424` | 只有「SLA 临期预警」+「最新工单动态」，缺「待我处理」聚合（超时工单 / 待审核订单 / 待接单 / 预约到期）+「查看全部」入口 |
| 流程节点缺关键指标 | `BusinessProcessMetro.vue:40-80` | 节点只有 `count` + 相对压力色，缺需求要求的**平均等待时长**、**异常数量** |
| 缺履约时效指标 | 看板整体 | 需求要求的「平均处理时长 / 平均派单时长 / 平均接单时长」仅以「派单时效分布」图表间接体现 |

---

## 11. 表格问题

- `BBPMSTable.vue` 内建 `v-loading`(L67) + 分页(L98-108)，但**无 Empty/Error 层**，且**仅 2 个页面使用**（大量列表页仍手写 el-table）；
- 列头英文：`BBPMSTable.vue:91` `label="Action"` → 影响 `leave/Approval.vue`、`sla/Expiring.vue`；
- 操作列无「更多」折叠：`customer-portal/operations.vue:128-134` 一行堆 4 个按钮（受理/处理中/解决待确认/驳回）；
- 列表页重复代码严重：`loading/list/total/query/fetchData/onSearch/onReset` + 弹窗 CRUD ≈ 10 处复制，建议抽 `useCrudPage` 组合式函数。

---

## 12. 详情页问题

- 无统一「概览 → 流程 → 详情」骨架，各页自行组织；
- **订单↔工单单向断裂**：订单详情看得到工单号但跳不过去；工单详情看得到订单 ID 但跳不过去 → 需求第 15 条的「客户↔订单↔工单↔装维」闭环未形成；
- 详情页无返回按钮；
- `customer/detail.vue:90`、`installer/map.vue:114` 已用中文 `el-empty`（✅）。

---

## 13. 弹窗问题

✅ **无弹窗套弹窗**（已专项排查，`el-dialog` 内无嵌套 `el-dialog`，`ElMessageBox` 仅在回调中调用）。

- 新增/编辑**全部用 `el-dialog`，无 Drawer**；
- 表单形态不一致：`customer/create.vue` 是整页表单，其余是弹窗（需求第 20 条建议复杂业务用独立页面/大 Drawer，`customer/create` 反而是对的）；
- 无统一的操作确认组件（危险操作确认文案各自手写）。

---

## 14. 空状态问题

- ✅ `BBPMSPanel`/`BBPMSChart` 默认 `暂无数据`，看板传场景化文案（暂无趋势数据/暂无派单记录/暂无临期工单…）；
- ❌ 英文未翻译：`order/detail.vue:149` `Not dispatched yet`、`installer/map.vue:119` `No installer locations`、`user/list.vue:173` `Enabled/Disabled`；
- ❌ 多数列表依赖 el-table 默认空态，无场景化文案（如「当前筛选条件下暂无订单，试试重置筛选」）。

---

## 15. 加载状态问题

- **两种方式并存**：`BBPMSPanel`/`BBPMSChart`/`BBPMSKpiCard` 用 skeleton；`BBPMSTable` 与多数页面用 `v-loading`；部分页面直接空白；
- 无统一的页面级 Loading/Skeleton 规范。

---

## 16. 错误处理问题

- ✅ `utils/request.ts` 拦截器已统一：业务 msg(L51)、403 静默(L60-65)、401 登录过期(L80)、500 中文(L67)、网络异常(L69)；
- ❌ 页面层重复 `try/catch` + `ElMessage.error`；
- ❌ `leave/Approval.vue:103` `catch { /* noop */ }` **吞掉异常**，用户无任何反馈；
- ❌ 多数列表 `fetchData` 无失败 toast（如 `customer/list.vue:21` 仅 `finally`），错误静默；
- ❌ 除 `BBPMSPanel` 外，页面级「加载失败 + 重试」UI 未统一。

---

## 17. 中文化问题（**散落最广**）

已逐条定位的英文硬编码：

| 文件:行号 | 原文 |
|---|---|
| `user/list.vue:121` | `Delete user ${row.username}?` |
| `user/list.vue:136` | `Roles updated` |
| `user/list.vue:173` | `Enabled` / `Disabled` |
| `user/list.vue:182` | `Roles`（列头） |
| `user/list.vue:207` | `label="Password"` |
| `user/list.vue:240` | `Assign Roles -` |
| `notify/template.vue:74` | `Delete template ${row.code}?` |
| `notify/template.vue:93` | `placeholder="Code / Subject / Content"` |
| `notify/template.vue:141` | `label="In-app"` |
| `notify/record.vue:51` | `Template parameters must be valid JSON` |
| `notify/record.vue:126` | `placeholder="e.g. ORDER_CREATED"` |
| `installer/map.vue:76-78` | InfoWindow `Phone:`/`Workload:`/`Rating:` |
| `installer/map.vue:119` | `empty-text="No installer locations"` |
| `menu/list.vue:102` | `['Dir','Menu','Button'][row.type-1]` |
| `resource/index.vue:293` | `{{ row.status }}` 裸显 `IN_STOCK` |
| `customer-portal/operations.vue:24,95,127,144` | 状态筛选与列裸显 `SUBMITTED` 等 |
| `workorder/list.vue:100` | `placeholder="Installer ID"` |
| `BBPMSTable.vue:91` | `label="Action"` |
| `BBPMSUpload.vue:33,80` | `File X exceeds XMB` / `Select File` |
| `BBPMSMapPicker.vue:94` | `Map disabled (AMap key missing)` |
| `order/detail.vue:149` | `Not dispatched yet` |

---

## 18. 响应式问题

- **无布局级适配**：全局 `@media` 仅存在于 5 个组件内部（`OrderTimeline` 760px、`DispatchDecisionPanel` 720px、`BusinessProcessMetro` 1280/720、`dashboard` 1439/1023、`order/detail` 1280），**侧边栏/Header/TagsView 无任何断点**；
- `stores/app.ts` 有 `device: 'desktop' | 'mobile'` 但**从未被使用**；
- 窄屏下 `el-table` 仅靠自身横向滚动，无卡片化降级；
- 侧边栏折叠是手动点击，无窄屏自动收起/抽屉模式。

---

## 19. Design Token 问题

`variables.scss` 现有 token **仅覆盖**：5 个语义色、侧边栏色、3 个高度、内容底色、字体族/字号、1 个圆角、2 个阴影。

**缺失**（需求第 24 条要求）：

- 背景/表面/边框层级：`--bg-base` / `--surface` / `--border` / `--divider`
- 文本层级：`--text-primary` / `--text-secondary` / `--text-disabled`
- 间距阶梯（4/8/12/16/24/32）、字号阶梯（12/13/14/16/18/20/24）
- 圆角阶梯、阴影阶梯
- 暗色主题变量

且存在硬编码散落：`global.scss` 中 `#303133`、`#909399`、`#fff` 直接写死。

---

## 20. 问题优先级矩阵

### P0-阻断级（影响「系统是否可信」）

1. **菜单双源 + 动态菜单未接线**（§2.1/2.2）——信息架构根因，且存在权限双源不一致风险
2. **状态两套映射且颜色冲突**（§6.2）——同一状态跨页颜色不同
3. **中文化散落 20+ 处**（§17）——含用户可见的 `Enabled/Disabled/Action/Delete user ?`
4. **订单↔工单无法互跳**（§8/§9/§12）——业务链路断裂，需求核心
5. **`log/operation.vue:66` 状态恒显示「失败」**（§6.5）——功能性 Bug
6. **通知角标硬编码 3**（§1）——假数据红线

### P1-体验级（影响「是否好用」）

7. Tab 标题无业务标识 + 无右键菜单（§4）
8. 看板 KPI 不可钻取 + 无待办中心（§10）
9. 列表页无统计摘要 + 无批量操作（§5/§11）
10. 危险操作确认不统一（订单侧缺，工单侧已有好范例）（§4）
11. 空态/加载/错误三态未统一（§14/§15/§16）
12. `PageHeader` 缺 `breadcrumb`/`back`，3 个页面传了不存在的 prop（§3）

### P2-打磨级

13. Design Token 补全（§19）
14. 列表页重复逻辑抽 `useCrudPage`（§11）
15. 响应式布局级适配（§18）
16. 菜单按业务域重新分组（§2.4）

---

## 21. Phase 2 建议方向（待确认）

1. **《BBPMS UI Design System》**：补全 Design Token（层级色/文本/间距/字号/圆角/阴影），定义 Button/Tag/Table/Form/Dialog/Drawer/Empty/Loading 规范，统一状态色板（以 `BBPMSStatusTag` 为唯一真源并扩充覆盖）。
2. **状态字典统一方案**：删除 `sla/Expiring.vue`、`attendance/Report.vue` 的重复 `STATUS_MAP`，收口所有硬编码 `:type`，补齐资源/考勤/客户自助状态。
3. **信息架构方案**：先做「菜单单一数据源」决策（推荐：路由为源 → 生成菜单，后端 `sys_menu` 补充权限与排序；或后端菜单为源 + 前端补齐缺失页面 + 中文化 + 图标映射），再谈业务域分组。
4. **看板改造**：KPI 卡增加钻取、新增「待办中心」、流程节点补等待时长与异常数。

> **重要约束提醒**：切除硬编码菜单、切到后端动态菜单前，必须先解决 §2.3 的三道障碍（菜单名中文化、缺失页面补齐或隐藏、图标体系映射），否则会出现英文菜单与死链。
