import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/auth/Login.vue'), meta: { guestOnly: true } },
  { path: '/register', component: () => import('../views/auth/Register.vue'), meta: { guestOnly: true } },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: '/posts' },
      { path: 'home', component: () => import('../views/home/Index.vue') },
      { path: 'profile', component: () => import('../views/user/Profile.vue'), meta: { requiresAuth: true } },
      { path: 'posts', component: () => import('../views/post/PostList.vue') },
      { path: 'posts/create', component: () => import('../views/post/PostCreate.vue'), meta: { requiresAuth: true } },
      { path: 'posts/:id', component: () => import('../views/post/PostDetail.vue') },
      { path: 'posts/:id/edit', component: () => import('../views/post/PostEdit.vue'), meta: { requiresAuth: true } },
      { path: 'my/posts', component: () => import('../views/post/MyPostList.vue'), meta: { requiresAuth: true } },
      { path: 'my/posts/:id/matches', component: () => import('../views/match/MatchResult.vue'), meta: { requiresAuth: true } },
      { path: 'threads', component: () => import('../views/chat/ThreadList.vue'), meta: { requiresAuth: true } },
      { path: 'threads/:id', component: () => import('../views/chat/ChatRoom.vue'), meta: { requiresAuth: true } },
      { path: 'notifications', component: () => import('../views/notification/NotifList.vue'), meta: { requiresAuth: true } },
      { path: 'stats', component: () => import('../views/stats/Dashboard.vue') },
      { path: 'admin/users', component: () => import('../views/admin/UserManage.vue'), meta: { requiresAuth: true, adminOnly: true } },
      { path: 'admin/posts', component: () => import('../views/admin/PostManage.vue'), meta: { requiresAuth: true, adminOnly: true } },
      { path: 'admin/match', component: () => import('../views/admin/MatchManage.vue'), meta: { requiresAuth: true, adminOnly: true } },
      { path: 'admin/stats', component: () => import('../views/admin/StatsManage.vue'), meta: { requiresAuth: true, adminOnly: true } },
      { path: '403', component: () => import('../views/error/Forbidden.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (authStore.token && !authStore.user) {
    try {
      await authStore.fetchMe()
    } catch (error) {
      authStore.clearAuth()
    }
  }

  if (to.meta.guestOnly && authStore.token) {
    return '/posts'
  }
  if (to.meta.requiresAuth && !authStore.token) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
  if (to.meta.adminOnly && !authStore.isAdmin) {
    return '/403'
  }
  return true
})

export default router
