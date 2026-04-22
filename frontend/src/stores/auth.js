import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe } from '../api/user'

const TOKEN_KEY = 'lost_found_token'
const USER_KEY = 'lost_found_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY)) : null)

  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  const setAuth = (newToken, newUser) => {
    token.value = newToken
    user.value = newUser
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_KEY, JSON.stringify(newUser))
  }

  const clearAuth = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  const fetchMe = async () => {
    const data = await getMe()
    user.value = data
    localStorage.setItem(USER_KEY, JSON.stringify(data))
    return data
  }

  return {
    token,
    user,
    isAdmin,
    setAuth,
    clearAuth,
    fetchMe,
  }
})
