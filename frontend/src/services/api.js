import axios from 'axios'

/**
 * Axios instance pre-configured with base URL and a request interceptor
 * that attaches the JWT Bearer token from localStorage on every request.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
})

// ─── Request Interceptor ─────────────────────────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('bloodlink_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ─── Response Interceptor ─────────────────────────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Auto-clear token on 401 Unauthorized
    if (error.response?.status === 401) {
      localStorage.removeItem('bloodlink_token')
      localStorage.removeItem('bloodlink_user')
      // Optionally redirect to login
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
