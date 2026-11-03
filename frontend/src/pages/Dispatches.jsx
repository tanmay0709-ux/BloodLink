import React, { useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { bloodAllocationService } from '../services/bloodAllocationService'

export default function Dispatches() {
  const { user } = useContext(AuthContext)
  const [dispatches, setDispatches] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadDispatches()
  }, [user])

  const loadDispatches = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await bloodAllocationService.getAllDispatches()
      setDispatches(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dispatches')
    } finally {
      setLoading(false)
    }
  }

  const inTransitCount = dispatches.filter((d) => d.status === 'DISPATCHED').length
  const deliveredCount = dispatches.filter((d) => d.status === 'DELIVERED').length

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <div>
          <h1 style={styles.title}>🚚 Blood Dispatches & Shipments</h1>
          <p style={styles.subtitle}>
            Track blood unit shipments in transit, chain-of-custody, and hospital receipt confirmations
          </p>
        </div>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}

      {/* Summary Cards */}
      <div style={styles.summaryCardRow}>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #e67e22' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#e67e22' }}>
            {inTransitCount}
          </span>
          <span style={styles.summaryLabel}>In Transit (Dispatched)</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #27ae60' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#27ae60' }}>
            {deliveredCount}
          </span>
          <span style={styles.summaryLabel}>Delivered & Received</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #2980b9' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#2980b9' }}>
            {dispatches.length}
          </span>
          <span style={styles.summaryLabel}>Total Shipments Tracked</span>
        </div>
      </div>

      {/* Dispatches Table */}
      {loading ? (
        <p style={{ color: '#6c757d' }}>Loading shipments…</p>
      ) : dispatches.length === 0 ? (
        <div style={styles.emptyCard}>
          <p style={{ color: '#888', fontStyle: 'italic', margin: 0 }}>
            No blood unit dispatch shipments recorded yet.
          </p>
        </div>
      ) : (
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.tableHead}>
                <th>Reference #</th>
                <th>Request ID</th>
                <th>Origin Blood Bank</th>
                <th>Destination Hospital</th>
                <th>Units</th>
                <th>Dispatch Date</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {dispatches.map((d) => (
                <tr key={d.id} style={styles.tableRow}>
                  <td><strong>{d.dispatchReference}</strong></td>
                  <td>#{d.requestId}</td>
                  <td>{d.bloodBankName}</td>
                  <td>{d.hospitalName}</td>
                  <td>{d.units?.length || 0} unit(s)</td>
                  <td>{d.dispatchDate}</td>
                  <td>
                    <span style={{
                      padding: '3px 8px',
                      borderRadius: '8px',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      background: d.status === 'DELIVERED' ? '#d4edda' : '#ffeeba',
                      color: d.status === 'DELIVERED' ? '#155724' : '#856404',
                    }}>
                      {d.status}
                    </span>
                  </td>
                  <td>
                    <Link
                      to={`/blood-requests/${d.requestId}`}
                      className="btn btn-primary"
                      style={{ fontSize: '0.8rem', padding: '4px 10px', textDecoration: 'none' }}
                    >
                      View Details
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

const styles = {
  page: { width: '94%', maxWidth: '1440px', margin: '0 auto', padding: '1.5rem 0' },
  headerRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' },
  title: { fontSize: '1.8rem', fontWeight: '800', color: '#1a202c', margin: 0 },
  subtitle: { color: '#6c757d', fontSize: '0.92rem', margin: '0.2rem 0 0 0' },
  summaryCardRow: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' },
  summaryCard: {
    background: '#fff',
    borderRadius: '10px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
    border: '1px solid #f1f3f5',
    padding: '1.25rem 1.5rem',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
  },
  summaryLabel: { fontSize: '0.8rem', color: '#6c757d', marginTop: '0.25rem', fontWeight: 600 },
  tableWrapper: { overflowX: 'auto', WebkitOverflowScrolling: 'touch', background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', padding: '1.25rem' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  tableHead: { background: '#f8f9fa', color: '#495057', textAlign: 'left', borderBottom: '2px solid #dee2e6' },
  tableRow: { borderBottom: '1px solid #f1f3f5' },
  emptyCard: { background: '#fff', padding: '2.5rem', textAlign: 'center', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5' },
}
