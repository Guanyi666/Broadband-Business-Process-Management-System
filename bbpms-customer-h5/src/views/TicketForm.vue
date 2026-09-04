<template>
  <div class="page">
    <van-nav-bar :title="form.type === 'REPAIR' ? '故障报修' : '投诉建议'" left-arrow @click-left="router.back()" />
    <van-form @submit="submit">
      <div class="page-body">
        <div class="card form-card">
          <van-field label="服务类型">
            <template #input><van-radio-group v-model="form.type" direction="horizontal"><van-radio name="REPAIR">故障报修</van-radio><van-radio name="COMPLAINT">投诉建议</van-radio></van-radio-group></template>
          </van-field>
          <van-field v-model="form.orderId" label="关联订单" is-link readonly placeholder="可选" @click="showOrders = true" />
          <van-field v-model="form.category" label="问题分类" placeholder="如：无法上网、网速慢、服务态度" />
          <van-field v-model="form.description" label="问题描述" type="textarea" rows="5" maxlength="1000" show-word-limit placeholder="请尽量详细描述问题" :rules="[{ required: true, message: '请填写问题描述' }]" />
        </div>
        <van-button block round type="primary" native-type="submit" :loading="loading">提交服务单</van-button>
      </div>
    </van-form>
    <van-popup v-model:show="showOrders" position="bottom" round>
      <van-picker :columns="orderOptions" @confirm="selectOrder" @cancel="showOrders = false" />
    </van-popup>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { createTicket, pageOrders } from '@/api/portal'
import type { OrderSummary } from '@/types'
const route = useRoute(); const router = useRouter(); const loading = ref(false); const showOrders = ref(false)
const orders = ref<OrderSummary[]>([])
const form = reactive({ type: (route.query.type === 'COMPLAINT' ? 'COMPLAINT' : 'REPAIR') as 'REPAIR' | 'COMPLAINT', orderId: '', category: '', description: '' })
const orderOptions = computed(() => [{ text: '不关联订单', value: '' }, ...orders.value.map(o => ({ text: `${o.packageName} · ${o.orderNo}`, value: String(o.id) }))])
function selectOrder({ selectedOptions }: { selectedOptions: Array<{ value: string }> }) { form.orderId = selectedOptions[0]?.value || ''; showOrders.value = false }
async function submit() { loading.value = true; try { const id = await createTicket({ ...form, orderId: form.orderId || undefined }); showSuccessToast('服务单已提交'); router.replace(`/tickets/${id}`) } finally { loading.value = false } }
onMounted(async () => { orders.value = (await pageOrders({ pageSize: 100 })).records || [] })
</script>
<style scoped>.form-card { padding: 8px 2px; }</style>
