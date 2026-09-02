<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { pageInstallers } from '@/api/installer'
import type { InstallerProfile } from '@/types/installer'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const router = useRouter()
const loading = ref(false)
const list = ref<InstallerProfile[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageInstallers({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onRowClick(row: InstallerProfile) {
  router.push(`/installer/profile/${row.id}`)
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="Installers">
      <template #extra>
        <el-button @click="router.push('/installer/map')">
          <el-icon><MapLocation /></el-icon> Map View
        </el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-table v-loading="loading" :data="list" stripe @row-click="onRowClick">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="Name" />
        <el-table-column prop="username" label="Username" width="120" />
        <el-table-column prop="phone" label="Phone" width="130" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }"><BBPMSStatusTag :status="row.status" :label="row.status" /></template>
        </el-table-column>
        <el-table-column prop="workload" label="Workload" width="100" />
        <el-table-column prop="rating" label="Rating" width="100" />
        <el-table-column label="Last Active" width="170">
          <template #default="{ row }">{{ formatDate(row.lastActiveAt) }}</template>
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