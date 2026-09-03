<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { useFullscreen } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import SidebarItem from './components/SidebarItem.vue'
import {
  Fold,
  Expand,
  FullScreen,
  Aim,
  Bell
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const auth = useAuthStore()

const { isFullscreen, toggle: toggleFs } = useFullscreen()

const breadcrumbs = computed(() =>
  route.matched.filter((m) => m.meta?.title).map((m) => ({
    title: m.meta.title as string,
    path: m.path
  }))
)

const activeMenu = computed(() => route.path)

/**
 * 权限过滤后的可见菜单（P0-1）
 * - 节点标注 perms/roles 与后端 sys_menu.perms、角色一致；
 * - 叶子节点需通过 auth.hasPermission / 角色校验；
 * - 分组节点仅在至少存在一个可见子节点时展示。
 */
function menuVisible(item: MenuConfig): boolean {
  if (item.roles && item.roles.length) {
    if (!item.roles.some((r) => auth.roles.includes(r))) return false
  }
  if (item.perms) {
    const list = Array.isArray(item.perms) ? item.perms : [item.perms]
    if (!list.some((p) => auth.hasPermission(p))) return false
  }
  return true
}

function filterMenus(items: MenuConfig[]): MenuConfig[] {
  const out: MenuConfig[] = []
  for (const item of items) {
    if (item.children && item.children.length) {
      const children = filterMenus(item.children)
      if (children.length && menuVisible(item)) {
        out.push({ ...item, children })
      }
    } else if (menuVisible(item)) {
      out.push(item)
    }
  }
  return out
}

const visibleMenus = computed(() => filterMenus(allMenus))

async function onMenuSelect(index: string) {
  if (route.path === index) return
  try {
    await router.push(index)
  } catch (error) {
    console.error('Navigation failed', error)
    ElMessage.error('页面打开失败，请重试')
  }
}

function goProfile() {
  router.push('/profile')
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await auth.logout()
  router.replace('/login')
}

const visitedViews = computed(() => app.visitedViews)

watch(
  () => route.path,
  () => {
    if (route.meta?.title) {
      app.addVisitedView({
        path: route.path,
        fullPath: route.fullPath,
        name: route.name as string,
        title: route.meta.title as string,
        affix: !!route.meta.affix
      })
    }
  },
  { immediate: true }
)

function closeTag(v: { path: string }) {
  app.removeVisitedView(v.path)
  if (route.path === v.path) {
    const last = visitedViews.value[visitedViews.value.length - 1]
    router.push(last?.path || '/dashboard')
  }
}

const userInitial = computed(() => {
  const u = auth.userInfo?.nickname || auth.userInfo?.username || 'U'
  return u.charAt(0).toUpperCase()
})
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="app.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!app.sidebarCollapsed">BBPMS</span>
        <span v-else>B</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="app.sidebarCollapsed"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        @select="onMenuSelect"
      >
        <SidebarItem
          v-for="m in visibleMenus"
          :key="m.path"
          :item="m"
          :base-path="m.path"
        />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-icon class="collapse-btn" @click="app.toggleSidebar">
            <component :is="app.sidebarCollapsed ? Expand : Fold" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="b in breadcrumbs" :key="b.path">
              {{ b.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'">
            <el-icon class="topbar-icon" @click="toggleFs">
              <component :is="isFullscreen ? Aim : FullScreen" />
            </el-icon>
          </el-tooltip>
          <el-tooltip content="通知">
            <el-badge :value="3" class="topbar-icon-badge">
              <el-icon class="topbar-icon"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          <el-dropdown>
            <div class="user-area">
              <el-avatar :size="32" class="user-avatar">{{ userInitial }}</el-avatar>
              <span class="username">{{ auth.userInfo?.nickname || auth.userInfo?.username || '用户' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <div class="tags-view">
        <el-tag
          v-for="v in visitedViews"
          :key="v.path"
          :closable="!v.affix"
          :type="v.path === route.path ? 'primary' : 'info'"
          effect="light"
          @click="router.push(v.path)"
          @close="closeTag(v)"
        >
          {{ v.title }}
        </el-tag>
      </div>

      <el-main class="main-content">
        <router-view v-slot="{ Component, route: r }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive>
              <component :is="Component" :key="r.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script lang="ts">
export interface MenuConfig {
  path: string
  title: string
  icon?: string
  children?: MenuConfig[]
  hidden?: boolean
  /** 所需权限码（与后端 sys_menu.perms 对齐；支持数组=任一即可） */
  perms?: string | string[]
  /** 所需角色码（支持数组=任一即可） */
  roles?: string[]
}

/** 全量菜单（带权限标注，展示前经 filterMenus 过滤） */
export const allMenus: MenuConfig[] = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine', perms: 'dashboard:view' },
  {
    path: '/customer',
    title: '客户管理',
    icon: 'User',
    perms: 'customer:view',
    children: [{ path: '/customer/list', title: '客户列表', icon: 'List', perms: 'customer:view' }]
  },
  {
    path: '/order',
    title: '订单管理',
    icon: 'Document',
    perms: 'order:view',
    children: [
      { path: '/order/list', title: '订单列表', icon: 'List', perms: 'order:view' },
      { path: '/order/create', title: '创建订单', icon: 'Plus', perms: 'order:create' }
    ]
  },
  {
    path: '/workorder',
    title: '工单管理',
    icon: 'Tools',
    perms: 'workorder:view',
    children: [
      { path: '/workorder/list', title: '工单列表', icon: 'List', perms: 'workorder:view' },
      { path: '/workorder/dispatch-board', title: '派单工作台', icon: 'Connection', perms: 'dispatch:manual' }
    ]
  },
  {
    path: '/installer',
    title: '装维管理',
    icon: 'Avatar',
    perms: 'installer:view',
    children: [
      { path: '/installer/list', title: '装维列表', icon: 'List', perms: 'installer:view' },
      { path: '/installer/map', title: '装维地图', icon: 'MapLocation', perms: 'installer:view' }
    ]
  },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Setting',
    roles: ['SUPER_ADMIN'],
    children: [
      { path: '/system/user', title: '用户管理', icon: 'UserFilled', perms: 'system:user:view' },
      { path: '/system/role', title: '角色管理', icon: 'Avatar', perms: 'system:role:view' },
      { path: '/system/menu', title: '菜单管理', icon: 'Menu', perms: 'system:menu:view' },
      { path: '/system/dept', title: '部门管理', icon: 'OfficeBuilding', perms: 'system:dept:view' }
    ]
  },
  {
    path: '/notify',
    title: '通知管理',
    icon: 'Message',
    perms: 'notify:view',
    children: [
      { path: '/notify/template', title: '消息模板', icon: 'Memo', perms: 'notify:template:view' },
      { path: '/notify/record', title: '消息记录', icon: 'ChatLineRound', perms: 'notify:record:view' }
    ]
  },
  { path: '/file', title: '文件管理', icon: 'Folder', perms: 'file:view' },
  {
    path: '/log',
    title: '日志管理',
    icon: 'Document',
    perms: 'log:view',
    roles: ['SUPER_ADMIN', 'AUDITOR'],
    children: [
      { path: '/log/operation', title: '操作日志', icon: 'Tickets', perms: 'log:view' },
      { path: '/log/login', title: '登录日志', icon: 'Key', perms: 'log:view' }
    ]
  },
  {
    path: '/attendance',
    title: '考勤管理',
    icon: 'Clock',
    perms: 'attendance:view',
    children: [{ path: '/attendance/team', title: '团队报表', icon: 'DataAnalysis', perms: 'attendance:view-all' }]
  },
  {
    path: '/leave',
    title: '请假管理',
    icon: 'TodoList',
    perms: 'leave:view',
    children: [{ path: '/leave/approvals', title: '请假审批', icon: 'Check', perms: 'leave:approve' }]
  },
  {
    path: '/sla',
    title: 'SLA 监控',
    icon: 'Warning',
    perms: 'workorder:sla:view',
    children: [{ path: '/sla/expiring', title: '工单时效', icon: 'AlarmClock', perms: 'workorder:sla:view' }]
  }
]
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100%;
}

.sidebar {
  background: $sidebar-bg;
  transition: width 0.25s;
  overflow-x: hidden;
  .logo {
    height: $header-height;
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    letter-spacing: 1px;
    background: rgba(255, 255, 255, 0.04);
  }
  :deep(.el-menu) {
    border-right: 0;
  }
}

.topbar {
  height: $header-height;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  .topbar-left {
    display: flex;
    align-items: center;
    gap: 16px;
    .collapse-btn {
      cursor: pointer;
      font-size: 20px;
    }
  }
  .topbar-right {
    display: flex;
    align-items: center;
    gap: 16px;
    .topbar-icon {
      font-size: 18px;
      cursor: pointer;
    }
    .user-area {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      .username {
        font-size: 14px;
      }
    }
  }
}

.tags-view {
  height: $tags-view-height;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 4px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  white-space: nowrap;
}

.main-content {
  background: $content-bg;
  padding: 16px;
  overflow: auto;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
