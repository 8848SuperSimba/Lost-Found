import http from './http'

export const getMe = () => http.get('/user/me')

export const updateMe = (payload) => http.put('/user/me', payload)

export const changePassword = (payload) => http.put('/user/change-password', payload)

export const getAdminUsers = (params) => http.get('/admin/users', { params })

export const updateUserStatus = (id, status) => http.put(`/admin/users/${id}/ban`, { status })
