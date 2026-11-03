import api from './api'

const BASE = '/blood-units'

async function getAll(filters = {}) {
  const params = {}
  if (filters.bloodType) params.bloodType = filters.bloodType
  if (filters.componentType) params.componentType = filters.componentType
  if (filters.status) params.status = filters.status
  if (filters.q) params.q = filters.q
  if (filters.bloodBankId) params.bloodBankId = filters.bloodBankId

  const res = await api.get(BASE, { params })
  return res.data?.data ?? []
}

async function getById(id) {
  const res = await api.get(`${BASE}/${id}`)
  return res.data?.data
}

async function getSummary(bloodBankId) {
  const params = bloodBankId ? { bloodBankId } : {}
  const res = await api.get(`${BASE}/summary`, { params })
  return res.data?.data
}

async function create(data) {
  const res = await api.post(BASE, data)
  return res.data?.data
}

async function update(id, data) {
  const res = await api.put(`${BASE}/${id}`, data)
  return res.data?.data
}

async function updateStatus(id, status) {
  const res = await api.patch(`${BASE}/${id}/status`, null, {
    params: { status },
  })
  return res.data?.data
}

async function checkExpired() {
  const res = await api.post(`${BASE}/check-expired`)
  return res.data?.data
}

export const bloodUnitService = {
  getAll,
  getById,
  getSummary,
  create,
  update,
  updateStatus,
  checkExpired,
}
