import http from './http'

export const listAuditLogs = (params) => http.get('/admin/audit-logs', { params })
