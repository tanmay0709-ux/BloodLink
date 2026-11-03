import React, { useState, useContext } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { authService } from '../services/authService'

// Redirect destination by role after login
function getDashboardPath(role) {
  switch (role) {
    case 'SYSTEM_ADMIN':      return '/dashboard'
    case 'BLOOD_BANK_STAFF':  return '/dashboard'
    case 'HOSPITAL_STAFF':    return '/dashboard'
    default:                  return '/dashboard'
  }
}

export default function Login() {
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const { login } = useContext(AuthContext)
  const navigate = useNavigate()

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.username || !form.password) {
      setError('Please fill in all fields.')
      return
    }

    setLoading(true)
    try {
      const data = await authService.login(form.username, form.password)
      login(data)
      navigate(getDashboardPath(data?.role))
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Invalid username or password.'
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
          <h1 style={styles.title}>Sign in to BloodLink</h1>
          <p style={styles.subtitle}>Enter your credentials to continue</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              name="username"
              type="text"
              autoComplete="username"
              placeholder="your_username"
              value={form.username}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={form.password}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={styles.submitBtn}
            disabled={loading}
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p style={styles.footer}>
          Don&apos;t have an account?{' '}
          <Link to="/register" style={{ color: '#c0392b', fontWeight: 600 }}>
            Register here
          </Link>
        </p>
      </div>
    </div>
  )
}

const styles = {
  page: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 'calc(100vh - 64px)',
    padding: '2rem 1rem',
    background: '#f8f9fa',
  },
  card: {
    background: '#fff',
    borderRadius: '12px',
    boxShadow: '0 8px 24px rgba(0,0,0,0.1)',
    padding: '2.5rem 2rem',
    width: '100%',
    maxWidth: '420px',
  },
  header: { textAlign: 'center', marginBottom: '1.8rem' },
  icon: { fontSize: '3rem', display: 'block', marginBottom: '0.6rem' },
  title: { fontSize: '1.6rem', fontWeight: '700', color: '#2c3e50', marginBottom: '0.3rem' },
  subtitle: { color: '#6c757d', fontSize: '0.9rem' },
  submitBtn: { width: '100%', marginTop: '0.5rem', padding: '0.75rem' },
  footer: { textAlign: 'center', marginTop: '1.5rem', fontSize: '0.9rem', color: '#6c757d' },
}
