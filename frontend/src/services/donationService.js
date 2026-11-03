import api from './api'

const BASE = '/donations'

async function getAll(bloodBankId) {
  const params = bloodBankId ? { bloodBankId } : {}
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

export const donationService = {
  getAll,
  getById,
  create,
  updateStatus,
}
