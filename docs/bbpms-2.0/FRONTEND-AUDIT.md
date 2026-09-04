# 前端审计（ITERATION 0）

> 审计时间：2026-09-03 · 只读审计

## 1. admin-web 路由/页面/菜单/权限对应表

路由共 **50 条**（含错误页/登录/个人中心）。核心业务路由与权限码：

| 路由 name | path | 页面(views) | 菜单分组 | meta.permission |
|---|---|---|---|---|
| Dashboard | /dashboard | dashboard/index.vue | 数据看板 | dashboard:view |
| CustomerList | /customer/list | customer/list.vue | 客户管理›客户列表 | customer:view |
| CustomerDetail | /customer/detail/:id | customer/detail.vue | (隐藏) | customer:view |
| OrderList | /order/list | order/list.vue | 订单管理›订单列表 | order:view |
| OrderCreate | /order/create | order/create.vue | 订单管理›创建订单 | order:create |
| OrderAudit | /order/audit | order/audit.vue | (隐藏) | order:audit |
| OrderDetail | /order/detail/:id | order/detail.vue | (隐藏) | order:view |
| WorkorderList | /workorder/list | workorder/list.vue | 工单管理›工单列表 | workorder:view |
| DispatchBoard | /workorder/dispatch-board | workorder/dispatch-board.vue | 工单管理›派单工作台 | workorder:dispatch |
| WorkorderDetail | /workorder/detail/:id | workorder/detail.vue | (隐藏) | workorder:view |
| InstallerList | /installer/list | installer/list.vue | 装维管理›装维列表 | installer:view |
| InstallerMap | /installer/map | installer/map.vue | 装维管理›装维地图 | installer:view |
| InstallerProfile | /installer/profile/:id | installer/profile.vue | (隐藏) | installer:view |
| UserList | /system/user | user/list.vue | 系统管理›用户管理 | system:user:list |
| RoleList | /system/role | role/list.vue | 系统管理›角色管理 | system:role:list |
| MenuList | /system/menu | menu/list.vue | 系统管理›菜单管理 | system:menu:list |
| DeptList | /system/dept | dept/list.vue | 系统管理›部门管理 | system:dept:list |
| NotifyTemplate | /notify/template | notify/template.vue | 通知管理›消息模板 | notify:template:view |
| NotifyRecord | /notify/record | notify/record.vue | 通知管理›消息记录 | notify:record:view |
| FileManager | /file | file/index.vue | 文件管理 | file:view |
| OperationLog | /log/operation | log/operation.vue | 日志管理›操作日志 | log:view |
| LoginLog | /log/login | log/login.vue | 日志管理›登录日志 | log:view |
| AttendanceReport | /attendance/team | attendance/Report.vue | 考勤›团队报表 | attendance:view-all |
| LeaveApproval | /leave/approvals | leave/Approval.vue | 请假›审批 | leave:approve |
| SlaExpiring | /sla/expiring | sla/Expiring.vue | SLA›工单时效 | workorder:sla:view |
| Profile | /profile | profile/index.vue | (个人中心) | – |
| error 403/404/500 | /403 /404 /500 | error/*.vue | (隐藏) | – |

**已完成**：全部页面带权限码 → 动态菜单过滤 + 路由守卫 403（前几轮实测 4 角色菜单正确）。

## 2. admin-web API 层与后端对应

- `src/api/` 按域组织：auth / dashboard / customer / order / workorder / dispatch / installer / notify / file / user / log / attendance / leave / sla
- 前几轮逐接口探针验证：admin 权限下 **关键接口全 200**；audit1 调 users/orders POST → 403
- 契约一致性：本轮未发现前端调用不存在后端接口（此前仪表盘契约问题已修复）

## 3. RBAC 前端实现

- `stores/auth.ts`：登录后存 token + `/auth/me` 返回 permissions + roles
- `layouts/AdminLayout.vue`：staticMenus 树带权限码，`filterMenus` 递归按 permissions/roles 过滤，无权限分组自动隐藏
- `router/index.ts`：meta.permission；守卫（`router.beforeEach`）校验权限，无权限统一跳 /403
- `utils/request.ts`：401→登录过期、403→暂无权限、500→服务器繁忙、网络错误→检查网络，全中文
- **按钮权限**：部分页面按 permission 控制按钮显隐（如无 order:audit 不显示审核按钮）

## 4. 核心页面交互完整性抽查

| 页面 | Loading | Empty | Error | 表单校验 | 分页 |
|---|---|---|---|---|---|
| 订单列表 order/list.vue | ✅ | ✅ | ✅ | ✅ 创建/编辑 | ✅ |
| 工单详情 workorder/detail.vue | ✅ | ✅(工单时间线) | ✅ | ✅ 改派原因必填 | – |
| 派单工作台 dispatch-board.vue | ✅ | ✅(待派空/无在线装维) | ✅ | ✅ 派单确认 | – |
| 客户/装维/系统管理 | ✅ | ✅ | ✅ | ✅ | ✅ |

结论：核心页面交互完整（前几轮已系统性补齐 Loading/Empty/Error/Success/中文化）。

## 5. installer-h5 现状（装维端）

- 技术栈：Vue3 + Vite（`bbpms-installer-h5`，build 脚本含 vue-tsc）
- 路由 9 条：
  - 登录 /login
  - 工单列表 /workorders + 工单详情 /workorders/:id + **施工提交 /workorders/:id/install**
  - 考勤签到 /attendance（Index + History）
  - 请假申请 /leave/apply + 我的申请 /leave/my
  - 个人中心 /profile
- API 层：auth / workorder / install / attendance / leave / file / http —— 与后端 Install/Attendance/Leave/WorkOrder Controller 对应
- **已有**：装维核心作业闭环（看工单→接单→施工提交含 ONU/SN/照片/签名→考勤→请假）
- **2.0 缺口（ITERATION 13 客户侧 H5）**：H5 目前是**装维端作业工具**，不是客户自助端。客户侧 H5（我的宽带/订单/安装进度/报障/评价）是全新建设

## 6. bbpms-app 澄清

> **bbpms-app 不是前端**，是后端 Spring Boot 单体应用（src/main/java/com/bbpms），含 e2e/ 测试目录。项目结构审计中已按后端处理。三端 = admin-web（管理）+ installer-h5（装维）+ 后端 app。

## 7. 问题清单

| 级别 | 问题 |
|---|---|
| P1 | ①installer-h5 施工提交/接单等核心流程缺自动化测试；②H5 无 403/错误页兜底（仅 not-found.vue） |
| P2 | ①installer-h5 无"施工中"细分阶段（预约/前往/到达），直接跳施工提交；②admin 有 55 个按钮权限点但前端未全部按按钮级显隐（部分页面仍显示后 403 的风险，已由后端兜底） |
| P3 | 客户侧 H5 全新建 |