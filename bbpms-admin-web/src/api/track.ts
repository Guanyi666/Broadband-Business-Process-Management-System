import request from '@/utils/request'
import type { OrderTrack, TrackStage, TrackEvent, TrackSummary, StageState } from '@/types/track'
import type { OrderStatus } from '@/types/order'
import type { WorkorderStatus } from '@/types/order'

/**
 * 双轨时间线轨迹接口（docs/TIMELINE_ANALYSIS.md §7.6 / §8）。
 * 后端同步实现中；前端严格按契约字段名消费。
 */

export async function getOrderTrack(id: number | string): Promise<OrderTrack> {
  return request<OrderTrack>({ url: `/orders/${id}/track`, method: 'GET' })
}

export async function getWorkorderTrack(id: number | string): Promise<OrderTrack> {
  return request<OrderTrack>({ url: `/work-orders/${id}/track`, method: 'GET' })
}

/* ------------------------------------------------------------------ */
/* 本地验证用 mock（仅当后端 /track 尚未就绪时，由调用方显式开启）。     */
/* 不伪造时间：传入的 realTimes 为空则对应阶段 time = null → 显示待处理。 */
/* ------------------------------------------------------------------ */

const ORDER_STAGE_DEFS: { code: string; name: string }[] = [
  { code: 'CREATED', name: '订单创建' },
  { code: 'AUDITED', name: '订单审核' },
  { code: 'WAIT_DISPATCH', name: '生成工单' },
  { code: 'DISPATCHED', name: '已派单' },
  { code: 'INSTALLING', name: '装维接单' },
  { code: 'INSTALLING', name: '上门安装' },
  { code: 'FINISHED', name: '安装完成' },
  { code: 'CLOSED', name: '订单归档' }
]

const ORDER_STATUS_RANK: Record<string, number> = {
  CREATED: 0,
  AUDITED: 1,
  WAIT_DISPATCH: 2,
  DISPATCHED: 3,
  INSTALLING: 4,
  FINISHED: 6,
  CLOSED: 7,
  CANCELLED: 99,
  REJECTED: 99
}

export interface MockTrackInput {
  status: string
  /** 真实主表时间字段，缺失即为 null（前端显示「待处理」） */
  realTimes?: Partial<Record<string, string | null>>
  /** 是否终态 */
  isTerminal?: boolean
}

/** 依据订单状态推导 8 节点骨架（状态/时间均来自真实数据或 null） */
export function buildMockOrderTrack(input: MockTrackInput): OrderTrack {
  const rank = ORDER_STATUS_RANK[input.status] ?? 0
  const terminal = input.isTerminal ?? ['CLOSED', 'CANCELLED', 'REJECTED'].includes(input.status)
  const exception = ['CANCELLED', 'REJECTED'].includes(input.status)

  const stages: TrackStage[] = ORDER_STAGE_DEFS.map((def, i) => {
    const t = input.realTimes?.[def.code] ?? null
    let state: StageState
    if (exception && i === 0) state = 'EXCEPTION'
    else if (terminal && i <= rank) state = 'DONE'
    else if (i < rank) state = 'DONE'
    else if (i === rank) state = terminal ? 'DONE' : 'CURRENT'
    else state = 'PENDING'
    return { code: def.code, name: def.name, state, time: t, operatorName: null, operatorRole: null, isAuto: false }
  })

  const events: TrackEvent[] = stages
    .filter((s) => s.state === 'DONE' && s.time)
    .map((s) => ({ time: s.time, title: s.name, desc: s.name, operatorName: s.operatorName, operatorRole: s.operatorRole, source: 'ORDER_AUDIT', isAuto: s.isAuto, remark: null }))

  const doneCount = stages.filter((s) => s.state === 'DONE').length
  const summary: TrackSummary = {
    currentStatus: input.status,
    currentStatusDesc: STATUS_DESC[input.status] ?? input.status,
    currentStageIndex: stages.findIndex((s) => s.state === 'CURRENT'),
    progress: `${doneCount}/${stages.length}`,
    elapsed: input.realTimes?.CREATED ? `${Math.max(1, Math.round((Date.now() - new Date(input.realTimes.CREATED.replace(/-/g, '/')).getTime()) / 60000))} 分钟` : undefined,
    waiting: undefined,
    isTerminal: terminal
  }

  return { stages, events, summary }
}

const STATUS_DESC: Record<string, string> = {
  CREATED: '待审核',
  AUDITED: '已审核',
  WAIT_DISPATCH: '待派单',
  DISPATCHED: '已派单',
  INSTALLING: '安装中',
  FINISHED: '已完成',
  CLOSED: '已归档',
  CANCELLED: '已取消',
  REJECTED: '已驳回'
}
