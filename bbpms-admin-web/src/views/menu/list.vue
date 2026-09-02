<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import type { MenuNode } from '@/types/auth'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const tree = ref<MenuNode[]>([])

const dialogVisible = ref(false)
const editing = ref<MenuNode | null>(null)
const form = reactive<Partial<MenuNode>>({
  parentId: 0,
  name: '',
  path: '',
  component: '',
  icon: '',
  type: 1,
  perm: '',
  sort: 0,
  visible: true,
  status: 1
})

async function fetchData() {
  loading.value = true
  try {
    tree.value = await listMenuTree()
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openCreate(parent?: MenuNode) {
  editing.value = null
  Object.assign(form, {
    parentId: parent?.id ?? 0,
    name: '', path: '', component: '', icon: '',
    type: 2, perm: '', sort: 0, visible: true, status: 1
  })
  dialogVisible.value = true
}

function openEdit(row: MenuNode) {
  editing.value = row
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function onSave() {
  if (editing.value) {
    await updateMenu(editing.value.id, form)
    ElMessage.success('Updated')
  } else {
    await createMenu(form)
    ElMessage.success('Created')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: MenuNode) {
  try {
    await ElMessageBox.confirm(`Delete menu ${row.name}?`, 'Confirm', { type: 'warning' })
  } catch { return }
  await deleteMenu(row.id)
  ElMessage.success('Deleted')
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="Menus">
      <template #extra>
        <el-button type="primary" @click="openCreate()">Add Top-level</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-table v-loading="loading" :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="Name" width="240" />
        <el-table-column label="Type" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'info' : row.type === 2 ? 'primary' : 'success'">
              {{ ['Dir', 'Menu', 'Button'][row.type - 1] || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="Path" />
        <el-table-column prop="component" label="Component" />
        <el-table-column prop="icon" label="Icon" width="120" />
        <el-table-column prop="perm" label="Permission" />
        <el-table-column prop="sort" label="Sort" width="80" />
        <el-table-column label="Visible" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visible !== false ? 'success' : 'info'">
              {{ row.visible !== false ? 'Yes' : 'No' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Action" width="240" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">Add Child</el-button>
            <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="onDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? 'Edit Menu' : 'Add Menu'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">Dir</el-radio>
            <el-radio :value="2">Menu</el-radio>
            <el-radio :value="3">Button</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Path"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="Component"><el-input v-model="form.component" /></el-form-item>
        <el-form-item label="Icon"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="Permission"><el-input v-model="form.perm" /></el-form-item>
        <el-form-item label="Sort"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="Visible">
          <el-switch v-model="form.visible" />
        </el-form-item>
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