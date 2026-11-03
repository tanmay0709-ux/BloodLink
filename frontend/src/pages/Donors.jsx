import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { donorService } from '../services/donorService'
import { bloodBankService } from '../services/bloodBankService'

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

export default function Donors() {
  const { user } = useContext(AuthContext)
  const navigate = useNavigate()
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const isBloodBankStaff = user?.role === 'BLOOD_BANK_STAFF'

  const [donors, setDonors] = useState([])
  const [bloodBanks, setBloodBanks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Search state
  const [searchQuery, setSearchQuery] = useState('')
  const [filterBloodType, setFilterBloodType] = useState('')

  // Register modal / form state
  const [showRegisterForm, setShowRegisterForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    bloodType: 'O_POSITIVE',
    gender: 'MALE',
    dateOfBirth: '',
    address: '',
    bloodBankId: '',
  })

  useEffect(() => {
    if (!isAdmin && !isBloodBankStaff) {
      setError('Access restricted to Blood Bank Staff and System Administrators.')
      setLoading(false)
      return
    }

    loadInitialData()
  }, [user])

  const loadInitialData = async () => {
    setLoading(true)
    setError('')
    try {
      const donorsData = await donorService.getAll()
      setDonors(donorsData)

      if (isAdmin) {
        const banksData = await bloodBankService.getAll()
        setBloodBanks(banksData.filter((b) => b.active))
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load donors')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async (e) => {
    if (e) e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const data = await donorService.search(searchQuery, filterBloodType || null)
      setDonors(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to search donors')
    } finally {
      setLoading(false)
    }
  }

  const handleResetSearch = async () => {
    setSearchQuery('')
    setFilterBloodType('')
    setLoading(true)
    try {
      const data = await donorService.getAll()
      setDonors(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset')
    } finally {
      setLoading(false)
    }
  }

  const handleRegisterSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    setSuccess('')

    try {
      const payload = {
        ...form,
        bloodBankId: form.bloodBankId ? Number(form.bloodBankId) : undefined,
      }
      await donorService.create(payload)
      setSuccess('Donor registered successfully!')
      setShowRegisterForm(false)
      setForm({
        fullName: '',
        email: '',
        phone: '',
        bloodType: 'O_POSITIVE',
        gender: 'MALE',
        dateOfBirth: '',
        address: '',
        bloodBankId: '',
      })
      // reload list
      const updated = await donorService.getAll()
      setDonors(updated)
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed')
    } finally {
      setSaving(false)
    }
  }

  const calculateAge = (dobString) => {
    if (!dobString) return '—'
    const dob = new Date(dobString)
    const diffMs = Date.now() - dob.getTime()
    const ageDt = new Date(diffMs)
    return Math.abs(ageDt.getUTCFullYear() - 1970)
  }

  const getBloodTypeLabel = (val) => {
    const item = BLOOD_TYPES.find((b) => b.value === val)
    return item ? item.label : val
  }

  const eligibleCount = donors.filter((d) => {
    if (!d.lastDonationDate) return true
    const diffDays = Math.floor((Date.now() - new Date(d.lastDonationDate).getTime()) / (1000 * 60 * 60 * 24))
    return diffDays >= 56
  }).length
  const cooldownCount = donors.length - eligibleCount

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <div>
          <h1 style={styles.title}>👥 Donor Management</h1>
          <p style={styles.subtitle}>
            Register, search, and manage blood donors and donation eligibility
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
            + Register Donor
          </button>
        )}
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '1rem' }}>{success}</div>}

      {/* Summary Metrics Row */}
      <div style={styles.summaryRow}>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #2980b9' }}>
          <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#2980b9' }}>
            {donors.length}
          </span>
          <span style={styles.summaryLabel}>Total Registered Donors</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #27ae60' }}>
          <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#27ae60' }}>
            {eligibleCount}
          </span>
          <span style={styles.summaryLabel}>Currently Eligible</span>
        </div>
        <div style={{ ...styles.summaryCard, borderTop: '4px solid #f39c12' }}>
          <span style={{ fontSize: '1.75rem', fontWeight: '800', color: '#f39c12' }}>
            {cooldownCount}
          </span>
          <span style={styles.summaryLabel}>In 56-Day Cooldown</span>
        </div>
      </div>

      {/* Registration Card */}
      {showRegisterForm && (
        <div style={styles.formCard}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>Register New Donor</h3>
            <button className="btn" onClick={() => setShowRegisterForm(false)}>Cancel</button>
          </div>

          <form onSubmit={handleRegisterSubmit}>
            <div style={styles.grid2}>
              <div className="form-group">
                <label>Full Name *</label>
                <input
                  type="text"
                  required
                  maxLength={100}
                  placeholder="e.g. John Doe"
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Blood Group *</label>
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
                <label>Phone Number *</label>
                <input
                  type="tel"
                  required
                  maxLength={20}
                  placeholder="e.g. 555-0144"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Email Address</label>
                <input
                  type="email"
                  maxLength={100}
                  placeholder="e.g. donor@example.com"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Date of Birth * (Must be 18-65)</label>
                <input
                  type="date"
                  required
                  value={form.dateOfBirth}
                  onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Gender</label>
                <select
                  value={form.gender}
                  onChange={(e) => setForm({ ...form, gender: e.target.value })}
                  style={styles.selectInput}
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              {isAdmin && (
                <div className="form-group">
                  <label>Assign to Blood Bank</label>
                  <select
                    value={form.bloodBankId}
                    onChange={(e) => setForm({ ...form, bloodBankId: e.target.value })}
                    style={styles.selectInput}
                  >
                    <option value="">-- Auto-select Active Blood Bank --</option>
                    {bloodBanks.map((b) => (
                      <option key={b.id} value={b.id}>{b.name}</option>
                    ))}
                  </select>
                </div>
              )}

              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Address</label>
                <input
                  type="text"
                  maxLength={255}
                  placeholder="Residential address"
                  value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Registering…' : 'Save Donor'}
              </button>
              <button type="button" className="btn" onClick={() => setShowRegisterForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Search and Filter Bar */}
      <div style={styles.searchBar}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', width: '100%' }}>
          <input
            type="text"
            placeholder="Search by name, phone, or email…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ flex: 2, minWidth: '220px', padding: '0.5rem 0.75rem', borderRadius: '6px', border: '1px solid #ced4da' }}
          />
          <select
            value={filterBloodType}
            onChange={(e) => setFilterBloodType(e.target.value)}
            style={{ flex: 1, minWidth: '140px', padding: '0.5rem', borderRadius: '6px', border: '1px solid #ced4da' }}
          >
            <option value="">All Blood Groups</option>
            {BLOOD_TYPES.map((b) => (
              <option key={b.value} value={b.value}>{b.label}</option>
            ))}
          </select>
          <button type="submit" className="btn btn-primary">Search</button>
          {(searchQuery || filterBloodType) && (
            <button type="button" className="btn" onClick={handleResetSearch}>Reset</button>
          )}
        </form>
      </div>

      {/* Donors Table */}
      {loading ? (
        <p style={{ color: '#6c757d' }}>Loading donors…</p>
      ) : donors.length === 0 ? (
        <div style={styles.emptyCard}>
          <p style={{ color: '#888', fontStyle: 'italic', margin: 0 }}>
            No donors found matching criteria.
          </p>
        </div>
      ) : (
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.tableHead}>
                <th>ID</th>
                <th>Full Name</th>
                <th>Blood Group</th>
                <th>Contact</th>
                <th>Age / DOB</th>
                <th>Blood Bank</th>
                <th>Last Donation</th>
                <th>Eligibility</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {donors.map((d) => (
                <tr key={d.id} style={styles.tableRow}>
                  <td>{d.id}</td>
                  <td>
                    <strong>{d.fullName}</strong>
                    {d.gender && <span style={styles.genderBadge}>{d.gender}</span>}
                  </td>
                  <td>
                    <span style={styles.bloodBadge}>
                      {getBloodTypeLabel(d.bloodType)}
                    </span>
                  </td>
                  <td>
                    <div>{d.phone}</div>
                    {d.email && <div style={{ fontSize: '0.8rem', color: '#6c757d' }}>{d.email}</div>}
                  </td>
                  <td>
                    {calculateAge(d.dateOfBirth)} yrs
                    <div style={{ fontSize: '0.75rem', color: '#888' }}>{d.dateOfBirth}</div>
                  </td>
                  <td>{d.bloodBank?.name || '—'}</td>
                  <td>{d.lastDonationDate || 'Never'}</td>
                  <td>
                    {d.lastDonationDate ? (
                      (() => {
                        const diffDays = Math.floor((Date.now() - new Date(d.lastDonationDate).getTime()) / (1000 * 60 * 60 * 24))
                        const isEligible = diffDays >= 56
                        return (
                          <span style={{
                            padding: '3px 8px',
                            borderRadius: '12px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            background: isEligible ? '#d4edda' : '#fff3cd',
                            color: isEligible ? '#155724' : '#856404',
                          }}>
                            {isEligible ? 'Eligible' : `${56 - diffDays}d cooldown`}
                          </span>
                        )
                      })()
                    ) : (
                      <span style={{
                        padding: '3px 8px',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        background: '#d4edda',
                        color: '#155724',
                      }}>
                        Eligible
                      </span>
                    )}
                  </td>
                  <td>
                    <Link
                      to={`/donors/${d.id}`}
                      className="btn btn-primary"
                      style={{ fontSize: '0.8rem', padding: '4px 10px', textDecoration: 'none' }}
                    >
                      View / Donate
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
  summaryRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '1rem',
    marginBottom: '1.5rem',
  },
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
  bloodBadge: { background: '#fdecea', color: '#c0392b', padding: '3px 8px', borderRadius: '8px', fontWeight: '700', fontSize: '0.85rem' },
  genderBadge: { marginLeft: '6px', fontSize: '0.75rem', color: '#6c757d', background: '#e9ecef', padding: '2px 6px', borderRadius: '4px' },
  emptyCard: { background: '#fff', padding: '2.5rem', textAlign: 'center', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #f1f3f5' },
}
