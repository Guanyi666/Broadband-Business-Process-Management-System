import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { showTabbar: true } },
  { path: '/orders', name: 'Orders', component: () => import('@/views/Orders.vue'), meta: { showTabbar: true } },
  { path: '/orders/apply', name: 'OrderApply', component: () => import('@/views/OrderApply.vue') },
  { path: '/orders/:id', name: 'OrderDetail', component: () => import('@/views/OrderDetail.vue') },
  { path: '/orders/:id/resubmit', name: 'OrderResubmit', component: () => import('@/views/OrderResubmit.vue') },
  { path: '/orders/:id/appointment', name: 'Appointment', component: () => import('@/views/Appointment.vue') },
  { path: '/orders/:id/evaluation', name: 'Evaluation', component: () => import('@/views/Evaluation.vue') },
  { path: '/services', name: 'Services', component: () => import('@/views/Services.vue'), meta: { showTabbar: true } },
  { path: '/tickets/new', name: 'TicketNew', component: () => import('@/views/TicketForm.vue') },
  { path: '/tickets/:id', name: 'TicketDetail', component: () => import('@/views/TicketDetail.vue') },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { showTabbar: true } },
  { path: '/profile/change', name: 'ProfileChange', component: () => import('@/views/ProfileChange.vue') },
  { path: '/messages', name: 'Messages', component: () => import('@/views/Messages.vue') }
]

const router = createRouter({ history: createWebHashHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
router.beforeEach((to) => {
  const token = sessionStorage.getItem('bbpms_customer_token')
  if (to.name !== 'Login' && !token) return { name: 'Login', query: { redirect: to.fullPath } }
  if (to.name === 'Login' && token) return { name: 'Home' }
  return true
})
export default router
