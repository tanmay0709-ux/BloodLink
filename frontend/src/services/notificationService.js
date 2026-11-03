import api from './api'

export const notificationService = {
  // Get notifications (unreadOnly boolean, limit number)
  getNotifications: async (unreadOnly = false, limit = 10) => {
    const response = await api.get('/notifications', {
      params: { unreadOnly, limit }
    })
    return response.data
  },

  // Get unread notification count
  getUnreadCount: async () => {
    const response = await api.get('/notifications/unread-count')
    return response.data
  },

  // Mark a single notification as read
  markAsRead: async (id) => {
    const response = await api.patch(`/notifications/${id}/read`)
    return response.data
  },

  // Mark all notifications as read
  markAllAsRead: async () => {
    const response = await api.patch('/notifications/mark-all-read')
    return response.data
  }
}

export default notificationService
