<template>
  <div class="page">
    <van-nav-bar title="服务单详情" left-arrow @click-left="router.back()" />
    <div v-if="ticket" class="page-body">
      <div class="card">
        <div class="row"><strong>{{ ticket.type === 'REPAIR' ? '故障报修' : '投诉建议' }}</strong><span class="status-pill">{{ text(ticket.status) }}</span></div>
        <p class="description">{{ ticket.description }}</p>
        <div class="muted">服务单号：{{ ticket.ticketNo }}</div>
      </div>
      <div class="card">
        <div class="card-title">处理进度</div>
        <van-steps direction="vertical" :active="active">
          <van-step>已提交</van-step><van-step>已受理</van-step><van-step>处理中</van-step><van-step>待确认</van-step><van-step>已关闭</van-step>
        </van-steps>
      </div>
      <div v-if="ticket.handleResult" class="card"><div class="card-title">处理结果</div><p>{{ ticket.handleResult }}</p></div>
      <van-button v-if="ticket.status === 'WAIT_CONFIRM'" block round type="primary" @click="confirm">确认问题已解决</van-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { confirmTicket, getTicket } from '@/api/portal'
import type { ServiceTicket } from '@/types'
const route = useRoute(); const router = useRouter(); const ticket = ref<ServiceTicket | null>(null)
const text = (s: string) => ({ SUBMITTED: '待受理', ACCEPTED: '已受理', PROCESSING: '处理中', WAIT_CONFIRM: '待确认', CLOSED: '已关闭', REJECTED: '已驳回' }[s] || s)
const active = computed(() => ({ SUBMITTED: 0, ACCEPTED: 1, PROCESSING: 2, WAIT_CONFIRM: 3, CLOSED: 4 }[ticket.value?.status || ''] ?? 0))
async function load() { ticket.value = await getTicket(route.params.id as string) }
async function confirm() { await confirmTicket(route.params.id as string); showSuccessToast('感谢确认'); load() }
onMounted(load)
</script>
<style scoped>.description { line-height: 1.7; color: #4d596e; }.card p { margin-bottom: 0; }</style>
