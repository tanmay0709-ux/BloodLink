import React, { useState, useEffect, useContext } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { authService } from '../services/authService'

const ROLES = [
  { value: 'HOSPITAL_STAFF',   label: 'Hospital Staff' },
  { value: 'BLOOD_BANK_STAFF', label: 'Blood Bank Staff' },
  { value: 'SYSTEM_ADMIN',     label: 'System Admin' },
]

export default function Register() {
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'HOSPITAL_STAFF',
    hospitalId: '',
    bloodBankId: '',
  })
  const [hospitals, setHospitals] = useState([])
  const [hospitalsLoading, setHospitalsLoading] = useState(false)
  const [bloodBanks, setBloodBanks] = useState([])
  const [bloodBanksLoading, setBloodBanksLoading] = useState(false)
  const [error, setError]     = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const { login } = useContext(AuthContext)
  const navigate  = useNavigate()

  useEffect(() => {
    const fetchFacilities = async () => {
      setHospitalsLoading(true)
      setBloodBanksLoading(true)
      try {
        const [hList, bList] = await Promise.all([
          authService.getActiveHospitals(),
          authService.getActiveBloodBanks(),
        ])
        setHospitals(hList)
        setBloodBanks(bList)
      } catch (err) {
        console.error('Failed to load active facilities', err)
      } finally {
        setHospitalsLoading(false)
        setBloodBanksLoading(false)
      }
    }
    fetchFacilities()
  }, [])

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
    setSuccess('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!form.username || !form.email || !form.password || !form.confirmPassword) {
      setError('Please fill in all fields.')
      return
    }
    if (form.role === 'HOSPITAL_STAFF' && !form.hospitalId) {
      setError('Please select a hospital for hospital staff registration.')
      return
    }
    if (form.role === 'BLOOD_BANK_STAFF' && !form.bloodBankId) {
      setError('Please select a blood bank for blood bank staff registration.')
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters.')
      return
    }

    setLoading(true)
    try {
      const data = await authService.register(
        form.username,
        form.email,
        form.password,
        form.role,
        form.role === 'HOSPITAL_STAFF' ? form.hospitalId : null,
        form.role === 'BLOOD_BANK_STAFF' ? form.bloodBankId : null
      )
      if (form.role === 'HOSPITAL_STAFF') {
        setSuccess(`Account created! Your association request for ${data.pendingHospitalName || 'the selected hospital'} is pending administrator approval.`)
      } else if (form.role === 'BLOOD_BANK_STAFF') {
        setSuccess(`Account created! Your association request for ${data.pendingBloodBankName || 'the selected blood bank'} is pending administrator approval.`)
      } else {
        setSuccess(`Account created! Welcome, ${data.username}.`)
      }
      login(data)
      setTimeout(() => navigate('/dashboard'), 1200)
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Registration failed. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <div style={styles.header}>
          <span style={styles.icon}>🩸</span>
          <h1 style={styles.title}>Create an Account</h1>
          <p style={styles.subtitle}>Join BloodLink to manage blood bank inventory</p>
        </div>

        {error   && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username" name="username" type="text"
              autoComplete="username" placeholder="your_username"
              value={form.username} onChange={handleChange} disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email" name="email" type="email"
              autoComplete="email" placeholder="you@example.com"
              value={form.email} onChange={handleChange} disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="role">Role</label>
            <select id="role" name="role" value={form.role} onChange={handleChange} disabled={loading}
              style={styles.select}>
              {ROLES.map(r => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>

          {form.role === 'HOSPITAL_STAFF' && (
            <div className="form-group">
              <label htmlFor="hospitalId">Hospital *</label>
              {hospitalsLoading ? (
                <p style={{ fontSize: '0.85rem', color: '#6c757d', margin: '0.5rem 0' }}>Loading available hospitals…</p>
              ) : (
                <select
                  id="hospitalId"
                  name="hospitalId"
                  value={form.hospitalId}
                  onChange={handleChange}
                  disabled={loading}
                  style={styles.select}
                  required
                >
                  <option value="">-- Select an active hospital --</option>
                  {hospitals.map(h => (
                    <option key={h.id} value={h.id}>
                      {h.name} {h.address ? `(${h.address})` : ''}
                    </option>
                  ))}
                </select>
              )}
              <small style={{ color: '#6c757d', fontSize: '0.8rem', display: 'block', marginTop: '0.35rem' }}>
                Note: Registering as Hospital Staff creates a pending request. An administrator must approve your association before hospital access is granted.
              </small>
            </div>
          )}

          {form.role === 'BLOOD_BANK_STAFF' && (
            <div className="form-group">
              <label htmlFor="bloodBankId">Blood Bank *</label>
              {bloodBanksLoading ? (
                <p style={{ fontSize: '0.85rem', color: '#6c757d', margin: '0.5rem 0' }}>Loading available blood banks…</p>
              ) : (
                <select
                  id="bloodBankId"
                  name="bloodBankId"
                  value={form.bloodBankId}
                  onChange={handleChange}
                  disabled={loading}
                  style={styles.select}
                  required
                >
                  <option value="">-- Select an active blood bank --</option>
                  {bloodBanks.map(b => (
                    <option key={b.id} value={b.id}>
                      {b.name} {b.address ? `(${b.address})` : ''}
                    </option>
                  ))}
                </select>
              )}
              <small style={{ color: '#6c757d', fontSize: '0.8rem', display: 'block', marginTop: '0.35rem' }}>
                Note: Registering as Blood Bank Staff creates a pending request. An administrator must approve your association before blood bank access is granted.
              </small>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password" name="password" type="password"
              autoComplete="new-password" placeholder="Min. 6 characters"
              value={form.password} onChange={handleChange} disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input
              id="confirmPassword" name="confirmPassword" type="password"
              autoComplete="new-password" placeholder="Repeat password"
              value={form.confirmPassword} onChange={handleChange} disabled={loading}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={loading}>
            {loading ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <p style={styles.footer}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: '#c0392b', fontWeight: 600 }}>Sign in</Link>
        </p>
      </div>
    </div>
  )
}

const styles = {
  page: {
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    minHeight: 'calc(100vh - 64px)', padding: '2rem 1rem', background: '#f8f9fa',
  },
  card: {
    background: '#fff', borderRadius: '12px',
    boxShadow: '0 8px 24px rgba(0,0,0,0.1)',
    padding: '2.5rem 2rem', width: '100%', maxWidth: '440px',
  },
  header: { textAlign: 'center', marginBottom: '1.8rem' },
  icon:   { fontSize: '3rem', display: 'block', marginBottom: '0.6rem' },
  title:  { fontSize: '1.6rem', fontWeight: '700', color: '#2c3e50', marginBottom: '0.3rem' },
  subtitle: { color: '#6c757d', fontSize: '0.9rem' },
  select: {
    width: '100%', padding: '0.5rem 0.75rem', borderRadius: '6px',
    border: '1px solid #ced4da', fontSize: '0.95rem', color: '#495057',
  },
  submitBtn: { width: '100%', marginTop: '0.5rem', padding: '0.75rem' },
  footer: { textAlign: 'center', marginTop: '1.5rem', fontSize: '0.9rem', color: '#6c757d' },
}
