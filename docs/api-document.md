# BBPMS 接口文档

> 在线文档：`http://localhost:8080/doc.html`（Knife4j / OpenAPI 3）。本文档为接口约定与端点速查。

## 1. 接口约定

### 统一响应 `R<T>`
```json
{ "code": 0, "msg": "success", "data": { ... }, "traceId": "..." }
```
`0` = 成功；非 0 见错误码。分页返回 `PageResp`：`{ records, total, pageNum, pageSize }`。

### 错误码分段
| 段 | 含义 |
|---|---|
| `1xxx` | 认证（401 未登录 / token 无效 / 过期） |
| `2xxx` | 订单 |
| `3xxx` | 工单 |
| `4xxx` | 派单 |
| `5xxx` | 安装 |
| `9xxx` | 系统 |

### 鉴权
- 除公开端点外，请求头须带 `Authorization: Bearer <accessToken>`。
- 写接口由 `@PreAuthorize("hasAuthority('xxx:yyy')")` 控制，权限码来自 `sys_menu.perms`。
- 未登录 → `401`（`R` 格式）；无权限 → `403`（`R` 格式）。

### 路径约定
- 全部业务端点统一 `**/api/**` 前缀（含 `/api/auth/*`）。
- 前端 `VITE_API_BASE=/api`，dev 经 Vite 代理透传到 `http://localhost:8080`。

## 2. 端点速查（按模块）

### 认证（公开：login/refresh/captcha/public-key；me/menus/logout 需 token）
```
POST   /api/auth/login        { username, password(RSA密文), captchaKey, captchaCode }
                              → { accessToken, token, refreshToken, expiresIn, user }
POST   /api/auth/refresh      ?refreshToken=...
POST   /api/auth/logout       (需 token)
GET    /api/auth/captcha
GET    /api/auth/public-key
GET    /api/auth/me           (需 token) → 当前用户信息 + roles + permissions
GET    /api/auth/menus        (需 token) → 当前用户菜单树
```

### 用户 / 角色 / 菜单 / 部门
```
POST   /api/users                    [system:user:add]
PUT    /api/users                    [system:user:edit]
DELETE /api/users/{id}               [system:user:delete]
GET    /api/users/{id} | /api/users/page   [system:user:view]
POST   /api/users/{id}/roles         [system:user:assign]
POST   /api/users/{id}/password      [system:user:edit]
POST   /api/roles / PUT /api/roles / DELETE /api/roles/{id} / GET /api/roles...
POST   /api/roles/{id}/menus         [system:role:assign]
POST   /api/menus / PUT /api/menus/{id} / DELETE /api/menus/{id}
GET    /api/menus/tree | /api/menus/perms
GET    /api/depts/tree | POST /api/depts | PUT /api/depts/{id} | DELETE /api/depts/{id}
```

### 客户 / 订单
```
POST   /api/customers                [customer:create]
GET    /api/customers/{id} | /page | /search   [customer:view]
GET    /api/customers/{id}/unmasked   [customer:view-sensitive]（超管明文）
POST   /api/orders                   [order:create]
GET    /api/orders/{id} | /page | /by-no/{no}  [order:view]
POST   /api/orders/{id}/audit        [order:audit]
POST   /api/orders/{id}/cancel       [order:cancel]
PUT    /api/orders/{id}/appointment  [order:update]
GET    /api/orders/{id}/timeline
```

### 工单
```
POST   /api/work-orders              [workorder:create]
GET    /api/work-orders/{id} | /page | /by-order/{oid} | /entity/{id}
GET    /api/work-orders/my           [workorder:view-own]
POST   /api/work-orders/{id}/accept  [workorder:accept]
POST   /api/work-orders/{id}/start   [workorder:start]
POST   /api/work-orders/{id}/complete [workorder:complete]
POST   /api/work-orders/{id}/transfer [workorder:transfer]
POST   /api/work-orders/{id}/cancel  [workorder:cancel]
PUT    /api/work-orders/{id}/status  [workorder:update-status]
POST   /api/work-orders/{id}/heartbeat
POST   /api/work-orders/{id}/report-stall [workorder:report-stall]
POST   /api/work-orders/{id}/resume  [workorder:resume]
POST   /api/work-orders/{id}/reassign [workorder:reassign]
POST   /api/work-orders/{id}/force-close [workorder:force-close]
GET    /api/work-orders/expiring     [workorder:sla:view]
```

### 派单
```
POST   /api/dispatch/auto            （系统：OrderAuditedListener 触发）
POST   /api/dispatch/manual          [dispatch:manual]
POST   /api/dispatch/{id}/reassign   [dispatch:reassign]
GET    /api/dispatch/candidates?orderId=
GET    /api/dispatch/records/page    [dispatch:view]
GET    /api/dispatch/stats?days=7
GET    /api/dispatch/rules  | PUT /api/dispatch/rules  [dispatch:rule:config]
```

### 安装（H5）
```
POST   /api/install/{woId}/arrive    [install:arrive]
POST   /api/install/{woId}/info      [install:info]
POST   /api/install/{woId}/photos    [install:photo]
POST   /api/install/{woId}/signature [install:sign]
POST   /api/install/{woId}/complete  [install:complete]
GET    /api/install/by-work-order/{id} | /page | /my | /progress/{woId}  [install:view]
```

### 考勤 / 请假
```
POST   /api/attendance/clock-in | clock-out | break/start | break/end   [attendance:clock]
GET    /api/attendance/today | /my   [attendance:view-self]
GET    /api/attendance/on-duty | /daily/{id} | /monthly/{id}            [attendance:view-all]
POST   /api/leave/apply | /{id}/cancel | /{id}/approve   [leave:apply / cancel / approve-l1|l2]
GET    /api/leave/my | /pending-approvals | /team-calendar | /{id}      [leave:view-self / view-all]
```

### 通知 / 日志 / 文件 / 监控
```
POST   /api/notify/sms | /wechat/template   [notify:sms:send / wechat:send]
GET    /api/notify/messages/page            [notify:view]
GET    /api/notify/templates | POST/PUT/DELETE  [notify:view / template:edit]
GET    /api/logs/operation/page | /api/logs/login/page   [log:view]
POST   /api/files/upload | GET /api/files/{id}/presign | /page | /by-biz | DELETE /{id}   [file:*]
GET    /actuator/health | /actuator/metrics
```

## 3. 权限码清单

权限码存于 `sys_menu.perms`，经 JWT `perms` claim 下发，供 `@PreAuthorize` 与前端按钮/路由守卫使用。前缀约定：`workorder:* / order:* / customer:* / dispatch:* / install:* / attendance:* / leave:* / notify:* / file:* / log:* / installer:* / system:user|role|menu|dept:*`。

> 前端实际权限字符串见 `bbpms-admin-web/src/router/index.ts` 与各视图 `v-permission` / `PermissionButton` 用法。若改动权限码，需同步：Java 注解 → `sys_menu.perms` → 前端路由/按钮。
