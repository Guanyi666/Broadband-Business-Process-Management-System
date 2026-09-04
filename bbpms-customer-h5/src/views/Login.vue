<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-mark">B</div>
      <h1>宽带服务助手</h1>
      <p>订单进度、在线报装、售后服务一站办理</p>
    </div>
    <van-form class="login-card" @submit="submit">
      <div class="login-title">客户账号登录</div>
      <van-cell-group inset>
        <van-field v-model="username" name="username" label="账号" left-icon="user-o" placeholder="请输入客户账号" :rules="[{ required: true, message: '请输入账号' }]" />
        <van-field v-model="password" name="password" label="密码" left-icon="lock" type="password" placeholder="请输入密码" :rules="[{ required: true, message: '请输入密码' }]" />
      </van-cell-group>
      <div class="submit-wrap"><van-button block round type="primary" native-type="submit" :loading="loading">登录</van-button></div>
      <p class="tip">账号由客服开通；忘记密码请联系人工客服重置</p>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { useAuthStore } from '@/stores/auth'

const username = ref('customer1')
const password = ref('admin123')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function submit() {
  loading.value = true
  try {
    await auth.signIn(username.value.trim(), password.value)
    showSuccessToast('登录成功')
    router.replace((route.query.redirect as string) || '/home')
  } catch (error) {
    if (error instanceof Error && error.message === '该账号不是客户自助账号') {
      showFailToast(error.message)
    }
  } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.login-page { min-height: 100vh; background: linear-gradient(160deg, #0750be 0 38%, #f4f7fb 38%); padding: 72px 18px 30px; }
.login-brand { color: #fff; text-align: center; margin-bottom: 38px; }
.brand-mark { width: 66px; height: 66px; margin: auto; border-radius: 20px; background: rgba(255,255,255,.18); display: grid; place-items: center; font-size: 34px; font-weight: 800; border: 1px solid rgba(255,255,255,.28); }
h1 { margin: 15px 0 6px; font-size: 25px; } p { margin: 0; font-size: 13px; opacity: .8; }
.login-card { background: #fff; border-radius: 20px; padding: 22px 4px 18px; box-shadow: 0 14px 38px rgba(20,51,100,.15); }
.login-title { font-weight: 700; font-size: 18px; padding: 0 20px 18px; }
.submit-wrap { padding: 24px 18px 10px; }
.tip { color: #8b95a8; text-align: center; opacity: 1; padding: 8px; }
</style>
