<template>
  <div class="page">
    <div class="profile-head">
      <div class="avatar">{{ profile?.customer.name?.slice(-1) || '客' }}</div>
      <div><h2>{{ profile?.customer.name || auth.user?.name }}</h2><p>{{ maskPhone(profile?.customer.phone) }}</p></div>
    </div>
    <div class="page-body profile-body">
      <div class="card">
        <van-cell title="登录账号" :value="profile?.account.username || '-'" icon="manager-o" />
        <van-cell title="安装地址" :label="profile?.customer.address || '未填写'" icon="location-o" />
      </div>
      <div class="card menu-card">
        <van-cell title="消息中心" icon="envelop-o" is-link to="/messages" />
        <van-cell title="资料变更" icon="edit" is-link to="/profile/change" />
        <van-cell title="修改密码" icon="shield-o" is-link @click="showPassword = true" />
      </div>
      <van-button block plain round type="danger" @click="logout">退出登录</van-button>
    </div>
    <van-dialog v-model:show="showPassword" title="修改密码" show-cancel-button :before-close="changePwd">
      <van-field v-model="pwd.oldPassword" type="password" label="原密码" placeholder="请输入原密码" />
      <van-field v-model="pwd.newPassword" type="password" label="新密码" placeholder="至少 6 个字符" />
    </van-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { changePassword, getProfile } from '@/api/portal'
import { useAuthStore } from '@/stores/auth'
import type { Profile } from '@/types'
const auth = useAuthStore(); const router = useRouter(); const profile = ref<Profile | null>(null); const showPassword = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '' })
const maskPhone = (v?: string) => !v ? '' : v.replace(/^(\d{3})\d+(\d{4})$/, '$1****$2')
async function changePwd(action: string) { if (action !== 'confirm') return true; if (pwd.newPassword.length < 6) return false; await changePassword(pwd); showSuccessToast('密码修改成功'); return true }
async function logout() { await auth.signOut(); router.replace('/login') }
onMounted(async () => { profile.value = await getProfile() })
</script>
<style scoped lang="scss">.profile-head { height: 210px; padding: 62px 28px; color: #fff; display: flex; align-items: center; gap: 16px; background: linear-gradient(135deg, #0750be, #4d91f4); }.avatar { width: 66px; height: 66px; display: grid; place-items: center; border-radius: 50%; background: rgba(255,255,255,.22); font-size: 28px; font-weight: 700; border: 2px solid rgba(255,255,255,.55); }.profile-head h2 { margin: 0 0 8px; }.profile-head p { margin: 0; opacity: .8; }.profile-body { margin-top: -28px; position: relative; }.menu-card { padding: 4px; }</style>
