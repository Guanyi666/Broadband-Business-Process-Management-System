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
      <h3>Customer Info</h3>
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="Name">{{ maskName(detail.name) }}</el-descriptions-item>
        <el-descriptions-item label="Phone">{{ maskPhone(detail.phone) }}</el-descriptions-item>
        <el-descriptions-item label="ID Card">{{ maskIdCard(detail.idCard) }}</el-descriptions-item>
        <el-descriptions-item label="Email">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Source">{{ detail.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Address" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Created" :span="2">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>