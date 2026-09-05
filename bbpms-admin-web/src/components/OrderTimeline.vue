<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { formatDate } from '@/utils/format'
import {
  parseTimeToMs,
  formatDuration,
  stateClass,
  currentStageIndex,
  waitingBaseMs
} from '@/utils/track'
import type { TrackStage, TrackEvent, TrackSummary, StageState } from '@/types/track'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

interface Props {
  /** 流程骨架（§7.6 stages），恒返回全部节点，驱动左侧业务状态轨与顶部 Steps */
  stages?: TrackStage[]
  /** 右轨人员 / 操作事件（§7.6 events） */
  events?: TrackEvent[]
  /** 顶部汇总卡数据（§7.6 summary） */
  summary?: TrackSummary | null
  /** 标题 */
  title?: string
  /** 兼容旧调用：裸状态码（优先由 summary.currentStatus 覆盖） */
  currentStatus?: string
}

const props = withDefaults(defineProps<Props>(), {
  stages: () => [],
  events: () => [],
  summary: null,
  title: '订单与履约双轨时间线',
  currentStatus: ''
})

const roleMap: Record<string, string> = {
  SYSTEM: '系统自动处理',
  ORDER_OPERATOR: '订单操作人员',
  SUPER_ADMIN: '超级管理员',
  CUSTOMER_SERVICE: '客服人员',
  DISPATCHER: '调度人员',
  INSTALLER: '装维人员',
  AUDITOR: '审核人员',
  CUSTOMER: '客户'
}

const hasStages = computed(() => (props.stages?.length ?? 0) > 0)

