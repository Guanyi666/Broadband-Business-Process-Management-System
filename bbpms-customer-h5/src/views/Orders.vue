<template>
  <div class="page">
    <van-nav-bar title="我的订单" />
    <van-tabs v-model:active="active" sticky @change="load">
      <van-tab title="全部" name="" />
      <van-tab title="处理中" name="PENDING_CS_CONFIRM" />
      <van-tab title="安装中" name="INSTALLING" />
      <van-tab title="已完成" name="CLOSED" />
    </van-tabs>
    <div class="page-body">
      <van-pull-refresh v-model="refreshing" @refresh="load">
        <div v-for="item in list" :key="item.id" class="card order" @click="router.push(`/orders/${item.id}`)">
          <div class="row"><span class="muted">{{ item.orderNo }}</span><span class="status-pill">{{ item.statusLabel }}</span></div>
          <h3>{{ item.packageName }}</h3>
          <p><van-icon name="location-o" /> {{ item.installAddress }}</p>
          <div class="row muted"><span>提交于 {{ format(item.createTime) }}</span><van-icon name="arrow" /></div>
        </div>
        <van-empty v-if="!list.length" class="empty-space" description="暂无订单" />
      </van-pull-refresh>
    </div>
    <van-floating-bubble axis="xy" icon="plus" magnetic="x" @click="router.push('/orders/apply')" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { pageOrders } from '@/api/portal'
import type { OrderSummary } from '@/types'
const router = useRouter()
const active = ref('')
const refreshing = ref(false)
const list = ref<OrderSummary[]>([])
const format = (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'
async function load() {
  try { list.value = (await pageOrders({ pageSize: 100, status: active.value || undefined })).records || [] }
  finally { refreshing.value = false }
}
onMounted(load)
</script>

<style scoped lang="scss">
.order h3 { margin: 14px 0 10px; }.order p { margin: 0 0 14px; color: #566279; font-size: 14px; }
</style>
