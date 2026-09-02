<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { pageCustomers } from '@/api/customer'
import type { Customer } from '@/api/customer'
import { maskPhone, maskIdCard, maskName, formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()

const loading = ref(false)
const list = ref<Customer[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageCustomers(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  fetchData()
}

function onReset() {
  query.keyword = ''
  query.pageNum = 1
  fetchData()
}

function onRowClick(row: Customer) {
  router.push(`/customer/detail/${row.id}`)
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="Customers" description="Customer information (masked in list view)" />
    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input
            v-model="query.keyword"
            placeholder="Name / Phone / Address"
            clearable
            style="width: 280px"
            @keyup.enter="onSearch"
          />
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe @row-click="onRowClick">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="Name">
          <template #default="{ row }">{{ maskName(row.name) }}</template>
        </el-table-column>
        <el-table-column label="Phone">
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="ID Card">
          <template #default="{ row }">{{ maskIdCard(row.idCard) }}</template>
        </el-table-column>
        <el-table-column prop="address" label="Address" show-overflow-tooltip />
        <el-table-column prop="source" label="Source" width="120" />
        <el-table-column label="Created">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Action" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="onRowClick(row)">Detail</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          @current-change="(p) => { query.pageNum = p; fetchData() }"
          @size-change="(s) => { query.pageSize = s; query.pageNum = 1; fetchData() }"
        />
      </div>
    </div>
  </div>
</template>