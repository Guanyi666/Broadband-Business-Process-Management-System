<script setup lang="ts">
/**
 * 数据看板（设计文档 §7 T03 三段式布局）
 *
 * ① 今日态势：1 张 L1 大卡 + 7 张 L2 指标卡（KPI / 人员计数）
 * ② 流程健康：趋势折线 + 派单策略环图 + 订单/工单状态分布 + 派单时效 + 平均派单评分仪表
 * ③ 待办与风险：SLA 临期预警（倒计时）+ 最新工单动态
 *
 * 红线：所有数据来自 useDashboard 真实接口；单模块失败独立降级 + 重试；
 *       空数据显示空态；绝不显示随机 / 硬编码数据。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Refresh, WarningFilled } from '@element-plus/icons-vue'
import BBPMSChart from '@/components/BBPMSChart.vue'
import BBPMSKpiCard from '@/components/BBPMSKpiCard.vue'
import BBPMSPanel from '@/components/BBPMSPanel.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'
import { useDashboard } from '@/composables/useDashboard'
import { buildBarOption, buildDonutOption, buildGaugeOption, buildLineOption } from '@/utils/chart-theme'
import type { KpiItem } from '@/types/dashboard'

// ---------- 数据编排 ----------
const days = ref(7)
const dashboard = useDashboard(days)
const { overview, trend, dispatch, buckets, sla, latest, counts } = dashboard

onMounted(() => {
  dashboard.refreshAll()
})

// ---------- KPI 辅助 ----------
function kpi(key: KpiItem['key']): KpiItem | null {
  return overview.data.value?.kpis?.find((k) => k.key === key) ?? null
}

const runningWorkorders = computed(() => kpi('runningWorkorders'))
const todayOrders = computed(() => kpi('todayOrders'))
const pendingAuditOrders = computed(() => kpi('pendingAuditOrders'))
const todayFinishedWorkorders = computed(() => kpi('todayFinishedWorkorders'))
const stalledWorkorders = computed(() => kpi('stalledWorkorders'))

const generatedAt = computed(() => overview.data.value?.generatedAt?.slice(0, 16) ?? '')

// ---------- 图表配置（computed，随模块数据响应式更新） ----------
const trendOption = computed(() => {
  const d = trend.data.value
  if (!d || !d.dates?.length) return buildLineOption([], [], '单')
  return buildLineOption(
    d.dates,
    [
      { name: '订单新建', data: d.orderCreatedCounts ?? [], area: true },
      { name: '工单完成', data: d.workOrderFinishedCounts ?? [] }
    ],
    '单'
  )
})
const trendEmpty = computed(() => !trend.data.value?.dates?.length)

const dispatchTotal = computed(() => {
  const d = dispatch.data.value
  if (!d) return 0
  return (d.autoCount ?? 0) + (d.manualCount ?? 0) + (d.reassignCount ?? 0)
})

const donutOption = computed(() => {
  const d = dispatch.data.value
  return buildDonutOption([
    { name: '自动派单', value: d?.autoCount ?? 0 },
    { name: '手动派单', value: d?.manualCount ?? 0 },
    { name: '改派', value: d?.reassignCount ?? 0 }
  ])
})

const orderDistOption = computed(() => {
  const dist = overview.data.value?.orderStatusDist ?? []
  return buildBarOption(
    dist.map((i) => i.statusDesc || i.status),
    dist.map((i) => i.count),
    '单',
    true
  )
})
const orderDistEmpty = computed(() => !(overview.data.value?.orderStatusDist?.length))

const workOrderDistOption = computed(() => {
  const dist = overview.data.value?.workOrderStatusDist ?? []
  return buildBarOption(
    dist.map((i) => i.statusDesc || i.status),
    dist.map((i) => i.count),
    '单',
    true
  )
})
const workOrderDistEmpty = computed(() => !(overview.data.value?.workOrderStatusDist?.length))

const bucketOption = computed(() => {
  const list = buckets.data.value ?? []
  return buildBarOption(
    list.map((b) => b.label || b.bucket),
    list.map((b) => b.count),
    '单'
  )
})
const bucketEmpty = computed(() => !(buckets.data.value?.length))

const gaugeOption = computed(() => buildGaugeOption(dispatch.data.value?.avgScore ?? 0, '分', 5))
const gaugeEmpty = computed(() => dispatch.data.value?.avgScore == null)

// ---------- SLA 倒计时（60s 心跳，仅本地展示刷新） ----------
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null
onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 60_000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

/** 距预计完成时间的剩余分钟数；负数表示已超时 */
function remainingMinutes(expected?: string | null): number | null {
  if (!expected) return null
  const ts = new Date(expected.replace(/-/g, '/')).getTime()
  if (Number.isNaN(ts)) return null
  return Math.round((ts - now.value) / 60_000)
}

