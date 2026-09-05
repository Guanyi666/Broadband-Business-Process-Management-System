/**
 * 轨迹（双轨时间线）纯逻辑工具：时间解析、时长格式化、节点状态分类。
 * 不依赖 Vue，便于独立单元测试与运行时校验。
 */
import type { StageState, TrackStage } from '@/types/track'

/** 将字符串时间解析为毫秒；非法 / 空 → null（不猜测、不伪造） */
export function parseTimeToMs(time?: string | null): number | null {
  if (!time) return null
  const ms = new Date(String(time).replace(/-/g, '/')).getTime()
  return Number.isNaN(ms) ? null : ms
}

/**
 * 计算两个时间戳之间的可读时长。
 * @param startMs 起始时间（进入当前阶段的基准，可为 null）
 * @param endMs   结束时间（通常为 now）
 * 返回「X 分钟 / X 小时 Y 分钟 / X 天 Y 小时」或 null（基准缺失）
 */
export function formatDuration(startMs: number | null, endMs: number): string | null {
  if (startMs == null || Number.isNaN(startMs)) return null
  const diff = endMs - startMs
  if (diff < 0) return '0 分钟'
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return '不到 1 分钟'
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remain = minutes % 60
  if (hours < 24) return remain ? `${hours} 小时 ${remain} 分钟` : `${hours} 小时`
  const days = Math.floor(hours / 24)
  const rh = hours % 24
  return rh ? `${days} 天 ${rh} 小时` : `${days} 天`
}

/** 节点状态 → CSS class 片段 */
export function stateClass(state: StageState): string {
  switch (state) {
    case 'DONE':
      return 'is-done'
    case 'CURRENT':
      return 'is-current'
    case 'EXCEPTION':
      return 'is-exception'
    case 'SKIP':
      return 'is-skip'
    default:
      return 'is-pending'
  }
}

/**
 * 当前所处阶段索引（0 基）。
 * 优先取后端显式标注 CURRENT 的节点；否则取第一个 PENDING（即正在等待的节点）；
 * SKIP（系统自动跳过）不作为"当前"，若全部为 SKIP/PENDING 且无 CURRENT → 取第一个非 DONE 节点。
 * 终态（全部 DONE/EXCEPTION、无 PENDING）且无显式 CURRENT 时返回 -1。
 */
export function currentStageIndex(stages: TrackStage[]): number {
  const explicit = stages.findIndex((s) => s.state === 'CURRENT')
  if (explicit >= 0) return explicit
  const pending = stages.findIndex((s) => s.state === 'PENDING')
  if (pending >= 0) return pending
  const skip = stages.findIndex((s) => s.state === 'SKIP')
  return skip >= 0 ? skip : -1
}

/**
 * 当前等待时长的基准时间（毫秒）：
 * - 优先用当前节点的真实 time；
 * - 若当前节点 time 为空，回退到它之前最后一个 DONE 节点的 time（进入本阶段的实际时刻）；
 * - 均无 → null。
 */
export function waitingBaseMs(stages: TrackStage[]): number | null {
  const idx = currentStageIndex(stages)
  if (idx < 0) return null
  const current = stages[idx]
  const self = parseTimeToMs(current.time)
  if (self != null) return self
  for (let i = idx - 1; i >= 0; i--) {
    if (stages[i].state === 'DONE') {
      const t = parseTimeToMs(stages[i].time)
      if (t != null) return t
    }
  }
  return null
}
