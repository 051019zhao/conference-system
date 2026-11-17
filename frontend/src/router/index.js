import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

/**
 * 路由:公共布局 + 管理后台布局两组;
 * meta.login 需登录;meta.roles 限定角色,不满足跳转首页
 */
const routes = [
  {
    path: '/',
    component: () => import('../layouts/PublicLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/public/Home.vue') },
      { path: 'cfp', name: 'cfp', component: () => import('../views/public/CallForPapers.vue') },
      { path: 'experts', name: 'experts', component: () => import('../views/public/Experts.vue') },
      { path: 'keynotes', name: 'keynotes', component: () => import('../views/public/Keynotes.vue') },
      { path: 'schedule', name: 'schedule', component: () => import('../views/public/Schedule.vue') },
      { path: 'accepted', name: 'accepted', component: () => import('../views/public/Accepted.vue') },
      { path: 'notifications', name: 'notifications', component: () => import('../views/public/Notifications.vue') },
      { path: 'notifications/:id', name: 'notificationDetail', component: () => import('../views/public/NotificationDetail.vue') },
      { path: 'materials', name: 'materials', component: () => import('../views/public/Materials.vue') },
      { path: 'login', name: 'login', component: () => import('../views/auth/Login.vue') },
      { path: 'register', name: 'register', component: () => import('../views/auth/Register.vue') },
      { path: 'author/papers', name: 'myPapers', component: () => import('../views/author/MyPapers.vue'), meta: { roles: ['AUTHOR'] } },
      { path: 'author/submit', name: 'submitPaper', component: () => import('../views/author/SubmitPaper.vue'), meta: { roles: ['AUTHOR'] } },
      { path: 'paper/:id', name: 'paperDetail', component: () => import('../views/author/PaperDetail.vue'), meta: { login: true } },
      { path: 'account', name: 'account', component: () => import('../views/account/Profile.vue'), meta: { login: true } },
      { path: 'expert/tasks', name: 'reviewTasks', component: () => import('../views/expert/ReviewTasks.vue'), meta: { roles: ['EXPERT'] } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', name: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', name: 'adminUsers', component: () => import('../views/admin/Users.vue') },
      { path: 'topics', name: 'adminTopics', component: () => import('../views/admin/Topics.vue') },
      { path: 'papers', name: 'adminPapers', component: () => import('../views/admin/Papers.vue') },
      { path: 'keynotes', name: 'adminKeynotes', component: () => import('../views/admin/Keynotes.vue') },
      { path: 'schedule', name: 'adminSchedule', component: () => import('../views/admin/Schedule.vue') },
      { path: 'notifications', name: 'adminNotifications', component: () => import('../views/admin/Notifications.vue') },
      { path: 'materials', name: 'adminMaterials', component: () => import('../views/admin/Materials.vue') },
      { path: 'logs', name: 'adminLogs', component: () => import('../views/admin/Logs.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      return { el: to.hash, top: 90, behavior: reduceMotion ? 'auto' : 'smooth' }
    }
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const store = useUserStore()
  if ((to.meta.login || to.meta.roles) && !store.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !to.meta.roles.includes(store.role)) {
    return { path: '/' }
  }
  return true
})

export default router
