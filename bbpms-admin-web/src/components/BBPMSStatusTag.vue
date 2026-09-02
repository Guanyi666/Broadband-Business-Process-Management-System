<script setup lang="ts">
interface Props {
  status?: string
  label?: string
  type?: 'primary' | 'success' | 'warning' | 'info' | 'danger'
  effect?: 'light' | 'dark' | 'plain'
  size?: 'small' | 'default' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  status: '',
  label: '',
  type: 'info',
  effect: 'light',
  size: 'default'
})

const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  PENDING: 'warning',
  AUDIT_PASS: 'primary',
  AUDIT_REJECT: 'danger',
  DISPATCHED: 'primary',
  ACCEPTED: 'primary',
  INSTALLING: 'warning',
  DONE: 'success',
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

const finalType = computed(() => props.type || map[props.status] || 'info')
const finalLabel = computed(() => props.label || props.status || '-')
</script>

<template>
  <el-tag :type="finalType" :effect="effect" :size="size" round>
    {{ finalLabel }}
  </el-tag>
</template>