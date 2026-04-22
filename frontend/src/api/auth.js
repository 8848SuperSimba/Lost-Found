import http from './http'

export const register = (payload) => http.post('/auth/register', payload)

export const login = (payload) => http.post('/auth/login', payload)
