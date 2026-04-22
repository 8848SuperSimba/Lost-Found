import http from './http'

export const getMatches = (postId) => http.get(`/posts/${postId}/matches`)

export const rematch = (postId) => http.post(`/posts/${postId}/rematch`)

export const triggerAllMatches = () => http.post('/admin/match/trigger')
