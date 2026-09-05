<script setup lang="ts">
import { computed } from 'vue'
import type { DispatchCandidate } from '@/types/order'

interface Props {
  candidates: DispatchCandidate[]
  selectedId?: number | string | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedId: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'select', candidate: DispatchCandidate): void
}>()

const selected = computed(() =>
  props.candidates.find((candidate) => String(candidate.installerId) === String(props.selectedId))
  || props.candidates[0]
  || null
)

const selectedRank = computed(() => {
  if (!selected.value) return 0
  return props.candidates.findIndex((candidate) => candidate.installerId === selected.value?.installerId) + 1
})

function percent(value?: number): number | null {
  if (value == null || Number.isNaN(Number(value))) return null
  return Math.max(0, Math.min(100, Math.round(Number(value) * 100)))
}

function metricsOf(candidate: DispatchCandidate) {
  return [
    { key: 'distance', label: '距离适配', value: percent(candidate.factorBreakdown?.sd) },
    { key: 'load', label: '负载余量', value: percent(candidate.factorBreakdown?.sl) },
    { key: 'skill', label: '技能匹配', value: percent(candidate.factorBreakdown?.ss ?? candidate.skillMatchScore) },
    { key: 'rating', label: '服务评价', value: percent(candidate.factorBreakdown?.sr ?? (candidate.rating != null ? candidate.rating / 5 : undefined)) }
  ]
}

function reasonOf(candidate: DispatchCandidate): string {
  const metrics = metricsOf(candidate)
    .filter((metric): metric is { key: string; label: string; value: number } => metric.value != null)
    .sort((a, b) => b.value - a.value)
  const advantages = metrics.filter((metric) => metric.value >= 70).slice(0, 2).map((metric) => metric.label)
  const risk = metrics.find((metric) => metric.value < 35)
  if (advantages.length && risk) return `优势：${advantages.join('、')}；关注：${risk.label}偏低`
  if (advantages.length) return `主要优势：${advantages.join('、')}`
  if (risk) return `建议重点关注：${risk.label}偏低`
  return '各项条件相对均衡'
}

function scoreType(score?: number): 'success' | 'warning' | 'info' {
  if ((score ?? 0) >= 80) return 'success'
  if ((score ?? 0) >= 60) return 'warning'
  return 'info'
}
</script>

<template>
  <div class="decision-panel" v-loading="loading">
    <el-empty v-if="!loading && !candidates.length" description="暂无满足接单条件的装维人员" :image-size="72" />

    <template v-else-if="selected">
      <section class="decision-focus" aria-live="polite">
        <header class="decision-focus__header">
          <div>
            <div class="decision-focus__eyebrow">
              {{ selectedRank === 1 ? '系统首选' : `当前选择 · 排名 ${selectedRank}` }}
            </div>
            <h4>{{ selected.name }}<small v-if="selected.username">{{ selected.username }}</small></h4>
            <p>{{ reasonOf(selected) }}</p>
          </div>
          <div class="decision-score">
            <strong>{{ selected.score ?? '-' }}</strong>
            <span>综合评分</span>
          </div>
        </header>

        <div class="decision-metrics">
          <div v-for="metric in metricsOf(selected)" :key="metric.key" class="decision-metric">
            <div class="decision-metric__head">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value == null ? '待补充' : `${metric.value}%` }}</strong>
            </div>
            <el-progress
              :percentage="metric.value ?? 0"
              :show-text="false"
              :status="metric.value != null && metric.value < 35 ? 'exception' : undefined"
            />
          </div>
        </div>

        <div class="decision-facts">
          <span><el-icon><Location /></el-icon>{{ selected.distanceKm == null ? '距离待补充' : `${selected.distanceKm.toFixed(1)} km` }}</span>
          <span><el-icon><Tools /></el-icon>当前 {{ selected.workload ?? 0 }} 单</span>
          <span><el-icon><Star /></el-icon>{{ selected.rating == null ? '暂无评分' : `${selected.rating.toFixed(1)} 分` }}</span>
          <span><el-icon><Phone /></el-icon>{{ selected.phone || '电话未维护' }}</span>
        </div>
      </section>

      <div class="candidate-list" role="listbox" aria-label="装维候选人排名">
        <button
          v-for="(candidate, index) in candidates"
          :key="candidate.installerId"
          type="button"
          role="option"
          class="candidate-row"
          :class="{ 'is-selected': candidate.installerId === selected.installerId }"
          :aria-selected="candidate.installerId === selected.installerId"
          @click="emit('select', candidate)"
        >
          <span class="candidate-rank" :class="{ 'is-top': index === 0 }">{{ index + 1 }}</span>
          <span class="candidate-person">
            <strong>{{ candidate.name }}</strong>
            <small>{{ candidate.username || candidate.phone || '未维护工号' }}</small>
          </span>
          <span class="candidate-data"><small>距离</small>{{ candidate.distanceKm == null ? '-' : `${candidate.distanceKm.toFixed(1)}km` }}</span>
          <span class="candidate-data"><small>负载</small>{{ candidate.workload ?? 0 }} 单</span>
          <span class="candidate-data"><small>评价</small>{{ candidate.rating == null ? '-' : candidate.rating.toFixed(1) }}</span>
          <el-tag :type="scoreType(candidate.score)" effect="light">{{ candidate.score ?? '-' }} 分</el-tag>
        </button>
      </div>

      <p class="decision-note">
        排名由距离、当前负载、技能匹配和服务评价共同计算；人工选择不会自动修改派单规则。
      </p>
    </template>
  </div>
