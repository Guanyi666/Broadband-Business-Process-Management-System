<template>
  <div class="page">
    <van-nav-bar title="预约改期" left-arrow @click-left="router.back()" />
    <van-form @submit="submit"><div class="page-body">
      <div class="card form-card">
        <van-field v-model="appointmentTime" label="新的时间" placeholder="YYYY-MM-DD HH:mm:ss" :rules="[{ required: true, message: '请输入新的预约时间' }]" />
        <van-field v-model="reason" label="改期原因" type="textarea" rows="3" placeholder="请说明改期原因" />
      </div>
      <p class="muted hint">订单开始施工后不可改期，修改记录会同步给客服和装维人员。</p>
      <van-button block round type="primary" native-type="submit" :loading="loading">确认改期</van-button>
    </div></van-form>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { reschedule } from '@/api/portal'
const route = useRoute(); const router = useRouter(); const appointmentTime = ref(''); const reason = ref(''); const loading = ref(false)
async function submit() { loading.value = true; try { await reschedule(route.params.id as string, { appointmentTime: appointmentTime.value, reason: reason.value }); showSuccessToast('预约时间已修改'); router.back() } finally { loading.value = false } }
</script>
<style scoped>.form-card { padding: 8px 2px; }.hint { padding: 0 8px 16px; line-height: 1.6; }</style>