const slaList = computed(() => sla.data.value ?? [])
const latestList = computed(() => latest.data.value ?? [])

function formatTime(t?: string | null): string {
  return t ? t.slice(5, 16) : '-'
}
</script>

<template>
  <div class="dashboard-page">
    <!-- 页头：标题 + 数据时间 + 时间窗口 + 刷新 -->
    <header class="dash-header">
      <div class="dash-header__titles">
        <h2 class="dash-header__title">数据看板</h2>
        <span v-if="generatedAt" class="dash-header__time">数据生成于 {{ generatedAt }}</span>
      </div>
      <div class="dash-header__actions">
        <el-radio-group v-model="days" size="small">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" size="small" :loading="overview.loading.value" @click="dashboard.refreshAll()">
          刷新
        </el-button>
      </div>
    </header>

    <!-- ① 今日态势 -->
    <section class="dash-section">
      <h3 class="section-title">今日态势</h3>

      <!-- overview 失败：仅此区块降级 + 重试，不影响下方模块 -->
      <div v-if="overview.error.value" class="dash-error">
        <el-icon :size="32" color="#f56c6c"><WarningFilled /></el-icon>
        <p class="dash-error__text">今日态势加载失败：{{ overview.error }}</p>
        <el-button type="primary" size="small" @click="overview.load()">重试</el-button>
      </div>

      <div v-else class="kpi-grid">
        <div class="kpi-grid__l1">
          <BBPMSKpiCard
            level="l1"
            tone="primary"
            icon="Odometer"
            label="进行中工单"
            :value="runningWorkorders?.value ?? null"
            :unit="runningWorkorders?.unit || '个'"
            :loading="overview.loading.value"
          />
        </div>
        <BBPMSKpiCard
          tone="primary"
          icon="Calendar"
          label="今日新增订单"
          :value="todayOrders?.value ?? null"
          :unit="todayOrders?.unit || '单'"
          :prev="todayOrders?.prevValue ?? null"
          :trend="todayOrders?.deltaRate ?? null"
          :loading="overview.loading.value"
        />
        <BBPMSKpiCard
          tone="warning"
          icon="Bell"
          label="待审核订单"
          :value="pendingAuditOrders?.value ?? null"
          :unit="pendingAuditOrders?.unit || '单'"
          :loading="overview.loading.value"
        />
        <BBPMSKpiCard
          tone="success"
          icon="CircleCheck"
          label="今日完成工单"
          :value="todayFinishedWorkorders?.value ?? null"
          :unit="todayFinishedWorkorders?.unit || '个'"
          :prev="todayFinishedWorkorders?.prevValue ?? null"
          :trend="todayFinishedWorkorders?.deltaRate ?? null"
          :loading="overview.loading.value"
        />
        <BBPMSKpiCard
          tone="danger"
          icon="WarningFilled"
          label="停滞工单"
          :value="stalledWorkorders?.value ?? null"
          :unit="stalledWorkorders?.unit || '个'"
          :loading="overview.loading.value"
        />

        <!-- 人员侧计数：counts 模块独立失败，单独降级 -->
        <template v-if="!counts.error.value">
          <BBPMSKpiCard
            tone="primary"
            icon="User"
            label="在线装维"
            :value="counts.data.value?.onlineInstallers ?? null"
            unit="人"
            :loading="counts.loading.value"
          />
          <BBPMSKpiCard
            tone="success"
            icon="Avatar"
            label="在岗人数"
            :value="counts.data.value?.onDuty ?? null"
            unit="人"
            :loading="counts.loading.value"
          />
          <BBPMSKpiCard
            tone="warning"
            icon="Document"
            label="待审批请假"
            :value="counts.data.value?.pendingLeaves ?? null"
            unit="人"
            :loading="counts.loading.value"
          />
        </template>
        <div v-else class="kpi-grid__counts-error">
          <span class="kpi-grid__counts-error-text">人员数据加载失败</span>
          <el-button type="primary" link size="small" @click="counts.load()">重试</el-button>
        </div>
      </div>
    </section>

    <!-- ② 流程健康 -->
    <section class="dash-section">
      <h3 class="section-title">流程健康</h3>
      <div class="bbpms-grid chart-grid">
        <BBPMSPanel
          class="bbpms-col-8 panel-trend"
          :title="`近 ${days} 日趋势`"
          subtitle="订单新建 vs 工单完成"
          :loading="trend.loading.value"
          :error="trend.error.value"
          @retry="trend.load()"
        >
          <BBPMSChart :option="trendOption" height="100%" :empty="trendEmpty" empty-text="暂无趋势数据" />
        </BBPMSPanel>

        <BBPMSPanel
          class="bbpms-col-4"
          title="派单策略构成"
          :subtitle="`近 ${days} 日`"
          :loading="dispatch.loading.value"
          :error="dispatch.error.value"
          @retry="dispatch.load()"
        >
          <BBPMSChart
            :option="donutOption"
            height="100%"
            :empty="dispatchTotal === 0"
            empty-text="暂无派单记录"
          />
        </BBPMSPanel>

        <BBPMSPanel
          class="bbpms-col-6"
          title="订单状态分布"
          subtitle="存量全量口径"
          :loading="overview.loading.value"
          :error="overview.error.value"
          @retry="overview.load()"
        >
          <BBPMSChart :option="orderDistOption" height="100%" :empty="orderDistEmpty" />
        </BBPMSPanel>

        <BBPMSPanel
          class="bbpms-col-6"
          title="工单状态分布"
          subtitle="存量全量口径"
          :loading="overview.loading.value"
          :error="overview.error.value"
          @retry="overview.load()"
        >
          <BBPMSChart :option="workOrderDistOption" height="100%" :empty="workOrderDistEmpty" />
        </BBPMSPanel>

        <BBPMSPanel
          class="bbpms-col-8"
          title="派单时效分布"
          subtitle="派单 → 接单耗时"
          :loading="buckets.loading.value"
          :error="buckets.error.value"
          @retry="buckets.load()"
        >
          <BBPMSChart :option="bucketOption" height="100%" :empty="bucketEmpty" empty-text="暂无派单时效数据" />
        </BBPMSPanel>

        <BBPMSPanel
          class="bbpms-col-4"
          title="平均派单评分"
          :subtitle="`近 ${days} 日`"
          :loading="dispatch.loading.value"
          :error="dispatch.error.value"
          @retry="dispatch.load()"
        >
          <BBPMSChart :option="gaugeOption" height="100%" :empty="gaugeEmpty" empty-text="暂无评分数据" />
        </BBPMSPanel>
      </div>
    </section>

    <!-- ③ 待办与风险 -->
    <section class="dash-section">
      <h3 class="section-title">待办与风险</h3>
      <div class="bbpms-grid risk-grid">
        <BBPMSPanel
          class="bbpms-col-6"
          title="SLA 临期预警"
          subtitle="预计完成时间 30 分钟内到期"
          :loading="sla.loading.value"
          :error="sla.error.value"
          :empty="slaList.length === 0"
          empty-text="暂无临期工单"
          @retry="sla.load()"
        >
          <ul class="risk-list">
            <li v-for="wo in slaList" :key="wo.id" class="risk-list__item">
              <div class="risk-list__main">
                <span class="risk-list__no">{{ wo.workNo }}</span>
                <span class="risk-list__addr" :title="wo.installAddress">{{ wo.installAddress || '—' }}</span>
              </div>
              <div class="risk-list__meta">
                <BBPMSStatusTag :status="wo.status" :label="wo.statusDesc || wo.status" />
                <el-tag
                  :type="(remainingMinutes(wo.expectedFinishTime) ?? 0) <= 0 ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ (remainingMinutes(wo.expectedFinishTime) ?? 0) <= 0 ? '已到期' : `剩 ${remainingMinutes(wo.expectedFinishTime)} 分钟` }}
                </el-tag>
              </div>
            </li>
          </ul>
        </BBPMSPanel>

        <BBPMSPanel
          class="bbpms-col-6"
          title="最新工单动态"
          :loading="latest.loading.value"
          :error="latest.error.value"
          :empty="latestList.length === 0"
          empty-text="暂无工单动态"
          @retry="latest.load()"
        >
          <ul class="risk-list">
            <li v-for="wo in latestList" :key="wo.id" class="risk-list__item">
              <div class="risk-list__main">
                <span class="risk-list__no">{{ wo.workNo }}</span>
                <span class="risk-list__addr" :title="wo.installAddress">{{ wo.installAddress || '—' }}</span>
              </div>
              <div class="risk-list__meta">
                <BBPMSStatusTag :status="wo.status" :label="wo.statusDesc || wo.status" />
                <span class="risk-list__time">{{ formatTime(wo.dispatchTime || wo.createTime) }}</span>
              </div>
            </li>
          </ul>
        </BBPMSPanel>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

