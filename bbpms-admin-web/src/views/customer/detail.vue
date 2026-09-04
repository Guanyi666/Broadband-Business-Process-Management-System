<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomer } from '@/api/customer'
import type { Customer } from '@/api/customer'
import {
  getCustomerAccount, openCustomerAccount, resetCustomerPassword, setCustomerAccountStatus,
  type CustomerAccount
} from '@/api/customerPortal'
import { formatDate, maskIdCard, maskName, maskPhone } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const auth = useAuthStore()
const detail = ref<Customer | null>(null)
const account = ref<CustomerAccount | null>(null)
const loading = ref(false)
const accountDialog = ref(false)
const accountForm = reactive({ username: '', password: 'customer123' })
const canManageAccount = auth.hasPermission('customer-portal:admin')

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getCustomer(route.params.id as string)
    if (canManageAccount) account.value = await getCustomerAccount(route.params.id as string)
  } finally {
    loading.value = false
  }
}

async function openAccount() {
  account.value = await openCustomerAccount({
    customerId: route.params.id as string,
    username: accountForm.username,
    password: accountForm.password
  })
  accountDialog.value = false
  ElMessage.success('客户自助账号已开通')
}

async function toggleAccount() {
  if (!account.value) return
  account.value = await setCustomerAccountStatus(route.params.id as string, account.value.status === 1 ? 0 : 1)
  ElMessage.success(account.value.status === 1 ? '账号已启用' : '账号已停用')
}

async function resetPassword() {
  const { value } = await ElMessageBox.prompt('请输入不少于 6 位的新密码', '重置客户密码', {
    inputType: 'password',
    inputPlaceholder: '6～64 个字符',
    inputPattern: /^.{6,64}$/,
    inputErrorMessage: '密码长度必须为 6～64 个字符'
  })
  await resetCustomerPassword(route.params.id as string, value)
  ElMessage.success('客户密码已重置')
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <div class="app-card">
      <h3>客户信息</h3>
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ maskName(detail.name) }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ maskPhone(detail.phone) }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ maskIdCard(detail.idCardNo) }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDate(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-if="canManageAccount" class="app-card" style="margin-top: 16px">
      <div class="page-toolbar">
        <h3 style="margin: 0">客户自助账号</h3>
        <div>
          <el-button v-if="!account" type="primary" @click="accountDialog = true">开通账号</el-button>
          <template v-else>
            <el-button @click="resetPassword">重置密码</el-button>
            <el-button :type="account.status === 1 ? 'danger' : 'success'" @click="toggleAccount">
              {{ account.status === 1 ? '停用账号' : '启用账号' }}
            </el-button>
          </template>
        </div>
      </div>
      <el-empty v-if="!account" description="尚未开通客户自助账号" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="登录账号">{{ account.username }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="account.status === 1 ? 'success' : 'danger'">{{ account.status === 1 ? '正常' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="绑定时间">{{ formatDate(account.bindTime) }}</el-descriptions-item>
        <el-descriptions-item label="最近登录">{{ formatDate(account.lastLoginTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <el-dialog v-model="accountDialog" title="开通客户自助账号" width="460px">
      <el-form label-width="90px">
        <el-form-item label="登录账号" required><el-input v-model="accountForm.username" placeholder="至少 4 个字符" /></el-form-item>
        <el-form-item label="初始密码" required><el-input v-model="accountForm.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialog = false">取消</el-button>
        <el-button type="primary" :disabled="accountForm.username.length < 4 || accountForm.password.length < 6" @click="openAccount">确认开通</el-button>
      </template>
    </el-dialog>
  </div>
</template>
