<template>
  <div class="page">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" />
    <div v-if="detail" class="page-body">
      <div class="card state-card">
        <div class="state-icon"><van-icon name="logistics" /></div>
        <div><div class="status-name">{{ statusLabel }}</div><div class="muted">订单号 {{ detail.order.orderNo }}</div></div>
      </div>
      <div class="card info-list">
        <div class="row"><span>宽带套餐</span><strong>{{ detail.order.packageName }}</strong></div>
        <div class="row"><span>安装地址</span><strong>{{ detail.order.installAddress }}</strong></div>
        <div class="row"><span>预约时间</span><strong>{{ format(detail.appointment?.appointmentTime) }}</strong></div>
        <div class="row"><span>资源状态</span><strong>{{ detail.order.resourceStatus || '-' }}</strong></div>
      </div>
      <div class="card">
        <div class="card-title">办理进度</div>
        <van-steps direction="vertical" :active="0" active-color="#1267e5">
          <van-step v-for="item in timeline" :key="`${item.eventTime}-${item.eventType}`">
            <h4>{{ item.description || item.eventType }}</h4><p>{{ format(item.eventTime) }} {{ item.operatorName || '' }}</p>
          </van-step>
        </van-steps>
      </div>
      <div class="actions">
        <van-button v-if="canResubmit" type="danger" plain @click="router.push(`/orders/${id}/resubmit`)">修改后重提</van-button>
        <van-button v-if="canReschedule" plain type="primary" @click="router.push(`/orders/${id}/appointment`)">预约改期</van-button>
        <van-button v-if="canEvaluate" type="primary" @click="router.push(`/orders/${id}/evaluation`)">服务评价</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getEvaluation, getOrder } from '@/api/portal'
import type { OrderDetail, TimelineItem } from '@/types'
const route = useRoute(); const router = useRouter(); const id = route.params.id as string
const detail = ref<OrderDetail | null>(null); const evaluated = ref(false)
const timeline = computed<TimelineItem[]>(() => [...(detail.value?.timeline || [])].reverse())
const status = computed(() => String(detail.value?.order.status || ''))
const statusNames: Record<string, string> = {
  PENDING_CS_CONFIRM: '待客服确认', CS_REJECTED: '客服已退回', CREATED: '审核处理中',
  AUDITED: '审核处理中', REJECTED: '审核未通过', WAIT_DISPATCH: '派单中',
  DISPATCHED: '待上门', INSTALLING: '上门安装中', FINISHED: '待确认/评价',
  CLOSED: '已完成', CANCELLED: '已取消'
}
const statusLabel = computed(() => statusNames[status.value] || status.value || '处理中')
const canResubmit = computed(() => status.value === 'CS_REJECTED')
const canReschedule = computed(() => !['INSTALLING', 'FINISHED', 'CLOSED', 'CANCELLED'].includes(status.value))
const canEvaluate = computed(() => ['FINISHED', 'CLOSED'].includes(status.value) && !evaluated.value)
const format = (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'
onMounted(async () => {
  detail.value = await getOrder(id)
  if (['FINISHED', 'CLOSED'].includes(String(detail.value.order.status))) {
    evaluated.value = !!(await getEvaluation(id))
  }
})
</script>

<style scoped lang="scss">
.state-card { display: flex; align-items: center; gap: 14px; }.state-icon { width: 48px; height: 48px; border-radius: 16px; display: grid; place-items: center; background: #eaf2ff; color: #1267e5; font-size: 25px; }.status-name { font-size: 20px; font-weight: 750; margin-bottom: 5px; }
.info-list .row { align-items: flex-start; padding: 9px 0; font-size: 14px; }.info-list span { color: #7d8798; min-width: 72px; }.info-list strong { text-align: right; font-weight: 500; }
h4 { margin: 0 0 5px; font-size: 14px; } .van-step p { margin: 0; color: #939bac; font-size: 12px; }.actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
</style>
