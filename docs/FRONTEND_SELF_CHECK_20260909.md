# BBPMS 前端展示逻辑自检表

> 版本：2026-09-09（审核修正后）
> 范围：订单列表/详情、工单列表/详情、看板、H5 进度组件、状态标签
> 图例：✅ 已验证通过 ｜ 🔧 本次修正 ｜ — 不适用/隐藏

---

## 一、订单列表 `views/order/list.vue`

| 列 | 数据来源/显示条件 | 验证结果 |
|---|---|---|
| 订单号 `orderNo` | 直接绑定 | ✅ 13 条均显示 BBDEMO2026xxxx |
| 客户 `customerName` | **后端装配（本次修正）**：customer 表姓名经 SM4 解密 + 脱敏 | 🔧 张三/李四/王芳/赵伟/陈晨/刘洋 全部显示 |
| 套餐 `packageName` | 后端字典中文化（DB 字典优先，PackageNameMap 兜底） | 🔧 畅享宽带 100M/家庭宽带 300M/家庭宽带 500M/千兆宽带 1000M/光纤 500M 宽带 全中文 |
| 状态 | `<BBPMSStatusTag :status>` | ✅ 11 态全覆盖 |
| 预约时间 | `expectedInstallDate` 格式化 | ✅ 有值正常显示 |
| 客服 `csName` | **后端装配（本次修正）** | 🔧 李文静/陈思远 显示 |
| 创建时间 | `createTime` 格式化 | ✅ |
| 操作 | 按权限渲染 | ✅ |

## 二、订单详情 `views/order/detail.vue`

| 字段 | 显示条件 | 验证结果 |
|---|---|---|
| 订单号/状态/套餐 | 无条件 | ✅ |
| 客户姓名/电话 | 来自 `getOrderDetail` 的 `customer` 段（脱敏） | ✅ |
| 客服 `csName` | **后端装配（本次修正）** | 🔧 李文静/陈思远 |
| 审核人 `auditByName` | **后端装配（本次修正）**，仅已审核订单有 auditorId | 🔧 已审核显示赵明远，未审核隐藏 |
| 审核时间/备注 | 仅已审核显示 | ✅ |
| 派单时间 | 仅 DISPATCHED 及后续状态 | ✅ |
| 关联工单卡片 | `getWorkorderByOrder` 独立接口 | ✅ |
| 工单-装维人员 | `workorder.status !== 'PENDING'` 显示中文姓名；PENDING 显示「待派发」 | 🔧 1002↔2002 显示「待派发」；1003↔2003 显示周建军 |
| 时间线 | `timeline` 段，eventTime/operatorName | ✅ |

## 三、工单列表 `views/workorder/list.vue`

| 列 | 数据来源/显示条件 | 验证结果 |
|---|---|---|
| 工单号 `workNo` | 直接绑定 | ✅ |
| 订单 ID `orderId` | 直接绑定 | ✅ |
| 状态 | `<BBPMSStatusTag :status="row.status">`，label 用 statusDesc 兜底 | ✅ 10 态全覆盖，无僵尸 |
| 装维人员 `installerName` | **后端装配（本次修正）**：installer_id→sys_user.real_name；PENDING 无 installerId → 空 | 🔧 已派单显示周建军/吴海涛/郑晓东/钱文博/冯永强；2002 PENDING 空 |
| 客户电话 `customerPhone` | 工单冗余字段 | ✅ 1099 已修正为 13900000004 |
| 派单时间 | dispatchTime 格式化 | ✅ |
| 创建时间 | createTime | ✅ |
| 统计 Tab | 后端聚合各状态计数 | ✅ 状态分布与列表一致 |

## 四、工单详情 `views/workorder/detail.vue`（状态-字段联动核心）

