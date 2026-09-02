<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDeptTree, createDept, updateDept, deleteDept } from '@/api/dept'
import type { DeptNode } from '@/types/auth'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const tree = ref<DeptNode[]>([])

const dialogVisible = ref(false)
const editing = ref<DeptNode | null>(null)
const form = reactive<Partial<DeptNode>>({ parentId: 0, name: '', leader: '', phone: '', sort: 0, status: 1 })

async function fetchData() {
  loading.value = true
  try {
    tree.value = await listDeptTree()
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openCreate(parent?: DeptNode) {
  editing.value = null
  Object.assign(form, { parentId: parent?.id ?? 0, name: '', leader: '', phone: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

function openEdit(row: DeptNode) {
  editing.value = row
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function onSave() {
  if (editing.value) {
    await updateDept(editing.value.id, form)
    ElMessage.success('Updated')
  } else {
    await createDept(form)
    ElMessage.success('Created')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: DeptNode) {
  try {
    await ElMessageBox.confirm(`Delete ${row.name}?`, 'Confirm', { type: 'warning' })
  } catch { return }
  await deleteDept(row.id)
  ElMessage.success('Deleted')
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="Departments">
      <template #extra>
        <el-button type="primary" @click="openCreate()">Add Top-level</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-table v-loading="loading" :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="Name" width="240" />
        <el-table-column prop="leader" label="Leader" width="160" />
        <el-table-column prop="phone" label="Phone" width="160" />
        <el-table-column prop="sort" label="Sort" width="80" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Enabled' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Action" width="220" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">Add Child</el-button>
            <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="onDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? 'Edit Dept' : 'Add Dept'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Leader"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="Phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="Sort"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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