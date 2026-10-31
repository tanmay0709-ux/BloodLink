import React from 'react'
import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <span style={styles.code}>404</span>
        <h1 style={styles.title}>Page Not Found</h1>
        <p style={styles.desc}>
          Oops! The page you&apos;re looking for doesn&apos;t exist or has been moved.
        </p>
        <Link to="/" className="btn btn-primary" style={styles.homeBtn}>
          ← Back to Home
        </Link>
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
    padding: '2rem',
    background: '#f8f9fa',
  },
  card: {
    background: '#fff',
    borderRadius: '12px',
    boxShadow: '0 8px 24px rgba(0,0,0,0.1)',
    padding: '3rem 2.5rem',
    textAlign: 'center',
    maxWidth: '420px',
    width: '100%',
  },
  code: {
    fontSize: '5rem',
    fontWeight: '900',
    color: '#c0392b',
    display: 'block',
    lineHeight: 1,
    marginBottom: '0.5rem',
  },
  title: {
    fontSize: '1.5rem',
    fontWeight: '700',
    color: '#2c3e50',
    marginBottom: '0.8rem',
  },
  desc: {
    color: '#6c757d',
    fontSize: '0.95rem',
    marginBottom: '2rem',
    lineHeight: 1.6,
  },
  homeBtn: {
    textDecoration: 'none',
    display: 'inline-block',
    padding: '0.65rem 1.5rem',
  },
}
