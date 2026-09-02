<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import * as ElIcons from '@element-plus/icons-vue'
import type { MenuConfig } from '../AdminLayout.vue'

const props = defineProps<{
  item: MenuConfig
  basePath?: string
}>()

const route = useRoute()

const hasChildren = computed(() => Array.isArray(props.item.children) && props.item.children.length > 0)

const onlyChild = computed(() => {
  if (!hasChildren.value) return props.item
  if (props.item.children!.length === 1) return props.item.children![0]
  return null
})

const resolvePath = (childPath?: string) => {
  if (!childPath) return props.basePath || props.item.path
  if (/^https?:\/\//.test(childPath)) return childPath
  if (childPath.startsWith('/')) return childPath
  return `${props.basePath || props.item.path}/${childPath}`
}

const activePath = computed(() => {
  if (!hasChildren.value) return resolvePath()
  return resolvePath(onlyChild.value?.path)
})

const isActive = computed(() => {
  if (route.path === activePath.value) return true
  return route.path.startsWith(props.item.path + '/')
})
</script>

<template>
  <template v-if="!hasChildren">
    <el-menu-item :index="activePath">
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