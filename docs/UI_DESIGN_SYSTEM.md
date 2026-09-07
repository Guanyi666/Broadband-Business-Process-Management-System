# BBPMS UI Design System

> 阶段：Phase 2（设计统一规范）＋ Phase 3（信息架构落地）＋ Phase 4（核心页面优化）
> 版本：v1.4　日期：2026-09-07
> 适用范围：`bbpms-admin-web`（管理端）。装维 H5 / 客户 H5 另有移动端规范，本版不覆盖。
> 落地状态：**已实施**（Design Token + 动态菜单 + Tabs 优化 + 状态收口 + 中文化 + 暗色收口 + 响应式 + EP 中文语言包 + PageHeader 升级，`npm run build` 通过）

---

## 0. 设计原则

1. **业务优先**：BBPMS 是企业业务系统，不是营销官网。所有视觉服务于「看清楚业务状态、知道下一步做什么」。
2. **禁止假数据**：任何数字、角标、状态必须来自真实接口（现存的 `:value="3"` 通知角标属于违规，Phase 4 必须接真实未读数）。
3. **单一真源**：颜色、状态、文案各只有一处定义，禁止页面内重复硬编码。
4. **克制动效**：只保留 hover / 状态过渡 / 数字变化 / 时间线节点 / Skeleton / Drawer 过渡；不做粒子、飞入、大面积玻璃拟态。
5. **可访问性优先于「高级感」**：对比度、可点击区域、加载反馈优先。

---

## 1. 技术机制（规范如何生效）

设计系统由**两层**组成，理解这点是落地的关键：

```
variables.scss（SCSS 变量，编译期）
    ↓  vite additionalData 全局注入 → 所有 .vue 的 <style lang="scss"> 可直接用
    ↓
theme.scss（CSS 变量 --bbpms-*，运行时）
    ↓  映射到 Element Plus 的 --el-* 变量
    ↓
所有 Element Plus 组件自动继承设计系统（Button/Table/Tag/Form/Dialog…）
```

- 引入顺序：`main.ts` 中 **element-plus.css → theme.scss → global.scss**（顺序不可颠倒，否则覆盖失效）
- 主题切换：`<html data-theme="dark">` 即可切换（`profile/index.vue` 已提供切换入口）

**开发约定**：

| 场景 | 用法 |
|---|---|
| 组件内写样式 | 用 SCSS 变量 `$spacing-4`、`$radius-base` |
| 需要跟随主题变化（背景/文字/边框） | 用 CSS 变量 `var(--bbpms-bg-base)` |
| 禁止 | 再写 `#303133`、`#fff`、`rgba(0,0,0,.06)` 等裸色值 |

---

## 2. 色彩

### 2.1 品牌与语义色

| Token | 值 | 用途 |
|---|---|---|
| `--bbpms-color-primary` | `#409eff` | 主操作、进行中状态、链接 |
| `--bbpms-color-success` | `#67c23a` | 成功、已完成、正常 |
| `--bbpms-color-warning` | `#e6a23c` | 警告、待处理、临期 |
| `--bbpms-color-danger` | `#f56c6c` | 危险、异常、驳回、超时 |
| `--bbpms-color-info` | `#909399` | 中性信息、已归档 |

### 2.2 业务语义色（状态色板专用）

| Token | 值 | 语义 |
|---|---|---|
| `--bbpms-color-processing` | `#409eff` | 进行中（已派单 / 已接单 / 施工中） |
| `--bbpms-color-pending` | `#e6a23c` | 待处理（待审核 / 待派单 / 待接单） |
| `--bbpms-color-done` | `#67c23a` | 已完成 |
| `--bbpms-color-exception` | `#f56c6c` | 异常（驳回 / 停滞 / 失败 / 超时） |
| `--bbpms-color-neutral` | `#909399` | 中性（已取消 / 已归档 / 未知） |

### 2.3 层级色（背景 / 表面 / 边框 / 填充）

