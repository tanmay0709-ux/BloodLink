import api from './api'

const BASE = '/donors'

async function getAll(bloodBankId) {
  const params = bloodBankId ? { bloodBankId } : {}
  const res = await api.get(BASE, { params })
  return res.data?.data ?? []
}

async function getById(id) {
  const res = await api.get(`${BASE}/${id}`)
  return res.data?.data
}

async function search(q, bloodType, bloodBankId) {
  const params = {}
  if (q) params.q = q
  if (bloodType) params.bloodType = bloodType
  if (bloodBankId) params.bloodBankId = bloodBankId
  const res = await api.get(`${BASE}/search`, { params })
  return res.data?.data ?? []
}

async function create(data) {
  const res = await api.post(BASE, data)
  return res.data?.data
}

async function update(id, data) {
  const res = await api.put(`${BASE}/${id}`, data)
  return res.data?.data
}

async function getDonations(id) {
  const res = await api.get(`${BASE}/${id}/donations`)
  return res.data?.data ?? []
}

export const donorService = {
  getAll,
  getById,
  search,
  create,
  update,
  getDonations,
}
