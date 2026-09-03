<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageMessages, sendSms } from '@/api/notify'
import type { NotifyMessage } from '@/api/notify'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref<NotifyMessage[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  channel: '',
  status: ''
})

const sendDialog = ref(false)
const sendForm = reactive({
  phone: '',
  templateCode: '',
  params: '{}'
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageMessages(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function onSearch() { query.pageNum = 1; fetchData() }
function onReset() { query.channel = ''; query.status = ''; query.pageNum = 1; fetchData() }

async function onSend() {
  if (!sendForm.phone || !sendForm.templateCode) {
    ElMessage.warning('Please enter a phone number and template code')
    return
  }
  let params: Record<string, any>
  try {
    params = JSON.parse(sendForm.params || '{}')
  } catch {
    ElMessage.warning('Template parameters must be valid JSON')
    return
  }
  const r = await sendSms({ phone: sendForm.phone, templateCode: sendForm.templateCode, params })
  ElMessage.success(`Message status: ${r.status}`)
  sendDialog.value = false
  sendForm.phone = ''
  sendForm.templateCode = ''
  sendForm.params = '{}'
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="Message Records">
      <template #extra>
        <el-button type="primary" @click="sendDialog = true">Send SMS</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-select v-model="query.channel" placeholder="Channel" clearable style="width: 140px">
            <el-option value="SMS" label="SMS" />
            <el-option value="WECHAT" label="WeChat" />
            <el-option value="APP_PUSH" label="Push" />
          </el-select>
          <el-select v-model="query.status" placeholder="Status" clearable style="width: 140px">
            <el-option value="PENDING" label="Pending" />
            <el-option value="SUCCESS" label="Success" />
            <el-option value="FAILED" label="Failed" />
          </el-select>
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="templateCode" label="Template" width="160" />
        <el-table-column prop="channel" label="Channel" width="100" />
        <el-table-column prop="receiver" label="Receiver" width="160" />
        <el-table-column prop="content" label="Content" show-overflow-tooltip />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Sent" width="170">
          <template #default="{ row }">{{ formatDate(row.sentAt) }}</template>
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

    <el-dialog v-model="sendDialog" title="Send SMS" width="520px">
      <el-form :model="sendForm" label-width="100px">
        <el-form-item label="Phone">
          <el-input v-model="sendForm.phone" placeholder="Mobile phone number" />
        </el-form-item>
        <el-form-item label="Template">
          <el-input v-model="sendForm.templateCode" placeholder="e.g. ORDER_CREATED" />
        </el-form-item>
        <el-form-item label="Parameters">
          <el-input v-model="sendForm.params" type="textarea" :rows="5" placeholder='{"name":"Alice"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialog = false">Cancel</el-button>
        <el-button type="primary" @click="onSend">Send</el-button>
      </template>
    </el-dialog>
  </div>
</template>
