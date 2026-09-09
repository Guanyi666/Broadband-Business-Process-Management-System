# BBPMS 前端展示数据全面审核修正报告

> 审核范围：全部种子数据（middleware/mysql/init）+ 前端全部展示页面（bbpms-admin-web / bbpms-customer-h5 / bbpms-installer-h5）
> 完成日期：2026-09-09
> 验证方式：运行库数据治理 + 后端接口端到端验证 + 前端构建验证

---

## 一、结论摘要

| 维度 | 治理前 | 治理后 |
|---|---|---|
| 工单总量（运行库） | 79 条（其中 **69 条 AUTO_CANCELLED 雪花僵尸**） | **10 条**（状态链 PENDING→DISPATCHED→IN_PROGRESS→STALLED→COMPLETED 完整） |
| 套餐字典 | 13 条（**10 条测试/重复**，编码连字符/下划线混用） | **4 条**（PKG_100M/300M/500M/1000M，全下划线，全中文名） |
| 客户 | 7 条（1 条密文乱码） | 6 条（全中文可读） |
| 部门 | 7 条（3 条验收临时部门） | 4 条（全中文名） |
| 系统用户 | 15 条（real_name 全 NULL，英文 nickname） | 15 条（real_name 全中文姓名） |
| 订单-工单状态联动 | 被 SLA 破坏（1003 DISPATCHED↔工单 AUTO_CANCELLED） | 13 组核对全部一致 |
| 前端「客服/审核人」 | **恒显示 `-`**（后端未装配姓名） | 显示中文姓名（李文静/陈思远/赵明远） |
| 前端「工单装维员」 | **恒为空**（后端从未装配 installerName） | 已派单显示中文姓名，PENDING 显示「待派发」 |
| SLA 演示环境 | 默认开启，30 分钟无人接单即 auto-cancel + 重派死循环 | dev 环境显式关闭，演示数据不再被误伤 |

---

## 二、修改前后对比明细

### 2.1 运行库治理（数据修正）

| 项目 | 处理 | 数量 |
|---|---|---|
| 僵尸工单（AUTO_CANCELLED 雪花 ID） | 删除 | 62 条 |
| SLA 重派残留雪花工单（本轮复现） | 删除 | 5 条 |
| 测试/重复套餐 | 删除，仅留 4 个业务套餐 | 10 条 |
| 密文乱码客户 | 删除 | 1 条 |
| 验收临时部门 | 删除 | 3 条 |
| 旧 1099 上海演示数据 | 删除/重建 | 1 条 |
| 非终态工单时间新鲜化（dispatch_time 拉回 SLA 窗口） | UPDATE | 6 条 |
| message 催单残留 | 删除 | 2 条 |
| 补 PKG_500M 套餐 | INSERT | 1 条 |
| 客户/部门/用户中文名 | UPDATE | 15 条用户 + 5 条部门 |

### 2.2 种子脚本修正（更新后种子数据 = 04/05/06/07 四个文件）

#### `04-seed-data.sql`（核心业务种子）
| 位置 | 修改前 | 修改后 |
|---|---|---|
| sys_dept 1/2 | `'BBPMS Root'` / `'Operations Dept'` | `'BBPMS 总部'` / `'运营部'` |
| sys_user INSERT | 无 `real_name` 列，nickname 为英文（`'Super Admin'`/`'CS Alice'`…） | 补 `real_name` 列，10 个用户全中文姓名（王建国/李文静/陈思远/赵明远/孙志强/周建军/吴海涛/郑晓东/钱文博/冯永强），nickname 中文（超级管理员/客服专员/审核专员/调度专员/装维工程师） |
| installer_profile | on_duty 部分为 0 | 全部 on_duty=1（在岗可派） |
| appointment INSERT | 缺 `status` 列 | 补 `status`，1101→PENDING、1102-1106→CONFIRMED 与 confirmed 位联动 |
| 2003 DISPATCHED 工单 dispatch_time | `NOW()-6HOUR`（超 SLA 30min 窗口） | `NOW()-10MINUTE`（窗口内） |

