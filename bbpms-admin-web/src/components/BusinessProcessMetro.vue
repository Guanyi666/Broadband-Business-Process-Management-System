<script setup lang="ts">
import { computed, ref } from 'vue'
import type { StatusDistItem } from '@/types/dashboard'

interface ProcessNode {
  id: string
  title: string
  subtitle: string
  count: number
  target: 'order' | 'workorder'
  status: string
}

interface Props {
  orderStatuses?: StatusDistItem[]
  workOrderStatuses?: StatusDistItem[]
  slaRiskCount?: number
  loading?: boolean
  error?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  orderStatuses: () => [],
  workOrderStatuses: () => [],
  slaRiskCount: 0,
  loading: false,
  error: null
})

const emit = defineEmits<{
  (e: 'select', payload: { target: 'order' | 'workorder'; status: string }): void
  (e: 'retry'): void
}>()

const xrayMode = ref(false)

function countOf(list: StatusDistItem[], ...statuses: string[]): number {
  return list
    .filter((item) => statuses.includes(item.status))
    .reduce((sum, item) => sum + (Number(item.count) || 0), 0)
}

const nodes = computed<ProcessNode[]>(() => {
  const order = props.orderStatuses
  const work = props.workOrderStatuses
  return [
    {
      id: 'acceptance', title: '订单受理', subtitle: '等待审核',
      count: countOf(order, 'CREATED'), target: 'order', status: 'CREATED'
    },
    {
      id: 'audited', title: '审核通过', subtitle: '准备生成工单',
      count: countOf(order, 'AUDITED'), target: 'order', status: 'AUDITED'
    },
    {
      id: 'pending', title: '待派单', subtitle: '进入调度池',
      count: Math.max(countOf(order, 'WAIT_DISPATCH'), countOf(work, 'PENDING')),
      target: 'workorder', status: 'PENDING'
    },
    {
      id: 'dispatched', title: '已派单', subtitle: '等待装维接单',
      count: countOf(work, 'DISPATCHED'), target: 'workorder', status: 'DISPATCHED'
    },
    {
      id: 'accepted', title: '装维接单', subtitle: '等待上门',
      count: countOf(work, 'ACCEPTED'), target: 'workorder', status: 'ACCEPTED'
    },
    {
      id: 'installing', title: '施工处理中', subtitle: '上门安装',
      count: countOf(work, 'IN_PROGRESS', 'STALLED'), target: 'workorder', status: 'IN_PROGRESS'
    },
    {
      id: 'completed', title: '业务完成', subtitle: '等待归档',
      count: Math.max(countOf(order, 'FINISHED'), countOf(work, 'COMPLETED')),
      target: 'workorder', status: 'COMPLETED'
    },
    {
      id: 'closed', title: '业务归档', subtitle: '流程结束',
      count: countOf(order, 'CLOSED'), target: 'order', status: 'CLOSED'
    }
  ]
})

const maxNodeCount = computed(() => Math.max(0, ...nodes.value.map((node) => node.count)))
const activeTotal = computed(() => nodes.value.slice(0, 6).reduce((sum, node) => sum + node.count, 0))
const rejectedCount = computed(() => countOf(props.orderStatuses, 'REJECTED'))
const stalledCount = computed(() => countOf(props.workOrderStatuses, 'STALLED'))
const cancelledCount = computed(() =>
  countOf(props.orderStatuses, 'CANCELLED') +
  countOf(props.workOrderStatuses, 'CANCELLED', 'AUTO_CANCELLED', 'FAILED')
)
const hasData = computed(() => [...props.orderStatuses, ...props.workOrderStatuses].length > 0)

function pressureOf(node: ProcessNode): 'normal' | 'warning' | 'danger' {
  if (node.id === 'installing' && (stalledCount.value > 0 || props.slaRiskCount > 0)) return 'danger'
  if (!maxNodeCount.value || !node.count) return 'normal'
  const ratio = node.count / maxNodeCount.value
  if (ratio >= 0.75) return 'danger'
  if (ratio >= 0.4) return 'warning'
  return 'normal'
}

function connectorWidth(index: number): string {
  if (index >= nodes.value.length - 1 || !maxNodeCount.value) return '2px'
  const flow = Math.min(nodes.value[index].count, nodes.value[index + 1].count)
  return `${2 + Math.round((flow / maxNodeCount.value) * 6)}px`
}

