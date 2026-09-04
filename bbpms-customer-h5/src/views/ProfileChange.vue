<template>
  <div class="page">
    <van-nav-bar title="资料变更" left-arrow @click-left="router.back()" />
    <van-form @submit="submit"><div class="page-body">
      <div class="card form-card">
        <div class="card-title">提交新的资料</div>
        <van-field v-model="form.name" label="姓名" placeholder="不修改请留空" />
        <van-field v-model="form.phone" label="联系电话" placeholder="不修改请留空" />
        <van-field v-model="form.idCardNo" label="身份证号" placeholder="不修改请留空" />
        <van-field v-model="form.address" label="联系地址" type="textarea" rows="2" placeholder="不修改请留空" />
      </div>
      <p class="muted hint">提交后由后台审核，审核通过前不会修改正式客户资料。</p>
      <van-button block round type="primary" native-type="submit" :loading="loading">提交审核</van-button>
      <h2 class="section-title">申请记录</h2>
      <div v-for="item in changes" :key="item.id" class="card">
        <div class="row"><strong>{{ item.changeFields }}</strong><span class="status-pill">{{ statusText(item.status) }}</span></div>
        <p v-if="item.reviewRemark" class="muted">审核意见：{{ item.reviewRemark }}</p>
      </div>
      <van-empty v-if="!changes.length" description="暂无变更申请" />
    </div></van-form>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { listProfileChanges, requestProfileChange } from '@/api/portal'
import type { ProfileChange } from '@/types'
const router = useRouter(); const loading = ref(false); const changes = ref<ProfileChange[]>([])
const form = reactive({ name: '', phone: '', idCardNo: '', address: '' })
const statusText = (s: string) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s)
async function load() { changes.value = await listProfileChanges() }
async function submit() { loading.value = true; try { await requestProfileChange(form); showSuccessToast('变更申请已提交'); Object.assign(form, { name: '', phone: '', idCardNo: '', address: '' }); load() } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>.form-card { padding: 14px 4px; }.card-title { padding: 0 12px; }.hint { padding: 0 8px 14px; }</style>
