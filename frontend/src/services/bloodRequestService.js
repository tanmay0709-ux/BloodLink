import api from './api'

const BASE = '/blood-requests'

async function getAll(params = {}) {
  const res = await api.get(BASE, { params })
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

async function updateStatus(id, status) {
  const res = await api.patch(`${BASE}/${id}/status`, null, {
    params: { status },
  })
  return res.data?.data
}

async function getMatchingUnits(id, bloodBankId) {
  const params = bloodBankId ? { bloodBankId } : {}
  const res = await api.get(`${BASE}/${id}/matching-units`, { params })
  return res.data?.data
}

export const bloodRequestService = {
  getAll,
  getById,
  create,
  updateStatus,
  getMatchingUnits,
}
