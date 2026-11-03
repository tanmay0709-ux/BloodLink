import api from './api'

const TOKEN_KEY = 'bloodlink_token'
const USER_KEY = 'bloodlink_user'

/**
 * Authenticate a user and store the JWT + user info in localStorage.
 * @param {string} username
 * @param {string} password
 * @returns {Promise<object>} JwtResponse data object
 */
async function login(username, password) {
  const response = await api.post('/auth/login', { username, password })
  const result = response.data?.data

  if (result?.token) {
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify({
      id: result.id,
      username: result.username,
      email: result.email,
      role: result.role,
      approvalStatus: result.approvalStatus,
      assignedHospitalId: result.assignedHospitalId,
      assignedHospitalName: result.assignedHospitalName,
      pendingHospitalId: result.pendingHospitalId,
      pendingHospitalName: result.pendingHospitalName,
      assignedBloodBankId: result.assignedBloodBankId,
      assignedBloodBankName: result.assignedBloodBankName,
      pendingBloodBankId: result.pendingBloodBankId,
      pendingBloodBankName: result.pendingBloodBankName,
    }))
  }

  return result
}

/**
 * Register a new user account.
 * @param {string} username
 * @param {string} email
 * @param {string} password
 * @param {string} role
 * @param {number|string|null} hospitalId
 * @param {number|string|null} bloodBankId
 * @returns {Promise<object>} JwtResponse data object
 */
async function register(username, email, password, role = 'HOSPITAL_STAFF', hospitalId = null, bloodBankId = null) {
  const payload = { username, email, password, role }
  if (hospitalId) {
    payload.hospitalId = Number(hospitalId)
  }
  if (bloodBankId) {
    payload.bloodBankId = Number(bloodBankId)
  }
  const response = await api.post('/auth/register', payload)
  const result = response.data?.data

  if (result?.token) {
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify({
      id: result.id,
      username: result.username,
      email: result.email,
      role: result.role,
      approvalStatus: result.approvalStatus,
      assignedHospitalId: result.assignedHospitalId,
      assignedHospitalName: result.assignedHospitalName,
      pendingHospitalId: result.pendingHospitalId,
      pendingHospitalName: result.pendingHospitalName,
      assignedBloodBankId: result.assignedBloodBankId,
      assignedBloodBankName: result.assignedBloodBankName,
      pendingBloodBankId: result.pendingBloodBankId,
      pendingBloodBankName: result.pendingBloodBankName,
    }))
  }

  return result
}

/**
 * Get active hospitals for registration selection dropdown (public).
 */
async function getActiveHospitals() {
  const response = await api.get('/auth/hospitals')
  return response.data?.data ?? []
}

/**
 * Get active blood banks for registration selection dropdown (public).
 */
async function getActiveBloodBanks() {
  const response = await api.get('/auth/blood-banks')
  return response.data?.data ?? []
}

/**
 * Clear stored session data (logout).
 */
function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/**
 * Retrieve the currently stored user object (or null).
 * @returns {object|null}
 */
function getCurrentUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * Retrieve the stored JWT token (or null).
 * @returns {string|null}
 */
function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export const authService = { login, register, logout, getCurrentUser, getToken, getActiveHospitals, getActiveBloodBanks }
