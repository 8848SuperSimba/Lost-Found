import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('lost_found_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code === 200) {
        return payload.data
      }
      ElMessage.error(payload.message || '请求失败')
      if (payload.code === 401) {
        localStorage.removeItem('lost_found_token')
        localStorage.removeItem('lost_found_user')
        if (!location.pathname.includes('/login')) {
          location.href = '/login'
        }
      }
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload
  },
  (error) => {
    ElMessage.error(error?.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default http
