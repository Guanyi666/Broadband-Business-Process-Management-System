<script setup lang="ts">
import { formatDate } from '@/utils/format'

interface TimelineEvent {
  id?: string | number
  /** 后端 OrderTimelineVO.eventType（CREATE / AUDIT / DISPATCH ...） */
  eventType?: string
  /** 事件描述（后端 description，可能为中文） */
  description?: string
  /** 来源系统（可选） */
  source?: string
  operatorName?: string
  eventTime?: string
  remark?: string
  /** 兼容历史字段 */
  action?: string
  operator?: string
  createdAt?: string
  status?: string
  [k: string]: any
}

interface Props {
  events: TimelineEvent[]
  title?: string
}

const props = withDefaults(defineProps<Props>(), { title: '订单动态' })

const colorMap: Record<string, string> = {
  CREATE: '#909399',
  AUDIT: '#67c23a',
  AUDIT_REJECT: '#f56c6c',
  REJECT: '#f56c6c',
  DISPATCH: '#409eff',
  ACCEPT: '#e6a23c',
  START: '#e6a23c',
  COMPLETE: '#67c23a',
  CANCEL: '#f56c6c',
  // 中文描述兼容
  '创建': '#909399',
  '审核通过': '#67c23a',
  '驳回': '#f56c6c',
  '派单': '#409eff',
  '接单': '#e6a23c',
  '开始': '#e6a23c',
  '完成': '#67c23a',
  '取消': '#f56c6c'
}

function colorOf(e: TimelineEvent) {
  const text = `${e.eventType || ''} ${e.description || ''}`.toUpperCase()
  for (const key in colorMap) if (text.includes(key.toUpperCase())) return colorMap[key]
  return '#909399'
}

function mainText(e: TimelineEvent): string {
  return e.description || e.eventType || e.action || ''
}

function subText(e: TimelineEvent): string {
  const parts: string[] = []
  if (e.operatorName) parts.push('操作人：' + e.operatorName)
  if (e.remark) parts.push(e.remark)
  return parts.join(' · ')
}
</script>

<template>
  <div class="order-timeline app-card">
    <div class="title">{{ title }}</div>
    <el-timeline>
      <el-timeline-item
        v-for="(e, i) in events"
        :key="i"
        :timestamp="formatDate(e.eventTime || e.createdAt)"
        :color="colorOf(e)"
        placement="top"
      >
        <div class="ev-action">{{ mainText(e) }}</div>
        <div class="ev-meta" v-if="subText(e)">{{ subText(e) }}</div>
      </el-timeline-item>
      <el-empty v-if="!events.length" description="暂无动态" :image-size="60" />
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
