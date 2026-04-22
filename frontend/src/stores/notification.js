import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '../api/notification'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  const refreshUnreadCount = async () => {
    const authStore = useAuthStore()
    if (!authStore.token) {
      unreadCount.value = 0
      return
    }
    try {
      unreadCount.value = await getUnreadCount()
    } catch (error) {
      unreadCount.value = 0
    }
  }

  return {
    unreadCount,
    refreshUnreadCount,
  }
})
