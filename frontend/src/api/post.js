import http from './http'

export const createPost = (payload) => http.post('/posts', payload)

export const listPosts = (params) => http.get('/posts', { params })

export const getPostDetail = (id) => http.get(`/posts/${id}`)

export const updatePost = (id, payload) => http.put(`/posts/${id}`, payload)

export const closePost = (id) => http.delete(`/posts/${id}`)

export const resolvePost = (id) => http.put(`/posts/${id}/resolve`)

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export const listAdminPosts = (params) => http.get('/admin/posts', { params })

export const closeAdminPost = (id, reason) => http.delete(`/admin/posts/${id}`, { data: reason ? { reason } : null })