/* ---------- 当前节点 / 实时等待 ---------- */
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  // 每分钟刷新一次，保证「当前等待」实时递增；组件卸载时清理，避免内存泄漏
  timer = setInterval(() => {
    now.value = Date.now()
  }, 60_000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const activeIndex = computed(() => (hasStages.value ? currentStageIndex(props.stages) : -1))
const isTerminal = computed(() => !!props.summary?.isTerminal)

const liveWaiting = computed<string | null>(() => {
  if (isTerminal.value) return null
  const base = hasStages.value ? waitingBaseMs(props.stages) : null
  return formatDuration(base, now.value)
})

/* ---------- 汇总卡 ---------- */
const summaryView = computed(() => {
  const s = props.summary
  const stages = props.stages ?? []
  const doneCount = stages.filter((st) => st.state === 'DONE').length
  const statusCode = s?.currentStatus || props.currentStatus || ''
  return {
    status: statusCode,
    statusDesc: s?.currentStatusDesc || '',
    progress: s?.progress || (stages.length ? `${doneCount}/${stages.length}` : ''),
    elapsed: s?.elapsed || '',
    waiting: liveWaiting.value || s?.waiting || (isTerminal.value ? '—' : '')
  }
})

/* ---------- 阶段图标 ---------- */
function stageIcon(state: StageState): string {
  switch (state) {
    case 'DONE':
      return '✓'
    case 'CURRENT':
      return '◉'
    case 'EXCEPTION':
      return '!'
    default:
      return '○'
  }
}

function roleText(role?: string | null): string {
  if (!role) return ''
  return roleMap[String(role).toUpperCase()] || role
}

/* ---------- 右轨事件辅助 ---------- */
function eventSourceText(source?: string): string {
  if (source === 'ORDER_AUDIT') return '订单流程'
  if (source === 'WORKORDER') return '工单流程'
  if (source === 'DISPATCH') return '派单流程'
  return source || '业务流程'
}
</script>

<template>
  <section class="dual-timeline app-card">
    <header class="dual-timeline__header">
      <div>
        <h3>{{ title }}</h3>
        <p>左侧追踪业务状态，右侧追踪人员与操作；骨架恒在，空数据不再整块消失。</p>
      </div>

      <!-- 汇总卡 -->
      <div class="summary-card">
        <div class="summary-card__item">
          <span class="summary-card__label">当前状态</span>
          <BBPMSStatusTag
            v-if="summaryView.status"
            :status="summaryView.status"
            :label="summaryView.statusDesc || undefined"
            size="small"
          />
          <span v-else class="summary-card__muted">—</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__label">流程进度</span>
          <strong class="summary-card__value">{{ summaryView.progress || '—' }}</strong>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__label">已用时</span>
          <strong class="summary-card__value">{{ summaryView.elapsed || '—' }}</strong>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__label">当前等待</span>
          <strong class="summary-card__value" :class="{ 'is-live': liveWaiting }">{{ summaryView.waiting || '—' }}</strong>
        </div>
      </div>
    </header>

    <!-- 顶部 Steps：横向展示完整流程与当前位置（四种视觉状态） -->
    <ol v-if="hasStages" class="steps" aria-label="流程步骤">
      <li
        v-for="(stage, index) in stages"
        :key="stage.code"
        class="steps__item"
        :class="[stateClass(stage.state), { 'is-active': index === activeIndex }]"
      >
        <span class="steps__dot">{{ stageIcon(stage.state) }}</span>
        <span class="steps__name">{{ stage.name }}</span>
        <span class="steps__meta">
          <template v-if="stage.time">{{ formatDate(stage.time, 'MM-DD HH:mm') }}</template>
          <template v-else-if="stage.state === 'EXCEPTION'">异常</template>
          <template v-else>待处理</template>
        </span>
        <span v-if="index < stages.length - 1" class="steps__line" aria-hidden="true" />
      </li>
    </ol>

    <!-- 双轨主体：左业务状态轨 | 中时间轴 | 右人员操作轨 -->
    <div v-if="hasStages" class="dual-rail">
      <!-- 左轨：业务状态（骨架节点，恒渲染） -->
      <div class="rail rail--left">
        <div
          v-for="(stage, index) in stages"
          :key="stage.code"
          class="stage-node"
          :class="[stateClass(stage.state), { 'is-active': index === activeIndex }]"
        >
          <span class="stage-node__icon">{{ stageIcon(stage.state) }}</span>
          <div class="stage-node__body">
            <strong>{{ stage.name }}</strong>
            <time v-if="stage.time">{{ formatDate(stage.time, 'MM-DD HH:mm') }}</time>
            <span v-else class="stage-node__pending">待处理</span>
            <small v-if="stage.operatorName">{{ stage.operatorName }} · {{ roleText(stage.operatorRole) }}</small>
            <small v-else-if="stage.isAuto" class="is-auto">系统自动</small>
          </div>
          <div v-if="index === activeIndex && !isTerminal && liveWaiting" class="stage-node__waiting">
            已等待 {{ liveWaiting }}
          </div>
          <p v-if="stage.state === 'EXCEPTION' && stage.remark" class="stage-node__remark">{{ stage.remark }}</p>
        </div>
      </div>

      <!-- 中轴：装饰性时间主轴 -->
      <div class="rail-axis" aria-hidden="true"><span /></div>

      <!-- 右轨：人员 / 操作事件 -->
      <div class="rail rail--right">
        <el-empty v-if="!events.length" description="暂无操作记录" :image-size="56" />
        <article
          v-for="(event, index) in events"
          :key="`${event.time ?? ''}-${index}`"
          class="event-node"
        >
          <div class="event-node__head">
            <strong>{{ event.title }}</strong>
            <el-tag v-if="event.isAuto" size="small" type="info" effect="plain">自动</el-tag>
            <el-tag v-else size="small" type="primary" effect="plain">人工</el-tag>
          </div>
          <p v-if="event.desc" class="event-node__desc">{{ event.desc }}</p>
          <div class="event-node__meta">
            <span class="people-avatar"><el-icon><User /></el-icon></span>
            <div>
              <strong>{{ event.operatorName || '系统或业务人员' }}</strong>
              <small v-if="event.operatorName">{{ roleText(event.operatorRole) }} · {{ eventSourceText(event.source) }}</small>
              <small v-else>{{ eventSourceText(event.source) }}</small>
            </div>
            <time class="event-node__time">{{ event.time ? formatDate(event.time, 'MM-DD HH:mm') : '待处理' }}</time>
          </div>
          <p v-if="event.remark" class="event-node__remark">{{ event.remark }}</p>
        </article>
      </div>
    </div>

    <!-- 降级：后端未提供骨架时，仅渲染事件（不整块消失） -->
    <div v-else class="legacy-events">
      <el-empty v-if="!events.length" description="暂无业务轨迹" :image-size="68" />
      <article
        v-for="(event, index) in events"
        :key="`${event.time ?? ''}-${index}`"
        class="event-node"
      >
        <div class="event-node__head">
          <strong>{{ event.title }}</strong>
          <el-tag v-if="event.isAuto" size="small" type="info" effect="plain">自动</el-tag>
          <el-tag v-else size="small" type="primary" effect="plain">人工</el-tag>
        </div>
        <p v-if="event.desc" class="event-node__desc">{{ event.desc }}</p>
        <div class="event-node__meta">
          <span class="people-avatar"><el-icon><User /></el-icon></span>
          <div>
            <strong>{{ event.operatorName || '系统或业务人员' }}</strong>
            <small v-if="event.operatorName">{{ roleText(event.operatorRole) }} · {{ eventSourceText(event.source) }}</small>
            <small v-else>{{ eventSourceText(event.source) }}</small>
          </div>
          <time class="event-node__time">{{ event.time ? formatDate(event.time, 'MM-DD HH:mm') : '待处理' }}</time>
        </div>
        <p v-if="event.remark" class="event-node__remark">{{ event.remark }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped lang="scss">
.dual-timeline { margin-top: 16px; overflow: hidden; }

/* ---------- 头部 + 汇总卡 ---------- */
.dual-timeline__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
.dual-timeline__header h3 { margin: 0; color: var(--el-text-color-primary); font-size: 16px; }
.dual-timeline__header p { margin: 5px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }

.summary-card { display: flex; gap: 10px; flex-wrap: wrap; }
.summary-card__item {
  display: flex; flex-direction: column; gap: 4px; min-width: 92px;
  padding: 8px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px;
  background: var(--el-fill-color-blank);
}
.summary-card__label { color: var(--el-text-color-secondary); font-size: 11px; }
.summary-card__value { color: var(--el-text-color-primary); font-size: 14px; font-weight: 600; font-variant-numeric: tabular-nums; }
.summary-card__value.is-live { color: var(--el-color-warning-dark-2); }
.summary-card__muted { color: var(--el-text-color-placeholder); }

/* ---------- 顶部 Steps ---------- */
.steps {
  display: flex; align-items: flex-start; gap: 0; list-style: none;
  margin: 0 0 18px; padding: 14px 8px; overflow-x: auto;
  border: 1px solid var(--el-border-color-lighter); border-radius: 10px;
  background: var(--el-fill-color-blank);
}
.steps__item {
  position: relative; flex: 1 1 0; min-width: 84px; display: flex; flex-direction: column; align-items: center; gap: 5px;
  color: var(--el-text-color-secondary);
}
.steps__dot {
  display: inline-flex; width: 26px; height: 26px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 13px; font-weight: 700; color: #fff;
  background: var(--el-color-info); flex: 0 0 auto;
}
.steps__name { font-size: 12px; font-weight: 600; white-space: nowrap; }
.steps__meta { font-size: 10px; color: var(--el-text-color-placeholder); white-space: nowrap; }
.steps__line {
  position: absolute; top: 13px; left: 50%; width: 100%; height: 2px;
  background: var(--el-border-color-light); z-index: 0;
}
.steps__item.is-done .steps__dot { background: var(--el-color-success); }
.steps__item.is-done .steps__name { color: var(--el-text-color-primary); }
.steps__item.is-done .steps__line { background: var(--el-color-success-light-5); }
.steps__item.is-current .steps__dot {
  background: var(--el-color-primary); box-shadow: 0 0 0 4px var(--el-color-primary-light-8);
}
.steps__item.is-current .steps__name { color: var(--el-color-primary); }
.steps__item.is-current .steps__meta { color: var(--el-color-primary); }
.steps__item.is-pending { opacity: .55; }
.steps__item.is-exception .steps__dot { background: var(--el-color-danger); }
.steps__item.is-exception .steps__name { color: var(--el-color-danger); }
.steps__item.is-exception .steps__meta { color: var(--el-color-danger); }

/* ---------- 双轨 ---------- */
.dual-rail {
  display: grid; grid-template-columns: minmax(0, 1fr) 116px minmax(0, 1fr);
  align-items: stretch; max-height: 520px; overflow-y: auto; padding: 4px;
}
.rail--left { display: flex; flex-direction: column; gap: 4px; padding-right: 10px; }
.rail--right { display: flex; flex-direction: column; gap: 12px; padding-left: 10px; }

.rail-axis { display: flex; justify-content: center; }
.rail-axis span { width: 2px; background: var(--el-border-color-light); border-radius: 2px; }

/* 左轨节点 */
.stage-node {
  position: relative; display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 10px;
  padding: 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px;
  background: var(--el-fill-color-blank);
}
.stage-node__icon {
  display: inline-flex; width: 26px; height: 26px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 13px; font-weight: 700; color: #fff; background: var(--el-color-info);
}
.stage-node__body { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.stage-node__body strong { color: var(--el-text-color-primary); font-size: 13px; }
.stage-node__body time { color: var(--el-text-color-secondary); font-size: 11px; font-variant-numeric: tabular-nums; }
.stage-node__body small { color: var(--el-text-color-secondary); font-size: 11px; }
.stage-node__pending { color: var(--el-text-color-placeholder); font-size: 11px; }
.stage-node__body small.is-auto { color: var(--el-color-info); }
.stage-node__waiting {
  grid-column: 1 / -1; margin-top: 6px; padding-top: 6px; border-top: 1px dashed var(--el-border-color-lighter);
  color: var(--el-color-warning-dark-2); font-size: 11px; font-weight: 600;
}
.stage-node__remark { grid-column: 1 / -1; margin: 4px 0 0; color: var(--el-color-danger); font-size: 11px; }

.stage-node.is-done .stage-node__icon { background: var(--el-color-success); }
.stage-node.is-done { border-color: var(--el-color-success-light-7); }
.stage-node.is-current { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-9); }
.stage-node.is-current .stage-node__icon { background: var(--el-color-primary); box-shadow: 0 0 0 4px var(--el-color-primary-light-8); }
.stage-node.is-current .stage-node__body strong { color: var(--el-color-primary); }
.stage-node.is-pending { opacity: .6; }
.stage-node.is-pending .stage-node__icon { background: var(--el-color-info-light-5); color: var(--el-text-color-placeholder); }
.stage-node.is-exception { border-color: var(--el-color-danger-light-7); background: var(--el-color-danger-light-9); }
.stage-node.is-exception .stage-node__icon { background: var(--el-color-danger); }

/* 右轨事件 */
.event-node {
  padding: 12px 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px;
  background: var(--el-fill-color-blank);
}
.event-node__head { display: flex; align-items: center; gap: 8px; }
.event-node__head strong { color: var(--el-text-color-primary); font-size: 13px; }
.event-node__desc { margin: 6px 0 0; color: var(--el-text-color-regular); font-size: 12px; }
.event-node__meta { display: flex; align-items: center; gap: 9px; margin-top: 8px; }
.event-node__meta > div { display: flex; min-width: 0; flex-direction: column; flex: 1; }
.event-node__meta strong { font-size: 12px; color: var(--el-text-color-primary); }
.event-node__meta small { margin-top: 2px; color: var(--el-text-color-secondary); font-size: 11px; }
.event-node__time { color: var(--el-text-color-secondary); font-size: 11px; white-space: nowrap; font-variant-numeric: tabular-nums; }
.event-node__remark { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 11px; }

.people-avatar {
  display: inline-flex; width: 30px; height: 30px; flex: 0 0 30px; align-items: center; justify-content: center;
  border-radius: 50%; background: var(--el-color-primary-light-9); color: var(--el-color-primary);
}

/* 降级事件列表 */
.legacy-events { display: flex; flex-direction: column; gap: 12px; }

/* ---------- 响应式 ---------- */
@media (max-width: 760px) {
  .dual-timeline__header { flex-direction: column; }
  .summary-card { width: 100%; }
  .dual-rail { grid-template-columns: 1fr; max-height: none; overflow: visible; }
  .rail--left { padding-right: 0; }
  .rail--right { padding-left: 0; margin-top: 12px; }
  .rail-axis { display: none; }
  .steps { flex-wrap: nowrap; }
}
</style>
