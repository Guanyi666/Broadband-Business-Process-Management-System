<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomer } from '@/api/customer'
import type { Customer } from '@/api/customer'
import { formatDate, maskIdCard, maskName, maskPhone } from '@/utils/format'

const route = useRoute()
const detail = ref<Customer | null>(null)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getCustomer(route.params.id as string)
  } finally {
    loading.value = false
  }
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
        <el-descriptions-item label="身份证号">{{ maskIdCard(detail.idCard) }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>