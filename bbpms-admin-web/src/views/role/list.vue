<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRoles, createRole, updateRole, deleteRole, assignMenus, getRoleMenuIds } from '@/api/role'
import { listMenuTree } from '@/api/menu'
import type { RoleInfo, MenuNode } from '@/types/auth'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref<RoleInfo[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const dialogVisible = ref(false)
const editing = ref<RoleInfo | null>(null)
const formRef = ref()
const form = reactive<Partial<RoleInfo>>({ code: '', name: '', remark: '', status: 1 })

const rules = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { min: 2, max: 50, message: '角色编码需在 2~50 个字符之间', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_:]+$/, message: '角色编码仅支持字母/数字/冒号/下划线', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '角色名称需在 2~50 个字符之间', trigger: 'blur' }
  ],
  remark: [{ max: 200, message: '备注不能超过 200 个字符', trigger: 'blur' }]
}

const menuDialogVisible = ref(false)
const menuLoading = ref(false)
const menuSaving = ref(false)
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
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (editing.value) {
    await updateRole(editing.value.id, form)
    ElMessage.success('更新成功')
  } else {
    await createRole(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: RoleInfo) {
  try {
    await ElMessageBox.confirm(`确定删除角色 ${row.name} 吗？`, '确认', { type: 'warning' })
  } catch { return }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function openAssignMenus(row: RoleInfo) {
  menuTarget.value = row
  menuDialogVisible.value = true
  menuLoading.value = true
  menuTree.value = []
  checkedKeys.value = []
  try {
    const [tree, assigned] = await Promise.all([listMenuTree(), getRoleMenuIds(row.id)])
    menuTree.value = tree || []
    checkedKeys.value = assigned || []
    await nextTick()
    treeRef.value?.setCheckedKeys?.(checkedKeys.value)
  } catch (error: any) {
    ElMessage.error(`菜单加载失败：${error?.response?.data?.msg || error?.message || '请稍后重试'}`)
  } finally {
    menuLoading.value = false
  }
}

async function onSaveMenus() {
  if (!menuTarget.value) return
  const checked = treeRef.value?.getCheckedKeys?.() ?? checkedKeys.value
  const halfChecked = treeRef.value?.getHalfCheckedKeys?.() ?? []
  menuSaving.value = true
  try {
    await assignMenus(menuTarget.value.id, [...new Set([...checked, ...halfChecked])])
    ElMessage.success('菜单已更新')
    menuDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(`菜单更新失败：${error?.response?.data?.msg || error?.message || '请稍后重试'}`)
  } finally {
    menuSaving.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="角色管理">
      <template #extra>
        <el-button type="primary" @click="openCreate">新增角色</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.keyword" placeholder="名称 / 编码" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssignMenus(row)">菜单</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑角色' : '新增角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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

    <el-dialog v-model="menuDialogVisible" :title="`分配菜单 - ${menuTarget?.name || ''}`" width="520px">
      <el-tree
        ref="treeRef"
        v-loading="menuLoading"
        :data="menuTree"
        :props="{ children: 'children', label: 'name' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedKeys"
        @check="(_: any, info: any) => { checkedKeys = info.checkedKeys }"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSaving" :disabled="menuLoading" @click="onSaveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
