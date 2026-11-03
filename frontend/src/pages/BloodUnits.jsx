import React, { useContext, useEffect, useState } from 'react'
import { AuthContext } from '../context/AuthContext'
import { bloodUnitService } from '../services/bloodUnitService'
import { donationService } from '../services/donationService'

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
  { value: 'WHOLE_BLOOD', label: 'Whole Blood (35 days)' },
  { value: 'RED_BLOOD_CELLS', label: 'Packed Red Blood Cells (42 days)' },
  { value: 'PLATELETS', label: 'Platelet Concentrate (5 days)' },
  { value: 'PLASMA', label: 'Fresh Frozen Plasma (365 days)' },
  { value: 'CRYOPRECIPITATE', label: 'Cryoprecipitate (365 days)' },
]

const STATUS_LIST = [
  'AVAILABLE',
  'RESERVED',
  'ISSUED',
  'EXPIRED',
  'DISCARDED',
  'QUARANTINED',
]

export default function BloodUnits() {
  const { user } = useContext(AuthContext)
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const isBloodBankStaff = user?.role === 'BLOOD_BANK_STAFF'

  const [units, setUnits] = useState([])
  const [summary, setSummary] = useState(null)
  const [completedDonations, setCompletedDonations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Filters state
  const [filterBloodType, setFilterBloodType] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [filterComponent, setFilterComponent] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  // Registration modal state
  const [showRegisterForm, setShowRegisterForm] = useState(false)
  const [registerSaving, setRegisterSaving] = useState(false)
  const [registerForm, setRegisterForm] = useState({
    donationId: '',
    componentType: 'WHOLE_BLOOD',
    quantity: 450,
    unitId: '',
    expiryDate: '',
    status: 'AVAILABLE',
    notes: '',
  })

  // Status update modal state
  const [selectedUnit, setSelectedUnit] = useState(null)
  const [newStatus, setNewStatus] = useState('')
  const [statusSaving, setStatusSaving] = useState(false)

  useEffect(() => {
    if (!isAdmin && !isBloodBankStaff) {
      setError('Access restricted to Blood Bank Staff and System Administrators.')
      setLoading(false)
      return
    }
    loadData()
  }, [user])

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      const [unitsData, summaryData, donationsData] = await Promise.all([
        bloodUnitService.getAll(),
        bloodUnitService.getSummary(),
        donationService.getAll(),
      ])
      setUnits(unitsData)
      setSummary(summaryData)
      setCompletedDonations(donationsData.filter((d) => d.status === 'COMPLETED'))
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load blood unit inventory')
    } finally {
      setLoading(false)
    }
  }

  const handleFilterSubmit = async (e) => {
    if (e) e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const data = await bloodUnitService.getAll({
        bloodType: filterBloodType || undefined,
        status: filterStatus || undefined,
        componentType: filterComponent || undefined,
        q: searchQuery || undefined,
      })
      setUnits(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to filter units')
    } finally {
      setLoading(false)
    }
  }

  const handleResetFilters = async () => {
    setFilterBloodType('')
    setFilterStatus('')
    setFilterComponent('')
    setSearchQuery('')
    setLoading(true)
    try {
      const data = await bloodUnitService.getAll()
      setUnits(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset')
    } finally {
      setLoading(false)
    }
  }

  const handleRegisterSubmit = async (e) => {
    e.preventDefault()
    if (!registerForm.donationId) {
      setError('Please select an eligible COMPLETED donation.')
      return
    }

    setRegisterSaving(true)
    setError('')
    setSuccess('')
    try {
      const payload = {
        donationId: Number(registerForm.donationId),
        componentType: registerForm.componentType,
        quantity: Number(registerForm.quantity),
        unitId: registerForm.unitId ? registerForm.unitId.trim() : undefined,
        expiryDate: registerForm.expiryDate || undefined,
        status: registerForm.status,
        notes: registerForm.notes || undefined,
      }
      await bloodUnitService.create(payload)
      setSuccess('Blood unit registered into inventory successfully!')
      setShowRegisterForm(false)
      setRegisterForm({
        donationId: '',
        componentType: 'WHOLE_BLOOD',
        quantity: 450,
        unitId: '',
        expiryDate: '',
        status: 'AVAILABLE',
        notes: '',
      })
      loadData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to register unit')
    } finally {
      setRegisterSaving(false)
    }
  }

  const handleStatusSubmit = async (e) => {
    e.preventDefault()
    if (!selectedUnit || !newStatus) return

    setStatusSaving(true)
    setError('')
    setSuccess('')
    try {
      await bloodUnitService.updateStatus(selectedUnit.id, newStatus)
      setSuccess(`Unit #${selectedUnit.unitId} status updated to ${newStatus}`)
      setSelectedUnit(null)
      loadData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update status')
    } finally {
      setStatusSaving(false)
    }
  }

  const getStatusBadgeStyle = (status) => {
    switch (status) {
      case 'AVAILABLE':
        return { bg: '#d4edda', color: '#155724' }
      case 'RESERVED':
        return { bg: '#fff3cd', color: '#856404' }
      case 'ISSUED':
        return { bg: '#cce5ff', color: '#004085' }
      case 'EXPIRED':
        return { bg: '#f8d7da', color: '#721c24' }
      case 'DISCARDED':
        return { bg: '#e2e3e5', color: '#383d41' }
      case 'QUARANTINED':
        return { bg: '#e8daef', color: '#5b2c6f' }
      default:
        return { bg: '#eee', color: '#333' }
    }
  }

  const getBloodTypeLabel = (val) => {
    const item = BLOOD_TYPES.find((b) => b.value === val)
    return item ? item.label : val
  }

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <div>
          <h1 style={styles.title}>📦 Blood Unit Inventory</h1>
          <p style={styles.subtitle}>
            Manage, verify, and track individual blood units and component inventory
          </p>
        </div>
        {(isAdmin || isBloodBankStaff) && !showRegisterForm && (
          <button
            className="btn btn-primary"
            onClick={() => {
              setShowRegisterForm(true)
              setError('')
              setSuccess('')
            }}
          >
            + Register Blood Unit
          </button>
        )}
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '1rem' }}>{success}</div>}

      {/* Stock Summary Section - Balanced Horizontal Grid */}
      {summary && (
        <div style={styles.topOverviewGrid}>
          {/* Status Metrics Box */}
          <div style={styles.overviewBox}>
            <h3 style={styles.overviewTitle}>📦 Unit Status Overview</h3>
            <div style={styles.summaryCardRow}>
              <div style={{ ...styles.summaryCard, borderTop: '4px solid #27ae60' }}>
                <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#27ae60' }}>
                  {summary.availableUnits}
                </span>
                <span style={styles.summaryLabel}>Available</span>
              </div>
              <div style={{ ...styles.summaryCard, borderTop: '4px solid #f39c12' }}>
                <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#f39c12' }}>
                  {summary.reservedUnits}
                </span>
                <span style={styles.summaryLabel}>Reserved</span>
              </div>
              <div style={{ ...styles.summaryCard, borderTop: '4px solid #8e44ad' }}>
                <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#8e44ad' }}>
                  {summary.quarantinedUnits}
                </span>
                <span style={styles.summaryLabel}>Quarantined</span>
              </div>
              <div style={{ ...styles.summaryCard, borderTop: '4px solid #e74c3c' }}>
                <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#e74c3c' }}>
                  {summary.expiredUnits}
                </span>
                <span style={styles.summaryLabel}>Expired</span>
              </div>
              <div style={{ ...styles.summaryCard, borderTop: '4px solid #2980b9' }}>
                <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#2980b9' }}>
                  {summary.issuedUnits}
                </span>
                <span style={styles.summaryLabel}>Issued</span>
              </div>
            </div>
          </div>

          {/* Blood Group Availability Grid */}
          <div style={styles.overviewBox}>
            <h3 style={styles.overviewTitle}>🩸 Available Stock by Blood Group</h3>
            <div style={styles.bloodTypeGrid}>
              {BLOOD_TYPES.map((bt) => {
                const count = summary.availableByBloodType?.[bt.value] || 0
                const ml = summary.quantityMlByBloodType?.[bt.value] || 0
                return (
                  <div key={bt.value} style={styles.bloodTypeCard}>
                    <div style={styles.bloodGroupBadge}>{bt.label}</div>
                    <div style={{ fontWeight: '800', fontSize: '1.1rem', color: count > 0 ? '#27ae60' : '#888' }}>
                      {count}
                    </div>
                    <div style={{ fontSize: '0.72rem', color: '#6c757d' }}>{ml} ml</div>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {/* Registration Form Card */}
      {showRegisterForm && (
        <div style={styles.formCard}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>Register New Blood Unit</h3>
            <button className="btn" onClick={() => setShowRegisterForm(false)}>Cancel</button>
          </div>

          <form onSubmit={handleRegisterSubmit}>
            <div style={styles.grid2}>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Source Donation (COMPLETED only) *</label>
                <select
                  value={registerForm.donationId}
                  onChange={(e) => setRegisterForm({ ...registerForm, donationId: e.target.value })}
                  style={styles.selectInput}
                  required
                >
                  <option value="">-- Select an eligible COMPLETED donation --</option>
                  {completedDonations.map((d) => (
                    <option key={d.id} value={d.id}>
                      Donation #{d.id} &bull; Donor: {d.donorName} &bull; Group: {getBloodTypeLabel(d.bloodType)} &bull; {d.donationDate} ({d.quantity} ml)
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Component Type *</label>
                <select
                  value={registerForm.componentType}
                  onChange={(e) => setRegisterForm({ ...registerForm, componentType: e.target.value })}
                  style={styles.selectInput}
                  required
                >
                  {COMPONENT_TYPES.map((c) => (
                    <option key={c.value} value={c.value}>{c.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Quantity (ml) *</label>
                <input
                  type="number"
                  min="1"
                  max="1000"
                  required
                  value={registerForm.quantity}
                  onChange={(e) => setRegisterForm({ ...registerForm, quantity: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Custom Unit ID (Optional - auto-generated if blank)</label>
                <input
                  type="text"
                  maxLength={50}
                  placeholder="e.g. BU-2026-001"
                  value={registerForm.unitId}
                  onChange={(e) => setRegisterForm({ ...registerForm, unitId: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Expiry Date (Optional - auto-calculated if blank)</label>
                <input
                  type="date"
                  value={registerForm.expiryDate}
                  onChange={(e) => setRegisterForm({ ...registerForm, expiryDate: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Initial Status</label>
                <select
                  value={registerForm.status}
                  onChange={(e) => setRegisterForm({ ...registerForm, status: e.target.value })}
                  style={styles.selectInput}
                >
                  <option value="AVAILABLE">AVAILABLE</option>
                  <option value="QUARANTINED">QUARANTINED</option>
                </select>
              </div>

              <div className="form-group">
                <label>Notes</label>
                <input
                  type="text"
                  maxLength={500}
                  placeholder="Storage shelf, lab batch #, etc."
                  value={registerForm.notes}
                  onChange={(e) => setRegisterForm({ ...registerForm, notes: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary" disabled={registerSaving}>
                {registerSaving ? 'Registering…' : 'Save Unit to Inventory'}
              </button>
              <button type="button" className="btn" onClick={() => setShowRegisterForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Status Update Modal */}
      {selectedUnit && (
        <div style={styles.modalOverlay}>
          <div style={styles.modalCard}>
            <h3 style={{ margin: 0, color: '#2c3e50', marginBottom: '0.5rem' }}>
              Update Status: {selectedUnit.unitId}
            </h3>
            <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#6c757d' }}>
              Group: <strong>{getBloodTypeLabel(selectedUnit.bloodType)}</strong> &bull; Component: {selectedUnit.componentDisplayName || selectedUnit.componentType}
            </p>

            <form onSubmit={handleStatusSubmit}>
              <div className="form-group">
                <label>New Status *</label>
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  style={styles.selectInput}
                  required
                >
                  <option value="">-- Select Status --</option>
                  {STATUS_LIST.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>

              {selectedUnit.expired && (
                <div className="alert alert-error" style={{ fontSize: '0.8rem', padding: '0.5rem', marginBottom: '1rem' }}>
                  ⚠️ This unit is EXPIRED. It can only be transitioned to DISCARDED.
                </div>
              )}

              <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
                <button type="submit" className="btn btn-primary" disabled={statusSaving || !newStatus}>
                  {statusSaving ? 'Updating…' : 'Update Status'}
                </button>
                <button type="button" className="btn" onClick={() => setSelectedUnit(null)}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div style={styles.searchBar}>
        <form onSubmit={handleFilterSubmit} style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', width: '100%' }}>
          <input
            type="text"
            placeholder="Search Unit ID…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ flex: 1, minWidth: '150px', padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
          />

          <select
            value={filterBloodType}
            onChange={(e) => setFilterBloodType(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
          >
            <option value="">All Blood Groups</option>
            {BLOOD_TYPES.map((b) => (
              <option key={b.value} value={b.value}>{b.label}</option>
            ))}
          </select>

          <select
            value={filterComponent}
            onChange={(e) => setFilterComponent(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
          >
            <option value="">All Components</option>
            {COMPONENT_TYPES.map((c) => (
              <option key={c.value} value={c.value}>{c.value}</option>
            ))}
          </select>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
          >
            <option value="">All Statuses</option>
            {STATUS_LIST.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>

          <button type="submit" className="btn btn-primary">Filter</button>
          {(filterBloodType || filterStatus || filterComponent || searchQuery) && (
            <button type="button" className="btn" onClick={handleResetFilters}>Reset</button>
          )}
        </form>
      </div>

      {/* Units Table */}
      {loading ? (
        <p style={{ color: '#6c757d' }}>Loading inventory…</p>
      ) : units.length === 0 ? (
        <div style={styles.emptyCard}>
          <p style={{ color: '#888', fontStyle: 'italic', margin: 0 }}>
            No blood units found matching filter criteria.
          </p>
        </div>
      ) : (
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.tableHead}>
                <th>Unit ID</th>
                <th>Blood Group</th>
                <th>Component</th>
                <th>Quantity</th>
                <th>Collection</th>
                <th>Expiry</th>
                <th>Status</th>
                <th>Blood Bank</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {units.map((u) => {
                const sStyle = getStatusBadgeStyle(u.status)
                const isExp = u.expired || u.status === 'EXPIRED'
                return (
                  <tr key={u.id} style={styles.tableRow}>
                    <td><strong>{u.unitId}</strong></td>
                    <td><span style={styles.bloodBadge}>{getBloodTypeLabel(u.bloodType)}</span></td>
                    <td>{u.componentDisplayName || u.componentType}</td>
                    <td>{u.quantity} ml</td>
                    <td>{u.collectionDate}</td>
                    <td>
                      <span style={{ color: isExp ? '#c0392b' : '#27ae60', fontWeight: isExp ? '700' : '500' }}>
                        {u.expiryDate} {isExp && '(Expired)'}
                      </span>
                    </td>
                    <td>
                      <span style={{
                        background: sStyle.bg,
                        color: sStyle.color,
                        padding: '3px 8px',
                        borderRadius: '10px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                      }}>
                        {u.status}
                      </span>
                    </td>
                    <td>{u.bloodBankName || '—'}</td>
                    <td>
                      {u.status !== 'ISSUED' && u.status !== 'DISCARDED' ? (
                        <button
                          className="btn"
                          style={{ fontSize: '0.8rem', padding: '3px 8px' }}
                          onClick={() => {
                            setSelectedUnit(u)
                            setNewStatus(u.status)
                          }}
                        >
                          Change Status
                        </button>
                      ) : (
                        <span style={{ color: '#aaa', fontSize: '0.8rem' }}>Closed</span>
                      )}
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
  topOverviewGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))',
    gap: '1.25rem',
    marginBottom: '1.5rem',
  },
  overviewBox: {
    background: '#fff',
    borderRadius: '10px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
    border: '1px solid #f1f3f5',
    padding: '1.25rem',
    display: 'flex',
    flexDirection: 'column',
  },
  overviewTitle: {
    margin: '0 0 0.85rem 0',
    fontSize: '0.98rem',
    color: '#2c3e50',
    fontWeight: 700,
    borderBottom: '1px solid #f1f3f5',
    paddingBottom: '0.5rem',
  },
  summaryCardRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(95px, 1fr))',
    gap: '0.65rem',
    flex: 1,
  },
  summaryCard: {
    background: '#f8f9fa',
    borderRadius: '8px',
    border: '1px solid #e9ecef',
    padding: '0.75rem 0.5rem',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    justifyContent: 'center',
  },
  summaryLabel: { fontSize: '0.75rem', color: '#6c757d', marginTop: '0.2rem', fontWeight: 600 },
  bloodTypeGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: '0.5rem',
    flex: 1,
  },
  bloodTypeCard: {
    border: '1px solid #e9ecef',
    borderRadius: '6px',
    padding: '0.5rem 0.35rem',
    textAlign: 'center',
    background: '#fafafa',
  },
  bloodGroupBadge: {
    color: '#c0392b',
    fontWeight: '800',
    fontSize: '0.85rem',
    marginBottom: '0.15rem',
  },
  formCard: { background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', padding: '1.5rem', marginBottom: '1.5rem' },
  grid2: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' },
  selectInput: { width: '100%', padding: '0.55rem', borderRadius: '6px', border: '1px solid #ced4da' },
  searchBar: { background: '#fff', borderRadius: '10px', padding: '1rem 1.25rem', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', marginBottom: '1.5rem' },
  tableWrapper: { overflowX: 'auto', WebkitOverflowScrolling: 'touch', background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5', padding: '1.25rem' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' },
  tableHead: { background: '#f8f9fa', color: '#495057', textAlign: 'left', borderBottom: '2px solid #dee2e6' },
  tableRow: { borderBottom: '1px solid #f1f3f5' },
  bloodBadge: { background: '#fdecea', color: '#c0392b', padding: '3px 8px', borderRadius: '6px', fontWeight: '700', fontSize: '0.85rem' },
  emptyCard: { background: '#fff', padding: '2.5rem', textAlign: 'center', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5' },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 2000,
  },
  modalCard: {
    background: '#fff',
    borderRadius: '10px',
    padding: '2rem',
    width: '100%',
    maxWidth: '440px',
    boxShadow: '0 4px 16px rgba(0,0,0,0.2)',
  },
}
