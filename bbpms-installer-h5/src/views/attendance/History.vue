<template>
  <div class="history-page">
    <van-nav-bar title="考勤历史" :border="false" left-arrow @click-left="$router.back()" fixed placeholder />
    <van-tabs v-model:active="activeTab" sticky offset-top="46px">
      <van-tab title="最近 30 天" name="daily">
        <van-list :loading="loading" :finished="finished" finished-text="没有更多了">
          <van-cell v-for="r in records" :key="r.id" :title="r.workDate" :label="statusLabel(r.status)">
            <template #value>
              <span v-if="r.workMinutes">{{ formatMinutes(r.workMinutes) }}</span>
              <span v-else>--</span>
            </template>
          </van-cell>
        </van-list>
      </van-tab>
      <van-tab title="按月" name="monthly">
        <van-cell title="本月统计" :value="monthly ? formatMinutes(monthly.totalWorkMinutes) : '--'" />
        <van-cell title="工作天数" :value="monthly ? `${monthly.workDays} 天` : '--'" />
        <van-cell title="迟到次数" :value="monthly ? `${monthly.lateCount} 次` : '--'" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { fetchMy, fetchMonthly, type AttendanceVO, type AttendanceSummaryVO } from '@/api/attendance'

defineOptions({ name: 'AttendanceHistory' })

const activeTab = ref<'daily' | 'monthly'>('daily')
const records = ref<AttendanceVO[]>([])
const monthly = ref<AttendanceSummaryVO | null>(null)
const loading = ref(false)
const finished = ref(false)

function statusLabel(s: string) {
  switch (s) {
    case 'ON_DUTY': return '在岗'
    case 'ON_BREAK': return '休息中'
    case 'AUTO_OFF': return '已自动签出'
    default: return s === 'OFF_DUTY' ? '已签出' : '未签到'
  }
}

function formatMinutes(min: number) {
  const h = Math.floor(min / 60); const m = min % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

async function loadDaily() {
  loading.value = true
  try {
    records.value = await fetchMy(30)
  } finally {
    loading.value = false
    finished.value = true
  }
}

async function loadMonthly() {
  monthly.value = await fetchMonthly()
}

watch(activeTab, (t) => { if (t === 'daily') loadDaily(); else loadMonthly() })

onMounted(loadDaily)
</script>

<style lang="scss" scoped>
.history-page { min-height: 100vh; background: var(--bbpms-bg); padding-bottom: 24px; }
</style>
