import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

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
    meta: { title: '登录', requiresAuth: false, hideInMenu: true }
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
        meta: { title: '数据看板', icon: 'DataLine', requiresAuth: true, affix: true, permission: 'dashboard:view' }
      }
    ]
  },
  {
    path: '/customer',
    component: Layout,
    redirect: '/customer/list',
    meta: { title: '客户管理', icon: 'User', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'CustomerList',
        component: () => import('@/views/customer/list.vue'),
        meta: { title: '客户列表', icon: 'List', requiresAuth: true, permission: 'customer:view' }
      },
      {
        path: 'detail/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/detail.vue'),
        meta: { title: '客户详情', requiresAuth: true, hideInMenu: true, permission: 'customer:view' }
      }
    ]
  },
  {
    path: '/order',
    component: Layout,
    redirect: '/order/list',
    meta: { title: '订单管理', icon: 'Document', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'OrderList',
        component: () => import('@/views/order/list.vue'),
        meta: { title: '订单列表', icon: 'List', requiresAuth: true, keepAlive: true, permission: 'order:view' }
      },
      {
        path: 'create',
        name: 'OrderCreate',
        component: () => import('@/views/order/create.vue'),
        meta: { title: '创建订单', icon: 'Plus', requiresAuth: true, permission: 'order:create' }
      },
      {
        path: 'audit',
        name: 'OrderAudit',
        component: () => import('@/views/order/audit.vue'),
        meta: { title: '订单审核', requiresAuth: true, hideInMenu: true, permission: 'order:audit' }
      },
      {
        path: 'detail/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情', requiresAuth: true, hideInMenu: true, permission: 'order:view' }
      }
    ]
  },
  {
    path: '/workorder',
    component: Layout,
    redirect: '/workorder/list',
    meta: { title: '工单管理', icon: 'Tools', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'WorkorderList',
        component: () => import('@/views/workorder/list.vue'),
        meta: { title: '工单列表', icon: 'List', requiresAuth: true, permission: 'workorder:view' }
      },
      {
        path: 'dispatch-board',
        name: 'DispatchBoard',
        component: () => import('@/views/workorder/dispatch-board.vue'),
        meta: { title: '派单工作台', icon: 'Connection', requiresAuth: true, permission: 'dispatch:manual' }
      },
      {
        path: 'detail/:id',
        name: 'WorkorderDetail',
        component: () => import('@/views/workorder/detail.vue'),
        meta: { title: '工单详情', requiresAuth: true, hideInMenu: true, permission: 'workorder:view' }
      }
    ]
  },
  {
    path: '/installer',
    component: Layout,
    redirect: '/installer/list',
    meta: { title: '装维管理', icon: 'Avatar', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'InstallerList',
        component: () => import('@/views/installer/list.vue'),
        meta: { title: '装维列表', icon: 'List', requiresAuth: true, permission: 'installer:view' }
      },
      {
        path: 'map',
        name: 'InstallerMap',
        component: () => import('@/views/installer/map.vue'),
        meta: { title: '装维地图', icon: 'MapLocation', requiresAuth: true, permission: 'installer:view' }
      },
      {
        path: 'profile/:id',
        name: 'InstallerProfile',
        component: () => import('@/views/installer/profile.vue'),
        meta: { title: '装维档案', requiresAuth: true, hideInMenu: true, permission: 'installer:view' }
      }
    ]
  },
  {
    path: '/resource',
    component: Layout,
    redirect: '/resource/list',
    meta: { title: '资源管理', icon: 'OfficeBuilding', requiresAuth: true, permission: 'resource:view' },
    children: [
      {
        path: 'list',
        name: 'ResourceList',
        component: () => import('@/views/resource/index.vue'),
        meta: { title: '网络资源台账', icon: 'List', requiresAuth: true, permission: 'resource:view' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'Setting', requiresAuth: true, roles: ['SUPER_ADMIN'] },
    children: [
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/list.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', requiresAuth: true, permission: 'system:user:view' }
      },
      {
        path: 'role',
        name: 'RoleList',
        component: () => import('@/views/role/list.vue'),
        meta: { title: '角色管理', icon: 'Avatar', requiresAuth: true, permission: 'system:role:view' }
      },
      {
        path: 'menu',
        name: 'MenuList',
        component: () => import('@/views/menu/list.vue'),
        meta: { title: '菜单管理', icon: 'Menu', requiresAuth: true, permission: 'system:menu:view' }
      },
      {
        path: 'dept',
        name: 'DeptList',
        component: () => import('@/views/dept/list.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', requiresAuth: true, permission: 'system:dept:view' }
      }
    ]
  },
  {
    path: '/notify',
    component: Layout,
    redirect: '/notify/template',
    meta: { title: '通知管理', icon: 'Message', requiresAuth: true },
    children: [
      {
        path: 'template',
        name: 'NotifyTemplate',
        component: () => import('@/views/notify/template.vue'),
        meta: { title: '消息模板', icon: 'Memo', requiresAuth: true, permission: 'notify:template:view' }
      },
      {
        path: 'record',
        name: 'NotifyRecord',
        component: () => import('@/views/notify/record.vue'),
        meta: { title: '消息记录', icon: 'ChatLineRound', requiresAuth: true, permission: 'notify:record:view' }
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
        meta: { title: '文件管理', icon: 'Folder', requiresAuth: true, permission: 'file:view' }
      }
    ]
  },
  {
    path: '/log',
    component: Layout,
    redirect: '/log/operation',
    meta: { title: '日志管理', icon: 'Document', requiresAuth: true, roles: ['SUPER_ADMIN', 'AUDITOR'] },
    children: [
      {
        path: 'operation',
        name: 'OperationLog',
        component: () => import('@/views/log/operation.vue'),
        meta: { title: '操作日志', icon: 'Tickets', requiresAuth: true, permission: 'log:view' }
      },
      {
        path: 'login',
        name: 'LoginLog',
        component: () => import('@/views/log/login.vue'),
        meta: { title: '登录日志', icon: 'Key', requiresAuth: true, permission: 'log:view' }
      }
    ]
  },
  {
    path: '/attendance',
    component: Layout,
    redirect: '/attendance/team',
    meta: { title: '考勤管理', icon: 'Clock', requiresAuth: true, permission: 'attendance:view-all' },
    children: [
      {
        path: 'team',
        name: 'AttendanceReport',
        component: () => import('@/views/attendance/Report.vue'),
        meta: { title: '团队报表', icon: 'DataAnalysis', requiresAuth: true, permission: 'attendance:view-all' }
      }
    ]
  },
  {
    path: '/leave',
    component: Layout,
    redirect: '/leave/approvals',
    meta: { title: '请假管理', icon: 'TodoList', requiresAuth: true },
    children: [
      {
        path: 'approvals',
        name: 'LeaveApproval',
        component: () => import('@/views/leave/Approval.vue'),
        meta: { title: '请假审批', icon: 'Check', requiresAuth: true, permission: 'leave:approve' }
      }
    ]
  },
  {
    path: '/sla',
    component: Layout,
    redirect: '/sla/expiring',
    meta: { title: 'SLA 监控', icon: 'Warning', requiresAuth: true, permission: 'workorder:sla:view' },
    children: [
      {
        path: 'expiring',
        name: 'SlaExpiring',
        component: () => import('@/views/sla/Expiring.vue'),
        meta: { title: '工单时效', icon: 'AlarmClock', requiresAuth: true, permission: 'workorder:sla:view' }
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
        meta: { title: '个人中心', icon: 'User', requiresAuth: true, hideInMenu: true }
      }
    ]
  },
  {
    path: '/user',
    redirect: '/system/user',
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/role',
    redirect: '/system/role',
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/menu',
    redirect: '/system/menu',
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/dept',
    redirect: '/system/dept',
    meta: { requiresAuth: true, hideInMenu: true }
  },
  {
    path: '/403',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无访问权限', hideInMenu: true, requiresAuth: false }
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hideInMenu: true, requiresAuth: false }
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

router.onError((error) => {
  NProgress.done()
  console.error('Router error', error)
  ElMessage.error('Page resource failed to load. Please refresh and try again.')
})

export default router
