<script setup lang="ts">
import { ref } from 'vue'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { uploadFile } from '@/api/file'

interface Props {
  accept?: string
  multiple?: boolean
  limit?: number
  listType?: 'text' | 'picture' | 'picture-card'
  maxSize?: number // MB
  uploadUrl?: string
}

const props = withDefaults(defineProps<Props>(), {
  multiple: false,
  limit: 5,
  listType: 'picture-card',
  maxSize: 10
})

const emit = defineEmits<{
  (e: 'success', files: { url: string; name: string; id?: string | number }[]): void
  (e: 'remove', file: UploadUserFile): void
}>()

const fileList = ref<UploadUserFile[]>([])
const loading = ref(false)

const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValid = props.maxSize ? file.size / 1024 / 1024 < props.maxSize : true
  if (!isValid) {
    ElMessage.error(`File ${file.name} exceeds ${props.maxSize}MB`)
    return false
  }
  return true
}

const handleRequest: UploadProps['httpRequest'] = async (options) => {
  loading.value = true
  try {
    const form = new FormData()
    form.append('file', options.file)
    const res = await uploadFile(form, (e) => {
      if (options.onProgress && e.total) {
        options.onProgress({ percent: (e.loaded / e.total) * 100 })
      }
    })
    options.onSuccess?.(res)
    emit('success', [res])
  } catch (e: any) {
    options.onError?.(e)
  } finally {
    loading.value = false
  }
}

const handleRemove: UploadProps['onRemove'] = (file) => {
  emit('remove', file)
}
</script>

<template>
  <el-upload
    v-model:file-list="fileList"
    :accept="accept"
    :multiple="multiple"
    :limit="limit"
    :list-type="listType"
    :before-upload="handleBeforeUpload"
    :http-request="handleRequest"
    :on-remove="handleRemove"
    :show-file-list="true"
    v-loading="loading"
  >
    <template v-if="listType === 'picture-card'">
      <el-icon><Plus /></el-icon>
    </template>
    <template v-else>
      <el-button type="primary">Select File</el-button>
    </template>
  </el-upload>
</template>