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

/** 渠道码 → 中文（下拉筛选值保持原始码，仅展示处映射） */
const CHANNEL_TEXT: Record<string, string> = {
  SMS: '短信',
  WECHAT: '微信',
  INAPP: '站内信'
}
function channelText(code?: string) {
  return (code && CHANNEL_TEXT[code]) || code || '-'
}

/** 发送状态 → 中文标签 */
const STATUS_TEXT: Record<string, string> = {
  PENDING: '待发送',
  SUCCESS: '成功',
  FAILED: '失败'
}
function statusText(code?: string) {
  return (code && STATUS_TEXT[code]) || code || '-'
}

async function onSend() {
  if (!sendForm.phone || !sendForm.templateCode) {
    ElMessage.warning('请输入手机号和模板编码')
    return
  }
  let params: Record<string, any>
  try {
    params = JSON.parse(sendForm.params || '{}')
  } catch {
    ElMessage.warning('变量参数必须是合法的 JSON 格式')
    return
  }
  const r = await sendSms({ phone: sendForm.phone, templateCode: sendForm.templateCode, params })
  ElMessage.success(`短信已发送（状态：${r.status}）`)
  sendDialog.value = false
  sendForm.phone = ''
  sendForm.templateCode = ''
  sendForm.params = '{}'
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="消息记录">
      <template #extra>
        <el-button type="primary" @click="sendDialog = true">发送短信</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-select v-model="query.channel" placeholder="渠道" clearable style="width: 140px">
            <el-option value="SMS" label="短信" />
            <el-option value="WECHAT" label="微信" />
            <el-option value="APP_PUSH" label="推送" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
            <el-option value="PENDING" label="待处理" />
            <el-option value="SUCCESS" label="成功" />
            <el-option value="FAILED" label="失败" />
          </el-select>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="templateCode" label="模板" width="160" />
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">{{ channelText(row.channel) }}</template>
        </el-table-column>
        <el-table-column prop="receiver" label="接收人" width="160" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发送时间" width="170">
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

    <el-dialog v-model="sendDialog" title="发送短信" width="520px">
      <el-form :model="sendForm" label-width="100px">
        <el-form-item label="手机号">
          <el-input v-model="sendForm.phone" placeholder="请输入手机号（逗号分隔）" />
        </el-form-item>
        <el-form-item label="模板">
          <el-input v-model="sendForm.templateCode" placeholder="如 ORDER_CREATED" />
        </el-form-item>
        <el-form-item label="变量参数">
          <el-input v-model="sendForm.params" type="textarea" :rows="5" placeholder='{"name":"Alice"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialog = false">取消</el-button>
        <el-button type="primary" @click="onSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>
