# BBPMS 订单与履约双轨时间线 — 分析与设计

> 阶段：第一步（代码审查）+ 第二步（实现分析）+ 第三步（数据模型设计）
> 调研方式：源码静态分析 + 直连 `bbpms-mysql` 只读查询，未修改任何文件与数据

---

## 一、结论速览

| 问题 | 根因归属 | 一句话根因 |
| --- | --- | --- |
| 刚创建订单显示"暂无业务轨迹" | 前端 + 数据层 | 组件 events 为空即整块 `el-empty`；且订单创建未写 `order_audit_log` |
| 只有"已派单"才出现时间线 | 数据层 | 仅 DISPATCHED 订单在 `order_audit_log` 有记录，其余状态 0 条 |
| 无法看出当前走到哪里 | 后端 + 前端 | 后端不返回骨架与阶段索引；组件无骨架概念 |
| 顶部"当前 CREATED / 0 个事件" | 前端 | 传入裸状态码未映射中文 |
| 无实时等待时长 | 前端 | 无定时器，仅计算相邻事件间隔 |
| 历史操作不完整 | 数据层 | 工单 15 条，时间线仅 10 条，每单最多 1 条 |

**最重要的一条**：订单表与工单表**已具备完整的真实阶段时间戳**，因此修复**不需要伪造任何事件**。

---

## 二、当前订单生命周期

### 2.1 状态枚举（`common/enums/OrderStatus.java:9-19`，11 个）

| 编码 | 中文 | 性质 |
| --- | --- | --- |
| PENDING_CS_CONFIRM | 待客服确认 | **死状态**（状态机无来源/去向） |
| CS_REJECTED | 客服已退回 | **死状态** |
| CREATED | 待审核 | 主干起点 |
| REJECTED | 已驳回 | 异常 |
| AUDITED | 已审核 | 主干 |
| WAIT_DISPATCH | 待派单 | 主干 |
| DISPATCHED | 已派单 | 主干 |
| INSTALLING | 安装中 | 主干 |
| FINISHED | 已完成 | 主干 |
| CLOSED | 已归档 | 主干终点 |
| CANCELLED | 已取消 | 异常 |

中文名写在**后端枚举 `desc`**，非数据库字典、非前端映射。

### 2.2 订单状态机（`common/statemachine/OrderStateMachine.java:22-38`）

```
CREATED   --AUDIT_PASS----->  AUDITED        (3 审核员)
CREATED   --AUDIT_REJECT--->  REJECTED       (3)
CREATED   --CANCEL--------->  CANCELLED      (2 客服, 6 客户)
REJECTED  --RESUBMIT------->  CREATED        (2)
REJECTED  --CANCEL--------->  CANCELLED      (2, 6)
AUDITED   --START_DISPATCH->  WAIT_DISPATCH  (4 调度)
WAIT_DISPATCH --DISPATCH_OK-----> DISPATCHED (4)
WAIT_DISPATCH --DISPATCH_TIMEOUT-> CANCELLED (4)
DISPATCHED --ACCEPT-------->  INSTALLING     (5 装维)
DISPATCHED --TRANSFER------>  WAIT_DISPATCH  (4, 5)
INSTALLING --COMPLETE------>  FINISHED       (5)
FINISHED  --CONFIRM------->  CLOSED         (2, 6)
```
终态：`CLOSED`、`CANCELLED`

**关键**：订单侧 `DISPATCHED --ACCEPT--> INSTALLING`，即**订单进入 INSTALLING 就代表装维已接单**。

### 2.3 订单表时间字段（`broadband_order`）

| 字段 | 含义 | 可用 |
| --- | --- | --- |
| create_time | 创建 | 非空 |
| audit_time | 审核通过 | 可空 |
| dispatch_time | 派单 | 可空 |
| completed_time | 完成 | 可空 |
| cancelled_time | 取消 | 可空 |

**缺**：`accept_time`（接单）、`start_time`（开始安装）——这两个时间**只在工单表**。

### 2.4 订单表人员字段（关键资产）

`customer_id`、`cs_id`（客服）、`auditor_id`（审核员）

---

## 三、当前工单生命周期

### 3.1 状态枚举（`WorkOrderStatus.java:9-18`，10 个）

PENDING 待派发 / DISPATCHED 已派单 / ACCEPTED 已接单 / IN_PROGRESS 施工中 / COMPLETED 已完成
异常：STALLED 已停滞 / REASSIGNING 改派中 / FAILED 失败 / CANCELLED 已取消 / AUTO_CANCELLED 自动取消

