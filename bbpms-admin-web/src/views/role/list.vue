<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRoles, createRole, updateRole, deleteRole, assignMenus } from '@/api/role'
import { listMenuTree } from '@/api/menu'
import type { RoleInfo, MenuNode } from '@/types/auth'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref<RoleInfo[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const dialogVisible = ref(false)
const editing = ref<RoleInfo | null>(null)
const form = reactive<Partial<RoleInfo>>({ code: '', name: '', remark: '', status: 1 })

const menuDialogVisible = ref(false)
const menuTarget = ref<RoleInfo | null>(null)
const menuTree = ref<MenuNode[]>([])
const checkedKeys = ref<any[]>([])
const treeRef = ref<any>()

async function fetchData() {
  loading.value = true
  try {
    const res = await pageRoles(query)
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
  Object.assign(form, { code: '', name: '', remark: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: RoleInfo) {
  editing.value = row
  Object.assign(form, { code: row.code, name: row.name, remark: row.remark || '', status: row.status ?? 1 })
  dialogVisible.value = true
}

async function onSave() {
  if (editing.value) {
    await updateRole(editing.value.id, form)
    ElMessage.success('Updated')
  } else {
    await createRole(form)
    ElMessage.success('Created')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: RoleInfo) {
  try {
    await ElMessageBox.confirm(`Delete role ${row.name}?`, 'Confirm', { type: 'warning' })
  } catch { return }
  await deleteRole(row.id)
  ElMessage.success('Deleted')
  fetchData()
}

async function openAssignMenus(row: RoleInfo) {
  menuTarget.value = row
  menuTree.value = await listMenuTree()
  checkedKeys.value = row.menus || []
  menuDialogVisible.value = true
}

async function onSaveMenus() {
  if (!menuTarget.value) return
  const checked = treeRef.value?.getCheckedKeys?.() ?? checkedKeys.value
  const halfChecked = treeRef.value?.getHalfCheckedKeys?.() ?? []
  await assignMenus(menuTarget.value.id, [...new Set([...checked, ...halfChecked])])
  ElMessage.success('Menus updated')
  menuDialogVisible.value = false
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="Roles">
      <template #extra>
        <el-button type="primary" @click="openCreate">Add Role</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.keyword" placeholder="Name / Code" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="code" label="Code" width="160" />
        <el-table-column prop="name" label="Name" width="160" />
        <el-table-column prop="remark" label="Remark" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Enabled' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Action" width="240" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssignMenus(row)">Menus</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing ? 'Edit Role' : 'Add Role'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Remark"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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

    <el-dialog v-model="menuDialogVisible" :title="`Assign Menus - ${menuTarget?.name || ''}`" width="520px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        :props="{ children: 'children', label: 'name' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedKeys"
        @check="(_: any, info: any) => { checkedKeys = info.checkedKeys }"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="onSaveMenus">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>