// —— 页头 ——
.dash-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  &__titles {
    display: flex;
    align-items: baseline;
    gap: 12px;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: #303133;
  }

  &__time {
    font-size: 12px;
    color: #909399;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.dash-section {
  min-width: 0; // 防 grid 子项被长内容撑破
}

// —— ① KPI 栅格：≥1440 五列（L1 跨 2）→ 1024-1439 三列 → <1024 两列 ——
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;

  &__l1 {
    grid-column: span 2;
  }

  &__counts-error {
    grid-column: span 3;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border: 1px dashed var(--el-border-color, #dcdfe6);
    border-radius: 8px;
    min-height: 104px;
  }

  &__counts-error-text {
    font-size: 13px;
    color: #909399;
  }
}

// —— 概览失败占位 ——
.dash-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 200px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 8px;

  &__text {
    margin: 0;
    font-size: 13px;
    color: #909399;
  }
}

// —— ② 图表区：面板体高度由断点决定，图表 height=100% 自适应 ——
.chart-grid {
  .bbpms-panel {
    :deep(.bbpms-panel__body) {
      height: 320px;
    }
  }
}

// —— ③ 待办与风险列表 ——
.risk-grid {
  .bbpms-panel {
    :deep(.bbpms-panel__body) {
      max-height: 360px;
      overflow-y: auto;
    }
  }
}

