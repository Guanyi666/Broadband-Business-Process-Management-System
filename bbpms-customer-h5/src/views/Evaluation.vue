<template>
  <div class="page">
    <van-nav-bar title="服务评价" left-arrow @click-left="router.back()" />
    <van-form @submit="submit"><div class="page-body">
      <div class="card rating-card">
        <h2>这次装维服务怎么样？</h2><p class="muted">您的评价会帮助我们持续改善服务</p>
        <van-rate v-model="form.overallScore" size="34" gutter="8" color="#ffad32" />
      </div>
      <div class="card detail-rating">
        <div class="row"><span>服务态度</span><van-rate v-model="form.serviceScore" /></div>
        <div class="row"><span>安装质量</span><van-rate v-model="form.qualityScore" /></div>
        <div class="row"><span>准时程度</span><van-rate v-model="form.punctualityScore" /></div>
      </div>
      <div class="card form-card"><van-field v-model="form.content" type="textarea" rows="4" maxlength="1000" show-word-limit placeholder="分享一下您的服务体验（选填）" /></div>
      <van-button block round type="primary" native-type="submit" :loading="loading">提交评价</van-button>
    </div></van-form>
  </div>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { evaluateOrder } from '@/api/portal'
const route = useRoute(); const router = useRouter(); const loading = ref(false)
const form = reactive({ overallScore: 5, serviceScore: 5, qualityScore: 5, punctualityScore: 5, content: '' })
async function submit() { loading.value = true; try { await evaluateOrder(route.params.id as string, form); showSuccessToast('感谢您的评价'); router.replace(`/orders/${route.params.id}`) } finally { loading.value = false } }
</script>
<style scoped lang="scss">.rating-card { text-align: center; padding: 30px 16px; }.rating-card h2 { margin: 0 0 8px; font-size: 20px; }.rating-card .van-rate { margin-top: 24px; }.detail-rating .row { padding: 10px 0; }.form-card { padding: 4px; }</style>