| Token | 亮色 | 暗色 | 用途 |
|---|---|---|---|
| `--bbpms-bg-page` | `#f0f2f5` | `#141414` | 内容区底色 |
| `--bbpms-bg-base` | `#ffffff` | `#1f1f1f` | 卡片、表格、面板 |
| `--bbpms-bg-surface` | `#ffffff` | `#262626` | 浮层（下拉、弹窗） |
| `--bbpms-bg-hover` | `#f5f7fa` | `#2b2b2b` | 行 / 项 hover |
| `--bbpms-bg-active` | `#ecf5ff` | `#17325a` | 选中态 |
| `--bbpms-border` | `#dcdfe6` | `#3a3a3a` | 常规边框 |
| `--bbpms-border-light` | `#e4e7ed` | `#333333` | 弱化边框 |
| `--bbpms-border-lighter` | `#ebeef5` | `#2e2e2e` | 分割线 |
| `--bbpms-fill-light` | `#f5f7fa` | `#262626` | 填充块 |

### 2.4 文本层级

| Token | 亮色 | 暗色 | 用途 |
|---|---|---|---|
| `--bbpms-text-primary` | `#303133` | `#e5eaf3` | 标题、正文 |
| `--bbpms-text-regular` | `#606266` | `#cfd3dc` | 次要正文 |
| `--bbpms-text-secondary` | `#909399` | `#a3a6ad` | 辅助说明、时间 |
| `--bbpms-text-placeholder` | `#a8abb2` | `#8d9095` | 占位符 |
| `--bbpms-text-disabled` | `#c0c4cc` | `#6c6e72` | 禁用 |

---

## 3. 字体

- 字体族：`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif`
- 数字/金额：使用 `$font-family-number` + `font-variant-numeric: tabular-nums`（表格/KPI 数字对齐，避免跳动）

| 级别 | 字号 | 用途 |
|---|---|---|
| xs | 12px | 辅助说明、角标、时间戳 |
| sm | 13px | 表格内容 |
| **base** | **14px** | 正文（默认） |
| md | 16px | 小标题、区块标题 |
| lg | 18px | 卡片数值 |
| xl | 20px | 页面标题 |
| xxl | 24px | 大屏 KPI |

行高：标题 `1.3`，正文 `1.5`。字重：常规 400 / 强调 500 / 小标题 600。

---

## 4. 间距（4pt 基准）

`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40`

| 场景 | 间距 |
|---|---|
| 卡片内边距 | 20px |
| 卡片间距 | 16px |
| 表单项之间 | 16px |
| 页面区块之间 | 24px |
| 工具栏内部元素 | 12px |
| 图标与文字 | 4–8px |

---

## 5. 圆角与阴影

**圆角**：`sm 2px / base 4px / md 6px / lg 8px / xl 12px / pill 999px`

> 系统整体偏「专业紧凑」，默认 4px；卡片类可用 6–8px；状态标签用 pill（圆角）。

**阴影（三级 elevation）**：

| 级别 | 值 | 场景 |
|---|---|---|
| 1 | `0 1px 3px rgba(0,0,0,.06)` | 静态卡片 |
| 2 | `0 2px 8px rgba(0,0,0,.08)` | hover 卡片、下拉 |
| 3 | `0 8px 24px rgba(31,45,61,.12)` | Dialog / Drawer |

---

## 6. 动效

| Token | 值 | 场景 |
|---|---|---|
| `--bbpms-duration-fast` | 150ms | hover、颜色过渡 |
| `--bbpms-duration-base` | 250ms | 展开收起、卡片浮起 |
| `--bbpms-duration-slow` | 350ms | Drawer / Dialog |
| `--bbpms-ease-out` | `cubic-bezier(.22,1,.36,1)` | 通用缓出 |

**允许**：hover 浮起（≤2px）、状态色过渡、数字滚动、时间线节点渐入、Skeleton 微光、Drawer/Dialog 过渡。
**禁止**：粒子、页面飞入、复杂背景动画、无意义旋转、大面积玻璃拟态。