.risk-list {
  margin: 0;
  padding: 0;
  list-style: none;

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 4px;
    border-bottom: 1px solid #f0f2f5;

    &:last-child {
      border-bottom: none;
    }
  }

  &__main {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__no {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
  }

  &__addr {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 320px;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 0 0 auto;
  }

  &__time {
    font-size: 12px;
    color: #c0c4cc;
  }
}

// —— 响应式（断点与 variables.scss 对齐：lg 1440 / md 1024）——
@media (min-width: 1024px) and (max-width: 1439px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);

    &__l1 {
      grid-column: span 3; // L1 卡独占一行，避免三列下挤压
    }
  }

  .chart-grid {
    .bbpms-col-8,
    .bbpms-col-6,
    .bbpms-col-4 {
      grid-column: span 12; // 图表纵向堆叠
    }

    .bbpms-panel :deep(.bbpms-panel__body) {
      height: 280px;
    }
  }
}

@media (max-width: 1023px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);

    &__l1 {
      grid-column: span 2;
    }

    &__counts-error {
      grid-column: span 2;
    }
  }

  .chart-grid,
  .risk-grid {
    .bbpms-col-8,
    .bbpms-col-6,
    .bbpms-col-4 {
      grid-column: span 12;
    }
  }

  .chart-grid .bbpms-panel :deep(.bbpms-panel__body) {
    height: 260px;
  }

  .risk-list__addr {
    max-width: 160px;
  }
}
</style>