#### `05-seed-demo-data.sql`（演示补齐数据）
| 位置 | 修改前 | 修改后 |
|---|---|---|
| sys_dept 3/4/5 | `'Field Ops A'`/`'Branch B'`/`'Sub Team 5'` | `'现场作业一组'`/`'昌平分部'`/`'装维五小队'` |
| sys_user 11-14 | 无 `real_name`，英文 nickname | 补 `real_name`（刘志鹏/杨立群/何建明/罗文斌），中文 nickname |
| 1099 订单地址 | `'上海市浦东新区演示路99号'` | `'北京市昌平区龙泽园街道智慧路88号'`（customer 固定 4 赵伟/昌平，cs_id=2） |
| 1099 套餐 | `'FIBER_500M'` | `'光纤 500M 宽带'` |
| 9011 工单客户电话 | `13900009999`（无主号码） | `13900000004`（赵伟真实号码） |
| 2010-2040 订单 | 状态 `CREATED`（与工单 DISPATCHED 矛盾） | `DISPATCHED`，补 auditor_id=4 / audit_time / audit_remark |
| 2010-2040 工单 dispatch_time | `NOW()-1/2/3/4 HOUR`（超 SLA） | `NOW()-5MINUTE`（窗口内） |
| 追加 install_record 种子 | 无 | 对应 2005/2006 完工工单，ONU SN 关联 net_onu/net_pon |

#### `06-resource-schema.sql`（资源台账）
| 位置 | 修改前 | 修改后 |
|---|---|---|
| 小区名 | `'朝阳区演示小区'`/`'海淀区演示小区'`/`'望京演示小区'` | `'朝阳区建国里小区'`/`'海淀区中关村小区'`/`'朝阳区望京小区'` |
| PON 1/1/1 used_ports | 0（与 ONU-0002 绑定矛盾） | 1（口径一致） |

#### `07-customer-portal-schema.sql`（客户门户）
| 位置 | 修改前 | 修改后 |
|---|---|---|
| 套餐编码 | `PKG-100M`/`PKG-300M`/`PKG-1000M`（连字符） | `PKG_100M`/`PKG_300M`/`PKG_1000M`（下划线，与订单 package_code 一致 → 套餐字典 100% 命中） |

### 2.3 前端展示逻辑修正

| 文件 | 修改前 | 修改后 |
|---|---|---|
| `workorder/detail.vue` | 全部时间字段无条件显示，PENDING 工单装维员显示 `-` | 新增 `fieldVisible` computed：PENDING 不显示装维员（显示「待派发」）；DISPATCHED+ 显示派单时间；ACCEPTED+ 显示接单时间；IN_PROGRESS+ 显示开始时间；COMPLETED/FAILED 显示完成时间 |
| `order/detail.vue` | 关联工单 PENDING 时装维员显示 `-` | PENDING 显示「待派发」，否则显示中文姓名 |

### 2.4 后端逻辑修正（支撑前端展示）

| 文件 | 修改前 | 修改后 |
|---|---|---|
| `OrderVO.java` | 无 csName/auditByName | 新增两字段 |
| `OrderServiceImpl.toOrderVO()` | 不装配姓名 | 经 SysUserMapper.selectBatchIds 批量装配（realName 优先，username 兜底） |
| `WorkOrderServiceImpl.toVO()/enrichDetail()` | installerName 恒 null | 按 installer_id 装配中文姓名；未派单保持 null |
| `application-dev.yml` | SLA 默认 enabled=true | dev 环境 `bbpms.workorder.sla.enabled: false`（根治演示环境 auto-cancel 死循环；生产仍受主配置约束） |

---

## 三、根因分析：SLA 死循环

**现象**：后端每次重启，运行库都会新增若干 AUTO_CANCELLED 雪花工单。

