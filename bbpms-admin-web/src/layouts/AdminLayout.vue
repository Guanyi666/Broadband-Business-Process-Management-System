<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { unreadCount as fetchUnreadCount } from '@/api/notify'
import { useFullscreen } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import SidebarItem from './components/SidebarItem.vue'
import type { MenuNode } from '@/types/auth'
import type { TagItem } from '@/stores/app'
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

// =============================================================================
// 动态菜单（Phase 3：后端按角色下发菜单，前端决定文案/图标）
// -----------------------------------------------------------------------------
// 后端 /api/auth/menus 返回该用户可访问的 sys_menu 树（type1目录/type2菜单/type3按钮）。
// sys_menu.path 与前端 allMenus.path 一一对应，但 name 为英文、icon 为旧体系，
// 因此「后端节点集合」决定显示哪些菜单，「前端 allMenus 元数据」决定显示什么。
// 双层一致性校验：前端元数据仍必须通过 perms/roles 校验（后端不可用时兜底）。
// 后端不可用 / 未返回菜单时，回退到本地的 perms/roles 静态过滤，功能不降级。
// =============================================================================

/**
 * 从后端菜单树中收集「已授权 path」集合。
 * - 目录 path 为绝对（'/order'），叶子 path 相对（'list'/'create'/…）→ 拼接父路径规范化
 * - 'detail/:id' 等带参数的隐藏详情页不参与菜单匹配，直接过滤
 */
function collectBackendPaths(nodes: MenuNode[] | null | undefined, parentPath = '', acc: Set<string> = new Set()): Set<string> {
  if (!nodes || !nodes.length) return acc
  for (const n of nodes) {
    if (n.path) {
      // type 1=目录(绝对 path) 2=菜单(通常相对) —— 统一规范化
      const p = n.path.startsWith('/') ? n.path : (parentPath ? `${parentPath.replace(/\/$/, '')}/${n.path}` : n.path)
      // 排除带 ':param' 的详情路径（不参与菜单匹配）
      if (!p.includes(':')) acc.add(p)
      collectBackendPaths(n.children, p, acc)
    } else {
      // 无 path 的按钮级节点（type=3）只贡献 perms，不贡献 path
      collectBackendPaths(n.children, parentPath, acc)
    }
  }
  return acc
}

/** 节点自身权限校验（perms / roles 二者任一命中即通过） */
function nodeAllowed(item: MenuConfig): boolean {
  if (item.roles && item.roles.length) {
    if (!item.roles.some((r) => auth.roles.includes(r))) return false
    return true // 角色命中即放行，无需再查 perms
  }
  if (item.perms) {
    const list = Array.isArray(item.perms) ? item.perms : [item.perms]
    return list.some((p) => auth.hasPermission(p))
  }
  return true
}

/** 后端已授权 path 过滤前端元数据（递归） */
function filterByBackend(items: MenuConfig[], backendPaths: Set<string>, backendPerms: Set<string>): MenuConfig[] {
  const out: MenuConfig[] = []
  for (const item of items) {
    const childOk = item.children && item.children.length
      ? filterByBackend(item.children, backendPaths, backendPerms)
      : undefined

    // 命中后端授权（path 或 perm 任一命中，兼容 button 级 perm）
    const hitBackend =
      backendPaths.has(item.path) ||
      backendPerms.has(String(item.perms || '')) ||
      (Array.isArray(item.perms) && item.perms.some((p) => backendPerms.has(p)))

    if (childOk && childOk.length) {
      // 目录：有可见子节点即展示（目录自身无需命中后端）
      out.push({ ...item, children: childOk })
    } else if (childOk === undefined) {
      // 叶子：必须命中后端授权
      if (hitBackend) out.push(item)
    }
  }
  return out
}

/** 静态权限过滤（后端菜单不可用时的兜底） */
function filterByLocal(items: MenuConfig[]): MenuConfig[] {
  const out: MenuConfig[] = []
  for (const item of items) {
    if (item.children && item.children.length) {
      const children = filterByLocal(item.children)
      if (children.length && nodeAllowed(item)) out.push({ ...item, children })
    } else if (nodeAllowed(item)) {
      out.push(item)
    }
  }
  return out
}

