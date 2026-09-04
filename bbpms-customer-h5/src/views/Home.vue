<template>
  <div class="page">
    <div class="hero">
      <p>你好，{{ auth.user?.name || auth.user?.username }}</p>
      <h1>我的宽带服务</h1>
      <p>随时掌握安装进度，在线办理服务</p>
    </div>
    <div class="page-body home-body">
      <div class="quick-grid">
        <button @click="router.push('/orders/apply')"><van-icon name="plus" /><span>自助报装</span></button>
        <button @click="router.push('/orders')"><van-icon name="orders-o" /><span>订单进度</span></button>
        <button @click="router.push('/tickets/new?type=REPAIR')"><van-icon name="warning-o" /><span>故障报修</span></button>
        <button @click="router.push('/tickets/new?type=COMPLAINT')"><van-icon name="service-o" /><span>投诉建议</span></button>
      </div>

      <h2 class="section-title">最近订单</h2>
      <div v-if="latest" class="card order-card" @click="router.push(`/orders/${latest.id}`)">
        <div class="row"><strong>{{ latest.packageName }}</strong><span class="status-pill">{{ latest.statusLabel }}</span></div>
        <p>{{ latest.installAddress }}</p>
        <div class="row muted"><span>{{ latest.orderNo }}</span><span>{{ format(latest.createTime) }}</span></div>
      </div>
      <van-empty v-else class="card" description="还没有宽带订单" />

      <div class="card notice" @click="router.push('/messages')">
        <van-icon name="volume-o" color="#1267e5" size="21" />
        <div><strong>消息中心</strong><p>安装完成和业务处理结果会在这里通知</p></div>
        <van-icon name="arrow" color="#a0a8b7" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { pageOrders } from '@/api/portal'
import type { OrderSummary } from '@/types'

const auth = useAuthStore()
const router = useRouter()
const latest = ref<OrderSummary | null>(null)
const format = (v?: string) => v ? dayjs(v).format('MM-DD HH:mm') : ''
onMounted(async () => { latest.value = (await pageOrders({ pageNum: 1, pageSize: 1 })).records?.[0] || null })
</script>

<style scoped lang="scss">
.home-body { margin-top: -22px; position: relative; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; background: #fff; border-radius: 16px; padding: 18px 8px; box-shadow: 0 6px 20px rgba(28,54,98,.08); }
.quick-grid button { border: 0; background: transparent; color: #354158; display: flex; flex-direction: column; align-items: center; gap: 8px; font-size: 13px; }
.quick-grid .van-icon { width: 45px; height: 45px; display: grid; place-items: center; border-radius: 14px; color: #1267e5; background: #eaf2ff; font-size: 22px; }
.order-card p { margin: 12px 0; color: #59657a; font-size: 14px; }
.notice { display: grid; grid-template-columns: 28px 1fr 18px; gap: 10px; align-items: center; margin-top: 18px; }
.notice strong { font-size: 15px; }.notice p { margin: 5px 0 0; color: #8993a5; font-size: 12px; }
</style>