---

## 7. 状态色板（**全系统唯一真源**）

实现位置：`src/components/BBPMSStatusTag.vue`（组件自动取色）。
**规则**：任何地方展示业务状态，必须使用 `<BBPMSStatusTag>`，**禁止**自写 `:type` 或自定义 `STATUS_MAP`（现有 `sla/Expiring.vue`、`attendance/Report.vue` 的两套属违规，Phase 3 收口）。

### 7.1 订单 OrderStatus

| 状态码 | 中文 | 颜色 |
|---|---|---|
| `CREATED` | 已创建 | warning |
| `PENDING_CS_CONFIRM` | 待客服确认 | warning |
| `AUDITED` | 已审核 | primary |
| `WAIT_DISPATCH` | 待派单 | warning |
| `DISPATCHED` | 已派单 | primary |
| `INSTALLING` | 安装中 | warning |
| `FINISHED` | 已完成 | success |
| `CLOSED` | 已归档 | info |
| `CANCELLED` | 已取消 | info |
| `REJECTED` | 已驳回 | danger |
| `CS_REJECTED` | 客服已退回 | danger |

### 7.2 工单 WorkOrderStatus

| 状态码 | 中文 | 颜色 |
|---|---|---|
| `PENDING` | 待派发 | warning |
| `DISPATCHED` | 已派单 | primary |
| `ACCEPTED` | 已接单 | primary |
| `IN_PROGRESS` | 施工中 | warning |
| `STALLED` | 已停滞 | **danger** |
| `REASSIGNING` | 改派中 | warning |
| `COMPLETED` | 已完成 | success |
| `FAILED` | 失败 | danger |
| `AUTO_CANCELLED` | 自动取消 | info |
| `CANCELLED` | 已取消 | info |

> ⚠️ 上表中 `STALLED=danger` / `AUTO_CANCELLED=info` 为**唯一正确值**。
> 当前 `sla/Expiring.vue:85-95` 使用了 `STALLED=warning`、`AUTO_CANCELLED=danger`、`IN_PROGRESS=success`、`ACCEPTED=warning`，**四处与真源冲突**，Phase 3 必须删除该重复映射。

### 7.3 其它域（Phase 3 需补充进真源）

| 域 | 状态 | 中文 | 颜色 |
|---|---|---|---|
| 资源 | `IN_STOCK` | 空闲 | success |
| 资源 | `IN_USE` | 占用中 | primary |
| 资源 | `FAULT` | 故障 | danger |
| 考勤 | `ON_DUTY` | 在岗 | success |
| 考勤 | `ON_BREAK` | 休息中 | info |
| 考勤 | `OFF_DUTY` | 离岗 | info |
| 考勤 | `AUTO_OFF` | 自动离岗 | warning |
| 装维 | `IDLE` | 空闲 | success |
| 装维 | `WORKING` | 作业中 | warning |
| 装维 | `OFFLINE` | 离线 | info |
| 客户自助 | `SUBMITTED` | 已提交 | warning |
| 客户自助 | `PROCESSING` | 处理中 | primary |
| 客户自助 | `RESOLVED` | 已解决 | success |
| 客户自助 | `REJECTED` | 已驳回 | danger |

---

## 8. 组件规范

### 8.1 Button

- 类型：主操作 `primary`、次要 `default`、文字按钮 `text`、危险 `danger`
- 每个按钮**必须**具备：hover / active / disabled / loading 四态
- 异步操作点击后立即置 `loading`，完成后给 `ElMessage.success` 并刷新数据
- 操作列超过 3 个 → 高频直接显示，低频收进「更多 ▼」（`el-dropdown`）

### 8.2 Tag（状态标签）

- 统一用 `<BBPMSStatusTag :status="..." />`，`round` 圆角，`effect="light"`
- 禁止在页面内手写 `el-tag :type=...` 表达业务状态

### 8.3 Table

