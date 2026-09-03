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
    ElMessage.success('更新成功')
  } else {
    await createDept(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: DeptNode) {
  try {
    await ElMessageBox.confirm(`确定删除 ${row.name} 吗？`, '确认', { type: 'warning' })
  } catch { return }
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="部门管理">
      <template #extra>
        <el-button type="primary" @click="openCreate()">新增顶级部门</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-table v-loading="loading" :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="名称" width="240" />
        <el-table-column prop="leader" label="负责人" width="160" />
        <el-table-column prop="phone" label="手机号" width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑部门' : '新增部门'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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