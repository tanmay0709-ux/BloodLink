import api from './api'

async function allocateUnits(requestId, unitIds, bloodBankId) {
  const payload = { unitIds, bloodBankId }
  const res = await api.post(`/blood-requests/${requestId}/allocate`, payload)
  return res.data?.data
}

async function getAllocations(requestId) {
  const res = await api.get(`/blood-requests/${requestId}/allocations`)
  return res.data?.data ?? []
}

async function dispatchRequest(requestId, data = {}) {
  const res = await api.post(`/blood-requests/${requestId}/dispatch`, data)
  return res.data?.data
}

async function getDispatch(requestId) {
  const res = await api.get(`/blood-requests/${requestId}/dispatch`)
  return res.data?.data
}

async function getAllDispatches() {
  const res = await api.get('/dispatches')
  return res.data?.data ?? []
}

async function getDispatchById(id) {
  const res = await api.get(`/dispatches/${id}`)
  return res.data?.data
}

async function confirmReceipt(dispatchId, data = {}) {
  const res = await api.post(`/dispatches/${dispatchId}/confirm-receipt`, data)
  return res.data?.data
}

export const bloodAllocationService = {
  allocateUnits,
  getAllocations,
  dispatchRequest,
  getDispatch,
  getAllDispatches,
  getDispatchById,
  confirmReceipt,
}
