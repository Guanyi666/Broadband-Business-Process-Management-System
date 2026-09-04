/**
 * Dashboard 数据编排层（设计文档 §3.3 / §4.2）
 *
 * 核心约定：
 *  - 7 个模块完全独立加载，单模块失败只置自身 error，不拖垮其他模块
 *  - 每个模块自带 load()，供面板级「重试」单独重发
 *  - days 变化（7/14/30）触发全量刷新（设计文档 §4.2 时间范围切换路径）
 *  - 绝不回落随机/硬编码数据：失败即 error，空即空
 *
 * 权限门控（ITERATION 2 收尾修复）：
 *  - 每个模块声明所需权限码 requirePerm；当前用户无该权限时：
 *      · 不发起任何数据请求
 *      · 该模块数据为空、loading=false、error=null
 *      · 由页面侧按无权限处理（不渲染面板或渲染空态），不出现任何报错提示
 *  - 已发起的请求若因权限不足被拒（HTTP 403），catch 中静默降级
 *    （error 置 null、数据置空），不弹错、不阻塞其他模块。
 */
import { ref, unref, watch, type Ref } from 'vue'
import { getDispatchDuration, getOverview, getTrend } from '@/api/dashboard'
import { getDispatchStats } from '@/api/dispatch'
import { fetchExpiring, pageWorkorders, type WorkOrderVO } from '@/api/workorder'
import { listOnlineInstallers } from '@/api/installer'
import { fetchOnDuty } from '@/api/attendance'
import { fetchPendingApprovals } from '@/api/leave'
import { useAuthStore } from '@/stores/auth'
import type {
  DashboardOverview,
  DashboardTrend,
  DispatchStat,
  DurationBucket
} from '@/types/dashboard'
import type { WorkorderItem } from '@/types/order'

/** 单个数据模块：数据 + 加载态 + 错误态 + 独立重试入口 + 权限门控 */
export interface DashboardModule<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  error: Ref<string | null>
  /** 当前用户是否拥有该模块所需权限；无权限时不渲染对应面板 */
  permitted: Ref<boolean>
  load: () => Promise<void>
}

/** 待办与风险区的人员侧计数汇总（来自既有 installer/attendance/leave 接口） */
export interface CountSummary {
  onlineInstallers: number
  onDuty: number
  pendingLeaves: number
}

/** 判断错误是否为 HTTP 403（权限不足） */
function isForbidden(e: unknown): boolean {
  return (
    e != null &&
    typeof e === 'object' &&
    'response' in e &&
    (e as { response?: { status?: number } }).response?.status === 403
  )
}

function toMessage(e: unknown): string {
  if (e instanceof Error && e.message) return e.message
  if (typeof e === 'string' && e) return e
  return '请求失败，请稍后重试'
}

/**
 * @param days 统计窗口天数；支持响应式 Ref（切换 7/14/30 时自动刷新）
 */
export function useDashboard(days: Ref<number> | number = 7) {
  const daysRef: Ref<number> = typeof days === 'number' ? ref(days) : days
  const auth = useAuthStore()

  function createModule<T>(
    fetcher: (d: number) => Promise<T>,
    requirePerm?: string | string[]
  ): DashboardModule<T> {
    const data = ref<T | null>(null) as Ref<T | null>
    const loading = ref(false)
    const error = ref<string | null>(null)
    const permitted = ref(!requirePerm || auth.hasPermission(requirePerm))

    async function load(): Promise<void> {
      // 无权限：不发请求，模块保持空态且不报错（页面侧已按 permitted 隐藏/留白）。
      if (!permitted.value) {
        data.value = null
        error.value = null
        return
      }
      loading.value = true
      error.value = null
      try {
        data.value = await fetcher(unref(daysRef))
      } catch (e) {
        if (isForbidden(e)) {
          // 已请求但 403：权限被后端拒绝（如权限数据未及时同步）。
          // 静默还原为「未加载」状态 —— 不提示、不阻断其他模块。
          data.value = null
          error.value = null
          permitted.value = false
        } else {
          data.value = null
          error.value = toMessage(e)
        }
      } finally {
        loading.value = false
      }
    }

    return { data, loading, error, permitted, load }
  }

  /** 模块①：今日态势 + 状态分布（KPI 与两个分布共用同一接口，失败时一起降级） */
  const overview = createModule<DashboardOverview>((d) => getOverview(d), 'dashboard:view')

  /** 模块②：趋势（订单新建 vs 工单完成，缺失日后端已补 0） */
  const trend = createModule<DashboardTrend>((d) => getTrend(d), 'dashboard:view')

  /** 模块③a：派单质量 —— 策略构成 + 平均评分（需要派单查看权限） */
  const dispatch = createModule<DispatchStat>((d) => getDispatchStats(d), 'dispatch:view')

  /** 模块③b：派单时效分桶（固定 5 桶，含 count=0） */
  const buckets = createModule<DurationBucket[]>(async (d) =>
    (await getDispatchDuration(d)).durationBuckets
  , 'dashboard:view')

  /** 模块④a：SLA 临期预警（预计完成时间 30 分钟内到期，需要 SLA 查看权限） */
  const sla = createModule<WorkOrderVO[]>(() => fetchExpiring(30), 'workorder:sla:view')

  /** 模块④b：最新工单动态（首页 8 条，工单查看权限） */
  const latest = createModule<WorkorderItem[]>(async () => {
    const page = await pageWorkorders({ pageNum: 1, pageSize: 8 })
    return page.list
  }, 'workorder:view')

  /** 模块④c：人员侧计数（在线装维 / 在岗 / 待审批请假）—— 三个接口权限不同，任一无权限即整体隐藏 */
  const counts = createModule<CountSummary>(async () => {
    const [online, onDuty, pending] = await Promise.all([
      listOnlineInstallers(),
      fetchOnDuty(),
      fetchPendingApprovals()
    ])
    return {
      onlineInstallers: online?.length ?? 0,
      onDuty: onDuty?.length ?? 0,
      pendingLeaves: pending?.length ?? 0
    }
  }, ['installer:view', 'attendance:view-all', 'leave:approve-l1', 'leave:approve-l2', 'leave:approve'])

  const modules = [overview, trend, dispatch, buckets, sla, latest, counts]

  /** 全量刷新：7 个模块并行（days 切换 / 手动刷新入口） */
  async function refreshAll(): Promise<void> {
    await Promise.all(modules.map((m) => m.load()))
  }

  watch(daysRef, () => {
    refreshAll()
  })

  return { days: daysRef, overview, trend, dispatch, buckets, sla, latest, counts, refreshAll }
}