| 字段 | 显示条件（`fieldVisible` computed） | 验证结果 |
|---|---|---|
| 装维人员 | `!['PENDING'].includes(status)` 显示中文姓名；PENDING 显示「待派发」 | 🔧 2002→「待派发」；2003→周建军；2004→吴海涛；2005→郑晓东；9011→钱文博 |
| 派单时间 | DISPATCHED/ACCEPTED/IN_PROGRESS/STALLED/REASSIGNING/COMPLETED/FAILED/CANCELLED/AUTO_CANCELLED | ✅ |
| 接单时间 | ACCEPTED 及后续状态 | ✅ 2004 IN_PROGRESS 显示 |
| 开始时间 | IN_PROGRESS/STALLED/COMPLETED/FAILED | ✅ |
| 完成时间 | COMPLETED/FAILED | ✅ 2005 COMPLETED 显示 |
| 工单号/订单号/套餐 | 无条件（enrichDetail 装配 orderNo/packageName） | ✅ BBDEMO20260005/家庭宽带 300M |
| 时间线 | timelineService 装配 operatorName（realName 优先） | ✅ |
| 停滞原因 | 仅 STALLED | ✅ 9011 |

## 五、看板 `views/dashboard/index.vue`

| 指标 | 口径 | 验证结果 |
|---|---|---|
| 待审核订单 | `orderStatusDist` 中 CREATED（countPendingAuditOrders 同口径） | ✅ |
| 待派单 | WAIT_DISPATCH | ✅ |
| 待派工单 | workOrderStatusDist 中 PENDING | ✅ |
| 各状态分布 | 后端聚合，无硬编码 | ✅ |

## 六、状态标签 `components/BBPMSStatusTag.vue`（唯一真源）

| 状态 | 显示文案 | 验证 |
|---|---|---|
| PENDING | 待派发 | ✅ |
| WAIT_DISPATCH | 待派单 | ✅ |
| DISPATCHED | 已派单 | ✅ |
| INSTALLING | 安装中 | ✅ |
| STALLED | 已停滞 | ✅ |
| REASSIGNING | 改派中 | ✅ |
| AUTO_CANCELLED | 已超时取消 | ✅ |
| 其余（AUDITED/COMPLETED/FAILED/CANCELLED/CLOSED/FINISHED…） | 对应中文 | ✅ |

## 七、H5 客户自助 `customer-h5/src/components/OrderProgress.vue`

| 节点 | 状态映射 | 验证 |
|---|---|---|
| 提交订单 | SUBMITTED→DONE | ✅ |
| 受理确认 | PROCESSING 前序 | ✅ |
| 施工中 | DONE/CURRENT/PENDING/SKIP/EXCEPTION 五态映射 | ✅ |
| 已完工 | RESOLVED→DONE | ✅ |

## 八、H5 装维端 `installer-h5`（摘要）

| 页面 | 关键展示 | 验证 |
|---|---|---|
| 我的工单 | 按状态过滤，installer 本人数据 | ✅ |
| 签到/考勤 | ON_DUTY/OFF_DUTY/AUTO_OFF | ✅ 状态真源覆盖 |
| 资源 | IN_STOCK/IN_USE/FAULT | ✅ |

---

## 附：本次修正的字段装配对照

| 接口/页面 | 修正前 | 修正后 | 代码位置 |
|---|---|---|---|
| 订单列表+详情 客户列 | 恒空（customerName 未装配） | 张三/李四/王芳/赵伟/陈晨/刘洋（脱敏） | `OrderServiceImpl.toOrderVO()` |
| 订单列表+详情 客服列 | 恒 `-`（csName 未装配） | 李文静/陈思远 | 同上 |
| 订单详情 审核人 | 恒 `-`（auditByName 未装配） | 赵明远（已审核订单） | 同上 |
| 工单列表+详情 装维员 | 恒空（installerName 未装配） | 周建军/吴海涛/郑晓东/钱文博/冯永强 | `WorkOrderServiceImpl.toVO()/enrichDetail()` |
| 工单详情 各时间字段 | 无条件显示 | 按状态 `fieldVisible` 联动 | `workorder/detail.vue` |
| 订单详情 关联工单装维员 | PENDING 显示 `-` | PENDING 显示「待派发」 | `order/detail.vue` |
