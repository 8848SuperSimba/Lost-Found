import http from './http'

export const createThread = (payload) => http.post('/threads', payload)

export const listThreads = () => http.get('/threads')

export const getThreadDetail = (threadId) => http.get(`/threads/${threadId}`)

export const getThreadMessages = (threadId, params) => http.get(`/threads/${threadId}/messages`, { params })

export const sendMessage = (threadId, payload) => http.post(`/threads/${threadId}/messages`, payload)
