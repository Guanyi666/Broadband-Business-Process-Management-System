<script setup lang="ts">
import { computed } from 'vue'
import { formatDate } from '@/utils/format'

interface TimelineEvent {
  id?: string | number
  eventType?: string
  description?: string
  source?: string
  fromStatus?: string | null
  fromStatusDesc?: string | null
  toStatus?: string | null
  toStatusDesc?: string | null
  operatorId?: string | number | null
  operatorName?: string | null
  operatorRole?: string | null
  eventTime?: string
  remark?: string | null
  action?: string
  operator?: string
  createdAt?: string
  createTime?: string
  status?: string
  [key: string]: any
}

interface Props {
  events: TimelineEvent[]
  title?: string
  currentStatus?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '业务轨迹',
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

function timestampOf(event: TimelineEvent): string {
  return event.eventTime || event.createdAt || event.createTime || ''
}

const normalizedEvents = computed(() => [...(props.events || [])]
  .filter(Boolean)
  .sort((left, right) => {
    const a = new Date(timestampOf(left).replace(/-/g, '/')).getTime()
    const b = new Date(timestampOf(right).replace(/-/g, '/')).getTime()
    return (Number.isNaN(a) ? Number.MAX_SAFE_INTEGER : a) - (Number.isNaN(b) ? Number.MAX_SAFE_INTEGER : b)
  }))

function businessText(event: TimelineEvent): string {
  if (event.fromStatusDesc && event.toStatusDesc) return `${event.fromStatusDesc} → ${event.toStatusDesc}`
  if (event.fromStatus && event.toStatus) return `${event.fromStatus} → ${event.toStatus}`
  return event.description || event.toStatusDesc || event.toStatus || event.eventType || event.action || '状态更新'
}

function sourceText(event: TimelineEvent): string {
  if (event.source === 'ORDER_AUDIT') return '订单流程'
  if (event.source === 'WORKORDER') return '工单流程'
  if (event.source === 'DISPATCH') return '派单流程'
  if (event.workOrderId) return '工单流程'
  return event.source || '业务流程'
}

function operatorText(event: TimelineEvent): string {
  const name = event.operatorName || event.operator
  const role = roleMap[String(event.operatorRole || '').toUpperCase()] || event.operatorRole
  if (name && role) return `${name} · ${role}`
  if (name) return name
  if (role) return role
  if (event.operatorId) return `操作人 #${event.operatorId}`
  return event.source === 'WORKORDER' ? '工单处理人员' : '系统或业务人员'
}

function isException(event: TimelineEvent): boolean {
  const text = `${event.eventType || ''} ${event.toStatus || ''} ${event.description || ''}`.toUpperCase()
  return ['REJECT', 'CANCEL', 'STALLED', 'FAILED', 'AUTO_CANCEL'].some((key) => text.includes(key))
}

function isSuccess(event: TimelineEvent): boolean {
  const text = `${event.eventType || ''} ${event.toStatus || ''}`.toUpperCase()
  return ['AUDIT_PASS', 'ACCEPTED', 'COMPLETE', 'COMPLETED', 'FINISHED', 'CLOSED'].some((key) => text.includes(key))
}

function durationBetween(index: number): string {
  if (index <= 0) return '流程起点'
  const previous = new Date(timestampOf(normalizedEvents.value[index - 1]).replace(/-/g, '/')).getTime()
  const current = new Date(timestampOf(normalizedEvents.value[index]).replace(/-/g, '/')).getTime()
  if (Number.isNaN(previous) || Number.isNaN(current) || current < previous) return '间隔未知'
  const minutes = Math.max(0, Math.round((current - previous) / 60_000))
  if (minutes < 1) return '即时流转'
  if (minutes < 60) return `等待 ${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remain = minutes % 60
  if (hours < 24) return `等待 ${hours} 小时${remain ? ` ${remain} 分钟` : ''}`
  const days = Math.floor(hours / 24)
  return `等待 ${days} 天 ${hours % 24} 小时`
}

const totalDuration = computed(() => {
  if (normalizedEvents.value.length < 2) return '暂无完整耗时'
  const first = new Date(timestampOf(normalizedEvents.value[0]).replace(/-/g, '/')).getTime()
  const last = new Date(timestampOf(normalizedEvents.value[normalizedEvents.value.length - 1]).replace(/-/g, '/')).getTime()
  if (Number.isNaN(first) || Number.isNaN(last)) return '暂无完整耗时'
  const hours = Math.max(0, Math.round((last - first) / 3_600_000 * 10) / 10)
  return hours < 24 ? `已记录 ${hours} 小时` : `已记录 ${(hours / 24).toFixed(1)} 天`
})
</script>

<template>
  <section class="dual-timeline app-card">
    <header class="dual-timeline__header">
      <div>
        <h3>{{ title }}</h3>
        <p>左侧追踪业务状态，右侧追踪人员与操作，等待时长按相邻事件计算。</p>
      </div>
      <div class="dual-timeline__summary">
        <el-tag v-if="currentStatus" effect="plain">当前 {{ currentStatus }}</el-tag>
        <span>{{ normalizedEvents.length }} 个事件 · {{ totalDuration }}</span>
      </div>
    </header>

    <div v-if="normalizedEvents.length" class="lane-head" aria-hidden="true">
      <span>业务状态轨</span>
      <i />
      <span>人员操作轨</span>
    </div>

    <div class="timeline-events">
      <article
        v-for="(event, index) in normalizedEvents"
        :key="event.id ?? `${timestampOf(event)}-${index}`"
        class="timeline-event"
        :class="{ 'is-exception': isException(event), 'is-success': isSuccess(event) }"
      >
        <div class="business-lane">
          <div class="lane-label">
            <el-tag size="small" effect="plain" :type="isException(event) ? 'danger' : isSuccess(event) ? 'success' : 'info'">
              {{ sourceText(event) }}
            </el-tag>
            <span>{{ durationBetween(index) }}</span>
          </div>
          <strong>{{ businessText(event) }}</strong>
          <p v-if="event.description && event.description !== businessText(event)">{{ event.description }}</p>
        </div>

        <div class="time-axis">
          <span class="time-axis__dot" />
          <time>{{ formatDate(timestampOf(event), 'MM-DD HH:mm') }}</time>
        </div>

        <div class="people-lane">
          <div class="people-lane__operator">
            <span class="people-avatar"><el-icon><User /></el-icon></span>
            <div>
              <strong>{{ operatorText(event) }}</strong>
              <small>{{ event.operatorRole ? (roleMap[String(event.operatorRole).toUpperCase()] || event.operatorRole) : sourceText(event) }}</small>
            </div>
          </div>
          <p>{{ event.remark || '完成状态流转，未填写额外备注' }}</p>
        </div>
      </article>
    </div>

    <el-empty v-if="!normalizedEvents.length" description="暂无业务轨迹" :image-size="68" />
  </section>
</template>

<style scoped lang="scss">
.dual-timeline { margin-top: 16px; overflow: hidden; }
.dual-timeline__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.dual-timeline__header h3 { margin: 0; color: var(--el-text-color-primary); font-size: 16px; }
.dual-timeline__header p { margin: 5px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.dual-timeline__summary { display: flex; align-items: center; gap: 10px; color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; }
.lane-head { display: grid; grid-template-columns: minmax(0, 1fr) 116px minmax(0, 1fr); align-items: center; margin-bottom: 8px; color: var(--el-text-color-secondary); font-size: 12px; font-weight: 600; }
.lane-head span:first-child { padding-right: 18px; text-align: right; }
.lane-head span:last-child { padding-left: 18px; }
.lane-head i { height: 1px; background: var(--el-border-color-lighter); }
.timeline-events { position: relative; }
.timeline-events::before { content: ''; position: absolute; top: 0; bottom: 0; left: 50%; width: 2px; transform: translateX(-1px); background: var(--el-border-color-light); }
.timeline-event { position: relative; display: grid; grid-template-columns: minmax(0, 1fr) 116px minmax(0, 1fr); align-items: center; min-height: 118px; }
.business-lane, .people-lane { padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; background: var(--el-fill-color-blank); }
.business-lane { margin-right: 18px; text-align: right; }
.people-lane { margin-left: 18px; }
.business-lane strong, .people-lane strong { color: var(--el-text-color-primary); font-size: 13px; }
.business-lane p, .people-lane p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.5; }
.lane-label { display: flex; align-items: center; justify-content: flex-end; gap: 8px; margin-bottom: 7px; color: var(--el-text-color-secondary); font-size: 11px; }
.time-axis { z-index: 1; display: flex; flex-direction: column; align-items: center; gap: 7px; color: var(--el-text-color-secondary); font-size: 11px; }
.time-axis__dot { width: 13px; height: 13px; border: 3px solid var(--el-bg-color); border-radius: 50%; background: var(--el-color-primary); box-shadow: 0 0 0 2px var(--el-color-primary-light-5); }
.timeline-event.is-exception .time-axis__dot { background: var(--el-color-danger); box-shadow: 0 0 0 2px var(--el-color-danger-light-5); }
.timeline-event.is-success .time-axis__dot { background: var(--el-color-success); box-shadow: 0 0 0 2px var(--el-color-success-light-5); }
.timeline-event.is-exception .business-lane { border-color: var(--el-color-danger-light-7); background: var(--el-color-danger-light-9); }
.people-lane__operator { display: flex; align-items: center; gap: 9px; }
.people-lane__operator > div { display: flex; min-width: 0; flex-direction: column; }
.people-lane__operator small { margin-top: 2px; color: var(--el-text-color-secondary); font-size: 11px; }
.people-avatar { display: inline-flex; width: 30px; height: 30px; flex: 0 0 30px; align-items: center; justify-content: center; border-radius: 50%; background: var(--el-color-primary-light-9); color: var(--el-color-primary); }

@media (max-width: 760px) {
  .dual-timeline__header { flex-direction: column; }
  .dual-timeline__summary { flex-wrap: wrap; white-space: normal; }
  .lane-head { display: none; }
  .timeline-events::before { left: 12px; }
  .timeline-event { display: grid; grid-template-columns: 25px minmax(0, 1fr); gap: 0; padding: 8px 0; }
  .time-axis { grid-column: 1; grid-row: 1 / span 2; align-self: stretch; justify-content: flex-start; padding-top: 18px; }
  .time-axis time { display: none; }
  .business-lane, .people-lane { grid-column: 2; margin: 0 0 7px 8px; text-align: left; }
  .lane-label { justify-content: flex-start; }
}
</style>
