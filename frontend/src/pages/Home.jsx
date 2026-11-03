import React from 'react'
import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <div style={styles.page}>
      {/* Hero */}
      <section style={styles.hero}>
        <div style={styles.heroContent}>
          <h1 style={styles.heroTitle}>🩸 BloodLink</h1>
          <p style={styles.heroSubtitle}>
            Hospital Blood Bank Management &amp; Emergency Blood Distribution System
          </p>
          <p style={styles.heroDesc}>
            BloodLink connects hospitals and blood banks in real time, enabling
            efficient inventory tracking and rapid response to emergency blood
            requests — so the right blood reaches the right patient at the right time.
          </p>
          <div style={styles.heroActions}>
            <Link to="/register" className="btn btn-primary" style={styles.ctaBtn}>
              Get Started
            </Link>
            <Link to="/login" className="btn btn-outline" style={styles.ctaBtnOutline}>
              Sign In
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section style={styles.features}>
        <h2 style={styles.sectionTitle}>Key Features</h2>
        <div style={styles.featureGrid}>
          {FEATURES.map((f) => (
            <div key={f.title} style={styles.featureCard}>
              <span style={styles.featureIcon}>{f.icon}</span>
              <h3 style={styles.featureTitle}>{f.title}</h3>
              <p style={styles.featureDesc}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Stats */}
      <section style={styles.stats}>
        {STATS.map((s) => (
          <div key={s.label} style={styles.statCard}>
            <span style={styles.statValue}>{s.value}</span>
            <span style={styles.statLabel}>{s.label}</span>
          </div>
        ))}
      </section>
    </div>
  )
}

const FEATURES = [
  {
    icon: '🏥',
    title: 'Multi-Hospital Management',
    desc: 'Manage blood inventory across multiple hospitals from a single unified dashboard.',
  },
  {
    icon: '🚨',
    title: 'Emergency Requests',
    desc: 'Create and track emergency blood requests with real-time status updates.',
  },
  {
    icon: '📊',
    title: 'Inventory Analytics',
    desc: 'Monitor stock levels, low-blood alerts, and historical usage trends.',
  },
  {
    icon: '🔐',
    title: 'Secure & Role-Based',
    desc: 'JWT-secured access with roles for admins, hospital staff, and blood bank managers.',
  },
]

const STATS = [
  { value: '8', label: 'Blood Types Tracked' },
  { value: '24/7', label: 'Real-Time Monitoring' },
  { value: '<1s', label: 'Emergency Alert Time' },
  { value: '100%', label: 'Open Source' },
]

const styles = {
  page: { fontFamily: 'inherit' },
  hero: {
    background: 'linear-gradient(135deg, #c0392b 0%, #922b21 100%)',
    color: '#fff',
    padding: '5rem 2rem',
    textAlign: 'center',
  },
  heroContent: { maxWidth: '850px', margin: '0 auto' },
  heroTitle: { fontSize: '3.5rem', fontWeight: '800', marginBottom: '0.5rem' },
  heroSubtitle: { fontSize: '1.25rem', opacity: 0.9, marginBottom: '1.2rem', fontWeight: '500' },
  heroDesc: { fontSize: '1.05rem', opacity: 0.85, lineHeight: 1.8, marginBottom: '2rem' },
  heroActions: { display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' },
  ctaBtn: {
    background: '#fff',
    color: '#c0392b',
    padding: '0.85rem 2.25rem',
    borderRadius: '8px',
    fontWeight: '700',
    fontSize: '1rem',
    textDecoration: 'none',
    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
  },
  ctaBtnOutline: {
    background: 'transparent',
    border: '2px solid #fff',
    color: '#fff',
    padding: '0.85rem 2.25rem',
    borderRadius: '8px',
    fontWeight: '700',
    fontSize: '1rem',
    textDecoration: 'none',
  },
  features: { padding: '4.5rem 1rem', width: '94%', maxWidth: '1400px', margin: '0 auto' },
  sectionTitle: {
    textAlign: 'center',
    fontSize: '2rem',
    fontWeight: '700',
    marginBottom: '2.5rem',
    color: '#2c3e50',
  },
  featureGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
    gap: '1.75rem',
  },
  featureCard: {
    background: '#fff',
    borderRadius: '12px',
    padding: '2.25rem 1.75rem',
    boxShadow: '0 4px 14px rgba(0,0,0,0.06)',
    border: '1px solid #eef0f2',
    textAlign: 'center',
  },
  featureIcon: { fontSize: '2.5rem', display: 'block', marginBottom: '1rem' },
  featureTitle: { fontSize: '1.1rem', fontWeight: '700', marginBottom: '0.6rem', color: '#2c3e50' },
  featureDesc: { fontSize: '0.9rem', color: '#6c757d', lineHeight: 1.6 },
  stats: {
    background: '#2c3e50',
    color: '#fff',
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
    gap: '0',
    padding: '3rem 2rem',
  },
  statCard: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '1rem 3rem',
    borderRight: '1px solid rgba(255,255,255,0.15)',
  },
  statValue: { fontSize: '2.5rem', fontWeight: '800', color: '#e74c3c' },
  statLabel: { fontSize: '0.9rem', opacity: 0.7, marginTop: '0.25rem' },
}