终态：`isTerminal()` = COMPLETED / CANCELLED / FAILED / AUTO_CANCELLED

系统**无独立"验收"状态**，工单 COMPLETED 即终态。按"不虚构业务状态"原则不新增验收节点。

### 3.2 工单表时间字段（`work_order`）

`create_time` / `dispatch_time` / `accept_time` / `start_time` / `finish_time` / `expected_finish_time`（SLA）

### 3.3 工单表人员字段（关键资产）

`installer_id`（装维）、`dispatcher_id`（调度）、`order_id`（→broadband_order.id）

### 3.4 一订单多工单

`work_order.order_id = broadband_order.id`，**1 个订单可对应多个工单**。
实测 BBDEMO20260003 有 2 条工单（一条 DISPATCHED、一条 AUTO_CANCELLED）。

---

## 四、当前时间线数据来源

### 4.1 后端聚合（`OrderTimelineServiceImpl.java:42-94`）

```
getTimeline(orderId)
  ├─ order_audit_log      → source=ORDER_AUDIT
  │    角色写死为 ORDER_OPERATOR / SYSTEM（:57，无真实角色）
  └─ work_order_timeline  → source=WORKORDER
       带 operator_id / operator_role
  合并 → 按 eventTime 升序 → enrichOperatorNames（join sys_user）
```

VO 预留了 `source=DISPATCH`，但**从未填充**——`dispatch_record` 未并入。

### 4.2 日志表结构

| 表 | 字段 | 缺 |
| --- | --- | --- |
| `order_audit_log` | order_id, auditor_id, from_status, to_status, remark, create_time | 无 operator_name、无 operator_role |
| `work_order_timeline` | work_order_id, from_status, to_status, operator_id, operator_role, remark, create_time | 无 operator_name、无 is_auto 标记 |
| `dispatch_record` | work_order_id, installer_id, strategy(AUTO/MANUAL/REASSIGN), score, candidates_json, reason | **未并入时间线** |

### 4.3 前端链路

```
src/views/order/detail.vue:119
  <OrderTimeline :events="detail?.timeline || []"
                 title="订单与履约双轨时间线"
                 :current-status="detail?.status || ''" />    ← 裸状态码
src/views/workorder/detail.vue:130                            ← 传 statusDesc（中文，不一致）
src/components/OrderTimeline.vue                              ← 重构主战场
```

`src/api/order.ts:116-121` 已有 `orderTimeline` 接口，但**全仓未调用**（死代码）。

---

## 五、根因：为什么只有"已派单"才有时间线

### 5.1 数据证据

| 订单号 | 状态 | order_audit_log 条数 |
| --- | --- | --- |
| BBDEMO20260001 | CREATED | **0** |
| BBDEMO20260002 | WAIT_DISPATCH | 0 |
| BBDEMO20260003 | DISPATCHED | 1 |
| BBDEMO20260004 | INSTALLING | 0 |
| BBDEMO20260005 | FINISHED | 0 |
| BBDEMO20260006 | CLOSED | 0 |
| BBDEMO20260007 | CANCELLED | 0 |
| BBDEMO20260008 | AUDITED | 0 |
| BBDEMO20260201~204 | DISPATCHED | 各 1 |

**规律清晰：只有 DISPATCHED 订单有日志。** 与用户描述的现象完全吻合。

更严重的是：**BBDEMO20260005（FINISHED 已竣工）的 order_audit_log 与 work_order_timeline 均为空**，尽管它的时间字段齐全。

### 5.2 根因链

```
seed 数据绕过 Service 层 + 状态流转未完整落日志   ← 根因中的根因
   ↓
order_audit_log / work_order_timeline 极度稀疏
   ↓
OrderTimelineServiceImpl 只聚合日志表，未使用主表已有的真实时间字段
   ↓
早期阶段返回 events = []
   ↓
OrderTimeline.vue:175  v-if="!normalizedEvents.length" → el-empty「暂无业务轨迹」
```

**前端没有任何按状态过滤的逻辑**——"只有已派单才显示"是数据供给问题，不是前端 bug。

---

## 六、缺失清单

### 6.1 数据质量问题（右轨展示的直接障碍）

