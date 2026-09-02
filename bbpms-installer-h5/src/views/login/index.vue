<template>
  <div class="login-page">
    <van-nav-bar title="装维登录" :border="false" />
    <div class="login-hero">
      <img class="logo" src="/icons/icon-192x192.png" alt="logo" />
      <h1>BBPMS 装维助手</h1>
      <p>为一线装维工程师而生</p>
    </div>
    <van-form @submit="handleSubmit" class="login-form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入工号或手机号"
          left-icon="user-o"
          :rules="[{ required: true, message: '请填写账号' }]"
        />
        <van-field
          v-model="form.password"
          :type="showPwd ? 'text' : 'password'"
          name="password"
          label="密码"
          placeholder="请输入密码"
          left-icon="lock"
          :right-icon="showPwd ? 'eye-o' : 'closed-eye'"
          @click-right-icon="showPwd = !showPwd"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
        <van-field
          v-model="form.captcha"
          name="captcha"
          label="验证码"
          placeholder="请输入验证码"
          left-icon="shield-o"
          maxlength="4"
        >
          <template #button>
            <img class="captcha-img" :src="captchaData?.image" alt="captcha" @click="refreshCaptcha" />
          </template>
        </van-field>
      </van-cell-group>

      <div class="login-submit">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
      </div>
    </van-form>

    <p class="footer-tip">登录即代表同意《装维服务协议》</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showFailToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { fetchCaptcha, fetchPublicKey } from '@/api/auth'
import type { CaptchaResult } from '@/types/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const showPwd = ref(false)
const loading = ref(false)
const captchaData = ref<CaptchaResult | null>(null)

const form = reactive({
  username: '',
  password: '',
  captcha: ''
})

async function refreshCaptcha() {
  try {
    captchaData.value = await fetchCaptcha()
  } catch {
    showFailToast('验证码加载失败')
  }
}

async function handleSubmit() {
  if (!form.username || !form.password) {
    showFailToast('请填写账号密码')
    return
  }
  loading.value = true
  try {
    let publicKey = import.meta.env.VITE_RSA_PUBLIC_KEY
    try {
      // Backend returns the raw PEM string in R.data.
      publicKey = await fetchPublicKey()
    } catch { /* fallback to env key */ }
    await auth.login(
      {
        username: form.username,
        password: form.password,
        captcha: form.captcha,
        captchaId: captchaData.value?.id
      },
      publicKey
    )
    showToast('登录成功')
    const redirect = (route.query.redirect as string) || '/workorders'
    router.replace(redirect)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败'
    showFailToast(msg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1989fa 0%, #ffffff 38%);
}
.login-hero {
  text-align: center;
  color: #fff;
  padding: 32px 16px 24px;
  .logo {
    width: 72px;
    height: 72px;
    border-radius: 18px;
    background: #fff;
    margin-bottom: 12px;
  }
  h1 { font-size: 22px; margin: 8px 0 4px; font-weight: 600; }
  p { font-size: 13px; opacity: 0.85; }
}
.login-form { margin-top: 16px; }
.captcha-img {
  width: 96px;
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
  background: #f7f8fa;
}
.login-submit { padding: 12px 20px 24px; }
.footer-tip {
  text-align: center;
  color: #969799;
  font-size: 12px;
  padding: 16px;
}
</style>
