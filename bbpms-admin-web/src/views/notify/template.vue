<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageTemplates, createTemplate, updateTemplate, deleteTemplate } from '@/api/notify'
import type { NotifyTemplate } from '@/api/notify'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref<NotifyTemplate[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const dialogVisible = ref(false)
const editing = ref<NotifyTemplate | null>(null)
const form = reactive<Partial<NotifyTemplate>>({
  code: '', channel: 'SMS', subject: '', content: '', status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await pageTemplates(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function onSearch() { query.pageNum = 1; fetchData() }
function onReset() { query.keyword = ''; query.pageNum = 1; fetchData() }

function openCreate() {
  editing.value = null
  Object.assign(form, { code: '', channel: 'SMS', subject: '', content: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: NotifyTemplate) {
  editing.value = row
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function onSave() {
  if (editing.value) {
    await updateTemplate(editing.value.id, form)
    ElMessage.success('Updated')
  } else {
    await createTemplate(form)
    ElMessage.success('Created')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: NotifyTemplate) {
  try {
    await ElMessageBox.confirm(`Delete template ${row.code}?`, 'Confirm', { type: 'warning' })
  } catch { return }
  await deleteTemplate(row.id)
  ElMessage.success('Deleted')
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="Notify Templates">
      <template #extra>
        <el-button type="primary" @click="openCreate">Add Template</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.keyword" placeholder="Code / Subject / Content" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="code" label="Code" width="160" />
        <el-table-column prop="channel" label="Channel" width="100" />
        <el-table-column prop="subject" label="Subject" width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="Content" show-overflow-tooltip />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Enabled' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Updated" width="170">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="Action" width="180" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="onDelete(row)">Delete</el-button>
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
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? 'Edit Template' : 'Add Template'" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="Channel">
          <el-select v-model="form.channel">
            <el-option value="SMS" label="SMS" />
            <el-option value="WECHAT" label="WeChat" />
            <el-option value="INAPP" label="In-app" />
          </el-select>
        </el-form-item>
        <el-form-item label="Subject"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="Content"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="Status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">Enabled</el-radio>
            <el-radio :value="0">Disabled</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="onSave">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>
