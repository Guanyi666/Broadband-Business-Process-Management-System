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
    ElMessage.success('创建成功')
  } else {
    await store.update(editing.value.id, form)
    ElMessage.success('更新成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: any) {
  try {
    await ElMessageBox.confirm(`Delete user ${row.username}?`, '确认', { type: 'warning' })
  } catch { return }
  await store.remove(row.id)
  ElMessage.success('删除成功')
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
    <PageHeader title="用户管理">
      <template #extra>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="store.query.keyword" placeholder="用户名 / 昵称" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="store.loading" :data="store.list" stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="deptName" label="所属部门" width="140" />
        <el-table-column label="角色" width="220">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin-right: 4px">{{ r.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Enabled' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssignRoles(row)">Roles</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增用户' : '编辑用户'" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="Password" v-if="dialogMode === 'create'">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.deptId" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="`Assign Roles - ${roleTarget?.username || ''}`" width="480px">
      <el-select v-model="roleIds" multiple style="width: 100%">
        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>