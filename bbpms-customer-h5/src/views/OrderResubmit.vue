<template>
  <div class="page">
    <van-nav-bar title="修改报装申请" left-arrow @click-left="router.back()" />
    <van-form @submit="submit">
      <div class="page-body">
        <div class="card form-card">
          <div class="card-title">按客服意见修改后重新提交</div>
          <van-field
            v-model="form.installAddress"
            label="安装地址"
            type="textarea"
            rows="2"
            :rules="[{ required: true, message: '请输入安装地址' }]"
          />
          <van-field v-model="form.appointmentTime" label="预约时间" placeholder="YYYY-MM-DD HH:mm:ss" />
          <van-field v-model="form.remark" label="修改说明" placeholder="选填" />
          <div class="resource-row">
            <van-button size="small" plain type="primary" :loading="checking" @click.prevent="check">重新核查资源</van-button>
            <span v-if="checked" :class="checked.status === 'RESOURCE_OK' ? 'ok' : 'bad'">{{ checked.message }}</span>
          </div>
        </div>
        <van-button
          block round type="primary" native-type="submit" :loading="submitting"
          :disabled="checked?.status !== 'RESOURCE_OK'"
        >重新提交</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { checkResource, getOrder, resubmitOrder } from '@/api/portal'
import type { ResourceCheckResult } from '@/types'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const checking = ref(false)
const submitting = ref(false)
const checked = ref<ResourceCheckResult | null>(null)
const form = reactive({ installAddress: '', appointmentTime: '', remark: '' })

async function check() {
  if (!form.installAddress) return
  checking.value = true
  try { checked.value = await checkResource({ address: form.installAddress }) }
  finally { checking.value = false }
}

async function submit() {
  submitting.value = true
  try {
    await resubmitOrder(id, {
      installAddress: form.installAddress,
      appointmentTime: form.appointmentTime || undefined,
      remark: form.remark || undefined
    })
    showSuccessToast('已重新提交，等待客服确认')
    router.replace(`/orders/${id}`)
  } finally { submitting.value = false }
}

onMounted(async () => {
  const detail = await getOrder(id)
  form.installAddress = detail.order.installAddress
  form.appointmentTime = detail.appointment?.appointmentTime || ''
})
</script>

<style scoped lang="scss">
.form-card { padding: 16px 4px; }
.form-card .card-title { padding: 0 12px; }
.resource-row { display: flex; gap: 10px; align-items: center; padding: 14px 12px 2px; font-size: 12px; }
.ok { color: #159660; }
.bad { color: #d94c46; }
</style>
