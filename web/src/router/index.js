import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser } from '../utils/auth'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/admin/login', component: () => import('../views/admin/AdminLogin.vue') },
  {
    path: '/',
    component: () => import('../layouts/UserLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', component: () => import('../views/Home.vue') },
      { path: 'cases', component: () => import('../views/CaseList.vue') },
      { path: 'cases/new', component: () => import('../views/CaseEdit.vue') },
      { path: 'cases/:id', component: () => import('../views/CaseDetail.vue') },
      { path: 'cases/:id/edit', component: () => import('../views/CaseEdit.vue') },
      { path: 'mycases', component: () => import('../views/MyCases.vue') },
      { path: 'radar', component: () => import('../views/RadarSearch.vue') },
      { path: 'questions', component: () => import('../views/Questions.vue') },
      { path: 'questions/:id', component: () => import('../views/QuestionDetail.vue') },
      { path: 'guides', component: () => import('../views/Guides.vue') },
      { path: 'guides/:id', component: () => import('../views/GuideDetail.vue') },
      { path: 'messages/:peerId?', component: () => import('../views/Messages.vue') },
      { path: 'profile', component: () => import('../views/MyProfile.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'banners', component: () => import('../views/admin/BannerManage.vue') },
      { path: 'users', component: () => import('../views/admin/UserManage.vue') },
      { path: 'cases', component: () => import('../views/admin/CaseManage.vue') },
      { path: 'clues', component: () => import('../views/admin/ClueManage.vue') },
      { path: 'guides', component: () => import('../views/admin/GuideManage.vue') },
      { path: 'questions', component: () => import('../views/admin/QuestionManage.vue') },
      { path: 'messages/:peerId?', component: () => import('../views/Messages.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const needAuth = to.matched.some(r => r.meta.requiresAuth)
  const needAdmin = to.matched.some(r => r.meta.admin)
  if (needAuth && !getToken()) {
    return needAdmin ? '/admin/login' : '/login'
  }
  if (needAdmin && getUser()?.role !== 'ADMIN') {
    return '/admin/login'
  }
  return true
})

export default router