const menusReady = ref(false)
const unreadCount = ref(0)

const dynamicMenus = computed(() => {
  const backendPaths = collectBackendPaths(auth.menus as MenuNode[])
  if (!backendPaths.size) return filterByLocal(allMenus)
  const backendPerms = new Set<string>(auth.permissions)
  const fromBackend = filterByBackend(allMenus, backendPaths, backendPerms)
  return fromBackend.length ? fromBackend : filterByLocal(allMenus)
})

// 双保险：后端空结果时也用本地过滤推导一次（避免误判）
const visibleMenus = computed(() => {
  const backendPaths = collectBackendPaths(auth.menus as MenuNode[])
  if (backendPaths.size) return dynamicMenus.value
  return filterByLocal(allMenus)
})

async function loadMenus() {
  try {
    await auth.fetchMenus()
  } catch (e) {
    // 后端菜单接口失败不阻断进入系统：回退到本地静态权限过滤
    console.warn('[AdminLayout] fetchMenus failed, fallback to local permission filter', e)
  } finally {
    menusReady.value = true
  }
}

let unreadTimer: ReturnType<typeof setInterval> | null = null

async function loadUnread() {
  if (!auth.isLoggedIn) return
  try {
    unreadCount.value = await fetchUnreadCount()
  } catch (e) {
    // 未读接口失败不打扰用户，下次轮询重试
    console.warn('[AdminLayout] fetch unread count failed', e)
  }
}

onMounted(() => {
  loadMenus()
  setupResponsive()
  loadUnread()
  // 每 60s 刷新一次未读数（轻量接口，轮询成本可忽略）
  unreadTimer = setInterval(loadUnread, 60_000)
})

// =============================================================================
// 响应式：<1024px 自动折叠侧边栏（宽屏恢复时不自动展开，尊重用户手动选择）
// -----------------------------------------------------------------------------
let mql: MediaQueryList | null = null
let mqlHandler: ((e: MediaQueryListEvent) => void) | null = null

function setupResponsive() {
  mql = window.matchMedia('(max-width: 1023px)')
  mqlHandler = (e: MediaQueryListEvent) => {
    if (e.matches && !app.sidebarCollapsed) {
      app.sidebarCollapsed = true
    }
  }
  mql.addEventListener('change', mqlHandler)
  // 初始化时若已处于小屏（如浏览器窗口较小打开页面），立即折叠
  if (mql.matches && !app.sidebarCollapsed) {
    app.sidebarCollapsed = true
  }
}

onBeforeUnmount(() => {
  mql?.removeEventListener('change', mqlHandler as EventListener)
  if (unreadTimer) clearInterval(unreadTimer)
})

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