const insight = computed(() => {
  if (stalledCount.value > 0) return `施工节点存在 ${stalledCount.value} 张停滞工单，建议优先下钻处理。`
  if (props.slaRiskCount > 0) return `当前有 ${props.slaRiskCount} 张工单临近 SLA，请优先检查施工与接单环节。`
  const busiest = nodes.value.slice(0, 6).reduce<ProcessNode | null>(
    (best, node) => !best || node.count > best.count ? node : best,
    null
  )
  if (!busiest?.count) return '当前没有进行中的业务存量。'
  return `当前业务存量主要集中在“${busiest.title}”，共 ${busiest.count} 单。`
})
</script>

<template>
  <section class="process-metro">
    <header class="process-metro__header">
      <div>
        <div class="process-metro__title-row">
          <h3>业务生命线</h3>
          <el-tag size="small" effect="plain">进行中 {{ activeTotal }} 单</el-tag>
        </div>
        <p>节点显示当前业务存量，点击可下钻到对应订单或工单。</p>
      </div>
      <div class="process-metro__mode">
        <span>流程 X 光</span>
        <el-switch v-model="xrayMode" aria-label="切换流程 X 光模式" />
      </div>
    </header>

    <div v-if="error" class="process-metro__state">
      <span>流程数据加载失败：{{ error }}</span>
      <el-button type="primary" link @click="emit('retry')">重试</el-button>
    </div>
    <div v-else-if="loading && !hasData" class="process-metro__state" v-loading="true">正在加载流程数据</div>
    <el-empty v-else-if="!hasData" description="暂无流程数据" :image-size="64" />

    <template v-else>
      <div class="metro-track" :class="{ 'is-xray': xrayMode }">
        <template v-for="(node, index) in nodes" :key="node.id">
          <button
            type="button"
            class="metro-node"
            :class="[`is-${pressureOf(node)}`, { 'is-empty': node.count === 0 }]"
            :title="`${node.title}：${node.count} 单，点击查看明细`"
            :aria-label="`${node.title}，当前 ${node.count} 单，点击查看明细`"
            @click="emit('select', { target: node.target, status: node.status })"
          >
            <span class="metro-node__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <strong>{{ node.count }}</strong>
            <span class="metro-node__title">{{ node.title }}</span>
            <small>{{ node.subtitle }}</small>
          </button>
          <div v-if="index < nodes.length - 1" class="metro-link" aria-hidden="true">
            <span class="metro-link__line" :style="{ height: connectorWidth(index) }" />
            <span class="metro-link__arrow">›</span>
          </div>
        </template>
      </div>

      <div class="process-metro__footer">
        <div class="exception-flow">
          <button type="button" @click="emit('select', { target: 'order', status: 'REJECTED' })">
            <span class="exception-dot is-warning" />审核驳回 <strong>{{ rejectedCount }}</strong>
          </button>
          <button type="button" @click="emit('select', { target: 'workorder', status: 'STALLED' })">
            <span class="exception-dot is-danger" />施工停滞 <strong>{{ stalledCount }}</strong>
          </button>
          <button type="button" @click="emit('select', { target: 'order', status: 'CANCELLED' })">
            <span class="exception-dot is-muted" />取消/失败 <strong>{{ cancelledCount }}</strong>
          </button>
        </div>
        <div class="process-insight" aria-live="polite">
          <el-icon><DataAnalysis /></el-icon>
          <span>{{ insight }}</span>
        </div>
      </div>

      <div v-if="xrayMode" class="xray-legend">
        <span><i class="legend-dot is-normal" />低存量</span>
        <span><i class="legend-dot is-warning" />中等存量</span>
        <span><i class="legend-dot is-danger" />高存量或存在 SLA 风险</span>
        <em>压力等级按当前节点存量相对值计算</em>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.process-metro {
  padding: 20px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  overflow: hidden;
}

.process-metro__header,
.process-metro__footer,
.process-metro__title-row,
.process-metro__mode,
.exception-flow,
.process-insight,
.xray-legend {
  display: flex;
  align-items: center;
}

.process-metro__header {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;

  h3 { margin: 0; font-size: 16px; color: var(--el-text-color-primary); }
  p { margin: 5px 0 0; font-size: 12px; color: var(--el-text-color-secondary); }
}

.process-metro__title-row { gap: 8px; }
.process-metro__mode { gap: 8px; font-size: 13px; color: var(--el-text-color-regular); white-space: nowrap; }
.process-metro__state { min-height: 150px; display: flex; align-items: center; justify-content: center; gap: 8px; color: var(--el-text-color-secondary); }

