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
  if (!form.code?.trim()) {
    ElMessage.error('请输入模板编码')
    return
  }
  if (!form.subject?.trim()) {
    ElMessage.error('请输入模板主题')
    return
  }
  if (!form.content?.trim()) {
    ElMessage.error('请输入模板内容')
    return
  }
  if (editing.value) {
    await updateTemplate(editing.value.id, form)
    ElMessage.success('更新成功')
  } else {
    await createTemplate(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: NotifyTemplate) {
  try {
    await ElMessageBox.confirm(`Delete template ${row.code}?`, '确认', { type: 'warning' })
  } catch { return }
  await deleteTemplate(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="通知模板">
      <template #extra>
        <el-button type="primary" @click="openCreate">新增模板</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.keyword" placeholder="Code / Subject / Content" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="channel" label="渠道" width="100" />
        <el-table-column prop="subject" label="主题" width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑模板' : '新增模板'" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="form.channel">
            <el-option value="SMS" label="短信" />
            <el-option value="WECHAT" label="微信" />
            <el-option value="INAPP" label="In-app" />
          </el-select>
        </el-form-item>
        <el-form-item label="主题"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