| 现象 | 影响 |
| --- | --- |
| `work_order_timeline.operator_id` **全部 NULL** | 无法 join 出操作人 |
| `order_audit_log.auditor_id` **全部 NULL** | 同上 |
| `sys_user.real_name` **全部 NULL**（15 行仅 1 行有值），`nickname` 有中文 | 姓名拿不到友好值 |
| 姓名解析逻辑：`real_name → username`，**从不使用 nickname**（`OrderTimelineServiceImpl.java:111-112`） | 姓名退化为 `install1`/`disp1` |
| `work_order_timeline` 无 `is_auto` 列 | 自动/人工靠 remark 文本推断，脆弱 |
| 订单表缺 `accept_time`/`start_time` | 接单/开始安装时间只能取工单表 |

### 6.2 后端能力缺失

- 无**流程骨架**接口（已 grep 确认无 skeleton/flow/step 端点）
- 不返回当前阶段索引 / 进度
- 不计算已用时 / 等待时长
- 未利用订单表 + 工单表已有的 9 个真实时间字段

### 6.3 前端能力缺失

- 无骨架，events 空即整块消失（`:175`）
- 无顶部 Steps
- 无实时刷新
- 订单详情传裸码、工单详情传中文，两处不一致
- 状态映射漏 `PENDING_CS_CONFIRM`、`CS_REJECTED`（真源 `BBPMSStatusTag.vue:28-59`）

### 6.4 现有接口清单

| 接口 | 路径 |
| --- | --- |
| 订单详情 | `GET /api/orders/{id}`（含 timeline） |
| 订单时间线 | `GET /api/orders/{id}/timeline` |
| 工单详情 | `GET /api/work-orders/{id}` |
| 工单时间线 | `GET /api/work-orders/timeline/{workOrderId}` |
| 派单候选 | `GET /api/dispatch/candidates` |
| 派单记录 | `GET /api/dispatch/records/page` |

---

## 七、数据模型设计（第三步）

### 7.1 核心原则

1. **不伪造事件**：主干节点时间全部取自订单表/工单表的**真实时间字段**
2. **时间字段为空 → 显示"待处理"，不计算、不猜测**
3. **骨架恒在**：无论订单处于哪个状态，8 个节点全部渲染
4. **区分 Steps 与 Timeline**：Steps 展示"应经过什么流程"，Timeline 展示"实际发生了什么"

### 7.2 人员解析策略（关键改进）

日志表 `operator_id` 全为 NULL，但**主表人员字段是真实可靠的**，因此主干节点操作人优先取主表：

| 节点 | 人员来源 |
| --- | --- |
| 订单创建 | `broadband_order.customer_id`（客户提交） |
| 订单审核 | `broadband_order.auditor_id` |
| 生成工单 | SYSTEM（自动） |
| 已派单 | `work_order.dispatcher_id` |
| 装维接单 | `work_order.installer_id` |
| 上门安装 | `work_order.installer_id` |
| 安装完成 | `work_order.installer_id` |
| 订单归档 | `broadband_order.cs_id`（客服确认） |

**姓名解析优先级需修复为**：`nickname → real_name → username`
（当前是 `real_name → username`，而 real_name 全 NULL，导致只能拿到 username）

**自动/人工判定**：
- `operator_role = SYSTEM` → 自动
- `dispatch_record.strategy = AUTO` → 自动派单
- 有具体人员 ID → 人工

### 7.3 订单履约骨架（8 节点）

| # | 节点 | 订单状态 | 工单状态 | 真实时间来源 | 操作人 |
| --- | --- | --- | --- | --- | --- |
| 1 | 订单创建 | CREATED | — | `broadband_order.create_time` | customer_id |
| 2 | 订单审核 | AUDITED | — | `broadband_order.audit_time` | auditor_id |
| 3 | 生成工单 | WAIT_DISPATCH | PENDING | `work_order.create_time` | SYSTEM |
| 4 | 已派单 | DISPATCHED | DISPATCHED | `broadband_order.dispatch_time` | dispatcher_id |
| 5 | 装维接单 | INSTALLING | ACCEPTED | `work_order.accept_time` | installer_id |
| 6 | 上门安装 | INSTALLING | IN_PROGRESS | `work_order.start_time` | installer_id |
| 7 | 安装完成 | FINISHED | COMPLETED | `work_order.finish_time` | installer_id |
| 8 | 订单归档 | CLOSED | — | `broadband_order.update_time` | cs_id |

