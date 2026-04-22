import http from './http'

export const getOverview = () => http.get('/stats/overview')

export const getCategoryStats = (params) => http.get('/stats/category', { params })

export const getAreaStats = (params) => http.get('/stats/area', { params })

export const getTrendStats = (params) => http.get('/stats/trend', { params })

export const getUserStats = () => http.get('/admin/stats/users')
