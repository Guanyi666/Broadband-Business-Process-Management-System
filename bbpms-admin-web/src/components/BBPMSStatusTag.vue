<script setup lang="ts">
import { computed } from 'vue'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

interface Props {
  status?: string
  /** 显式文案，优先级最高（如后端返回的 statusDesc） */
  label?: string
  /** 显式颜色，留空时走「调用方 typeMap → 内置映射 → info」兜底链 */
  type?: TagType | ''
  effect?: 'light' | 'dark' | 'plain'
  size?: 'small' | 'default' | 'large'
  /** 调用方补充的状态 → 颜色映射 */
  typeMap?: Record<string, TagType>
}

const props = withDefaults(defineProps<Props>(), {
  status: '',
  label: '',
  type: '',
  effect: 'light',
  size: 'default',
  typeMap: () => ({})
})

/** 状态枚举 → 中文（全局唯一真源，覆盖订单 / 工单 / 装维 / 通用） */
const STATUS_TEXT: Record<string, string> = {
  // 订单 OrderStatus
  CREATED: '已创建',
  REJECTED: '已驳回',
  AUDITED: '已审核',
  WAIT_DISPATCH: '待派单',
  DISPATCHED: '已派单',
  INSTALLING: '安装中',
  FINISHED: '已完成',
  CLOSED: '已归档',
  CANCELLED: '已取消',
  // 工单 WorkOrderStatus
  PENDING: '待派发',
  ACCEPTED: '已接单',
  IN_PROGRESS: '施工中',
  STALLED: '已停滞',
  REASSIGNING: '改派中',
  COMPLETED: '已完成',
  FAILED: '失败',
  AUTO_CANCELLED: '自动取消',
  // 装维 / 考勤状态
  IDLE: '空闲',
  WORKING: '作业中',
  OFFLINE: '离线',
  ON_BREAK: '休息中',
  // 通用
  SUCCESS: '成功',
  WARN: '警告',
  UP: '正常',
  DOWN: '异常',
  UNKNOWN: '未知'
}

const map: Record<string, TagType> = {
  PENDING: 'warning',
  CREATED: 'warning',
  REJECTED: 'danger',
  AUDITED: 'primary',
  WAIT_DISPATCH: 'warning',
  AUDIT_PASS: 'primary',
  AUDIT_REJECT: 'danger',
  DISPATCHED: 'primary',
  ACCEPTED: 'primary',
  INSTALLING: 'warning',
  DONE: 'success',
  FINISHED: 'success',
  CLOSED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'info',
  AUTO_CANCELLED: 'info',
  STALLED: 'danger',
  REASSIGNING: 'warning',
  ASSIGNED: 'primary',
  IN_PROGRESS: 'warning',
  IDLE: 'success',
  WORKING: 'warning',
  OFFLINE: 'info',
  ON_BREAK: 'info',
  UP: 'success',
  DOWN: 'danger',
  WARN: 'warning',
  UNKNOWN: 'info',
  SUCCESS: 'success',
  FAILED: 'danger'
}

const finalType = computed<TagType>(() => props.type || props.typeMap?.[props.status] || map[props.status] || 'info')
// 兜底链：显式 label → 中文映射 → 原状态码 → '-'
const finalLabel = computed(() => props.label || STATUS_TEXT[props.status] || props.status || '-')
</script>

<template>
  <el-tag :type="finalType" :effect="effect" :size="size" round>
    {{ finalLabel }}
  </el-tag>
</template>