function goNotify() {
  // 进入消息记录页立即清零角标（页面内还会调用接口标记已读）
  unreadCount.value = 0
  router.push('/notify/record')
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

// =============================================================================
// Tabs：业务标题 + 右键关闭菜单
// -----------------------------------------------------------------------------
// 详情页复用同一 Tab（store 按 name 去重），标题带业务标识：
//   订单详情 → 订单 BBD...001（不再出现「订单详情×N」）
// 详情页通过 route.query.__title 传入业务标题（由各详情页在数据加载后设置）
// =============================================================================

/** Tab 标题：优先页面自报的业务标题（query.__title），否则取路由 meta */
function tagTitle(v: { path: string; title: string; name?: string }): string {
  if (route.path === v.path && route.query.__title) return route.query.__title as string
  return v.title
}

interface TagContext {
  visible: boolean
  x: number
  y: number
  current: TagItem
}
const tagContext = ref<TagContext | null>(null)

function openTagContext(v: TagItem, e: MouseEvent) {
  tagContext.value = { visible: true, x: e.clientX, y: e.clientY, current: v }
}

function hideTagContext() {
  tagContext.value = null
}

async function handleTagCommand(command: string) {
  const ctx = tagContext.value
  tagContext.value = null
  if (!ctx) return
  const current = ctx.current
  if (command === 'close-current') {
    closeTag(current)
  } else if (command === 'close-others') {
    app.removeOtherViews(current.path)
    if (!visitedViews.value.some((v) => v.path === route.path)) {
      router.push(current.path)
    }
  } else if (command === 'close-all') {
    app.removeAllViews()
    const remaining = visitedViews.value.find((v) => v.affix)
    if (!remaining) router.push('/dashboard')
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
          <el-tooltip :content="unreadCount > 0 ? `通知记录（${unreadCount} 条未读）` : '通知记录'">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="topbar-notify-badge">
              <el-icon class="topbar-icon" @click="goNotify"><Bell /></el-icon>
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

      <div class="tags-view" @click.self="hideTagContext">
        <el-tag
          v-for="(v, i) in visitedViews"
          :key="v.path"
          :closable="!v.affix"
          :type="v.path === route.path ? 'primary' : 'info'"
          effect="light"
          class="tags-view__item"
          @click="router.push(v.path)"
          @close="closeTag(v)"
          @contextmenu.prevent="openTagContext(v, $event)"
        >
          {{ tagTitle(v) }}
        </el-tag>
        <!-- 自绘右键菜单（fixed 定位） -->
        <teleport to="body">
          <div
            v-if="tagContext"
            class="tag-context-menu"
            :style="{ left: tagContext.x + 'px', top: tagContext.y + 'px' }"
            @click.stop
          >
            <div class="tag-context-menu__item" :class="{ 'is-disabled': tagContext.current.affix }" @click="handleTagCommand('close-current')">关闭当前</div>
            <div class="tag-context-menu__item" @click="handleTagCommand('close-others')">关闭其他</div>
            <div class="tag-context-menu__item" @click="handleTagCommand('close-all')">关闭全部</div>
          </div>
        </teleport>
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
    path: '/customer-portal',
    title: '客户自助业务',
    icon: 'Service',
    perms: 'customer-portal:admin',
    children: [{ path: '/customer-portal/operations', title: '业务处理台', icon: 'Service', perms: 'customer-portal:admin' }]
  },
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
    path: '/resource',
    title: '资源管理',
    icon: 'OfficeBuilding',
    perms: 'resource:view',
    children: [
      { path: '/resource/list', title: '网络资源台账', icon: 'List', perms: 'resource:view' },
      { path: '/resource/package', title: '套餐资源', icon: 'Goods', perms: 'resource:view' }
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
  background: var(--el-bg-color);
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
    .topbar-notify-badge {
      display: inline-flex;
      line-height: 1;
      :deep(.el-badge__content) {
        transform: translateY(-40%) translateX(60%);
      }
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

// —— 响应式：<1024px 折叠侧边栏由 JS matchMedia 控制；这里做顶栏细节适配 ——
@media (max-width: 1023px) {
  .topbar {
    padding: 0 12px;
    .topbar-left {
      gap: 10px;
      .topbar-breadcrumb {
        :deep(.el-breadcrumb__inner) {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 140px;
        }
      }
    }
    .topbar-right {
      gap: 10px;
      .username {
        display: none; // 小屏隐藏用户名，保留头像
      }
    }
  }
  .main-content {
    padding: 12px;
    overflow-x: auto; // 页面内容横向溢出时整体可滚动
  }
}

.tags-view {
  height: $tags-view-height;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 4px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  white-space: nowrap;
  .tags-view__item {
    cursor: pointer;
    user-select: none;
  }
}

/* Tab 右键菜单（fixed 自绘） */
.tag-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 120px;
  background: var(--el-bg-color);
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 4px 0;
  &__item {
    padding: 8px 16px;
    font-size: 13px;
    color: var(--el-text-color-primary);
    cursor: pointer;
    &:hover { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
    &.is-disabled { color: var(--el-text-color-disabled); cursor: not-allowed; &:hover { background: transparent; color: var(--el-text-color-disabled); } }
  }
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
