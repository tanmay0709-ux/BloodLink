import React, { useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { bloodRequestService } from '../services/bloodRequestService'
import { hospitalService } from '../services/hospitalService'

const BLOOD_TYPES = [
  { value: 'A_POSITIVE', label: 'A+' },
  { value: 'A_NEGATIVE', label: 'A-' },
  { value: 'B_POSITIVE', label: 'B+' },
  { value: 'B_NEGATIVE', label: 'B-' },
  { value: 'AB_POSITIVE', label: 'AB+' },
  { value: 'AB_NEGATIVE', label: 'AB-' },
  { value: 'O_POSITIVE', label: 'O+' },
  { value: 'O_NEGATIVE', label: 'O-' },
]

const COMPONENT_TYPES = [
  { value: 'RED_BLOOD_CELLS', label: 'Packed Red Blood Cells (PRBC)' },
  { value: 'WHOLE_BLOOD', label: 'Whole Blood' },
  { value: 'PLATELETS', label: 'Platelet Concentrate' },
  { value: 'PLASMA', label: 'Fresh Frozen Plasma (FFP)' },
  { value: 'CRYOPRECIPITATE', label: 'Cryoprecipitate' },
]

const PRIORITIES = ['ROUTINE', 'URGENT', 'EMERGENCY']
const STATUSES = ['PENDING', 'REVIEWED', 'CANCELLED', 'REJECTED']

export default function BloodRequests() {
  const { user } = useContext(AuthContext)
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const isHospitalStaff = user?.role === 'HOSPITAL_STAFF'
  const isPendingStaff = isHospitalStaff && (user?.approvalStatus === 'PENDING' || !user?.assignedHospitalId)
  const isApprovedStaff = isHospitalStaff && !isPendingStaff
  const isBloodBankStaff = user?.role === 'BLOOD_BANK_STAFF'
  const isPendingBankStaff = isBloodBankStaff && (user?.approvalStatus === 'PENDING' || !user?.assignedBloodBankId)
  const canCreate = (isApprovedStaff && user?.assignedHospitalId) || isAdmin

  const [requests, setRequests] = useState([])
  const [hospitals, setHospitals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Filters
  const [filterPriority, setFilterPriority] = useState('')
  const [filterStatus, setFilterStatus] = useState('')

  // Create form modal
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({
    bloodType: 'O_POSITIVE',
    componentType: 'RED_BLOOD_CELLS',
    quantity: 1,
    priority: 'ROUTINE',
    requiredDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    clinicalReason: '',
    hospitalId: '',
  })

  useEffect(() => {
    loadData()
  }, [user])

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await bloodRequestService.getAll()
      setRequests(data)

      if (isAdmin) {
        const hList = await hospitalService.getAll()
        setHospitals(hList.filter((h) => h.active))
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load blood requests')
    } finally {
      setLoading(false)
    }
  }

  const handleFilterSubmit = async (e) => {
    if (e) e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const params = {}
      if (filterPriority) params.priority = filterPriority
      if (filterStatus) params.status = filterStatus
      const data = await bloodRequestService.getAll(params)
      setRequests(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Filter failed')
    } finally {
      setLoading(false)
    }
  }

  const handleResetFilters = async () => {
    setFilterPriority('')
    setFilterStatus('')
    setLoading(true)
    try {
      const data = await bloodRequestService.getAll()
      setRequests(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Reset failed')
    } finally {
      setLoading(false)
    }
  }

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const payload = {
        ...form,
        quantity: Number(form.quantity),
        hospitalId: form.hospitalId ? Number(form.hospitalId) : undefined,
      }
      await bloodRequestService.create(payload)
      setSuccess('Blood request submitted successfully!')
      setShowCreateModal(false)
      setForm({
        bloodType: 'O_POSITIVE',
        componentType: 'RED_BLOOD_CELLS',
        quantity: 1,
        priority: 'ROUTINE',
        requiredDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
        clinicalReason: '',
        hospitalId: '',
      })
      loadData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit request')
    } finally {
      setSubmitting(false)
    }
  }

  const getPriorityBadgeStyle = (priority) => {
    switch (priority) {
      case 'EMERGENCY':
        return { bg: '#c0392b', color: '#fff', border: '1px solid #962d22' }
      case 'URGENT':
        return { bg: '#e67e22', color: '#fff', border: '1px solid #d35400' }
      case 'ROUTINE':
      default:
        return { bg: '#2980b9', color: '#fff', border: '1px solid #1f618d' }
    }
  }

  const getStatusBadgeStyle = (status) => {
    switch (status) {
      case 'PENDING':
        return { bg: '#fff3cd', color: '#856404' }
      case 'REVIEWED':
        return { bg: '#d4edda', color: '#155724' }
      case 'CANCELLED':
        return { bg: '#e2e3e5', color: '#383d41' }
      case 'REJECTED':
        return { bg: '#f8d7da', color: '#721c24' }
      default:
        return { bg: '#eee', color: '#333' }
    }
  }

  const getBloodTypeLabel = (val) => {
    const item = BLOOD_TYPES.find((b) => b.value === val)
    return item ? item.label : val
  }

  const pendingCount = requests.filter((r) => r.status === 'PENDING').length
  const emergencyCount = requests.filter((r) => r.priority === 'EMERGENCY').length
  const urgentCount = requests.filter((r) => r.priority === 'URGENT').length

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <div>
          <h1 style={styles.title}>📋 Blood Requests</h1>
          <p style={styles.subtitle}>
            Submit, track, and review clinical blood component requests and matching inventory
          </p>
        </div>
        {canCreate && !showCreateModal && (
          <button
            className="btn btn-primary"
            onClick={() => {
              setShowCreateModal(true)
              setError('')
              setSuccess('')
            }}
          >
            + Create Blood Request
          </button>
        )}
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '1rem' }}>{success}</div>}

      {isPendingStaff && (
        <div style={{ background: '#fff3cd', color: '#856404', border: '1px solid #ffeeba', marginBottom: '1.25rem', padding: '1rem 1.25rem', borderRadius: '8px', fontSize: '0.95rem' }}>
          <strong>⏳ Association Pending Approval:</strong> Your request to associate with <strong>{user?.pendingHospitalName || 'the selected hospital'}</strong> is awaiting administrator approval. You will be able to create and manage hospital requests once approved.
        </div>
      )}

      {isPendingBankStaff && (
        <div style={{ background: '#fff3cd', color: '#856404', border: '1px solid #ffeeba', marginBottom: '1.25rem', padding: '1rem 1.25rem', borderRadius: '8px', fontSize: '0.95rem' }}>
          <strong>⏳ Association Pending Approval:</strong> Your request to associate with <strong>{user?.pendingBloodBankName || 'the selected blood bank'}</strong> is awaiting administrator approval. You will be able to review requests and manage allocations once approved.
        </div>
      )}

      {/* Summary Cards */}
      <div style={styles.summaryCardRow}>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #c0392b' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#c0392b' }}>
            {emergencyCount}
          </span>
          <span style={styles.summaryLabel}>Emergency Priority</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #e67e22' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#e67e22' }}>
            {urgentCount}
          </span>
          <span style={styles.summaryLabel}>Urgent Priority</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #f39c12' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#f39c12' }}>
            {pendingCount}
          </span>
          <span style={styles.summaryLabel}>Pending Requests</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #27ae60' }}>
          <span style={{ fontSize: '1.8rem', fontWeight: '800', color: '#27ae60' }}>
            {requests.length}
          </span>
          <span style={styles.summaryLabel}>Total Visible</span>
        </div>
      </div>

      {/* Create Request Modal */}
      {showCreateModal && canCreate && (
        <div style={styles.formCard}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>Create New Blood Request</h3>
            <button className="btn" onClick={() => setShowCreateModal(false)}>Cancel</button>
          </div>

          <form onSubmit={handleCreateSubmit}>
            <div style={styles.grid2}>
              {isAdmin && (
                <div className="form-group" style={{ gridColumn: 'span 2' }}>
                  <label>Requesting Hospital *</label>
                  <select
                    value={form.hospitalId}
                    onChange={(e) => setForm({ ...form, hospitalId: e.target.value })}
                    style={styles.selectInput}
                    required
                  >
                    <option value="">-- Select Active Hospital --</option>
                    {hospitals.map((h) => (
                      <option key={h.id} value={h.id}>{h.name}</option>
                    ))}
                  </select>
                </div>
              )}

              <div className="form-group">
                <label>Recipient Blood Group *</label>
                <select
                  value={form.bloodType}
                  onChange={(e) => setForm({ ...form, bloodType: e.target.value })}
                  style={styles.selectInput}
                  required
                >
                  {BLOOD_TYPES.map((b) => (
                    <option key={b.value} value={b.value}>{b.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Component Type *</label>
                <select
                  value={form.componentType}
                  onChange={(e) => setForm({ ...form, componentType: e.target.value })}
                  style={styles.selectInput}
                  required
                >
                  {COMPONENT_TYPES.map((c) => (
                    <option key={c.value} value={c.value}>{c.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Quantity (Units) *</label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  required
                  value={form.quantity}
                  onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Priority Level *</label>
                <select
                  value={form.priority}
                  onChange={(e) => setForm({ ...form, priority: e.target.value })}
                  style={styles.selectInput}
                  required
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Required Date *</label>
                <input
                  type="date"
                  required
                  value={form.requiredDate}
                  onChange={(e) => setForm({ ...form, requiredDate: e.target.value })}
                />
              </div>

              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Clinical Reason / Diagnosis Notes</label>
                <textarea
                  rows="3"
                  maxLength={500}
                  placeholder="e.g. Scheduled cardiac bypass surgery, severe acute anemia..."
                  value={form.clinicalReason}
                  onChange={(e) => setForm({ ...form, clinicalReason: e.target.value })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit Blood Request'}
              </button>
              <button type="button" className="btn" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Filter Bar */}
      <div style={styles.searchBar}>
        <form onSubmit={handleFilterSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <select
            value={filterPriority}
            onChange={(e) => setFilterPriority(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
          >
            <option value="">All Priorities</option>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
          >
            <option value="">All Statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>

          <button type="submit" className="btn btn-primary">Filter</button>
          {(filterPriority || filterStatus) && (
            <button type="button" className="btn" onClick={handleResetFilters}>Reset</button>
          )}
        </form>
      </div>

      {/* Request Table */}
      {loading ? (
        <p style={{ color: '#6c757d' }}>Loading requests…</p>
      ) : requests.length === 0 ? (
        <div style={styles.emptyCard}>
          <p style={{ color: '#888', fontStyle: 'italic', margin: 0 }}>
            No blood requests found.
          </p>
        </div>
      ) : (
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.tableHead}>
                <th>ID</th>
                <th>Hospital</th>
                <th>Blood Group</th>
                <th>Component</th>
                <th>Units</th>
                <th>Priority</th>
                <th>Required Date</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((r) => {
                const pStyle = getPriorityBadgeStyle(r.priority)
                const sStyle = getStatusBadgeStyle(r.status)
                return (
                  <tr key={r.id} style={styles.tableRow}>
                    <td>#{r.id}</td>
                    <td><strong>{r.hospitalName}</strong></td>
                    <td><span style={styles.bloodBadge}>{getBloodTypeLabel(r.bloodType)}</span></td>
                    <td>{r.componentDisplayName || r.componentType}</td>
                    <td>{r.quantity} {r.quantity === 1 ? 'unit' : 'units'}</td>
                    <td>
                      <span style={{
                        background: pStyle.bg,
                        color: pStyle.color,
                        border: pStyle.border,
                        padding: '3px 8px',
                        borderRadius: '6px',
                        fontSize: '0.75rem',
                        fontWeight: '700',
                      }}>
                        {r.priority}
                      </span>
                    </td>
                    <td>{r.requiredDate}</td>
                    <td>
                      <span style={{
                        background: sStyle.bg,
                        color: sStyle.color,
                        padding: '3px 8px',
                        borderRadius: '10px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                      }}>
                        {r.status}
                      </span>
                    </td>
                    <td>
                      <Link
                        to={`/blood-requests/${r.id}`}
                        className="btn btn-primary"
                        style={{ fontSize: '0.8rem', padding: '4px 10px', textDecoration: 'none' }}
                      >
                        View Details
                      </Link>
                    </td>
                  </tr>
                )
              })}
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
  summaryCardRow: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1.5rem' },
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
  formCard: { background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', padding: '1.5rem', marginBottom: '1.5rem' },
  grid2: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' },
  selectInput: { width: '100%', padding: '0.55rem', borderRadius: '6px', border: '1px solid #ced4da' },
  searchBar: { background: '#fff', borderRadius: '10px', padding: '1rem 1.25rem', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', marginBottom: '1.5rem' },
  tableWrapper: { overflowX: 'auto', WebkitOverflowScrolling: 'touch', background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', padding: '1.25rem' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  tableHead: { background: '#f8f9fa', color: '#495057', textAlign: 'left', borderBottom: '2px solid #dee2e6' },
  tableRow: { borderBottom: '1px solid #f1f3f5' },
  bloodBadge: { background: '#fdecea', color: '#c0392b', padding: '3px 8px', borderRadius: '6px', fontWeight: '700', fontSize: '0.85rem' },
  emptyCard: { background: '#fff', padding: '2.5rem', textAlign: 'center', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5' },
}
