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
const captchaError = ref('')

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadPublicKey() {
  try {
    publicKey.value = await getPublicKey()
  } catch (e) {
    publicKey.value = ''
    ElMessage.warning('获取公钥失败，密码将以明文方式传输（仅供本地开发）')
  }
}

/**
 * 拉取验证码。
 * 后端 LineCaptcha(160, 60) 生成的图片宽高比为 8:3，
 * 因此容器固定为 120×45（同比 8:3）且使用 object-fit: contain，
 * 保证「完整显示 + 比例不变形 + 不裁切 + 不溢出」。
 */
async function loadCaptcha() {
  captchaLoading.value = true
  captchaError.value = ''
  captchaImg.value = ''
  try {
    const c = await getCaptcha()
    form.captchaId = c.captchaId
    if (c.image) {
      const raw = c.image.replace(/\s+/g, '')
      captchaImg.value = raw.startsWith('data:') ? raw : `data:image/png;base64,${raw}`
    } else {
      captchaError.value = '验证码为空，请点击重试'
    }
  } catch (e: any) {
    captchaError.value = e?.message || '加载失败'
  } finally {
    captchaLoading.value = false
  }
}

/** 图片解码失败兜底：不显示破图，转为可点击重试的占位 */
function onCaptchaImgError() {
  captchaImg.value = ''
  captchaError.value = '加载失败'
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
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.replace(redirect)
    } catch (e: any) {
      ElMessage.error(e?.message || '登录失败，请检查账号或验证码')
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
        <div class="brand-sub">宽带业务工单管理系统</div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @keyup.enter="onLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="'User'" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="'Lock'"
            size="large"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="form.captcha" placeholder="请输入验证码" :prefix-icon="'Picture'" size="large" />
            <!-- 验证码四态：图片 / 加载中 / 加载失败 / hover 提示刷新 -->
            <div
              class="captcha-img"
              :class="{ 'is-error': !!captchaError }"
              role="button"
              :title="captchaError ? '加载失败，点击重试' : '点击刷新验证码'"
              @click="loadCaptcha"
            >
              <img
                v-if="captchaImg"
                :src="captchaImg"
                alt="验证码"
                @error="onCaptchaImgError"
              />
              <span v-else-if="captchaLoading" class="captcha-placeholder">加载中…</span>
              <span v-else class="captcha-placeholder">{{ captchaError || '点击刷新' }}</span>
              <span v-if="captchaImg && !captchaLoading" class="captcha-refresh">点击刷新</span>
            </div>
          </div>
        </el-form-item>
        <div class="form-extra">
          <el-checkbox v-model="form.remember">记住我</el-checkbox>
        </div>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-btn"
          @click="onLogin"
        >
          登 录
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
  // 容器 120×45 与后端 LineCaptcha(160×60) 同比例（8:3），
  // 配合 object-fit: contain 保证完整显示、比例不变形、不裁切。
  .captcha-img {
    position: relative;
    flex: 0 0 120px; // 不被输入框挤压
    width: 120px;
    height: 45px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #f5f7fa;
    overflow: hidden;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      border-color: var(--el-color-primary, #409eff);
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
    }

    &.is-error {
      border-color: var(--el-color-danger, #f56c6c);
    }

    img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: contain; // 完整显示，不裁切
      object-position: center;
    }

    // hover 时浮出的「点击刷新」提示，不遮挡关键像素（顶部细条）
    .captcha-refresh {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      padding: 1px 0;
      font-size: 10px;
      line-height: 14px;
      text-align: center;
      color: #fff;
      background: rgba(0, 0, 0, 0.45);
      opacity: 0;
      transition: opacity 0.2s ease;
      pointer-events: none;
    }

    &:hover .captcha-refresh {
      opacity: 1;
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