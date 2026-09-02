<template>
  <div class="attendance-page">
    <van-nav-bar title="考勤打卡" :border="false" left-arrow @click-left="onBack" fixed placeholder />

    <div class="card-status" :class="statusClass">
      <div class="big">{{ statusLabel }}</div>
      <div class="sub" v-if="today?.clockInAt">
        签到: {{ formatTime(today.clockInAt) }}
        <span v-if="today.workMinutes"> · 已工作 {{ formatMinutes(today.workMinutes) }}</span>
      </div>
      <div class="sub" v-else>今日尚未签到</div>
    </div>

    <van-cell-group inset class="block">
      <van-cell title="今日工作时长" :value="today?.workMinutes != null ? formatMinutes(today.workMinutes) : '0 分钟'" />
      <van-cell title="本月累计" :value="monthly ? formatMinutes(monthly.totalWorkMinutes) : '加载中...'" />
      <van-cell title="本月迟到次数" :value="monthly ? `${monthly.lateCount} 次` : '--'" />
    </van-cell-group>

    <div class="action-bar">
      <van-button v-if="!isOnDuty" type="primary" block round :loading="busy" @click="onClockIn">
        签到
      </van-button>
      <template v-else>
        <van-button v-if="today?.status !== 'ON_BREAK'" type="warning" block round :loading="busy" @click="onBreakStart">
          开始休息
        </van-button>
        <van-button v-else type="success" block round :loading="busy" @click="onBreakEnd" style="margin-top: 12px">
          结束休息
        </van-button>
        <van-button type="danger" block round :loading="busy" @click="onClockOut" style="margin-top: 12px">
          签出
        </van-button>
      </template>
    </div>

    <van-cell-group inset class="block">
      <van-cell title="历史记录" is-link @click="$router.push('/attendance/history')" />
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showToast } from 'vant'
import { getCurrentPosition } from '@/utils/geo'
import { clockIn, clockOut, breakStart, breakEnd, fetchToday, fetchMonthly, type AttendanceVO } from '@/api/attendance'

defineOptions({ name: 'AttendanceIndex' })

const router = useRouter()
const today = ref<AttendanceVO | null>(null)
const monthly = ref<any>(null)
const busy = ref(false)

const isOnDuty = computed(() => {
  const s = today.value?.status
  return s === 'ON_DUTY' || s === 'ON_BREAK'
})

const statusLabel = computed(() => {
  switch (today.value?.status) {
    case 'ON_DUTY': return '在岗'
    case 'ON_BREAK': return '休息中'
    case 'AUTO_OFF': return '已自动签出'
    case 'OFF_DUTY': return '已签出'
    default: return '未签到'
  }
})

const statusClass = computed(() => {
  switch (today.value?.status) {
    case 'ON_DUTY': return 'on-duty'
    case 'ON_BREAK': return 'on-break'
    case 'AUTO_OFF': return 'auto-off'
    case 'OFF_DUTY': return 'off-duty'
    default: return 'pending'
  }
})

function formatTime(iso: string) {
  return iso.substring(11, 16)
}
function formatMinutes(min: number) {
  const h = Math.floor(min / 60)
  const m = min % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

async function refresh() {
  try {
    today.value = await fetchToday()
    monthly.value = await fetchMonthly()
  } catch (e) {
    /* ignored */
  }
}

async function withGps<T>(fn: (lat?: number, lng?: number) => Promise<T>) {
  busy.value = true
  try {
    let lat, lng
    try { const pos = await getCurrentPosition(); lat = pos.lat; lng = pos.lng } catch { /* no GPS */ }
    return await fn(lat, lng)
  } finally {
    busy.value = false
  }
}

async function onClockIn() {
  await withGps(async (lat, lng) => {
    try {
      today.value = await clockIn({ lat, lng, source: 'GPS' })
      showSuccessToast('签到成功')
      await refresh()
    } catch (e: any) {
      showFailToast(e?.message || '签到失败')
    }
  })
}

async function onClockOut() {
  await withGps(async (lat, lng) => {
    try {
      today.value = await clockOut({ lat, lng })
      showSuccessToast('签出成功')
      await refresh()
    } catch (e: any) {
      showFailToast(e?.message || '签出失败')
    }
  })
}

async function onBreakStart() {
  busy.value = true
  try {
    today.value = await breakStart()
    showSuccessToast('开始休息')
  } catch (e: any) { showFailToast(e?.message || '操作失败') }
  finally { busy.value = false }
}

async function onBreakEnd() {
  busy.value = true
  try {
    today.value = await breakEnd()
    showSuccessToast('休息结束')
  } catch (e: any) { showFailToast(e?.message || '操作失败') }
  finally { busy.value = false }
}

function onBack() { router.back() }

onMounted(refresh)
</script>

<style lang="scss" scoped>
.attendance-page {
  min-height: 100vh;
  background: var(--bbpms-bg);
  padding-bottom: 24px;
}
.card-status {
  margin: 16px 12px;
  padding: 24px 16px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #6c757d, #495057);
  .big { font-size: 32px; font-weight: 600; margin-bottom: 4px; }
  .sub { font-size: 14px; opacity: 0.9; }
  &.on-duty   { background: linear-gradient(135deg, #1989fa, #0570e0); }
  &.on-break  { background: linear-gradient(135deg, #ffb84d, #f59f00); }
  &.auto-off  { background: linear-gradient(135deg, #dc3545, #b02a37); }
  &.off-duty  { background: linear-gradient(135deg, #6c757d, #495057); }
  &.pending   { background: linear-gradient(135deg, #adb5bd, #6c757d); }
}
.block { margin: 12px 0; }
.action-bar { padding: 16px 12px; }
</style>
