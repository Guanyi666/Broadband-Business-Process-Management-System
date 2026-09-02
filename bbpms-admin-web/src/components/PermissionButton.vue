<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

interface Props {
  permission?: string | string[]
  role?: string | string[]
  hideWhenNoPerm?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  hideWhenNoPerm: true
})

const auth = useAuthStore()

const allowed = computed(() => {
  if (!props.permission && !props.role) return true
  if (props.permission && !auth.hasPermission(props.permission)) return false
  if (props.role) {
    const arr = Array.isArray(props.role) ? props.role : [props.role]
    if (!arr.some((r) => auth.roles.includes(r))) return false
  }
  return true
})
</script>

<template>
  <slot v-if="allowed" />
  <slot v-else-if="!hideWhenNoPerm" name="fallback" />
</template>