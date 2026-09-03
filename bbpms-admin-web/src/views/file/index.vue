<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile, presign } from '@/api/file'
import PageHeader from '@/components/PageHeader.vue'

const fileList = ref<any[]>([])
const percent = ref(0)
const uploading = ref(false)

const presignForm = reactive({
  attachmentId: ''
})

const presignResult = ref<any>(null)

function beforeUpload(file: File) {
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

async function customUpload(options: any) {
  uploading.value = true
  percent.value = 0
  try {
    const form = new FormData()
    form.append('file', options.file)
    const res = await uploadFile(form, (e) => {
      if (e.total) percent.value = Math.round((e.loaded / e.total) * 100)
    })
    fileList.value.push(res)
    options.onSuccess(res)
    ElMessage.success('上传成功')
  } catch (e: any) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

async function onPresign() {
  if (!presignForm.attachmentId) {
    ElMessage.error('请先输入附件 ID')
    return
  }
  presignResult.value = await presign(presignForm.attachmentId)
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="文件管理" />

    <div class="app-card">
      <h3>Upload (Direct)</h3>
      <el-upload
        :file-list="fileList"
        :http-request="customUpload"
        :before-upload="beforeUpload"
        :show-file-list="true"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或 <em>click to upload</em></div>
        <template #tip>
          <div class="text-muted">Max 20MB. Direct multipart upload.</div>
        </template>
      </el-upload>
      <el-progress v-if="uploading" :percentage="percent" class="mt-16" />
    </div>

    <div class="app-card">
      <h3>预签名下载</h3>
      <el-form :model="presignForm" label-width="120px" style="max-width: 560px">
        <el-form-item label="附件 ID"><el-input v-model="presignForm.attachmentId" placeholder="如 1" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onPresign">获取下载链接</el-button>
        </el-form-item>
      </el-form>
      <pre v-if="presignResult" class="presign-result">{{ JSON.stringify(presignResult, null, 2) }}</pre>
    </div>
  </div>
</template>

<style scoped lang="scss">
.presign-result {
  background: #f5f7fa;
  border-radius: $radius-base;
  padding: 12px;
  font-size: 12px;
  max-height: 320px;
  overflow: auto;
}
</style>