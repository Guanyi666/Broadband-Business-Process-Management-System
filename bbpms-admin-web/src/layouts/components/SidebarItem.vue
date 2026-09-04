<script setup lang="ts">
import { computed } from 'vue'
import * as ElIcons from '@element-plus/icons-vue'
import type { MenuConfig } from '../AdminLayout.vue'

const props = defineProps<{
  item: MenuConfig
  basePath?: string
}>()

const hasChildren = computed(() => Array.isArray(props.item.children) && props.item.children.length > 0)

const resolvePath = (childPath?: string) => {
  if (!childPath) return props.basePath || props.item.path
  if (/^https?:\/\//.test(childPath)) return childPath
  if (childPath.startsWith('/')) return childPath
  return `${props.basePath || props.item.path}/${childPath}`
}

/**
 * 当前叶子项自身的完整路径。
 * 关键修复：叶子项必须用「自己的 path」解析，而不是 resolvePath()（无参会回退到父分组 basePath，
 * 导致 el-menu select 的 index = 父分组路径，点击子菜单实际跳转父分组 redirect（如 创建订单→/order/list））。
 */
const selfPath = computed(() => resolvePath(props.item.path))
</script>

<template>
  <template v-if="!hasChildren">
    <el-menu-item :index="selfPath">
      <el-icon v-if="item.icon">
        <component :is="(ElIcons as any)[item.icon] || ElIcons.Document" />
      </el-icon>
      <template #title>{{ item.title }}</template>
    </el-menu-item>
  </template>

  <el-sub-menu v-else :index="item.path">
    <template #title>
      <el-icon v-if="item.icon">
        <component :is="(ElIcons as any)[item.icon] || ElIcons.Menu" />
      </el-icon>
      <span>{{ item.title }}</span>
    </template>
    <SidebarItem
      v-for="c in item.children"
      :key="c.path"
      :item="c"
      :base-path="item.path"
    />
  </el-sub-menu>
</template>

<script lang="ts">
export default { name: 'SidebarItem' }
</script>