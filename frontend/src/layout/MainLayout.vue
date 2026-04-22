<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'

const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

let timer = null

const menuItems = computed(() => {
  const baseItems = [
    { label: '帖子广场', path: '/posts' },
    { label: '发布信息', path: '/posts/create', auth: true },
    { label: '我的帖子', path: '/my/posts', auth: true },
    { label: '我的会话', path: '/threads', auth: true },
    { label: '通知中心', path: '/notifications', auth: true },
    { label: '数据统计', path: '/stats' },
  ]
  const adminItems = [
    { label: '用户管理', path: '/admin/users', admin: true },
    { label: '帖子管理', path: '/admin/posts', admin: true },
    { label: '匹配管理', path: '/admin/match', admin: true },
    { label: '管理统计', path: '/admin/stats', admin: true },
  ]
  return [...baseItems, ...adminItems].filter((item) => {
    if (item.auth && !authStore.token) return false
    if (item.admin && !authStore.isAdmin) return false
    return true
  })
})

const handleCommand = async (command) => {
  if (command === 'logout') {
    authStore.clearAuth()
    await router.push('/login')
    return
  }
  await router.push(command)
}

onMounted(async () => {
  await notificationStore.refreshUnreadCount()
  timer = setInterval(() => notificationStore.refreshUnreadCount(), 30000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #fff; border-bottom: 1px solid #ebeef5">
      <div style="display: flex; align-items: center; justify-content: space-between; height: 100%">
        <el-space :size="18">
          <div style="font-size: 18px; font-weight: 700; cursor: pointer" @click="router.push('/home')">
            校园失物招领系统
          </div>
          <el-link
            v-for="item in menuItems"
            :key="item.path"
            :type="router.currentRoute.value.path.startsWith(item.path) ? 'primary' : 'default'"
            :underline="false"
            @click="router.push(item.path)"
          >
            <span>
              {{ item.label }}
              <el-badge
                v-if="item.path === '/notifications' && notificationStore.unreadCount > 0"
                :value="notificationStore.unreadCount"
                :max="99"
                style="margin-left: 6px"
              />
            </span>
          </el-link>
        </el-space>

        <div>
          <el-dropdown v-if="authStore.token" @command="handleCommand">
            <span style="cursor: pointer">
              {{ authStore.user?.nickname || authStore.user?.username || '用户' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-space v-else>
            <el-button text type="primary" @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </el-space>
        </div>
      </div>
    </el-header>

    <el-main style="padding: 0">
      <router-view />
    </el-main>
  </el-container>
</template>
