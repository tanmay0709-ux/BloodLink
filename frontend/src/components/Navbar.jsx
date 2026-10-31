import React, { useContext } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useContext(AuthContext)
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <nav style={styles.nav}>
      <div style={styles.brand}>
        <Link to="/" style={styles.brandLink}>
          🩸 BloodLink
        </Link>
      </div>

      <ul style={styles.links}>
        <li>
          <Link to="/" style={styles.link}>Home</Link>
        </li>

        {user ? (
          <>
            <li>
              <Link to="/dashboard" style={styles.link}>Dashboard</Link>
            </li>
            <li>
              <span style={styles.username}>👤 {user.username}</span>
            </li>
            <li>
              <button onClick={handleLogout} style={styles.logoutBtn}>
                Logout
              </button>
            </li>
          </>
        ) : (
          <>
            <li>
              <Link to="/login" style={styles.link}>Login</Link>
            </li>
            <li>
              <Link to="/register" style={styles.registerLink}>Register</Link>
            </li>
          </>
        )}
      </ul>
    </nav>
  )
}

const styles = {
  nav: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 2rem',
    height: '64px',
    background: '#c0392b',
    color: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
    position: 'sticky',
    top: 0,
    zIndex: 1000,
  },
  brand: {
    fontSize: '1.4rem',
    fontWeight: '700',
  },
  brandLink: {
    color: '#fff',
    textDecoration: 'none',
    letterSpacing: '0.5px',
  },
  links: {
    display: 'flex',
    alignItems: 'center',
    gap: '1.5rem',
    listStyle: 'none',
    margin: 0,
    padding: 0,
  },
  link: {
    color: 'rgba(255,255,255,0.9)',
    textDecoration: 'none',
    fontWeight: '500',
    fontSize: '0.95rem',
    transition: 'color 0.2s',
  },
  registerLink: {
    color: '#fff',
    background: 'rgba(255,255,255,0.2)',
    padding: '0.35rem 0.9rem',
    borderRadius: '6px',
    textDecoration: 'none',
    fontWeight: '600',
    fontSize: '0.9rem',
  },
  username: {
    color: 'rgba(255,255,255,0.85)',
    fontSize: '0.9rem',
  },
  logoutBtn: {
    background: 'rgba(255,255,255,0.15)',
    border: '1.5px solid rgba(255,255,255,0.5)',
    color: '#fff',
    padding: '0.35rem 0.9rem',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: '600',
    fontSize: '0.9rem',
    transition: 'background 0.2s',
  },
}
