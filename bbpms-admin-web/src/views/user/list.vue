<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listRoles } from '@/api/role'
import { assignRoles } from '@/api/user'
import type { RoleInfo } from '@/types/auth'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const store = useUserStore()
const roles = ref<RoleInfo[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editing = ref<any>(null)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  deptId: '',
  status: 1,
  roleIds: [] as (number | string)[]
})

const roleDialogVisible = ref(false)
const roleTarget = ref<any>(null)
const roleIds = ref<(number | string)[]>([])

async function fetchRoles() {
  roles.value = await listRoles()
}

async function fetchData() {
  await store.fetchPage()
}

onMounted(async () => {
  await fetchRoles()
  await fetchData()
})

function onSearch() {
  store.query.keyword = form.username || ''
  store.query.pageNum = 1
  fetchData()
}

function onReset() {
  store.resetQuery()
  fetchData()
}

function openCreate() {
  dialogMode.value = 'create'
  editing.value = null
  Object.assign(form, {
    username: '', password: '', nickname: '', email: '', phone: '',
    deptId: '', status: 1, roleIds: []
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  dialogMode.value = 'edit'
  editing.value = row
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname || '',
    email: row.email || '',
    phone: row.phone || '',
    deptId: row.deptId || '',
    status: row.status ?? 1,
    roleIds: (row.roles || []).map((r: any) => r.id)
  })
  dialogVisible.value = true
}

async function onSave() {
  if (dialogMode.value === 'create') {
    await store.create(form as any)
    ElMessage.success('Created')
  } else {
    await store.update(editing.value.id, form)
    ElMessage.success('Updated')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: any) {
  try {
    await ElMessageBox.confirm(`Delete user ${row.username}?`, 'Confirm', { type: 'warning' })
  } catch { return }
  await store.remove(row.id)
  ElMessage.success('Deleted')
  fetchData()
}

function openAssignRoles(row: any) {
  roleTarget.value = row
  roleIds.value = (row.roles || []).map((r: any) => r.id)
  roleDialogVisible.value = true
}

async function onSaveRoles() {
  await assignRoles(roleTarget.value.id, roleIds.value)
  ElMessage.success('Roles updated')
  roleDialogVisible.value = false
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="Users">
      <template #extra>
        <el-button type="primary" @click="openCreate">Add User</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="store.query.keyword" placeholder="Username / Nickname" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="store.loading" :data="store.list" stripe>
        <el-table-column prop="username" label="Username" width="140" />
        <el-table-column prop="nickname" label="Nickname" width="140" />
        <el-table-column prop="phone" label="Phone" width="140" />
        <el-table-column prop="email" label="Email" />
        <el-table-column prop="deptName" label="Department" width="140" />
        <el-table-column label="Roles" width="220">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin-right: 4px">{{ r.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Enabled' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Created" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Action" width="220" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssignRoles(row)">Roles</el-button>
            <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="onDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="store.total"
          :current-page="store.query.pageNum"
          :page-size="store.query.pageSize"
          @current-change="(p) => { store.query.pageNum = p; fetchData() }"
          @size-change="(s) => { store.query.pageSize = s; store.query.pageNum = 1; fetchData() }"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? 'Add User' : 'Edit User'" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Username">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="Password" v-if="dialogMode === 'create'">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="Nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="Dept">
          <el-input v-model="form.deptId" />
        </el-form-item>
        <el-form-item label="Status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">Enabled</el-radio>
            <el-radio :value="0">Disabled</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Roles">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="onSave">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="`Assign Roles - ${roleTarget?.username || ''}`" width="480px">
      <el-select v-model="roleIds" multiple style="width: 100%">
        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="onSaveRoles">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>