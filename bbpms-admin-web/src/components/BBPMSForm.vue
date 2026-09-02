<script setup lang="ts" generic="T extends Record<string, any>">
import type { FormInstance, FormRules } from 'element-plus'

interface Props {
  model: T
  rules?: FormRules
  labelWidth?: string
  labelPosition?: 'left' | 'right' | 'top'
  inline?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  labelWidth: '120px',
  labelPosition: 'right',
  inline: false
})

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'reset'): void
}>()

const formRef = ref<FormInstance>()

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) emit('submit')
  })
}

function reset() {
  formRef.value?.resetFields()
  emit('reset')
}

defineExpose({ formRef, submit, reset })
</script>

<template>
  <el-form
    ref="formRef"
    :model="props.model"
    :rules="props.rules"
    :label-width="props.labelWidth"
    :label-position="props.labelPosition"
    :inline="props.inline"
    class="bbpms-form"
  >
    <slot :model="props.model" />
    <el-form-item v-if="$slots.buttons">
      <slot name="buttons" />
    </el-form-item>
  </el-form>
</template>

<style scoped lang="scss">
.bbpms-form {
  background: #fff;
  border-radius: $radius-base;
  padding: 20px;
}
</style>