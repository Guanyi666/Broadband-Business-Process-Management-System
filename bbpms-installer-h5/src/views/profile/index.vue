<template>
  <div class="profile-page">
    <van-nav-bar title="我的" :border="false" fixed placeholder />
    <div class="hero">
      <img class="avatar" src="/icons/icon-192x192.png" alt="avatar" />
      <div class="info">
        <h2>{{ userInfo?.name || '装维师傅' }}</h2>
        <p>{{ userInfo?.deptName || '装维团队' }}</p>
        <p class="meta">工号:{{ userInfo?.username || '--' }}</p>
      </div>
    </div>

    <van-cell-group inset class="block">
      <van-cell title="今日完成" value="0 单" />
      <van-cell title="本周完成" value="0 单" />
      <van-cell title="本月完成" value="0 单" />
      <van-cell title="客户评分">
        <template #value>
          <van-rate v-model="rating" readonly allow-half color="#ffd21e" size="14" />
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset class="block" @click="$router.push('/attendance')">
      <van-cell title="考勤打卡" is-link :value="attendanceHint" />
      <van-cell title="我的考勤历史" is-link to="/attendance/history" />
    </van-cell-group>

    <van-cell-group inset class="block">
      <van-cell title="我的请假" is-link to="/leave/my" />
      <van-cell title="申请请假" is-link to="/leave/apply" />
    </van-cell-group>

    <van-cell-group inset class="block">
      <van-cell title="手动上报位置" is-link @click="reportLocation" />
    </van-cell-group>

    <van-cell-group inset class="block">
      <van-cell title="修改密码" is-link />
      <van-cell title="通知设置" is-link @click="onNotify" />
      <van-cell title="关于" is-link @click="onAbout" />
      <van-cell title="退出登录" is-link @click="onLogout" />
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showSuccessToast, showConfirmDialog, showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getCurrentPosition } from '@/utils/geo'
import { fetchToday, type AttendanceVO } from '@/api/attendance'

defineOptions({ name: 'ProfileIndex' })

const router = useRouter()
const auth = useAuthStore()
const userInfo = computed(() => auth.userInfo)
const rating = ref(5)
const today = ref<AttendanceVO | null>(null)

const attendanceHint = computed(() => {
  const s = today.value?.status
  if (s === 'ON_DUTY') return '在岗'
  if (s === 'ON_BREAK') return '休息中'
  if (s === 'OFF_DUTY') return '今日已签出'
  if (s === 'AUTO_OFF') return '已自动签出'
  return '未签到'
})

async function refreshToday() {
  try { today.value = await fetchToday() } catch { /* noop */ }
}

async function reportLocation() {
  try {
    const pos = await getCurrentPosition()
    showSuccessToast(`位置已上报 (${pos.lng.toFixed(4)}, ${pos.lat.toFixed(4)})`)
  } catch {
    showToast('请开启定位')
  }
}

function onNotify() { showToast('通知设置开发中') }
function onAbout() {
  showDialog({
    title: '关于 BBPMS 装维端',
    message: `版本:1.0.0\n构建:${new Date().toISOString().slice(0, 10)}`
  })
}
function onLogout() {
  showConfirmDialog({ title: '退出登录', message: '确认退出当前账号?' })
    .then(() => { auth.logout(); router.replace('/login') })
    .catch(() => null)
}

onMounted(refreshToday)
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: var(--bbpms-bg);
  padding-bottom: 80px;
}
.hero {
  display: flex;
  gap: 14px;
  padding: 24px 16px;
  background: linear-gradient(135deg, #1989fa, #0570e0);
  color: #fff;
  align-items: center;
  .avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #fff;
    border: 2px solid rgba(255, 255, 255, 0.4);
  }
  .info {
    flex: 1;
    h2 { font-size: 18px; font-weight: 600; margin: 0 0 4px; }
    p { font-size: 12px; opacity: 0.9; margin: 2px 0; }
  }
}
.block { margin: 12px 0; }
</style>
