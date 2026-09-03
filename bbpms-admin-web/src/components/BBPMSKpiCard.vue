<script setup lang="ts">
/**
 * KPI 指标卡
 *
 * 视觉层级：l1（数值 32px/700，跨列大卡）/ l2（数值 20px/600）
 * tone 决定左侧 3px 色条与图标色。
 * 诚实设计：prev / trend 为 null（后端未提供对比基准）时不渲染环比，绝不伪造。
 */
import { computed, type Component } from 'vue'

type Tone = 'primary' | 'success' | 'warning' | 'danger' | 'info'

interface Props {
  label: string
  value: number | string | null
  unit?: string
  level?: 'l1' | 'l2'
  tone?: Tone
  icon?: string | Component
  prev?: number | null
  trend?: number | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  unit: '',
  level: 'l2',
  tone: 'primary',
  icon: undefined,
  prev: null,
  trend: null,
  loading: false
})

const toneColor = computed(() => `var(--el-color-${props.tone}, #409eff)`)

/** 真实数据缺失显示 '-'，不伪造 0 */
const displayValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return '-'
  return String(props.value)
})

/** 仅当 prev>0 且 trend 非 null 才渲染环比 */
const showTrend = computed(() => props.prev !== null && props.prev > 0 && props.trend !== null)

const trendText = computed(() => {
  const t = props.trend ?? 0
  if (t > 0) return `环比 +${Math.abs(t).toFixed(1)}%`
  if (t < 0) return `环比 -${Math.abs(t).toFixed(1)}%`
  return '环比持平'
})

const trendClass = computed(() => {
  const t = props.trend ?? 0
  if (t > 0) return 'is-up'
  if (t < 0) return 'is-down'
  return 'is-flat'
})
</script>

<template>
  <div class="kpi-card" :class="{ 'is-l1': level === 'l1' }">
    <span class="kpi-card__stripe" :style="{ background: toneColor }" />
    <div class="kpi-card__body">
      <div class="kpi-card__head">
        <span class="kpi-card__label">{{ label }}</span>
        <el-icon v-if="icon" class="kpi-card__icon" :size="level === 'l1' ? 22 : 18" :style="{ color: toneColor }">
          <component :is="icon" v-if="typeof icon !== 'string'" />
          <span v-else :class="icon" />
        </el-icon>
      </div>

      <template v-if="loading">
        <div class="kpi-card__skeleton kpi-card__skeleton--value" />
        <div class="kpi-card__skeleton kpi-card__skeleton--sub" />
      </template>
      <template v-else>
        <div class="kpi-card__value-row">
          <span class="kpi-card__value">{{ displayValue }}</span>
          <span v-if="unit" class="kpi-card__unit">{{ unit }}</span>
        </div>
        <div v-if="showTrend" class="kpi-card__trend">
          <span :class="trendClass">{{ trendText }}</span>
          <span class="kpi-card__prev">上期 {{ prev }}</span>
        </div>
        <div v-else class="kpi-card__trend kpi-card__trend--muted">较上期数据暂不可用</div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.kpi-card {
  position: relative;
  display: flex;
  min-height: 104px;
  padding: 16px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }

  &__stripe {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
  }

  &__body {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 8px;
    padding-left: 4px;
    min-width: 0;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__label {
    font-size: 13px;
    color: var(--el-text-color-secondary, #909399);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__value-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
    min-width: 0;
  }

  &__value {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
    line-height: 1.2;
  }

  &__unit {
    font-size: 12px;
    color: var(--el-text-color-secondary, #909399);
  }

  &__trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;

    &--muted {
      color: var(--el-text-color-placeholder, #c0c4cc);
    }

    .is-up {
      color: var(--el-color-success, #67c23a);
    }

    .is-down {
      color: var(--el-color-danger, #f56c6c);
    }

    .is-flat {
      color: var(--el-text-color-secondary, #909399);
    }
  }

  &__prev {
    color: var(--el-text-color-placeholder, #c0c4cc);
  }

  &__skeleton {
    border-radius: 4px;
    background: linear-gradient(90deg, #f0f2f5 25%, #e6e8eb 37%, #f0f2f5 63%);
    background-size: 400% 100%;
    animation: kpi-shimmer 1.2s ease infinite;

    &--value {
      width: 64px;
      height: 24px;
    }

    &--sub {
      width: 110px;
      height: 14px;
    }
  }

  &.is-l1 {
    min-height: 128px;

    .kpi-card__value {
      font-size: 32px;
      font-weight: 700;
    }

    .kpi-card__label {
      font-size: 14px;
    }
  }
}

@keyframes kpi-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