- 统一使用 `BBPMSTable`（内建 loading + 分页）
- 空态：必须给 `empty-text`，文案场景化（见 §9）
- 列头全中文（现 `BBPMSTable.vue:91` 的 `Action` 必须改为「操作」）
- 数字列：右对齐 + 等宽数字；时间列：固定宽度 + 统一 `YYYY-MM-DD HH:mm`
- 操作列：`fixed="right"`，避免窄屏挤压

### 8.4 Form

- 标签宽度统一 `100px`（复杂表单 `120px`）
- 必填项 `required` + `rules` 校验（前后端双校验，规则需一致）
- 表单密度：低密度，强调填写体验；单行最多 2 列
- 提交按钮 loading + 成功后提示并返回

### 8.5 Dialog / Drawer

| 场景 | 选择 |
|---|---|
| 简单确认、单一字段 | Dialog（小） |
| 表单（客户/角色/菜单等 CRUD） | Dialog（中，≤560px） |
| 详情查看、复杂业务（创建订单） | Drawer（≥720px）或独立页面 |

- **禁止弹窗套弹窗**；`ElMessageBox` 仅在事件回调中调用
- 危险操作（取消订单、驳回、删除、改派）必须说明**后果**：
  ```
  确认取消订单？
  订单：BBDEMO20260001　客户：张*
  取消后该订单将终止后续履约流程。
  [取消] [确认取消]
  ```

### 8.6 Loading

| 场景 | 方式 |
|---|---|
| 页面/卡片首次加载 | Skeleton（`.bbpms-skeleton`） |
| 表格刷新、局部更新 | `v-loading` |
| 按钮异步 | `loading` 状态 |

禁止：加载时整页空白。

### 8.7 Empty（空状态）

统一 `.bbpms-empty` 容器，文案必须**场景化中文**：

| 场景 | 主文案 | 说明 |
|---|---|---|
| 无数据 | 暂无数据 | — |
| 筛选无结果 | 当前筛选条件下暂无数据 | 试试调整筛选条件 +「重置筛选」按钮 |
| 流程未开始 | 尚未开始 | 如：该流程将在订单审核通过后开始 |
| 无权限 | 暂无访问权限 | 请联系管理员开通 |
| 加载失败 | 数据加载失败 | 请检查网络后重试 +「重新加载」按钮 |

**禁止**：英文空态（现 `order/detail.vue:149` `Not dispatched yet`、`installer/map.vue:119` `No installer locations` 等属违规，Phase 3 收口）。

### 8.8 Timeline / Steps

- 时间线：统一 `OrderTimeline`（订单/工单双轨），节点状态：完成 / 当前 / 待处理 / 跳过 / 异常
- 步骤条：流程节点必须显示**名称 + 数量 + 状态**，进一步补充平均等待时长与异常数（Phase 4 看板）
- 节点动画：仅允许渐入 + 连线过渡

---

## 9. 信息密度

| 页面类型 | 密度 | 说明 |
|---|---|---|
| Dashboard | 高 | 多指标、多图表并列 |
| 列表页 | 中高 | 搜索 + 统计摘要 + 表格紧凑 |
| 详情页 | 中 | 概览 → 流程 → 详情，分层展示 |
| 表单页 | 低 | 强调填写体验，留白充足 |

---

## 10. 响应式

断点：`768 / 1024 / 1280 / 1440`

- `< 1024`：侧边栏自动折叠；栅格 `.bbpms-col-*` 降级为 12 格通栏
- `< 768`：表格横向滚动；Dialog 宽度 90%

> 现状：仅 5 个组件内有局部 `@media`，**无布局级适配**（Phase 4 补齐）。

---

## 11. 已知限制（本版未覆盖，列入 Phase 3/4）

1. 暗色主题仅完成基础层级（背景/文本/边框/填充），部分页面仍有硬编码色值，暗色下观感待 Phase 4 页面改造后打磨
2. 状态真源尚未覆盖资源/考勤/客户自助等域（§7.3 为待补清单）
3. 组件规范（Table/Dialog/Empty）尚未全部落地为组件能力，当前为约定
4. 通知角标 `:value="3"` 假数据未修（Phase 4 接真实未读数）

