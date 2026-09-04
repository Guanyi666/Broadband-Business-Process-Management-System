<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomer, getCustomerUnmasked } from '@/api/customer'
import type { Customer } from '@/api/customer'
import { formatDate, maskIdCard, maskName, maskPhone } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const detail = ref<Customer | null>(null)
/** 明文数据：仅在具备 customer:view-sensitive 权限且接口成功时赋值，否则保持 null（降级为脱敏） */
const unmasked = ref<Customer | null>(null)
const loading = ref(false)

const canViewSensitive = computed(() => auth.hasPermission('customer:view-sensitive'))
/** 展示用数据：优先明文，降级为脱敏 */
const display = computed<Customer | null>(() => unmasked.value ?? detail.value)
/** 当前是否以明文方式展示 */
const isPlaintext = computed(() => !!unmasked.value)

function idCardOf(c?: Customer | null) {
  return c?.idCard ?? c?.idCardNo ?? ''
}

async function fetchData() {
  loading.value = true
  unmasked.value = null
  try {
    detail.value = await getCustomer(route.params.id as string)
    // 仅当用户具备敏感信息查看权限时尝试拉取明文；403 等异常静默降级为脱敏数据
    if (canViewSensitive.value) {
      try {
        unmasked.value = await getCustomerUnmasked(route.params.id as string)
      } catch {
        unmasked.value = null
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <div class="app-card">
      <div class="detail-header">
        <h3>客户信息</h3>
        <el-tag v-if="isPlaintext" type="success" size="small">明文（已授权 customer:view-sensitive）</el-tag>
        <el-tag v-else type="info" size="small">已脱敏</el-tag>
      </div>
      <el-descriptions :column="2" border v-if="display">
        <el-descriptions-item label="ID">{{ display.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ isPlaintext ? display.name : maskName(display.name) }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ isPlaintext ? display.phone : maskPhone(display.phone) }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ isPlaintext ? idCardOf(display) : maskIdCard(idCardOf(display)) }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ display.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ display.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ display.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDate(display.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<style scoped lang="scss">
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  h3 {
    margin: 0;
  }
}
</style>
