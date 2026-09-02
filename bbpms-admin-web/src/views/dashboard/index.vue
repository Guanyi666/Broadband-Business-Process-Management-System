<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import {
  getDashboardKpi,
  getOrderTrend,
  getOrderStatusDist,
  getDispatchEfficiency,
  type DashboardKpi,
  type TrendPoint,
  type StatusSlice,
  type EfficiencyBucket
} from '@/api/dashboard'

const kpi = ref<DashboardKpi>({
  todayOrders: 0,
  todayAuditPending: 0,
  runningWorkorders: 0,
  completedToday: 0
})

const trendRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
const barRef = ref<HTMLDivElement>()

let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

async function initTrendChart(data: TrendPoint[]) {
  if (!trendRef.value) return
  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d) => d.date) },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'Orders',
        type: 'line',
        smooth: true,
        data: data.map((d) => d.count),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.4)' },
            { offset: 1, color: 'rgba(64,158,255,0)' }
          ])
        },
        lineStyle: { color: '#409eff' },
        itemStyle: { color: '#409eff' }
      }
    ]
  })
}

async function initPieChart(data: StatusSlice[]) {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: data.map((d) => ({ name: d.status, value: d.count }))
      }
    ]
  })
}

async function initBarChart(data: EfficiencyBucket[]) {
  if (!barRef.value) return
  barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d) => d.bucket) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.count),
        barWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67c23a' },
            { offset: 1, color: '#b3e19d' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  })
}

function onResize() {
  trendChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(async () => {
  try {
    const [k, t, s, e] = await Promise.all([
      getDashboardKpi(),
      getOrderTrend(7),
      getOrderStatusDist(),
      getDispatchEfficiency()
    ])
    kpi.value = k
    initTrendChart(t)
    initPieChart(s)
    initBarChart(e)
  } catch (e) {
    // chart fallback mock
    initTrendChart(
      Array.from({ length: 7 }).map((_, i) => ({
        date: `D-${6 - i}`,
        count: Math.floor(Math.random() * 50) + 10
      }))
    )
    initPieChart([
      { status: 'Pending', count: 12 },
      { status: 'Approved', count: 24 },
      { status: 'Installing', count: 16 },
      { status: 'Done', count: 38 },
      { status: 'Cancelled', count: 3 }
    ])
    initBarChart([
      { bucket: '<30m', count: 8 },
      { bucket: '30-60m', count: 14 },
      { bucket: '1-2h', count: 22 },
      { bucket: '2-4h', count: 9 },
      { bucket: '>4h', count: 4 }
    ])
  }
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<template>
  <div class="dashboard app-container">
    <div class="kpi-row">
      <div class="kpi-card kpi-blue">
        <div class="kpi-label">Today's Orders</div>
        <div class="kpi-value">{{ kpi.todayOrders }}</div>
        <div class="kpi-icon"><el-icon><Document /></el-icon></div>
      </div>
      <div class="kpi-card kpi-orange">
        <div class="kpi-label">Pending Audit</div>
        <div class="kpi-value">{{ kpi.todayAuditPending }}</div>
        <div class="kpi-icon"><el-icon><Bell /></el-icon></div>
      </div>
      <div class="kpi-card kpi-purple">
        <div class="kpi-label">Running Workorders</div>
        <div class="kpi-value">{{ kpi.runningWorkorders }}</div>
        <div class="kpi-icon"><el-icon><Tools /></el-icon></div>
      </div>
      <div class="kpi-card kpi-green">
        <div class="kpi-label">Completed Today</div>
        <div class="kpi-value">{{ kpi.completedToday }}</div>
        <div class="kpi-icon"><el-icon><CircleCheck /></el-icon></div>
      </div>
    </div>

    <div class="chart-row">
      <div class="app-card chart-card chart-trend">
        <div class="chart-title">7-day Order Trend</div>
        <div ref="trendRef" class="chart" />
      </div>
      <div class="app-card chart-card chart-pie">
        <div class="chart-title">Order Status Distribution</div>
        <div ref="pieRef" class="chart" />
      </div>
    </div>

    <div class="app-card chart-card chart-bar">
      <div class="chart-title">Dispatch Efficiency (duration buckets)</div>
      <div ref="barRef" class="chart" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  .kpi-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 16px;
  }
  .kpi-card {
    position: relative;
    background: #fff;
    border-radius: $radius-base;
    padding: 20px;
    overflow: hidden;
    box-shadow: $shadow-card;
    .kpi-label {
      color: #909399;
      font-size: 13px;
    }
    .kpi-value {
      font-size: 30px;
      font-weight: 700;
      margin-top: 8px;
    }
    .kpi-icon {
      position: absolute;
      right: 16px;
      bottom: 16px;
      font-size: 38px;
      opacity: 0.18;
    }
  }
  .kpi-blue { background: linear-gradient(135deg, #409eff, #79bbff); color: #fff; .kpi-label { color: rgba(255,255,255,0.85); } .kpi-icon { color: #fff; } }
  .kpi-orange { background: linear-gradient(135deg, #e6a23c, #f3d19e); color: #fff; .kpi-label { color: rgba(255,255,255,0.85); } .kpi-icon { color: #fff; } }
  .kpi-purple { background: linear-gradient(135deg, #8e44ad, #c39bd3); color: #fff; .kpi-label { color: rgba(255,255,255,0.85); } .kpi-icon { color: #fff; } }
  .kpi-green { background: linear-gradient(135deg, #67c23a, #a3d977); color: #fff; .kpi-label { color: rgba(255,255,255,0.85); } .kpi-icon { color: #fff; } }

  .chart-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }
  .chart-card {
    .chart-title {
      font-weight: 600;
      margin-bottom: 8px;
    }
    .chart {
      height: 320px;
    }
  }
  .chart-bar .chart {
    height: 280px;
  }
}
</style>