import React, { createContext, useState, useCallback, useMemo } from 'react'
import { authService } from '../services/authService'

export const AuthContext = createContext(null)

/**
 * AuthProvider wraps the app and exposes { user, login, logout } via context.
 *
 * - `user`   : the current user object { username, email, role } or null
 * - `login`  : function(data) — stores JWT response data and updates state
 * - `logout` : function() — clears auth state and localStorage
 */
export function AuthProvider({ children }) {
  // Initialise from localStorage so auth persists across page refreshes
  const [user, setUser] = useState(() => authService.getCurrentUser())

  const login = useCallback((data) => {
    // `data` is the JwtResponse object returned by authService.login / .register
    if (data) {
      setUser({
        id: data.id,
        username: data.username,
        email: data.email,
        role: data.role,
        approvalStatus: data.approvalStatus,
        assignedHospitalId: data.assignedHospitalId,
        assignedHospitalName: data.assignedHospitalName,
        pendingHospitalId: data.pendingHospitalId,
        pendingHospitalName: data.pendingHospitalName,
        assignedBloodBankId: data.assignedBloodBankId,
        assignedBloodBankName: data.assignedBloodBankName,
        pendingBloodBankId: data.pendingBloodBankId,
        pendingBloodBankName: data.pendingBloodBankName,
      })
    }
  }, [])

  const logout = useCallback(() => {
    authService.logout()
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
