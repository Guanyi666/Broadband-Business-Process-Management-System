<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import JSEncrypt from 'jsencrypt'
import { getCaptcha, getPublicKey, login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const captchaLoading = ref(false)
const publicKey = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captcha: '',
  remember: false
})

const captchaImg = ref('')

const rules = {
  username: [{ required: true, message: 'Username is required', trigger: 'blur' }],
  password: [{ required: true, message: 'Password is required', trigger: 'blur' }],
  captcha: [{ required: true, message: 'Captcha is required', trigger: 'blur' }]
}

async function loadPublicKey() {
  try {
    publicKey.value = await getPublicKey()
  } catch (e) {
    publicKey.value = ''
    ElMessage.warning('Public key unavailable; password will be sent in plaintext (dev only)')
  }
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const c = await getCaptcha()
    form.captchaId = c.captchaId
    if (c.image) {
      const raw = c.image.replace(/\s+/g, '')
      captchaImg.value = raw.startsWith('data:') ? raw : `data:image/png;base64,${raw}`
    }
  } catch (e) {
    console.error('Failed to load captcha:', e)
  } finally {
    captchaLoading.value = false
  }
}

onMounted(async () => {
  await loadPublicKey()
  await loadCaptcha()
})

function encryptPassword(pwd: string): string {
  if (!publicKey.value) return pwd
  try {
    const enc = new JSEncrypt()
    enc.setPublicKey(publicKey.value)
    return enc.encrypt(pwd) || pwd
  } catch {
    return pwd
  }
}

async function onLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      const pwd = encryptPassword(form.password)
      await auth.login({
        username: form.username,
        password: pwd,
        captchaId: form.captchaId,
        captcha: form.captcha
      })
      await auth.fetchUserInfo()
      ElMessage.success('Welcome back!')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.replace(redirect)
    } catch (e: any) {
      ElMessage.error(e?.message || 'Login failed')
      await loadCaptcha()
      form.captcha = ''
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-card">
      <div class="brand">
        <div class="brand-logo">B</div>
        <div class="brand-name">BBPMS Admin</div>
        <div class="brand-sub">Broadband Package Product Management System</div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @keyup.enter="onLogin"
      >
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Enter username" :prefix-icon="'User'" size="large" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="Enter password"
            :prefix-icon="'Lock'"
            size="large"
          />
        </el-form-item>
        <el-form-item label="Captcha" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="form.captcha" placeholder="Enter captcha" :prefix-icon="'Picture'" size="large" />
            <div class="captcha-img" @click="loadCaptcha">
              <img v-if="captchaImg" :src="captchaImg" alt="captcha" />
              <span v-else v-loading="captchaLoading" class="captcha-placeholder">Loading</span>
            </div>
          </div>
        </el-form-item>
        <div class="form-extra">
          <el-checkbox v-model="form.remember">Remember me</el-checkbox>
          <a class="forgot">Forgot password?</a>
        </div>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-btn"
          @click="onLogin"
        >
          Sign in
        </el-button>
      </el-form>

      <div class="footer">© 2026 BBPMS</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1890ff 0%, #001529 100%);
  overflow: hidden;
  padding: 16px;
}
.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(255,255,255,0.10), transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(64,158,255,0.20), transparent 50%);
  pointer-events: none;
}
.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 32px 36px 24px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}
.brand {
  text-align: center;
  margin-bottom: 24px;
  &-logo {
    width: 56px;
    height: 56px;
    margin: 0 auto 12px;
    border-radius: 14px;
    background: linear-gradient(135deg, #1890ff, #001529);
    color: #fff;
    font-size: 26px;
    font-weight: 700;
    line-height: 56px;
  }
  &-name {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
  }
  &-sub {
    color: #909399;
    font-size: 13px;
    margin-top: 4px;
  }
}
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
  .captcha-img {
    width: 120px;
    height: 40px;
    border-radius: 4px;
    background: #f5f7fa;
    overflow: hidden;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .captcha-placeholder {
      font-size: 12px;
      color: #909399;
    }
  }
}
.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  .forgot {
    font-size: 13px;
    color: $primary-color;
    cursor: pointer;
  }
}
.login-btn {
  width: 100%;
}
.footer {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 16px;
}
</style>