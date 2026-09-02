<template>
  <div class="my-page">
    <van-nav-bar title="我的请假" :border="false" left-arrow @click-left="$router.back()" fixed placeholder />
    <div class="apply-cta">
      <van-button type="primary" round block @click="$router.push('/leave/apply')">申请请假</van-button>
    </div>

    <van-list :loading="loading" :finished="finished" finished-text="没有更多了">
      <van-cell-group v-for="r in records" :key="r.id" inset class="leave-card">
        <van-cell>
          <template #title>
            <div class="header">
              <van-tag :type="statusType(r.status)" round>{{ statusLabel(r.status) }}</van-tag>
              <span class="type">{{ r.leaveType }}</span>
            </div>
          </template>
        </van-cell>
        <van-cell title="时间" :value="`${formatDate(r.startAt)} ~ ${formatDate(r.endAt)}`" />
        <van-cell title="时长" :value="`${r.totalHours} 小时`" />
        <van-cell title="原因" :label="r.reason" />
        <van-cell title="审批进度">
          <template #value>
            <van-steps :active="r.currentLevel" direction="horizontal" active-color="#1989fa" inactive-color="#dcdee0">
              <van-step>L1</van-step>
              <van-step v-if="r.requiredLevel >= 2">L2</van-step>
            </van-steps>
            <div class="meta" v-if="r.level1ApproverName">
              L1: {{ r.level1ApproverName }} {{ formatDate(r.level1DecidedAt) }}
            </div>
            <div class="meta" v-if="r.level2ApproverName">
              L2: {{ r.level2ApproverName }} {{ formatDate(r.level2DecidedAt) }}
            </div>
          </template>
        </van-cell>
        <van-cell v-if="r.status === 'PENDING'">
          <template #value>
            <van-button size="small" plain type="danger" @click="onCancel(r.id)">撤销</van-button>
          </template>
        </van-cell>
      </van-cell-group>
      <div v-if="!loading && records.length === 0" class="empty">
        暂无请假记录
      </div>
    </van-list>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { showSuccessToast, showFailToast, showConfirmDialog } from 'vant'
import { fetchMyLeaves, cancelLeave, type LeaveVO } from '@/api/leave'

defineOptions({ name: 'LeaveMyApplications' })

const records = ref<LeaveVO[]>([])
const loading = ref(false)
const finished = ref(false)

function statusType(s: string): 'success' | 'warning' | 'danger' | 'primary' | 'default' {
  switch (s) {
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    case 'CANCELLED': return 'default'
    default: return 'warning'
  }
}
function statusLabel(s: string) {
  return ({ APPROVED: '已批准', REJECTED: '已拒绝', CANCELLED: '已撤销', PENDING: '审批中' } as any)[s] || s
}
function formatDate(iso?: string) {
  if (!iso) return '--'
  return iso.substring(0, 16).replace('T', ' ')
}

async function load() {
  loading.value = true
  try {
    records.value = await fetchMyLeaves()
  } finally {
    loading.value = false
    finished.value = true
  }
}

async function onCancel(id: number) {
  try {
    await showConfirmDialog({ title: '撤销', message: '确认撤销该请假申请?' })
  } catch { return }
  try {
    await cancelLeave(id)
    showSuccessToast('已撤销')
    await load()
  } catch (e: any) {
    showFailToast(e?.message || '撤销失败')
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.my-page { min-height: 100vh; background: var(--bbpms-bg); padding-bottom: 24px; }
.apply-cta { padding: 12px; }
.leave-card { margin: 12px 0; }
.header { display: flex; gap: 8px; align-items: center; }
.type { font-weight: 500; }
.meta { font-size: 12px; color: #969799; margin-top: 4px; }
.empty { text-align: center; color: #969799; padding: 24px; }
</style>
