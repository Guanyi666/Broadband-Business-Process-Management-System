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
  ElMessage.success('Profile saved (UI only)')
}

async function onChangePwd() {
  if (pwdForm.newPwd !== pwdForm.confirm) {
    ElMessage.error('Passwords do not match')
    return
  }
  if (!auth.userInfo?.id) {
    ElMessage.error('Not signed in')
    return
  }
  await changePassword(auth.userInfo.id, { oldPassword: pwdForm.oldPwd, newPassword: pwdForm.newPwd })
  ElMessage.success('Password changed')
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
    <PageHeader title="Profile" />

    <div class="app-card">
      <h3>Account</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Username">{{ auth.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="Roles">
          <el-tag v-for="r in auth.roles" :key="r" style="margin-right: 4px">{{ r }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Dept">{{ auth.userInfo?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag type="success">Active</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="app-card">
      <h3>Update Info</h3>
      <el-form :model="profileForm" label-width="120px" style="max-width: 560px">
        <el-form-item label="Nickname"><el-input v-model="profileForm.nickname" /></el-form-item>
        <el-form-item label="Email"><el-input v-model="profileForm.email" /></el-form-item>
        <el-form-item label="Phone"><el-input v-model="profileForm.phone" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSaveProfile">Save</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="app-card">
      <h3>Change Password</h3>
      <el-form :model="pwdForm" label-width="160px" style="max-width: 560px">
        <el-form-item label="Old Password"><el-input v-model="pwdForm.oldPwd" type="password" show-password /></el-form-item>
        <el-form-item label="New Password"><el-input v-model="pwdForm.newPwd" type="password" show-password /></el-form-item>
        <el-form-item label="Confirm"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onChangePwd">Change Password</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="app-card">
      <h3>Appearance</h3>
      <el-radio-group :model-value="theme" @change="onThemeChange">
        <el-radio-button value="light">Light</el-radio-button>
        <el-radio-button value="dark">Dark</el-radio-button>
      </el-radio-group>
    </div>
  </div>
</template>