.metro-track {
  display: flex;
  align-items: center;
  width: 100%;
}

.metro-node {
  position: relative;
  display: flex;
  flex: 0 0 104px;
  min-height: 128px;
  padding: 12px 8px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-primary);
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;

  &:hover { transform: translateY(-3px); border-color: var(--el-color-primary); box-shadow: 0 8px 20px rgba(64, 158, 255, .14); }
  &:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
  strong { font-size: 28px; line-height: 1; font-variant-numeric: tabular-nums; color: var(--el-color-primary); }
  small { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 11px; }
}

.metro-node__index { position: absolute; top: 8px; left: 9px; font-size: 10px; color: var(--el-text-color-placeholder); }
.metro-node__title { margin-top: 9px; font-size: 13px; font-weight: 600; white-space: nowrap; }
.metro-node.is-empty { opacity: .58; }

.metro-link { position: relative; display: flex; flex: 1 1 24px; min-width: 18px; align-items: center; color: var(--el-color-primary); }
.metro-link__line { display: block; width: 100%; min-height: 2px; border-radius: 10px; background: linear-gradient(90deg, var(--el-color-primary-light-5), var(--el-color-primary)); transition: height .2s ease; }
.metro-link__arrow { position: absolute; right: -1px; top: 50%; transform: translateY(-53%); font-size: 21px; }

.metro-track.is-xray {
  .metro-node.is-warning { border-color: var(--el-color-warning); background: var(--el-color-warning-light-9); strong { color: var(--el-color-warning-dark-2); } }
  .metro-node.is-danger { border-color: var(--el-color-danger); background: var(--el-color-danger-light-9); box-shadow: 0 0 0 4px var(--el-color-danger-light-8); strong { color: var(--el-color-danger); } }
  .metro-node.is-danger:not(.is-empty)::after { content: ''; position: absolute; inset: -5px; border: 1px solid var(--el-color-danger-light-5); border-radius: 16px; animation: risk-pulse 1.8s ease-out infinite; pointer-events: none; }
}

.process-metro__footer { justify-content: space-between; gap: 16px; margin-top: 20px; padding-top: 14px; border-top: 1px solid var(--el-border-color-lighter); }
.exception-flow { gap: 8px; flex-wrap: wrap; }
.exception-flow button { display: inline-flex; align-items: center; gap: 5px; padding: 5px 9px; border: 0; border-radius: 14px; background: var(--el-fill-color-light); color: var(--el-text-color-regular); cursor: pointer; font-size: 12px; }
.exception-flow button:hover { color: var(--el-color-primary); }
.exception-flow strong { font-variant-numeric: tabular-nums; }
.exception-dot,
.legend-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-info); }
.is-warning { &.exception-dot, &.legend-dot { background: var(--el-color-warning); } }
.is-danger { &.exception-dot, &.legend-dot { background: var(--el-color-danger); } }
.is-normal { &.legend-dot { background: var(--el-color-success); } }
.is-muted { &.exception-dot { background: var(--el-text-color-placeholder); } }
.process-insight { gap: 6px; justify-content: flex-end; color: var(--el-text-color-regular); font-size: 12px; text-align: right; }
.xray-legend { justify-content: flex-end; gap: 14px; flex-wrap: wrap; margin-top: 10px; font-size: 11px; color: var(--el-text-color-secondary); }
.xray-legend span { display: inline-flex; align-items: center; gap: 5px; }
.xray-legend em { font-style: normal; color: var(--el-text-color-placeholder); }

@keyframes risk-pulse {
  0% { opacity: .8; transform: scale(.98); }
  100% { opacity: 0; transform: scale(1.06); }
}

@media (prefers-reduced-motion: reduce) {
  .metro-node, .metro-node.is-danger::after { animation: none !important; transition: none; }
}

@media (max-width: 1280px) {
  .metro-track { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
  .metro-node { width: 100%; min-height: 112px; flex-basis: auto; }
  .metro-link { display: none; }
}

@media (max-width: 720px) {
  .process-metro { padding: 16px; }
  .process-metro__header, .process-metro__footer { align-items: flex-start; flex-direction: column; }
  .metro-track { grid-template-columns: repeat(2, 1fr); }
  .process-insight { text-align: left; justify-content: flex-start; }
  .xray-legend { justify-content: flex-start; }
}
</style>
