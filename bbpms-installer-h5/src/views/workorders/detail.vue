<template>
  <div class="wo-detail-page">
    <van-nav-bar :title="`工单 ${wo?.workNo || ''}`" left-arrow fixed placeholder @click-left="$router.back()" />
    <template v-if="wo">
      <van-cell-group inset title="工单信息" class="block">
        <van-cell title="工单号" :value="wo.workNo" />
        <van-cell title="安装地址" :value="wo.installAddress" />
        <van-cell title="联系电话" :value="wo.customerPhone" is-link @click="callPhone" />
        <van-cell title="套餐">
          <template #value>
            <van-tag plain type="primary">{{ wo.packageName }}</van-tag>
          </template>
        </van-cell>
        <van-cell title="派单时间" :value="formatDate(wo.dispatchTime)" />
      </van-cell-group>

      <van-cell-group inset title="处理状态" class="block">
        <van-cell title="当前状态">
          <template #value>
            <van-tag :type="statusType(wo.status)">{{ statusLabel(wo.status) }}</van-tag>
          </template>
        </van-cell>
      </van-cell-group>

      <div class="action-bar safe-bottom">
        <template v-if="wo.status === 'DISPATCHED'">
          <van-button type="primary" block round :loading="accepting" @click="handleAccept">
            立即接单
          </van-button>
        </template>
        <template v-else-if="['ACCEPTED', 'IN_PROGRESS', 'STALLED', 'REASSIGNING'].includes(wo.status)">
          <van-button type="primary" block round @click="goInstall">前往装机</van-button>
        </template>
        <template v-else-if="wo.status === 'COMPLETED'">
          <van-button block @click="goInstall">查看装机记录</van-button>
        </template>
      </div>
    </template>
    <van-skeleton v-else title :row="6" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useWorkOrderStore } from '@/stores/workorder'
import { acceptWorkOrder } from '@/api/workorder'
import { statusLabel, statusType } from '@/constants/workorder'
import { formatDate } from '@/utils/format'
import type { WorkOrder } from '@/types/workorder'

const props = defineProps<{ id: string }>()
const router = useRouter()
const store = useWorkOrderStore()
const wo = ref<WorkOrder | null>(null)
const accepting = ref(false)

function callPhone() {
  if (wo.value) location.href = `tel:${wo.value.customerPhone}`
}
function goInstall() {
  router.push(`/workorders/${props.id}/install`)
}

async function handleAccept() {
  if (!wo.value) return
  accepting.value = true
  try {
    const updated = await acceptWorkOrder(props.id)
    wo.value = updated
    store.upsertLocal(updated)
    showSuccessToast('接单成功')
  } catch (e: unknown) {
    showFailToast(e instanceof Error ? e.message : '接单失败')
  } finally {
    accepting.value = false
  }
}

onMounted(async () => {
  try {
    wo.value = await store.fetchDetail(props.id)
  } catch (e: unknown) {
    showFailToast(e instanceof Error ? e.message : '工单加载失败')
  }
})
</script>

<style lang="scss" scoped>
.wo-detail-page { min-height: 100vh; background: var(--bbpms-bg); padding-bottom: 96px; }
.block { margin: 12px 0; }
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 8px 12px calc(8px + env(safe-area-inset-bottom));
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
  z-index: 10;
}
</style>
