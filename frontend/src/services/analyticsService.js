import api from './api'

export const analyticsService = {
  // Get dashboard analytics tailored to the user's role and facility
  getDashboardAnalytics: async () => {
    const response = await api.get('/analytics/dashboard')
    return response.data
  }
}

export default analyticsService
