<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { pageLoginLogs } from '@/api/log'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  status: '',
  dateRange: [] as string[]
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: query.pageNum, pageSize: query.pageSize, username: query.username, status: query.status }
    if (query.dateRange?.length === 2) {
      params.startTime = `${query.dateRange[0]} 00:00:00`
      params.endTime = `${query.dateRange[1]} 23:59:59`
    }
    const res = await pageLoginLogs(params)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function onSearch() { query.pageNum = 1; fetchData() }
function onReset() {
  query.username = ''; query.status = ''; query.dateRange = []; query.pageNum = 1; fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="登录日志" />

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px; flex-wrap: wrap">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width: 180px" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
            <el-option value="SUCCESS" label="成功" />
            <el-option value="FAILED" label="失败" />
          </el-select>
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="ip" label="IP" width="160" />
        <el-table-column prop="location" label="Location" width="140" />
        <el-table-column prop="terminal" label="Terminal" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
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
        />
      </div>
    </div>
  </div>
</template>
