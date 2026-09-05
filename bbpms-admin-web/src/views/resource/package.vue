<script setup lang="ts">
/**
 * 套餐资源管理（资源管理 → 套餐资源）
 * 列表展示套餐中/英文名称、带宽规格、状态；支持新增、编辑、删除/停用、按名称搜索。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  pagePackages, createPackage, updatePackage, deletePackage, togglePackageStatus,
  type PackageItem, type PackageSavePayload
} from '@/api/package'

const loading = ref(false)
const list = ref<PackageItem[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (query.status !== undefined) params.status = query.status
    const res = await pagePackages(params)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() { query.pageNum = 1; fetchData() }
function onReset() { query.keyword = ''; query.status = undefined; query.pageNum = 1; fetchData() }

/* ---------------- 新增 / 编辑 ---------------- */
const dialogVisible = ref(false)
const dialogTitle = ref('新增套餐')
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  code: '',
  name: '',
  nameEn: '',
  speedMbps: 100,
  monthlyFee: 99,
  description: '',
  status: 1,
  sort: 0
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入套餐编码', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: '套餐编码需以字母开头，仅支持字母/数字/下划线/连字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入套餐名称', trigger: 'blur' },
    { min: 2, max: 50, message: '套餐名称需在 2~50 个字符之间', trigger: 'blur' }
  ],
  speedMbps: [
    { required: true, message: '请输入带宽规格', trigger: 'blur' },
    {
      validator: (_: unknown, value: number, callback: (err?: Error) => void) => {
        const n = Number(value)
        if (!Number.isInteger(n) || n < 1 || n > 10000) {
          callback(new Error('带宽规格需在 1M - 10,000M 之间，请输入有效的整数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  monthlyFee: [
    { required: true, message: '请输入月租费', trigger: 'blur' },
    {
      validator: (_: unknown, value: number, callback: (err?: Error) => void) => {
        const n = Number(value)
        if (Number.isNaN(n) || n < 0) {
          callback(new Error('月租费不能为负数'))
        } else if (n > 100000) {
          callback(new Error('月租费不能超过 100,000 元'))
        } else if (String(value).split('.')[1]?.length > 2) {
          callback(new Error('月租费最多允许 2 位小数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  nameEn: [{ max: 128, message: '英文名称不能超过 128 个字符', trigger: 'blur' }],
  description: [{ max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增套餐'
  Object.assign(form, { id: undefined, code: '', name: '', nameEn: '', speedMbps: 100, monthlyFee: 99, description: '', status: 1, sort: 0 })
  dialogVisible.value = true
}

function openEdit(row: PackageItem) {
  dialogTitle.value = '编辑套餐'
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, nameEn: row.nameEn || '',
    speedMbps: row.speedMbps, monthlyFee: Number(row.monthlyFee), description: row.description || '',
    status: row.status, sort: row.sort
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload: PackageSavePayload = {
      code: form.code.trim(), name: form.name.trim(), nameEn: form.nameEn.trim(),
      speedMbps: Number(form.speedMbps), monthlyFee: Number(form.monthlyFee),
      description: form.description, status: form.status, sort: Number(form.sort || 0)
    }
    if (form.id) await updatePackage(form.id, payload)
    else await createPackage(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

/* ---------------- 删除 / 停用 ---------------- */
async function onDelete(row: PackageItem) {
  await ElMessageBox.confirm(`确认删除套餐「${row.name}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await deletePackage(row.id)
  ElMessage.success('已删除')
  fetchData()
}

async function onToggleStatus(row: PackageItem) {
  const next = row.status === 1 ? 0 : 1
  await togglePackageStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="page">
    <PageHeader title="套餐资源" description="管理可售宽带套餐：中英文名称、带宽规格、启用状态" />

    <div class="app-card package-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px; flex-wrap: wrap">
          <el-input v-model="query.keyword" placeholder="套餐名称 / 编码" clearable style="width: 220px" @keyup.enter="onSearch" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增套餐</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="code" label="套餐编码" width="130" />
        <el-table-column prop="name" label="套餐名称" min-width="140" />
        <el-table-column prop="nameEn" label="英文名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.nameEn || '-' }}</template>
        </el-table-column>
        <el-table-column label="带宽规格" width="110">
          <template #default="{ row }">{{ row.speedMbps }}M</template>
        </el-table-column>
        <el-table-column label="月租费" width="110">
          <template #default="{ row }">¥{{ Number(row.monthlyFee).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="onToggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
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
          @size-change="(s) => { query.pageSize = s; query.pageNum = 1; fetchData() }"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="套餐编码" prop="code">
          <el-input v-model="form.code" placeholder="如 PKG-200M" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" placeholder="如 200M 宽带" />
        </el-form-item>
        <el-form-item label="英文名称" prop="nameEn">
          <el-input v-model="form.nameEn" placeholder="如 200M Broadband（可选）" />
        </el-form-item>
        <el-form-item label="带宽规格" prop="speedMbps">
          <el-input-number v-model="form.speedMbps" :min="1" :max="10000" /> <span class="unit">Mbps</span>
        </el-form-item>
        <el-form-item label="月租费" prop="monthlyFee">
          <el-input-number v-model="form.monthlyFee" :min="0" :precision="2" :step="10" /> <span class="unit">元/月</span>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="套餐描述（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.package-card { padding-bottom: 4px; }
.page-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.unit { margin-left: 8px; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
