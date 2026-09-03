/**
 * Dashboard 领域类型（设计文档 §3.3，与后端 T02 VO 严格对齐）
 *
 * 对应关系：
 *  - KpiItem          ← com.bbpms.dashboard.vo.KpiItemVO
 *  - StatusDistItem   ← com.bbpms.dashboard.vo.StatusDistItemVO
 *  - DashboardOverview← com.bbpms.dashboard.vo.DashboardOverviewVO
 *  - DashboardTrend   ← com.bbpms.dashboard.vo.DashboardTrendVO
 *  - DashboardDispatch← com.bbpms.dashboard.vo.DashboardDispatchVO
 *  - DispatchStat     ← com.bbpms.dispatch.vo.DispatchStatVO
 */

/** KPI 指标键（后端固定返回 6 个，缺一即视为契约破坏） */
export type KpiKey =
  | 'todayOrders'
  | 'todayFinishedOrders'
  | 'pendingAuditOrders'
  | 'runningWorkorders'
  | 'todayFinishedWorkorders'
  | 'stalledWorkorders'

/**
 * 单个 KPI 项。
 * 口径说明（§3.4）：存量指标（待审核/进行中/停滞）prevValue/deltaRate 恒为 null，
 * 前端不渲染环比——这是诚实的设计，不得伪造对比基准。
 */
export interface KpiItem {
  key: KpiKey
  value: number | null
  prevValue: number | null
  deltaRate: number | null
  unit: string
}

/** 状态分布项（订单 8 态 / 工单 10 态，存量全量口径） */
export interface StatusDistItem {
  status: string
  statusDesc: string
  count: number
}

/** GET /dashboard/overview 响应 */
export interface DashboardOverview {
  generatedAt: string
  days: number
  kpis: KpiItem[]
  orderStatusDist: StatusDistItem[]
  workOrderStatusDist: StatusDistItem[]
}

/** GET /dashboard/trend 响应（三数组严格等长，缺失日已由后端补 0） */
export interface DashboardTrend {
  dates: string[]
  orderCreatedCounts: number[]
  workOrderFinishedCounts: number[]
}

/** 派单时效分桶编码（固定 5 桶，含 count=0） */
export type BucketCode = 'LT_30M' | 'M30_60' | 'H1_2' | 'H2_4' | 'GT_4H'

/** 单个时效桶 */
export interface DurationBucket {
  bucket: BucketCode
  label: string
  count: number
}

/** GET /dashboard/dispatch-duration 响应 */
export interface DashboardDispatch {
  days: number
  durationBuckets: DurationBucket[]
}

/** GET /dispatch/stats 响应（回看 days 天的派单聚合） */
export interface DispatchStat {
  date: string | null
  total: number
  autoCount: number
  manualCount: number
  reassignCount: number
  avgScore: number | null
}

/** 待办与风险区的人员侧计数汇总（来自既有 installer/attendance/leave 接口） */
export interface CountSummary {
  onlineInstallers: number
  onDuty: number
  pendingLeaves: number
}
