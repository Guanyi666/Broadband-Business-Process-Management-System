<template>
  <div class="page">
    <van-nav-bar title="客户服务" />
    <div class="page-body">
      <div class="service-grid">
        <button @click="router.push('/tickets/new?type=REPAIR')"><van-icon name="warning-o" /><strong>故障报修</strong><span>宽带异常快速反馈</span></button>
        <button @click="router.push('/tickets/new?type=COMPLAINT')"><van-icon name="service-o" /><strong>投诉建议</strong><span>服务问题在线提交</span></button>
      </div>
      <h2 class="section-title">我的服务单</h2>
      <div v-for="item in tickets" :key="item.id" class="card ticket" @click="router.push(`/tickets/${item.id}`)">
        <div class="row"><strong>{{ item.type === 'REPAIR' ? '故障报修' : '投诉建议' }}</strong><span class="status-pill">{{ statusText(item.status) }}</span></div>
        <p>{{ item.description }}</p>
        <div class="row muted"><span>{{ item.ticketNo }}</span><span>{{ format(item.createTime) }}</span></div>
      </div>
      <van-empty v-if="!tickets.length" description="暂无服务单" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { pageTickets } from '@/api/portal'
import type { ServiceTicket } from '@/types'
const router = useRouter(); const tickets = ref<ServiceTicket[]>([])
const format = (v?: string) => v ? dayjs(v).format('MM-DD HH:mm') : ''
const statusText = (s: string) => ({ SUBMITTED: '待受理', ACCEPTED: '已受理', PROCESSING: '处理中', WAIT_CONFIRM: '待确认', CLOSED: '已关闭', REJECTED: '已驳回' }[s] || s)
onMounted(async () => { tickets.value = (await pageTickets({ pageSize: 100 })).records || [] })
</script>
<style scoped lang="scss">
.service-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.service-grid button { border: 0; border-radius: 16px; background: #fff; padding: 20px 12px; box-shadow: 0 5px 18px rgba(28,54,98,.06); display: flex; flex-direction: column; align-items: flex-start; gap: 8px; color: #27334a; }.service-grid .van-icon { color: #1267e5; font-size: 28px; }.service-grid strong { font-size: 16px; }.service-grid span { color: #8993a5; font-size: 12px; }.ticket p { color: #59657a; margin: 12px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
