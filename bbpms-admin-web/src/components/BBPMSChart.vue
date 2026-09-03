<script setup lang="ts">
/**
 * ECharts 统一封装组件
 *
 * 职责：
 *  - 持有 echarts 实例生命周期（init / setOption / resize / dispose）
 *  - ResizeObserver 自动跟随容器尺寸（折叠侧边栏等容器变化也能触发，非仅 window resize）
 *  - loading（骨架屏）/ empty（空态）覆盖层与图表互斥展示
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption, ECharts } from 'echarts'

interface Props {
  option: EChartsOption
  height?: number | string
  loading?: boolean
  empty?: boolean
  emptyText?: string
  autoresize?: boolean
  notMerge?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  height: 320,
  loading: false,
  empty: false,
  emptyText: '暂无数据',
  autoresize: true,
  notMerge: true
})

const emit = defineEmits<{ (e: 'click', params: unknown): void }>()

const containerRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function heightStyle(h: number | string): string {
  return typeof h === 'number' ? `${h}px` : h
}

onMounted(() => {
  if (!containerRef.value) return
  chart = echarts.init(containerRef.value)
  chart.on('click', (params: unknown) => emit('click', params))
  if (!props.empty && !props.loading) {
    chart.setOption(props.option, { notMerge: props.notMerge })
  }
  if (props.autoresize) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(containerRef.value)
  }
})

watch(
  () => props.option,
  (option) => {
    if (!chart || props.empty || props.loading) return
    chart.setOption(option, { notMerge: props.notMerge })
  },
  { deep: true }
)

watch(
  () => [props.empty, props.loading] as const,
  ([empty, loading]) => {
    if (!chart) return
    if (!empty && !loading) chart.setOption(props.option, { notMerge: props.notMerge })
  }
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})

defineExpose({
  resize: () => chart?.resize(),
  getInstance: () => chart,
  dispose: () => {
    chart?.dispose()
    chart = null
  }
})
</script>

<template>
  <div class="bbpms-chart" :style="{ height: heightStyle(height) }">
    <div ref="containerRef" class="bbpms-chart__canvas" />
    <div v-if="loading" class="bbpms-chart__overlay" aria-busy="true">
      <div class="bbpms-chart__skeleton">
        <span
          v-for="i in 5"
          :key="i"
          class="bbpms-chart__bar"
          :style="{ height: `${18 + ((i * 13) % 42)}%` }"
        />
      </div>
    </div>
    <div v-else-if="empty" class="bbpms-chart__overlay">
      <div class="bbpms-chart__empty">
        <span class="bbpms-chart__empty-text">{{ emptyText }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.bbpms-chart {
  position: relative;
  width: 100%;

  &__canvas {
    width: 100%;
    height: 100%;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-bg-color, #fff);
  }

  &__skeleton {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    width: 70%;
    height: 60%;
  }

  &__bar {
    flex: 1;
    border-radius: 4px;
    background: linear-gradient(90deg, #f0f2f5 25%, #e6e8eb 37%, #f0f2f5 63%);
    background-size: 400% 100%;
    animation: bbpms-chart-shimmer 1.2s ease infinite;
  }

  &__empty {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #909399;
  }

  &__empty-text {
    font-size: 13px;
  }
}

@keyframes bbpms-chart-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
