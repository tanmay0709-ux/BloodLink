import api from './api'

const BASE = '/hospitals'

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

async function approveStaff(hospitalId, userId) {
  const res = await api.post(`${BASE}/${hospitalId}/approve-staff/${userId}`)
  return res.data?.data
}

async function rejectStaff(hospitalId, userId) {
  const res = await api.post(`${BASE}/${hospitalId}/reject-staff/${userId}`)
  return res.data?.data
}

async function assignStaff(hospitalId, userId) {
  const res = await api.post(`${BASE}/${hospitalId}/assign-staff/${userId}`)
  return res.data?.data
}

async function unassignStaff(hospitalId, userId) {
  await api.delete(`${BASE}/${hospitalId}/unassign-staff/${userId}`)
}

export const hospitalService = {
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
