import http from './http'

export const listNotifications = (params) => http.get('/notifications', { params })

export const getUnreadCount = () => http.get('/notifications/unread-count')

export const markRead = (id) => http.put(`/notifications/${id}/read`)

export const markAllRead = () => http.put('/notifications/read-all')