**异常分支**（条件渲染，非固定节点）：
- REJECTED（驳回）→ 插在节点 2 之后，时间取 `audit_time`，原因取 remark
- CANCELLED（取消）→ 终止流程，时间取 `cancelled_time`
- 工单 STALLED / REASSIGNING / AUTO_CANCELLED / FAILED → 在对应节点标记异常

**多工单处理**：取"主工单"= 非 AUTO_CANCELLED/CANCELLED 的最新一条；改派产生的历史工单在 Timeline 中作为事件展示，不覆盖主干。

### 7.4 工单处理骨架（6 节点）

| # | 节点 | 状态 | 真实时间来源 | 操作人 |
| --- | --- | --- | --- | --- |
| 1 | 工单创建 | PENDING | `create_time` | SYSTEM |
| 2 | 已派单 | DISPATCHED | `dispatch_time` | dispatcher_id |
| 3 | 已接单 | ACCEPTED | `accept_time` | installer_id |
| 4 | 施工中 | IN_PROGRESS | `start_time` | installer_id |
| 5 | 已完成 | COMPLETED | `finish_time` | installer_id |

异常分支：STALLED / REASSIGNING / FAILED / CANCELLED / AUTO_CANCELLED

### 7.5 节点状态判定

```
DONE       该节点真实时间存在，且不是最后一个已完成节点
CURRENT    最后一个已完成节点（流程未终结）→ 高亮 + 实时等待时长
PENDING    时间字段为空
EXCEPTION  当前状态 ∈ {REJECTED, CANCELLED, STALLED, FAILED, REASSIGNING, AUTO_CANCELLED}
```

### 7.6 响应结构

```jsonc
{
  "stages": [                       // Steps + 左轨骨架，恒返回全部节点
    {
      "code": "DISPATCHED",         // 节点编码
      "name": "已派单",              // 中文名
      "state": "CURRENT",           // DONE | CURRENT | PENDING | EXCEPTION
      "time": "2026-09-05 14:32",   // 真实时间，无则 null
      "operatorName": "调度员A",
      "operatorRole": "调度人员",
      "isAuto": false
    }
  ],
  "events": [                       // 右轨 Timeline，真实操作事件
    {
      "time": "2026-09-05 13:35",
      "title": "订单审核",
      "desc": "待审核 → 已审核",
      "operatorName": "审核员B",
      "operatorRole": "审核人员",
      "source": "ORDER_AUDIT",
      "isAuto": false,
      "remark": "资料齐全"
    }
  ],
  "summary": {
    "currentStatus": "DISPATCHED",
    "currentStatusDesc": "已派单",
    "currentStageIndex": 3,
    "progress": "4/8",
    "elapsed": "1小时12分钟",       // create_time → now（未终结）或 → completed_time
    "waiting": "18分钟",            // 最后一个事件 → now
    "isTerminal": false
  }
}
```

---

## 八、实施计划（第四步）

按「后端数据 → API → 前端状态映射 → Steps → 双轨 Timeline → UI」顺序：

| 步骤 | 内容 | 层 |
| --- | --- | --- |
| 1 | 新增轨迹合成服务：主表时间字段 + 主表人员字段 + 日志补充 | 后端 |
| 2 | 修复姓名解析：`nickname → real_name → username` | 后端 |
| 3 | 新增 `GET /api/orders/{id}/track` 与 `GET /api/work-orders/{id}/track` | 后端 |
| 4 | 前端 API 层 + TS 类型 | 前端 |
| 5 | 状态映射统一到 `BBPMSStatusTag` 真源，补齐漏项 | 前端 |
| 6 | 顶部 Steps 组件 | 前端 |
| 7 | `OrderTimeline.vue` 骨架驱动重写 | 前端 |
| 8 | 顶栏汇总卡（当前状态/进度/已用时/等待）实时刷新 | 前端 |

---

## 九、风险与约束

| 风险 | 应对 |
| --- | --- |
| 历史订单时间字段为 NULL | 按"不伪造"原则显示"待处理"，不猜测 |
| 1 订单多工单 | 取主工单（非取消的最新一条），改派历史作为事件 |
| seed 数据日志稀疏 | 主干不依赖日志，用主表时间字段；日志仅补充细节 |
| 死状态 PENDING_CS_CONFIRM/CS_REJECTED | 不纳入骨架（状态机无流转），仅做映射兜底 |
| 修改现有 timeline 字段可能影响其他调用方 | 保留原 `timeline` 字段不变，新增 `track` 字段/接口 |
