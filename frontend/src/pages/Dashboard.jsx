import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import api from '../services/api'

export default function Dashboard() {
  const { user } = useContext(AuthContext)
  const navigate = useNavigate()

  const [inventory, setInventory] = useState([])
  const [emergencies, setEmergencies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Redirect if not logged in
  useEffect(() => {
    if (!user) {
      navigate('/login')
    }
  }, [user, navigate])

  // Fetch dashboard data
  useEffect(() => {
    if (!user) return
    const fetchData = async () => {
      try {
        const [invRes, emgRes] = await Promise.all([
          api.get('/inventory'),
          api.get('/emergency'),
        ])
        setInventory(invRes.data?.data ?? [])
        setEmergencies(emgRes.data?.data ?? [])
      } catch (err) {
        setError('Failed to load dashboard data. Is the backend running?')
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [user])

  if (!user) return null

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>Welcome to BloodLink Dashboard</h1>
        <p style={styles.subtitle}>
          Hello, <strong>{user.username}</strong>! Role:{' '}
          <span style={styles.roleBadge}>{user.role}</span>
        </p>
      </div>

      {error && <div className="alert alert-error" style={styles.alertBox}>{error}</div>}

      {/* Summary Cards */}
      <div style={styles.cardRow}>
        <SummaryCard icon="🩸" label="Inventory Records" value={loading ? '…' : inventory.length} color="#c0392b" />
        <SummaryCard icon="🚨" label="Emergency Requests" value={loading ? '…' : emergencies.length} color="#e67e22" />
        <SummaryCard
          icon="⚠️"
          label="Pending Emergencies"
          value={loading ? '…' : emergencies.filter(e => e.status === 'PENDING').length}
          color="#e74c3c"
        />
        <SummaryCard
          icon="✅"
          label="Fulfilled Requests"
          value={loading ? '…' : emergencies.filter(e => e.status === 'FULFILLED').length}
          color="#27ae60"
        />
      </div>

      {/* Inventory Table */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>Blood Inventory</h2>
        {loading ? (
          <p style={styles.loadingText}>Loading inventory…</p>
        ) : inventory.length === 0 ? (
          <p style={styles.emptyText}>No inventory records found.</p>
        ) : (
          <div style={styles.tableWrapper}>
            <table style={styles.table}>
              <thead>
                <tr style={styles.tableHead}>
                  <th>ID</th>
                  <th>Blood Type</th>
                  <th>Quantity (ml)</th>
                  <th>Hospital</th>
                  <th>Last Updated</th>
                </tr>
              </thead>
              <tbody>
                {inventory.map((item) => (
                  <tr key={item.id} style={styles.tableRow}>
                    <td>{item.id}</td>
                    <td><span style={styles.bloodBadge}>{item.bloodType}</span></td>
                    <td>{item.quantity}</td>
                    <td>{item.hospital}</td>
                    <td>{item.lastUpdated ? new Date(item.lastUpdated).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Emergency Requests Table */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>Emergency Requests</h2>
        {loading ? (
          <p style={styles.loadingText}>Loading requests…</p>
        ) : emergencies.length === 0 ? (
          <p style={styles.emptyText}>No emergency requests found.</p>
        ) : (
          <div style={styles.tableWrapper}>
            <table style={styles.table}>
              <thead>
                <tr style={styles.tableHead}>
                  <th>ID</th>
                  <th>Blood Type</th>
                  <th>Quantity</th>
                  <th>Hospital</th>
                  <th>Status</th>
                  <th>Created At</th>
                </tr>
              </thead>
              <tbody>
                {emergencies.map((req) => (
                  <tr key={req.id} style={styles.tableRow}>
                    <td>{req.id}</td>
                    <td><span style={styles.bloodBadge}>{req.bloodType}</span></td>
                    <td>{req.quantity}</td>
                    <td>{req.hospital}</td>
                    <td><StatusBadge status={req.status} /></td>
                    <td>{req.createdAt ? new Date(req.createdAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function SummaryCard({ icon, label, value, color }) {
  return (
    <div style={{ ...styles.summaryCard, borderTop: `4px solid ${color}` }}>
      <span style={{ fontSize: '2rem' }}>{icon}</span>
      <span style={{ fontSize: '2rem', fontWeight: '800', color }}>{value}</span>
      <span style={{ fontSize: '0.85rem', color: '#6c757d' }}>{label}</span>
    </div>
  )
}

function StatusBadge({ status }) {
  const colorMap = {
    PENDING: { bg: '#fff3cd', color: '#856404' },
    APPROVED: { bg: '#cce5ff', color: '#004085' },
    FULFILLED: { bg: '#d4edda', color: '#155724' },
    REJECTED: { bg: '#f8d7da', color: '#721c24' },
    CANCELLED: { bg: '#e2e3e5', color: '#383d41' },
  }
  const c = colorMap[status] || { bg: '#eee', color: '#333' }
  return (
    <span style={{
      background: c.bg,
      color: c.color,
      padding: '2px 8px',
      borderRadius: '12px',
      fontSize: '0.8rem',
      fontWeight: '600',
    }}>
      {status}
    </span>
  )
}

const styles = {
  page: { maxWidth: '1100px', margin: '0 auto', padding: '2rem 1rem' },
  header: { marginBottom: '2rem' },
  title: { fontSize: '1.8rem', fontWeight: '800', color: '#2c3e50', marginBottom: '0.4rem' },
  subtitle: { color: '#6c757d', fontSize: '1rem' },
  roleBadge: {
    background: '#c0392b',
    color: '#fff',
    padding: '2px 10px',
    borderRadius: '12px',
    fontSize: '0.8rem',
    fontWeight: '600',
  },
  alertBox: { marginBottom: '1.5rem' },
  cardRow: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' },
  summaryCard: {
    background: '#fff',
    borderRadius: '10px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.4rem',
    alignItems: 'center',
    textAlign: 'center',
  },
  section: { background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', padding: '1.5rem', marginBottom: '1.5rem' },
  sectionTitle: { fontSize: '1.2rem', fontWeight: '700', color: '#2c3e50', marginBottom: '1rem' },
  loadingText: { color: '#6c757d' },
  emptyText: { color: '#aaa', fontStyle: 'italic' },
  tableWrapper: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  tableHead: { background: '#f1f3f5', color: '#495057', textAlign: 'left' },
  tableRow: { borderBottom: '1px solid #dee2e6' },
  bloodBadge: {
    background: '#fdecea',
    color: '#c0392b',
    padding: '2px 8px',
    borderRadius: '10px',
    fontWeight: '700',
    fontSize: '0.8rem',
  },
}