---

## 12. 本次落地文件

| 文件 | 变更 |
|---|---|
| `src/assets/styles/variables.scss` | 重写为完整 Design Token（12 类），保留旧变量别名向后兼容 |
| `src/assets/styles/theme.scss` | **新增** —— CSS 变量体系 + 映射 Element Plus `--el-*` + 暗色主题 |
| `src/assets/styles/global.scss` | 硬编码色值改为 token；新增文本/空态/错误/骨架工具类 |
| `src/main.ts` | 引入 theme.scss（在 element-plus.css 之后） |

验证：`npm run build` 通过（见本节末构建结果）。

## 13. Phase 3 信息架构落地（v1.1）

### 13.1 动态菜单（按角色隐藏无权限菜单）

- **方案**：后端 `/api/auth/menus`（按 `sys_role_menu` 角色返回菜单树）决定「显示哪些 path」，前端 `allMenus` 元数据决定「显示什么文案/图标」。中文菜单 + 无死链 + 后端仍做 URL 权限校验（前端隐藏≠后端安全）。
- 后端菜单接口失败/空结果时**降级为本地静态权限过滤**（`filterByLocal`），功能不降级。
- 实施文件：`src/layouts/AdminLayout.vue`（`collectBackendPaths` / `filterByBackend` / `filterByLocal`）。

### 13.2 Tabs 优化

- 同一路由名（name）只保留一个 Tab —— 订单详情不再重复堆积；详情页通过 `route.query.__title` 更新 Tab 标题为业务标识（「订单 BBD...001」）。
- Tab 右键菜单：关闭当前 / 关闭其他 / 关闭全部。
- 实施文件：`src/stores/app.ts`、`src/layouts/AdminLayout.vue`、`order/detail.vue`、`workorder/detail.vue`。

### 13.3 状态映射收口

- 唯一真源 `BBPMSStatusTag.vue` 补全考勤（ON_DUTY/OFF_DUTY/AUTO_OFF）、资源（IN_STOCK/IN_USE/FAULT）、客户自助（SUBMITTED/PROCESSING/RESOLVED）。
- 删除 `sla/Expiring.vue`、`attendance/Report.vue` 两套冲突 `STATUS_MAP`（原 STALLED=warning、IN_PROGRESS=success 等与真源冲突，现统一 STALLED=danger、IN_PROGRESS=warning）。

### 13.4 订单 ↔ 工单双向互跳

- 订单详情「关联工单」卡片：真实数据来自 `GET /work-orders/by-order/{orderId}`，显示工单号 + 状态 + 装维，点击跳转工单详情；空态「尚未派单」。
- 工单详情「关联订单」：显示订单号链接，点击跳转订单详情。

### 13.5 中文化与 Bug 修复

- 日志状态恒显示「失败」Bug：`log/operation.vue` `row.status === 1` → `row.status === 'SUCCESS'`（后端返回字符串）。
- 英文文案清零（用户可见）：`Delete user?`→「确认删除用户…」、`Enabled/Disabled`→「启用/停用」、`Roles updated`→「角色更新成功」、`Not dispatched yet`→「尚未派单」、`No installer locations`→「暂无装维位置信息」、地图错误 4 处、`Action` 列头→「操作」、`Installer ID` placeholder→「装维 ID」等。
- 通知角标 `:value="3"` 假数据移除 → 点击图标跳真实消息记录页（未读数接口待后端提供后接线）。

### 13.6 剩余事项（Phase 4）