</template>

<style scoped lang="scss">
.decision-panel { min-height: 180px; }
.decision-focus { padding: 16px; border-radius: 10px; background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-fill-color-blank)); border: 1px solid var(--el-color-primary-light-7); }
.decision-focus__header { display: flex; justify-content: space-between; gap: 20px; }
.decision-focus__eyebrow { margin-bottom: 5px; color: var(--el-color-primary); font-size: 12px; font-weight: 600; }
.decision-focus h4 { margin: 0; font-size: 18px; color: var(--el-text-color-primary); }
.decision-focus h4 small { margin-left: 8px; color: var(--el-text-color-secondary); font-size: 12px; font-weight: 400; }
.decision-focus p { margin: 6px 0 0; color: var(--el-text-color-regular); font-size: 12px; }
.decision-score { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 78px; }
.decision-score strong { color: var(--el-color-primary); font-size: 30px; line-height: 1; font-variant-numeric: tabular-nums; }
.decision-score span { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 11px; }
.decision-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-top: 16px; }
.decision-metric__head { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 6px; color: var(--el-text-color-regular); font-size: 11px; }
.decision-metric__head strong { color: var(--el-text-color-primary); font-weight: 600; font-variant-numeric: tabular-nums; }
.decision-facts { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--el-color-primary-light-8); color: var(--el-text-color-regular); font-size: 12px; }
.decision-facts span { display: inline-flex; align-items: center; gap: 4px; }
.candidate-list { margin-top: 14px; border-top: 1px solid var(--el-border-color-lighter); }
.candidate-row { display: grid; grid-template-columns: 34px minmax(130px, 1fr) repeat(3, 72px) 70px; align-items: center; gap: 10px; width: 100%; padding: 10px 8px; border: 0; border-bottom: 1px solid var(--el-border-color-lighter); background: transparent; color: var(--el-text-color-regular); text-align: left; cursor: pointer; transition: background-color .15s ease; }
.candidate-row:hover, .candidate-row.is-selected { background: var(--el-color-primary-light-9); }
.candidate-row.is-selected { box-shadow: inset 3px 0 var(--el-color-primary); }
.candidate-row:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: -2px; }
.candidate-rank { display: inline-flex; width: 25px; height: 25px; align-items: center; justify-content: center; border-radius: 50%; background: var(--el-fill-color); color: var(--el-text-color-secondary); font-size: 12px; }
.candidate-rank.is-top { background: var(--el-color-warning-light-8); color: var(--el-color-warning-dark-2); font-weight: 600; }
.candidate-person { display: flex; min-width: 0; flex-direction: column; }
.candidate-person strong { overflow: hidden; color: var(--el-text-color-primary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.candidate-person small, .candidate-data small { color: var(--el-text-color-secondary); font-size: 10px; }
.candidate-data { display: flex; flex-direction: column; font-size: 12px; font-variant-numeric: tabular-nums; }
.decision-note { margin: 10px 0 0; color: var(--el-text-color-secondary); font-size: 11px; }

@media (max-width: 720px) {
  .decision-metrics { grid-template-columns: repeat(2, 1fr); }
  .candidate-row { grid-template-columns: 30px minmax(100px, 1fr) 58px 62px; }
  .candidate-row .candidate-data:nth-of-type(4), .candidate-row .candidate-data:nth-of-type(5) { display: none; }
}
</style>
