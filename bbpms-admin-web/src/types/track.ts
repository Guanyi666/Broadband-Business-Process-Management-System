/**
 * 订单 / 工单「双轨时间线」轨迹数据结构
 * 严格遵循 docs/TIMELINE_ANALYSIS.md §7.6 响应结构契约。
 * 后端 GET /api/orders/{id}/track 与 GET /api/work-orders/{id}/track 按此返回。
 */

/** 节点视觉状态（左轨业务状态轨 + Steps 共用） */
export type StageState = 'DONE' | 'CURRENT' | 'PENDING' | 'EXCEPTION'

/** 单个流程阶段（骨架节点，恒返回全部节点，无论是否已完成） */
export interface TrackStage {
  /** 节点编码，如 CREATED / DISPATCHED */
  code: string
  /** 中文名，如「已派单」 */
  name: string
  /** 节点状态：已完成 / 当前 / 待处理 / 异常 */
  state: StageState
  /** 真实时间，无则 null → 前端显示「待处理」，绝不伪造 */
  time?: string | null
  /** 操作人姓名（主表真实人员字段，缺则 null） */
  operatorName?: string | null
  /** 操作人角色中文，如「调度人员」 */
  operatorRole?: string | null
  /** 是否系统自动操作 */
  isAuto?: boolean
  /** 异常原因（仅 EXCEPTION 节点） */
  remark?: string | null
}

/** 右轨人员 / 操作事件 */
export interface TrackEvent {
  /** 真实时间，无则 null */
  time?: string | null
  /** 事件标题，如「订单审核」 */
  title: string
  /** 业务描述，如「待审核 → 已审核」 */
  desc?: string
  /** 操作人姓名 */
  operatorName?: string | null
  /** 操作人角色中文 */
  operatorRole?: string | null
  /** 来源：ORDER_AUDIT / WORKORDER / DISPATCH */
  source?: string
  /** 是否系统自动操作（区分自动 / 人工） */
  isAuto?: boolean
  /** 备注说明 */
  remark?: string | null
}

/** 顶部汇总卡数据 */
export interface TrackSummary {
  /** 当前状态码，如 DISPATCHED */
  currentStatus?: string
  /** 当前状态中文，如「已派单」 */
  currentStatusDesc?: string
  /** 当前所处阶段索引（0 基），用于 Steps 高亮 */
  currentStageIndex?: number
  /** 进度文案，如「4/8」 */
  progress?: string
  /** 已用时文案，如「1小时12分钟」 */
  elapsed?: string
  /** 当前等待文案，如「18分钟」 */
  waiting?: string
  /** 是否已终结（CLOSED / CANCELLED / FAILED ...） */
  isTerminal?: boolean
}

/** 聚合响应：骨架 + 事件 + 汇总 */
export interface OrderTrack {
  stages: TrackStage[]
  events: TrackEvent[]
  summary: TrackSummary
}

export type WorkorderTrack = OrderTrack
