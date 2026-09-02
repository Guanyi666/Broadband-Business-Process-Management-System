import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    requiresAuth?: boolean
    permission?: string | string[]
    roles?: string[]
    hideInMenu?: boolean
    keepAlive?: boolean
    affix?: boolean
    order?: number
  }
}

const Layout = () => import('@/layouts/AdminLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: 'Login', requiresAuth: false, hideInMenu: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true }
  },
  {
    path: '/dashboard',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: 'Dashboard', icon: 'DataLine', requiresAuth: true, affix: true }
      }
    ]
  },
  {
    path: '/customer',
    component: Layout,
    redirect: '/customer/list',
    meta: { title: 'Customer', icon: 'User', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'CustomerList',
        component: () => import('@/views/customer/list.vue'),
        meta: { title: 'Customer List', icon: 'List', requiresAuth: true }
      },
      {
        path: 'detail/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/detail.vue'),
        meta: { title: 'Customer Detail', requiresAuth: true, hideInMenu: true }
      }
    ]
  },
  {
    path: '/order',
    component: Layout,
    redirect: '/order/list',
    meta: { title: 'Order', icon: 'Document', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'OrderList',
        component: () => import('@/views/order/list.vue'),
        meta: { title: 'Order List', icon: 'List', requiresAuth: true, keepAlive: true }
      },
      {
        path: 'create',
        name: 'OrderCreate',
        component: () => import('@/views/order/create.vue'),
        meta: { title: 'Create Order', icon: 'Plus', requiresAuth: true, permission: 'order:create' }
      },
      {
        path: 'audit',
        name: 'OrderAudit',
        component: () => import('@/views/order/audit.vue'),
        meta: { title: 'Audit', requiresAuth: true, hideInMenu: true }
      },
      {
        path: 'detail/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: 'Order Detail', requiresAuth: true, hideInMenu: true }
      }
    ]
  },
  {
    path: '/workorder',
    component: Layout,
    redirect: '/workorder/list',
    meta: { title: 'Workorder', icon: 'Tools', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'WorkorderList',
        component: () => import('@/views/workorder/list.vue'),
        meta: { title: 'Workorder List', icon: 'List', requiresAuth: true }
      },
      {
        path: 'dispatch-board',
        name: 'DispatchBoard',
        component: () => import('@/views/workorder/dispatch-board.vue'),
        meta: { title: 'Dispatch Board', icon: 'Connection', requiresAuth: true, permission: 'dispatch:manual' }
      },
      {
        path: 'detail/:id',
        name: 'WorkorderDetail',
        component: () => import('@/views/workorder/detail.vue'),
        meta: { title: 'Workorder Detail', requiresAuth: true, hideInMenu: true }
      }
    ]
  },
  {
    path: '/installer',
    component: Layout,
    redirect: '/installer/list',
    meta: { title: 'Installer', icon: 'Avatar', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'InstallerList',
        component: () => import('@/views/installer/list.vue'),
        meta: { title: 'Installer List', icon: 'List', requiresAuth: true }
      },
      {
        path: 'map',
        name: 'InstallerMap',
        component: () => import('@/views/installer/map.vue'),
        meta: { title: 'Installer Map', icon: 'MapLocation', requiresAuth: true }
      },
      {
        path: 'profile/:id',
        name: 'InstallerProfile',
        component: () => import('@/views/installer/profile.vue'),
        meta: { title: 'Installer Profile', requiresAuth: true, hideInMenu: true }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/user',
    meta: { title: 'System', icon: 'Setting', requiresAuth: true, roles: ['SUPER_ADMIN'] },
    children: [
      {
        path: '/user',
        name: 'UserList',
        component: () => import('@/views/user/list.vue'),
        meta: { title: 'Users', icon: 'UserFilled', requiresAuth: true, permission: 'system:user:view' }
      },
      {
        path: '/role',
        name: 'RoleList',
        component: () => import('@/views/role/list.vue'),
        meta: { title: 'Roles', icon: 'Avatar', requiresAuth: true, permission: 'system:role:view' }
      },
      {
        path: '/menu',
        name: 'MenuList',
        component: () => import('@/views/menu/list.vue'),
        meta: { title: 'Menus', icon: 'Menu', requiresAuth: true, permission: 'system:menu:view' }
      },
      {
        path: '/dept',
        name: 'DeptList',
        component: () => import('@/views/dept/list.vue'),
        meta: { title: 'Departments', icon: 'OfficeBuilding', requiresAuth: true, permission: 'system:dept:view' }
      }
    ]
  },
  {
    path: '/notify',
    component: Layout,
    redirect: '/notify/template',
    meta: { title: 'Notification', icon: 'Message', requiresAuth: true },
    children: [
      {
        path: 'template',
        name: 'NotifyTemplate',
        component: () => import('@/views/notify/template.vue'),
        meta: { title: 'Templates', icon: 'Memo', requiresAuth: true }
      },
      {
        path: 'record',
        name: 'NotifyRecord',
        component: () => import('@/views/notify/record.vue'),
        meta: { title: 'Message Records', icon: 'ChatLineRound', requiresAuth: true }
      }
    ]
  },
  {
    path: '/file',
    component: Layout,
    children: [
      {
        path: '',
        name: 'FileManager',
        component: () => import('@/views/file/index.vue'),
        meta: { title: 'Files', icon: 'Folder', requiresAuth: true }
      }
    ]
  },
  {
    path: '/log',
    component: Layout,
    redirect: '/log/operation',
    meta: { title: 'Logs', icon: 'Document', requiresAuth: true, roles: ['SUPER_ADMIN', 'AUDITOR'] },
    children: [
      {
        path: 'operation',
        name: 'OperationLog',
        component: () => import('@/views/log/operation.vue'),
        meta: { title: 'Operation Logs', icon: 'Tickets', requiresAuth: true }
      },
      {
        path: 'login',
        name: 'LoginLog',
        component: () => import('@/views/log/login.vue'),
        meta: { title: 'Login Logs', icon: 'Key', requiresAuth: true }
      }
    ]
  },
  {
    path: '/monitor',
    component: Layout,
    redirect: '/monitor/metrics',
    meta: { title: 'Monitor', icon: 'Monitor', requiresAuth: true, roles: ['SUPER_ADMIN'] },
    children: [
      {
        path: 'metrics',
        name: 'MonitorMetrics',
        component: () => import('@/views/monitor/metrics.vue'),
        meta: { title: 'Metrics', icon: 'TrendCharts', requiresAuth: true }
      },
      {
        path: 'health',
        name: 'MonitorHealth',
        component: () => import('@/views/monitor/health.vue'),
        meta: { title: 'Health', icon: 'FirstAidKit', requiresAuth: true }
      }
    ]
  },
  {
    path: '/attendance',
    component: Layout,
    redirect: '/attendance/team',
    meta: { title: 'Attendance', icon: 'Clock', requiresAuth: true, permission: 'attendance:view-all' },
    children: [
      {
        path: 'team',
        name: 'AttendanceReport',
        component: () => import('@/views/attendance/Report.vue'),
        meta: { title: 'Team Report', icon: 'DataAnalysis', requiresAuth: true }
      }
    ]
  },
  {
    path: '/leave',
    component: Layout,
    redirect: '/leave/approvals',
    meta: { title: 'Leave', icon: 'TodoList', requiresAuth: true },
    children: [
      {
        path: 'approvals',
        name: 'LeaveApproval',
        component: () => import('@/views/leave/Approval.vue'),
        meta: { title: 'Approvals', icon: 'Check', requiresAuth: true, permission: 'leave:approve' }
      }
    ]
  },
  {
    path: '/sla',
    component: Layout,
    redirect: '/sla/expiring',
    meta: { title: 'SLA Monitor', icon: 'Warning', requiresAuth: true, permission: 'workorder:sla:view' },
    children: [
      {
        path: 'expiring',
        name: 'SlaExpiring',
        component: () => import('@/views/sla/Expiring.vue'),
        meta: { title: 'Work-Order SLA', icon: 'AlarmClock', requiresAuth: true }
      }
    ]
  },
  {
    path: '/profile',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: 'Profile', icon: 'User', requiresAuth: true, hideInMenu: true }
      }
    ]
  },
  {
    path: '/403',
    component: () => import('@/views/error/403.vue'),
    meta: { title: 'Forbidden', hideInMenu: true, requiresAuth: false }
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: 'Not Found', hideInMenu: true, requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { requiresAuth: false, hideInMenu: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 })
})

const WHITE_LIST = ['/login', '/403', '/404']

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const auth = useAuthStore()

  if (to.meta.title) document.title = `${to.meta.title} - BBPMS Admin`

  if (WHITE_LIST.includes(to.path)) {
    return next()
  }

  const token = getToken()
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // lazy fill user info
  if (!auth.userInfo) {
    try {
      await auth.fetchUserInfo()
    } catch (e) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }

  // role check
  if (to.meta.roles && (to.meta.roles as string[]).length) {
    const ok = (to.meta.roles as string[]).some((r) => auth.roles.includes(r))
    if (!ok) return next('/403')
  }

  // permission check
  if (to.meta.permission) {
    if (!auth.hasPermission(to.meta.permission)) {
      return next('/403')
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router