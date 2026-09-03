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

async function onMenuSelect(index: string) {
  if (route.path === index) return
  try {
    await router.push(index)
  } catch (error) {
    console.error('Navigation failed', error)
    ElMessage.error('Page could not be opened. Please retry.')
  }
}

function goProfile() {
  router.push('/profile')
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('Are you sure to logout?', 'Confirm', { type: 'warning' })
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
          v-for="m in staticMenus"
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
          <el-tooltip :content="isFullscreen ? 'Exit fullscreen' : 'Fullscreen'">
            <el-icon class="topbar-icon" @click="toggleFs">
              <component :is="isFullscreen ? Aim : FullScreen" />
            </el-icon>
          </el-tooltip>
          <el-tooltip content="Notifications">
            <el-badge :value="3" class="topbar-icon-badge">
              <el-icon class="topbar-icon"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          <el-dropdown>
            <div class="user-area">
              <el-avatar :size="32" class="user-avatar">{{ userInitial }}</el-avatar>
              <span class="username">{{ auth.userInfo?.nickname || auth.userInfo?.username || 'User' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">Profile</el-dropdown-item>
                <el-dropdown-item divided @click="onLogout">Logout</el-dropdown-item>
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
}

export const staticMenus: MenuConfig[] = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine' },
  {
    path: '/customer',
    title: '客户管理',
    icon: 'User',
    children: [{ path: '/customer/list', title: '客户列表', icon: 'List' }]
  },
  {
    path: '/order',
    title: '订单管理',
    icon: 'Document',
    children: [
      { path: '/order/list', title: '订单列表', icon: 'List' },
      { path: '/order/create', title: '创建订单', icon: 'Plus' }
    ]
  },
  {
    path: '/workorder',
    title: '工单管理',
    icon: 'Tools',
    children: [
      { path: '/workorder/list', title: '工单列表', icon: 'List' },
      { path: '/workorder/dispatch-board', title: '派单工作台', icon: 'Connection' }
    ]
  },
  {
    path: '/installer',
    title: '装维管理',
    icon: 'Avatar',
    children: [
      { path: '/installer/list', title: '装维列表', icon: 'List' },
      { path: '/installer/map', title: '装维地图', icon: 'MapLocation' }
    ]
  },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/system/user', title: '用户管理', icon: 'UserFilled' },
      { path: '/system/role', title: '角色管理', icon: 'Avatar' },
      { path: '/system/menu', title: '菜单管理', icon: 'Menu' },
      { path: '/system/dept', title: '部门管理', icon: 'OfficeBuilding' }
    ]
  },
  {
    path: '/notify',
    title: '通知管理',
    icon: 'Message',
    children: [
      { path: '/notify/template', title: '消息模板', icon: 'Memo' },
      { path: '/notify/record', title: '消息记录', icon: 'ChatLineRound' }
    ]
  },
  { path: '/file', title: '文件管理', icon: 'Folder' },
  {
    path: '/log',
    title: '日志管理',
    icon: 'Document',
    children: [
      { path: '/log/operation', title: '操作日志', icon: 'Tickets' },
      { path: '/log/login', title: '登录日志', icon: 'Key' }
    ]
  },
  {
    path: '/monitor',
    title: '系统监控',
    icon: 'Monitor',
    children: [
      { path: '/monitor/metrics', title: '监控指标', icon: 'TrendCharts' },
      { path: '/monitor/health', title: '健康检查', icon: 'FirstAidKit' }
    ]
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
