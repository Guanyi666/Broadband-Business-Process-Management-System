<template>
  <div class="wo-list-page">
    <van-nav-bar title="我的工单" :border="false" fixed placeholder />
    <van-tabs v-model:active="activeTab" sticky offset-top="46px" @change="onTabChange">
      <van-tab v-for="t in tabs" :key="t.key" :title="t.title" :name="t.key">
        <van-list
          v-model:loading="loading[t.key]"
          :finished="finished[t.key]"
          finished-text="没有更多了"
          @load="onLoad(t.key)"
          :immediate-check="false"
          class="wo-list"
        >
          <div v-for="wo in currentList" :key="wo.id" class="wo-card" @click="goDetail(wo)">
            <div class="head">
              <span class="order-no">{{ wo.workNo }}</span>
              <van-tag :type="statusType(wo.status)">{{ statusLabel(wo.status) }}</van-tag>
            </div>
            <div class="addr">
              <van-icon name="location-o" />
              <span>{{ wo.installAddress }}</span>
            </div>
            <div class="meta">
              <span><van-icon name="phone-o" /> {{ wo.customerPhone }}</span>
              <span><van-icon name="clock-o" /> {{ formatDate(wo.dispatchTime) }}</span>
            </div>
            <div class="pkg">
              <van-tag plain type="primary">{{ wo.packageName }}</van-tag>
            </div>
            <div class="actions" @click.stop>
              <van-button
                v-if="wo.status === 'DISPATCHED'"
                size="small" type="primary" round @click="goAccept(wo)"
              >接单</van-button>
              <van-button
                v-else-if="['ACCEPTED', 'IN_PROGRESS', 'STALLED', 'REASSIGNING'].includes(wo.status)"
                size="small" type="success" round @click="goInstall(wo)"
              >装机</van-button>
              <van-button v-else size="small" plain round @click="goDetail(wo)">查看</van-button>
            </div>
          </div>
          <van-empty
            v-if="!loading[t.key] && currentList.length === 0"
            :description="`暂无${activeTabTitle}`"
          />
        </van-list>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkOrderStore, type TabKey } from '@/stores/workorder'
import type { WorkOrder, WorkOrderStatus } from '@/types/workorder'
import { statusLabel, statusType } from '@/constants/workorder'
import { formatDate } from '@/utils/format'

defineOptions({ name: 'WorkorderList' })

const router = useRouter()
const store = useWorkOrderStore()

// Server-side filters: backend /my filters by a single status.
const tabs = [
  { key: 'DISPATCHED', title: '待接单', status: 'DISPATCHED' as WorkOrderStatus },
  { key: 'IN_PROGRESS', title: '进行中', status: 'IN_PROGRESS' as WorkOrderStatus },
  { key: 'COMPLETED', title: '已完成', status: 'COMPLETED' as WorkOrderStatus }
]

const activeTab = ref<string>('DISPATCHED')
const activeTabTitle = computed(() => tabs.find((t) => t.key === activeTab.value)?.title || '')
const loading = reactive<Record<string, boolean>>({ DISPATCHED: false, IN_PROGRESS: false, COMPLETED: false })
const finished = reactive<Record<string, boolean>>({ DISPATCHED: false, IN_PROGRESS: false, COMPLETED: false })
const currentList = computed(() => store.tabs[activeTab.value as TabKey]?.list || [])

function goDetail(wo: WorkOrder) { router.push(`/workorders/${wo.id}`) }
function goInstall(wo: WorkOrder) { router.push(`/workorders/${wo.id}/install`) }
function goAccept(wo: WorkOrder) { router.push(`/workorders/${wo.id}?action=accept`) }

async function loadTab(key: string, refresh = false) {
  const tab = tabs.find((t) => t.key === key)!
  const tabKey = tab.key as TabKey
  if (refresh) {
    store.tabs[tabKey].list = []
    store.tabs[tabKey].page = 0
    store.tabs[tabKey].finished = false
  }
  loading[key] = true
  try {
    await store.fetchList(tab.status, refresh)
    finished[key] = store.tabs[tabKey].finished
  } finally {
    loading[key] = false
  }
}

function onTabChange(name: string | number) {
  activeTab.value = String(name)
  if (currentList.value.length === 0 && !finished[activeTab.value]) loadTab(activeTab.value, false)
}

function onLoad(key: string) {
  if (finished[key]) return
  loadTab(key, false)
}

onMounted(() => loadTab(activeTab.value, true))
</script>

<style lang="scss" scoped>
.wo-list-page {
  min-height: 100vh;
  background: var(--bbpms-bg);
  padding-bottom: 56px;
}
.wo-list {
  padding: 8px 12px 16px;
  min-height: 60vh;
}
.wo-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  .head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    .order-no { font-size: 14px; font-weight: 600; color: #323233; }
  }
  .addr {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #323233;
    font-size: 14px;
    margin-bottom: 8px;
    .van-icon { color: #1989fa; }
  }
  .meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 12px;
    color: #969799;
    margin-bottom: 8px;
    .van-icon { margin-right: 2px; }
  }
  .pkg {
    margin-bottom: 8px;
  }
  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 8px;
    border-top: 1px solid #f7f8fa;
  }
}
</style>
