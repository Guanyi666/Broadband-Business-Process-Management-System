<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, auditOrder } from '@/api/order'
import { formatDate, maskName, maskPhone } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const order = ref<any>(null)

const form = reactive({
  pass: true,
  remark: ''
})

async function fetchOrder() {
  loading.value = true
  try {
    const id = route.query.id as string
    if (!id) {
      ElMessage.error('缺少订单 ID')
      router.back()
      return
    }
    order.value = await getOrderDetail(id)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!order.value) return
  submitting.value = true
  try {
    await auditOrder(order.value.id, { pass: form.pass, remark: form.remark })
    ElMessage.success(form.pass ? 'Approved' : 'Rejected')
    router.push(`/order/detail/${order.value.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(fetchOrder)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader title="订单审核" description="审核通过或驳回该订单" />

    <div class="app-card">
      <h3>订单摘要</h3>
      <el-descriptions v-if="order" :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ maskName(order.customerName) }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ maskPhone(order.customerPhone) }}</el-descriptions-item>
        <el-descriptions-item label="套餐">{{ order.packageName }}</el-descriptions-item>
        <el-descriptions-item label="客服">{{ order.csName }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ order.address }}</el-descriptions-item>
        <el-descriptions-item label="预约时间" :span="2">{{ formatDate(order.appointmentAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="app-card">
      <h3>审核意见</h3>
      <el-form :model="form" label-width="120px" style="max-width: 600px">
        <el-form-item label="审核决定">
          <el-radio-group v-model="form.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="审核备注（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">确认</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>