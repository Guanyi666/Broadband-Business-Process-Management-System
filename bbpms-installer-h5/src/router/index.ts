import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/workorders' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', showTabbar: false }
  },
  {
    path: '/workorders',
    name: 'WorkorderList',
    component: () => import('@/views/workorders/list.vue'),
    meta: { title: '工单', showTabbar: true, keepAlive: true }
  },
  {
    path: '/workorders/:id',
    name: 'WorkorderDetail',
    component: () => import('@/views/workorders/detail.vue'),
    meta: { title: '工单详情', showTabbar: false },
    props: true
  },
  {
    path: '/workorders/:id/install',
    name: 'WorkorderInstall',
    component: () => import('@/views/workorders/install.vue'),
    meta: { title: '装机流程', showTabbar: false },
    props: true
  },
  {
    path: '/attendance',
    name: 'AttendanceIndex',
    component: () => import('@/views/attendance/Index.vue'),
    meta: { title: '考勤打卡', showTabbar: false }
  },
  {
    path: '/attendance/history',
    name: 'AttendanceHistory',
    component: () => import('@/views/attendance/History.vue'),
    meta: { title: '考勤历史', showTabbar: false }
  },
  {
    path: '/leave/apply',
    name: 'LeaveApply',
    component: () => import('@/views/leave/Apply.vue'),
    meta: { title: '申请请假', showTabbar: false }
  },
  {
    path: '/leave/my',
    name: 'LeaveMyApplications',
    component: () => import('@/views/leave/MyApplications.vue'),
    meta: { title: '我的请假', showTabbar: false }
  },
  {
    path: '/profile',
    name: 'ProfileIndex',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: '我的', showTabbar: true, keepAlive: true }
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/not-found.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, _from, next) => {
  if (to.meta?.title) document.title = `${to.meta.title} - BBPMS 装维端`
  const token = sessionStorage.getItem('bbpms_installer_token')
  if (to.name !== 'Login' && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && token) {
    next({ name: 'WorkorderList' })
  } else {
    next()
  }
})

export default router
