<script setup lang="ts">
import { formatDate } from '@/utils/format'

interface TimelineEvent {
  id?: string | number
  action: string
  operator?: string
  operatorName?: string
  remark?: string
  createdAt: string
  status?: string
  [k: string]: any
}

interface Props {
  events: TimelineEvent[]
  title?: string
}

const props = withDefaults(defineProps<Props>(), { title: 'Timeline' })

const colorMap: Record<string, string> = {
  CREATE: '#909399',
  AUDIT: '#67c23a',
  AUDIT_REJECT: '#f56c6c',
  DISPATCH: '#409eff',
  ACCEPT: '#e6a23c',
  START: '#e6a23c',
  COMPLETE: '#67c23a',
  CANCEL: '#f56c6c'
}

function colorOf(action: string) {
  const k = action.toUpperCase()
  for (const key in colorMap) if (k.includes(key)) return colorMap[key]
  return '#909399'
}
</script>

<template>
  <div class="order-timeline app-card">
    <div class="title">{{ title }}</div>
    <el-timeline>
      <el-timeline-item
        v-for="(e, i) in events"
        :key="i"
        :timestamp="formatDate(e.createdAt)"
        :color="colorOf(e.action)"
        placement="top"
      >
        <div class="ev-action">{{ e.action }}</div>
        <div class="ev-meta">
          <span v-if="e.operatorName">by {{ e.operatorName }}</span>
          <span v-if="e.remark"> · {{ e.remark }}</span>
        </div>
      </el-timeline-item>
      <el-empty v-if="!events.length" description="No events" :image-size="60" />
    </el-timeline>
  </div>
</template>

<style scoped lang="scss">
.order-timeline {
  .title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }
  .ev-action {
    font-weight: 600;
    color: #303133;
  }
  .ev-meta {
    color: #909399;
    font-size: 13px;
    margin-top: 2px;
  }
}
</style>