<template>
  <div class="page">
    <van-nav-bar title="自助报装" left-arrow @click-left="router.back()" />
    <van-form @submit="submit">
      <div class="page-body">
        <div class="card">
          <div class="card-title">选择宽带套餐</div>
          <van-radio-group v-model="form.packageCode">
            <van-cell-group>
              <van-cell v-for="pkg in packages" :key="pkg.code" clickable @click="form.packageCode = pkg.code">
                <template #title><strong>{{ pkg.name }}</strong><div class="muted">{{ pkg.description }}</div></template>
                <template #label><span class="price">¥{{ pkg.monthlyFee }}/月</span> · {{ pkg.speedMbps }}Mbps</template>
                <template #right-icon><van-radio :name="pkg.code" /></template>
              </van-cell>
            </van-cell-group>
          </van-radio-group>
        </div>
        <div class="card form-card">
          <div class="card-title">安装信息</div>
          <van-field v-model="form.installAddress" label="安装地址" type="textarea" rows="2" placeholder="例如：北京市朝阳区建国路1号1号楼1单元101" :rules="[{ required: true, message: '请输入安装地址' }]" />
          <van-field v-model="form.roomNo" label="房号" placeholder="选填，如 101" />
          <van-field v-model="form.appointmentTime" label="预约时间" placeholder="YYYY-MM-DD HH:mm:ss" />
          <van-field v-model="form.contactPhone" label="联系电话" placeholder="请输入联系电话" />
          <van-field v-model="form.remark" label="备注" placeholder="选填" />
          <div class="resource-row">
            <van-button size="small" plain type="primary" :loading="checking" @click.prevent="check">资源核查</van-button>
            <span v-if="checked" :class="checked.status === 'RESOURCE_OK' ? 'ok' : 'bad'">{{ checked.message }}</span>
          </div>
        </div>
        <van-button block round type="primary" native-type="submit" :loading="submitting" :disabled="checked?.status !== 'RESOURCE_OK'">提交报装申请</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { checkResource, createOrder, listPackages } from '@/api/portal'
import type { PackageInfo, ResourceCheckResult } from '@/types'
const router = useRouter()
const packages = ref<PackageInfo[]>([])
const checked = ref<ResourceCheckResult | null>(null)
const checking = ref(false)
const submitting = ref(false)
const form = reactive({ packageCode: '', installAddress: '', roomNo: '', appointmentTime: '', contactPhone: '', remark: '' })
async function check() {
  if (!form.installAddress) return
  checking.value = true
  try { checked.value = await checkResource({ address: form.installAddress, roomNo: form.roomNo || undefined }) }
  finally { checking.value = false }
}
async function submit() {
  if (!form.packageCode) {
    showToast('请选择套餐')
    return
  }
  if (!form.installAddress.trim()) {
    showToast('请输入安装地址')
    return
  }
  if (form.contactPhone.trim() && !/^1[3-9]\d{9}$/.test(form.contactPhone.trim())) {
    showToast('手机号格式不正确')
    return
  }
  if (form.appointmentTime.trim() && !/^\d{4}-\d{2}-\d{2}(\s+\d{2}:\d{2}(:\d{2})?)?$/.test(form.appointmentTime.trim())) {
    showToast('预约时间格式应为 YYYY-MM-DD HH:mm:ss')
    return
  }
  submitting.value = true
  try {
    const id = await createOrder({ ...form, appointmentTime: form.appointmentTime || undefined, roomNo: form.roomNo || undefined })
    showSuccessToast('申请已提交，等待客服确认')
    router.replace(`/orders/${id}`)
  } finally { submitting.value = false }
}
onMounted(async () => { packages.value = await listPackages(); form.packageCode = packages.value[0]?.code || '' })
</script>

<style scoped lang="scss">
.form-card { padding: 16px 4px; }.form-card .card-title { padding: 0 12px; }.price { color: #e5572d; font-weight: 700; }
.resource-row { display: flex; gap: 10px; align-items: center; padding: 14px 12px 2px; font-size: 12px; }.ok { color: #159660; }.bad { color: #d94c46; }
</style>
