<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import * as ElIcons from '@element-plus/icons-vue'
import type { MenuConfig } from '../AdminLayout.vue'

const router = useRouter()

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

/**
 * 点击一级模块：展开子菜单的同时进入该模块默认页。
 * 此前 el-sub-menu 标题只展开不跳转，用户需再点一次子项才切换模块（表现为「点两次」）。
 * 各模块路由均已配置 redirect（如 /order -> /order/list），push 父路径即可落到默认页。
 */
function onGroupClick() {
  const target = props.item.path
  if (!target || /^https?:\/\//.test(target)) return
  router.push(target).catch(() => {
    // 目标模块无对应路由时保持原样，不影响菜单展开/收起
  })
}
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
      <div class="sidebar-submenu-title" @click="onGroupClick">
        <el-icon v-if="item.icon">
          <component :is="(ElIcons as any)[item.icon] || ElIcons.Menu" />
        </el-icon>
        <span>{{ item.title }}</span>
      </div>
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
<style scoped lang="scss">
/* 一级模块标题：铺满整行保证点击热区；保留 el-sub-menu 原有的展开/收起行为 */
.sidebar-submenu-title {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
</style>
