import api from './api'

async function listUsers() {
  const res = await api.get('/admin/users')
  return res.data?.data ?? []
}

export const adminService = { listUsers }
