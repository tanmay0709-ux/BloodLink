import api from './api'

const BASE = '/blood-banks'

async function getAll() {
  const res = await api.get(BASE)
  return res.data?.data ?? []
}

async function getById(id) {
  const res = await api.get(`${BASE}/${id}`)
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

async function remove(id) {
  await api.delete(`${BASE}/${id}`)
}

async function getStaff(id) {
  const res = await api.get(`${BASE}/${id}/staff`)
  return res.data?.data ?? []
}

async function getPendingStaff(id) {
  const res = await api.get(`${BASE}/${id}/pending-staff`)
  return res.data?.data ?? []
}

async function approveStaff(bloodBankId, userId) {
  const res = await api.post(`${BASE}/${bloodBankId}/approve-staff/${userId}`)
  return res.data?.data
}

async function rejectStaff(bloodBankId, userId) {
  await api.post(`${BASE}/${bloodBankId}/reject-staff/${userId}`)
}

async function assignStaff(bloodBankId, userId) {
  const res = await api.post(`${BASE}/${bloodBankId}/assign-staff/${userId}`)
  return res.data?.data
}

async function unassignStaff(bloodBankId, userId) {
  await api.delete(`${BASE}/${bloodBankId}/unassign-staff/${userId}`)
}

export const bloodBankService = {
  getAll,
  getById,
  create,
  update,
  remove,
  getStaff,
  getPendingStaff,
  approveStaff,
  rejectStaff,
  assignStaff,
  unassignStaff,
}
