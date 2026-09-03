<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { changePassword } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'

const auth = useAuthStore()

const profileForm = reactive({
  nickname: auth.userInfo?.nickname || '',
  email: auth.userInfo?.email || '',
  phone: auth.userInfo?.phone || ''
})

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirm: ''
})

const theme = ref(localStorage.getItem('bbpms_theme') || 'light')

function onSaveProfile() {
  // backend update not in scope of this stub; show success toast
  ElMessage.success('已保存（仅界面演示）')
}

async function onChangePwd() {
  if (pwdForm.newPwd !== pwdForm.confirm) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  if (!auth.userInfo?.id) {
    ElMessage.error('未登录')
    return
  }
  await changePassword(auth.userInfo.id, { oldPassword: pwdForm.oldPwd, newPassword: pwdForm.newPwd })
  ElMessage.success('密码修改成功')
  pwdForm.oldPwd = ''
  pwdForm.newPwd = ''
  pwdForm.confirm = ''
}

function onThemeChange(v: string) {
  theme.value = v
  localStorage.setItem('bbpms_theme', v)
  document.documentElement.setAttribute('data-theme', v)
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="个人中心" />

    <div class="app-card">
      <h3>账号</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ auth.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-for="r in auth.roles" :key="r" style="margin-right: 4px">{{ r }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="部门">{{ auth.userInfo?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success">正常</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="app-card">
      <h3>更新信息</h3>
      <el-form :model="profileForm" label-width="120px" style="max-width: 560px">
        <el-form-item label="昵称"><el-input v-model="profileForm.nickname" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="profileForm.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="profileForm.phone" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSaveProfile">保存</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="app-card">
      <h3>修改密码</h3>
      <el-form :model="pwdForm" label-width="160px" style="max-width: 560px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPwd" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPwd" type="password" show-password /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onChangePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="app-card">
      <h3>外观</h3>
      <el-radio-group :model-value="theme" @change="onThemeChange">
        <el-radio-button value="light">浅色</el-radio-button>
        <el-radio-button value="dark">深色</el-radio-button>
      </el-radio-group>
    </div>
  </div>
</template>