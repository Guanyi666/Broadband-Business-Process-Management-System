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
    ElMessage.success('更新成功')
  } else {
    await createMenu(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function onDelete(row: MenuNode) {
  try {
    await ElMessageBox.confirm(`确定删除菜单 ${row.name} 吗？`, '确认', { type: 'warning' })
  } catch { return }
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <PageHeader title="菜单管理">
      <template #extra>
        <el-button type="primary" @click="openCreate()">Add Top-level</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-table v-loading="loading" :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="名称" width="240" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'info' : row.type === 2 ? 'primary' : 'success'">
              {{ ['Dir', 'Menu', 'Button'][row.type - 1] || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" />
        <el-table-column prop="component" label="组件路径" />
        <el-table-column prop="icon" label="图标" width="120" />
        <el-table-column prop="perm" label="权限标识" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="是否可见" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visible !== false ? 'success' : 'info'">
              {{ row.visible !== false ? 'Yes' : 'No' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑菜单' : '新增菜单'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="路由地址"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="组件路径"><el-input v-model="form.component" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perm" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="是否可见">
          <el-switch v-model="form.visible" />
        </el-form-item>
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