**链路**（`WorkOrderSlaScheduler.scanDispatchTimeout()`，每 30s 扫描）：
1. 种子 DISPATCHED 工单 `dispatch_time` 为 `NOW()-N HOUR`，超过 `accept-timeout-minutes: 30`；
2. 调度器判定「派单超时未接单」→ `autoCancel`（状态改 AUTO_CANCELLED）；
3. 紧接着调用 `dispatchService.autoDispatch(orderId)` **重派**，生成新雪花工单；
4. 新工单派给装维员后无人接单，30 分钟后再次超时 → 循环。

**演示环境影响**：真实生产有装维员接单，超时取消是正确行为；演示环境无人接单，循环必然发生，且重派每次生成新 ID，僵尸工单无限增长。

**根治**：dev 配置显式关闭 SLA（`enabled: false`），演示数据不再被调度器误伤；种子 dispatch_time 也统一改到新鲜窗口内，即使误开 SLA 也有缓冲。

---

## 四、订单-工单状态联动核对表（治理后运行库实测）

| 订单 ID | 订单状态 | 关联工单 | 工单状态 | 装维员 | 核对 |
|---|---|---|---|---|---|
| 1001 | CREATED | 无 | — | — | ✓ 未派单无工单 |
| 1002 | WAIT_DISPATCH | 2002 | PENDING | 无（前端显示「待派发」） | ✓ |
| 1003 | DISPATCHED | 2003 | DISPATCHED | 周建军（installer6） | ✓ |
| 1004 | INSTALLING | 2004 | IN_PROGRESS | 吴海涛（installer7） | ✓ |
| 1005 | FINISHED | 2005 | COMPLETED | 郑晓东（installer8） | ✓ |
| 1006 | CLOSED | 2006 | COMPLETED | 钱文博（installer9） | ✓ |
| 1007 | CANCELLED | 无 | — | — | ✓ |
| 1008 | AUDITED | 无 | — | — | ✓ 待派单 |
| 1099 | INSTALLING | 9011 | STALLED | 钱文博（installer9） | ✓ STALLED 为施工中子态，订单保持 INSTALLING |
| 2010-2040 | DISPATCHED | 同名工单 | DISPATCHED | 周建军/吴海涛/郑晓东/钱文博 | ✓ |

**补充核对**：订单详情「客服」「审核人」→ 李文静/陈思远（cs1/cs2）、赵明远（audit1），与 sys_user 中文姓名一致 ✓

---

## 五、验证记录

| 验证项 | 结果 |
|---|---|
| 后端编译（JDK21, Maven offline） | ✓ exit 0 |
| 前端构建（vue-tsc + vite build） | ✓ 14s，exit 0 |
| 登录（admin / RSA 加密密码） | ✓ 200 |
| 订单列表 `/api/orders/page` | ✓ 13 条，套餐名全中文 |
| 订单详情 `/api/orders/{id}` | ✓ packageName/csName/auditByName/dispatchTime 全部返回 |
| 工单列表 `/api/work-orders/page` | ✓ 10 条，0 条 AUTO_CANCELLED，installerName 中文 |
| 工单详情 `/api/work-orders/{id}` | ✓ PENDING 无装维员，其余中文姓名 |
| SLA 扫描周期后复查 | ✓ 35s 后 0 新增 AUTO_CANCELLED（dev 关闭生效） |
| 套餐字典启动日志 | ✓ 「刷新完成，共 4 条健康套餐中文名」 |

---

## 六、涉及文件清单

```
middleware/mysql/init/04-seed-data.sql
middleware/mysql/init/05-seed-demo-data.sql
middleware/mysql/init/06-resource-schema.sql
middleware/mysql/init/07-customer-portal-schema.sql
bbpms-app/src/main/java/com/bbpms/order/vo/OrderVO.java
bbpms-app/src/main/java/com/bbpms/order/service/impl/OrderServiceImpl.java
bbpms-app/src/main/java/com/bbpms/workorder/service/impl/WorkOrderServiceImpl.java
bbpms-app/src/main/resources/application-dev.yml
bbpms-admin-web/src/views/order/detail.vue
bbpms-admin-web/src/views/workorder/detail.vue
```