1. 看板 KPI 卡片可钻取（点击 → 列表自动筛选） —— **✅ 已落地（v1.2）**
2. 看板「待办中心」聚合（待我处理：超时/待审/待接单） —— **✅ 已落地（v1.2）**
3. 暗色主题下页面硬编码色值收口 —— **✅ 已落地（v1.3）：全系统 19 文件裸色值收口为 `var(--el-*)`；保留品牌渐变/深底白字/合法 fallback（详见 .workbuddy/memory 日志）**
4. 创建订单独立页面/大型 Drawer —— **✅ 已是独立页面（228 行表单），无需改**
5. 列表页统计摘要（全部/待审核/处理中/已完成）+ 空态文案场景化 —— **✅ 已落地（v1.2，复用 overview 状态分布做 Tab 计数）**
6. 响应式布局级适配 —— **✅ 已落地（v1.3）：<1024px 侧边栏自动折叠（matchMedia，宽屏不自动展开）+ 顶栏小屏适配 + BBPMSTable 横向滚动/分页换行**

## 15. 后续待办（v1.3 之后）

1. 通知未读数角标 —— **✅ 已落地（v1.4）**：`message` 表加 `is_read` 字段；后端新增 `GET /api/notify/unread/count`（当前用户 INAPP+SUCCESS 未读数，仅登录即可访问）+ `POST /api/notify/messages/read`（标记全部已读，仅限本人消息）；前端 AdminLayout 顶部铃铛接 `el-badge` 未读角标（60s 轮询 + 进入通知页立即清零）、notify/record 页进入时自动标记已读并展示「已读/未读」列（仅 INAPP 显示）。真实业务链路：UrgeService 催单 / InstallNotifyListener 安装完成写入的 INAPP 站内信。
2. P1 页面容器统一 —— **✅ 已审查完毕无需改**：installer/map、attendance/Report、leave/Approval、sla/Expiring 均已用 `BBPMSTable`（统一容器+空态）或 `el-card`+真实接口。
3. sla/Expiring 死代码：`pageWorkOrders` 无效导入已删、`确认恢复工单 ?` 中文标点已修 —— **✅ 已落地（v1.3）**
4. Element Plus 中文语言包：`app.use(ElementPlus, { locale: zhCn })` —— **✅ 已落地（v1.3）**：EP 组件内置文案（表格空态/分页/日期）全中文
5. notify/record 中文化：渠道/状态原始码 → 中文映射 + 渠道下拉 APP_PUSH→INAPP —— **✅ 已落地（v1.3）**
6. installer/list 状态真源：`:label="row.status"` → 走 BBPMSStatusTag 真源 —— **✅ 已落地（v1.3）**
7. PageHeader 组件升级：新增 `breadcrumb`/`back` props（修复 3 页传了 `:breadcrumb` 但组件未定义）；order/workorder/installer 三个详情页加返回按钮 —— **✅ 已落地（v1.3）**
8. customer-portal/operations 工单筛选下拉英文码 → 中文标签 —— **✅ 已落地（v1.3）**
9. order/audit 审核页状态 `{{ order.status }}` → BBPMSStatusTag 真源 —— **✅ 已落地（v1.3）**

## 14. Phase 4 核心页面落地（v1.2）

### 14.1 看板 KPI 钻取 + 待办中心

- `BBPMSKpiCard` 新增 `to`/`click`（可点击态：hover 浮起、focus 轮廓、role=button）
- 5 张 KPI 卡钻取：进行中工单→工单列表 IN_PROGRESS；今日新增→订单列表；待审核→订单列表 CREATED；今日完成→工单列表 COMPLETED；停滞→工单列表 STALLED
- 新增「待办中心」区块：停滞工单(红)/待审核订单(黄)/待派单工单(蓝)/SLA临期(黄)，全部真实数据 + 可点击钻取；空态「暂无待办，一切正常」

### 14.2 列表统计摘要

- 订单/工单列表 Tabs 升级为「标签 + 计数徽标」（全部 N / 各状态 N），数据来自 `/dashboard/overview` 状态分布（存量全量口径，与看板一致）
- 权限门控 + 接口失败静默降级，不阻断列表主功能
- 列表页 URL `?status=` 钻取原本已支持，看板钻取闭环直接生效
