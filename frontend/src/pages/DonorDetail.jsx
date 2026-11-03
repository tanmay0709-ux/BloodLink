import React, { useContext, useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { donorService } from '../services/donorService'
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

export default function DonorDetail() {
  const { id } = useParams()
  const { user } = useContext(AuthContext)
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const isBloodBankStaff = user?.role === 'BLOOD_BANK_STAFF'

  const [donor, setDonor] = useState(null)
  const [donations, setDonations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Edit Donor state
  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState({})
  const [editSaving, setEditSaving] = useState(false)

  // Record Donation state
  const [showDonationForm, setShowDonationForm] = useState(false)
  const [donationSaving, setDonationSaving] = useState(false)
  const [donationForm, setDonationForm] = useState({
    donationDate: new Date().toISOString().split('T')[0],
    bloodType: '',
    quantity: 450,
    location: '',
    status: 'COMPLETED',
    notes: '',
  })

  useEffect(() => {
    fetchDonorAndDonations()
  }, [id])

  const fetchDonorAndDonations = async () => {
    setLoading(true)
    setError('')
    try {
      const [donorData, donationsData] = await Promise.all([
        donorService.getById(id),
        donorService.getDonations(id),
      ])
      setDonor(donorData)
      setDonations(donationsData)
      setEditForm({
        fullName: donorData.fullName,
        email: donorData.email || '',
        phone: donorData.phone,
        bloodType: donorData.bloodType,
        gender: donorData.gender || 'MALE',
        dateOfBirth: donorData.dateOfBirth,
        address: donorData.address || '',
      })
      setDonationForm((prev) => ({
        ...prev,
        bloodType: donorData.bloodType,
        location: donorData.bloodBank?.name || '',
      }))
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load donor details')
    } finally {
      setLoading(false)
    }
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    setEditSaving(true)
    setError('')
    setSuccess('')
    try {
      const updated = await donorService.update(id, editForm)
      setDonor(updated)
      setSuccess('Donor information updated successfully!')
      setIsEditing(false)
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed')
    } finally {
      setEditSaving(false)
    }
  }

  const handleRecordDonation = async (e) => {
    e.preventDefault()
    setDonationSaving(true)
    setError('')
    setSuccess('')
    try {
      const payload = {
        donorId: Number(id),
        donationDate: donationForm.donationDate,
        bloodType: donationForm.bloodType,
        quantity: Number(donationForm.quantity),
        location: donationForm.location,
        status: donationForm.status,
        notes: donationForm.notes,
      }
      await donationService.create(payload)
      setSuccess('Donation recorded successfully!')
      setShowDonationForm(false)
      // Refresh donor and donations
      fetchDonorAndDonations()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record donation')
    } finally {
      setDonationSaving(false)
    }
  }

  const getBloodTypeLabel = (val) => {
    const item = BLOOD_TYPES.find((b) => b.value === val)
    return item ? item.label : val
  }

  const getEligibilityInfo = () => {
    if (!donor) return null
    if (!donor.lastDonationDate) {
      return { eligible: true, message: 'Eligible to donate (No previous record)' }
    }
    const lastDate = new Date(donor.lastDonationDate)
    const diffDays = Math.floor((Date.now() - lastDate.getTime()) / (1000 * 60 * 60 * 24))
    if (diffDays >= 56) {
      return { eligible: true, message: `Eligible to donate (${diffDays} days since last donation)` }
    } else {
      const remaining = 56 - diffDays
      const nextDate = new Date(lastDate.getTime() + 56 * 24 * 60 * 60 * 1000).toLocaleDateString()
      return { eligible: false, message: `Cooldown active: ${remaining} days remaining. Next eligible date: ${nextDate}` }
    }
  }

  if (loading) {
    return (
      <div style={styles.page}>
        <p style={{ color: '#6c757d' }}>Loading donor profile…</p>
      </div>
    )
  }

  if (error && !donor) {
    return (
      <div style={styles.page}>
        <div className="alert alert-error">{error}</div>
        <Link to="/donors" className="btn" style={{ marginTop: '1rem', display: 'inline-block' }}>
          ← Back to Donors
        </Link>
      </div>
    )
  }

  const eligibility = getEligibilityInfo()

  return (
    <div style={styles.page}>
      <div style={styles.navRow}>
        <Link to="/donors" style={styles.backLink}>← Back to Donors List</Link>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '1rem' }}>{success}</div>}

      {/* Profile Header */}
      <div style={styles.profileHeader}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', flexWrap: 'wrap' }}>
          <span style={styles.bloodAvatar}>{getBloodTypeLabel(donor.bloodType)}</span>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              <h1 style={styles.donorName}>{donor.fullName}</h1>
              {eligibility && (
                <span style={{
                  padding: '3px 10px',
                  borderRadius: '12px',
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  background: eligibility.eligible ? '#d4edda' : '#fff3cd',
                  color: eligibility.eligible ? '#155724' : '#856404',
                  border: `1px solid ${eligibility.eligible ? '#c3e6cb' : '#ffeeba'}`,
                }}>
                  {eligibility.eligible ? '● Eligible to Donate' : '⏳ In Cooldown'}
                </span>
              )}
            </div>
            <p style={styles.donorMeta}>
              Donor ID: <strong>#{donor.id}</strong> &bull; Registered with: <strong>{donor.bloodBank?.name || '—'}</strong> &bull; Total Donations: <strong>{donations.length}</strong>
            </p>
          </div>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          {!isEditing && (
            <button className="btn btn-outline" onClick={() => setIsEditing(true)}>
              ✏️ Edit Profile
            </button>
          )}
          {(isAdmin || isBloodBankStaff) && !showDonationForm && (
            <button
              className="btn btn-primary"
              onClick={() => {
                setShowDonationForm(true)
                setError('')
                setSuccess('')
              }}
            >
              🩸 + Record Donation
            </button>
          )}
        </div>
      </div>

      {/* Two-Column Responsive Split Layout */}
      <div style={styles.splitLayout}>
        {/* Left Column: Eligibility & Donor Profile Info */}
        <div style={styles.leftCol}>
          {/* Eligibility Banner / Card */}
          {eligibility && (
            <div style={{
              ...styles.card,
              borderLeft: `5px solid ${eligibility.eligible ? '#27ae60' : '#f39c12'}`,
              padding: '1.25rem',
            }}>
              <h3 style={{ margin: '0 0 0.4rem 0', fontSize: '1rem', color: eligibility.eligible ? '#1e7e34' : '#d35400' }}>
                {eligibility.eligible ? '✅ Donation Status: Eligible' : '⏳ Donation Cooldown Active'}
              </h3>
              <p style={{ margin: 0, fontSize: '0.88rem', color: '#495057', lineHeight: 1.5 }}>
                {eligibility.message}
              </p>
            </div>
          )}

          {/* Info Details Grid */}
          <div style={styles.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f1f3f5', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, color: '#2c3e50', fontSize: '1.05rem', fontWeight: 700 }}>
                📋 Donor Information
              </h3>
              <span style={styles.bloodBadge}>{getBloodTypeLabel(donor.bloodType)}</span>
            </div>
            <div style={styles.detailList}>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Phone Number</span>
                <span style={styles.infoValue}>{donor.phone}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Email</span>
                <span style={styles.infoValue}>{donor.email || '—'}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Date of Birth</span>
                <span style={styles.infoValue}>{donor.dateOfBirth}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Gender</span>
                <span style={styles.infoValue}>{donor.gender || '—'}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Address</span>
                <span style={styles.infoValue}>{donor.address || '—'}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Last Donation</span>
                <span style={{ ...styles.infoValue, fontWeight: 700, color: '#c0392b' }}>{donor.lastDonationDate || 'Never'}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Assigned Facility</span>
                <span style={styles.infoValue}>{donor.bloodBank?.name || '—'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Forms & Donation History */}
        <div style={styles.rightCol}>

      {/* Edit Form */}
      {isEditing && (
        <div style={styles.card}>
          <h3 style={{ marginTop: 0, color: '#2c3e50' }}>Edit Donor Information</h3>
          <form onSubmit={handleEditSubmit}>
            <div style={styles.grid2}>
              <div className="form-group">
                <label>Full Name *</label>
                <input
                  type="text"
                  required
                  value={editForm.fullName}
                  onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Blood Group *</label>
                <select
                  value={editForm.bloodType}
                  onChange={(e) => setEditForm({ ...editForm, bloodType: e.target.value })}
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
                  value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Email Address</label>
                <input
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Date of Birth *</label>
                <input
                  type="date"
                  required
                  value={editForm.dateOfBirth}
                  onChange={(e) => setEditForm({ ...editForm, dateOfBirth: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Gender</label>
                <select
                  value={editForm.gender}
                  onChange={(e) => setEditForm({ ...editForm, gender: e.target.value })}
                  style={styles.selectInput}
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Address</label>
                <input
                  type="text"
                  value={editForm.address}
                  onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary" disabled={editSaving}>
                {editSaving ? 'Saving…' : 'Save Changes'}
              </button>
              <button type="button" className="btn" onClick={() => setIsEditing(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Record Donation Form Card */}
      {showDonationForm && (
        <div style={{ ...styles.card, border: '2px solid #c0392b' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0, color: '#c0392b' }}>🩸 Record New Donation</h3>
            <button className="btn" onClick={() => setShowDonationForm(false)}>Cancel</button>
          </div>

          <form onSubmit={handleRecordDonation}>
            <div style={styles.grid2}>
              <div className="form-group">
                <label>Donation Date *</label>
                <input
                  type="date"
                  required
                  value={donationForm.donationDate}
                  onChange={(e) => setDonationForm({ ...donationForm, donationDate: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Blood Group *</label>
                <select
                  value={donationForm.bloodType}
                  onChange={(e) => setDonationForm({ ...donationForm, bloodType: e.target.value })}
                  style={styles.selectInput}
                  required
                >
                  {BLOOD_TYPES.map((b) => (
                    <option key={b.value} value={b.value}>{b.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Quantity (ml) * (Standard: 450 ml)</label>
                <input
                  type="number"
                  min="50"
                  max="1000"
                  required
                  value={donationForm.quantity}
                  onChange={(e) => setDonationForm({ ...donationForm, quantity: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Collection Location / Camp</label>
                <input
                  type="text"
                  maxLength={150}
                  placeholder="e.g. Main Blood Bank Center"
                  value={donationForm.location}
                  onChange={(e) => setDonationForm({ ...donationForm, location: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Donation Status</label>
                <select
                  value={donationForm.status}
                  onChange={(e) => setDonationForm({ ...donationForm, status: e.target.value })}
                  style={styles.selectInput}
                >
                  <option value="COMPLETED">COMPLETED</option>
                  <option value="PENDING">PENDING</option>
                  <option value="REJECTED">REJECTED</option>
                </select>
              </div>

              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Clinical Notes</label>
                <input
                  type="text"
                  maxLength={500}
                  placeholder="Health screening notes, hemoglobin level, etc."
                  value={donationForm.notes}
                  onChange={(e) => setDonationForm({ ...donationForm, notes: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary" disabled={donationSaving}>
                {donationSaving ? 'Recording…' : 'Confirm & Save Donation'}
              </button>
              <button type="button" className="btn" onClick={() => setShowDonationForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Donation History Section */}
      <div style={styles.card}>
        <h3 style={{ marginTop: 0, color: '#2c3e50' }}>📜 Donation History</h3>
        {donations.length === 0 ? (
          <p style={{ color: '#888', fontStyle: 'italic', margin: '1rem 0' }}>
            No past donations recorded for this donor.
          </p>
        ) : (
          <div style={styles.tableWrapper}>
            <table style={styles.table}>
              <thead>
                <tr style={styles.tableHead}>
                  <th>ID</th>
                  <th>Donation Date</th>
                  <th>Blood Group</th>
                  <th>Quantity</th>
                  <th>Location</th>
                  <th>Status</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                {donations.map((d) => (
                  <tr key={d.id} style={styles.tableRow}>
                    <td>#{d.id}</td>
                    <td><strong>{d.donationDate}</strong></td>
                    <td><span style={styles.bloodBadge}>{getBloodTypeLabel(d.bloodType)}</span></td>
                    <td>{d.quantity} ml</td>
                    <td>{d.location || '—'}</td>
                    <td>
                      <span style={{
                        padding: '3px 8px',
                        borderRadius: '10px',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        background: d.status === 'COMPLETED' ? '#d4edda' : d.status === 'PENDING' ? '#fff3cd' : '#f8d7da',
                        color: d.status === 'COMPLETED' ? '#155724' : d.status === 'PENDING' ? '#856404' : '#721c24',
                      }}>
                        {d.status}
                      </span>
                    </td>
                    <td>{d.notes || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  </div>
</div>
  )
}

const styles = {
  page: { width: '94%', maxWidth: '1440px', margin: '0 auto', padding: '1.5rem 0' },
  navRow: { marginBottom: '1rem' },
  backLink: { color: '#c0392b', textDecoration: 'none', fontWeight: 600, fontSize: '0.92rem' },
  profileHeader: {
    background: '#fff',
    borderRadius: '10px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
    border: '1px solid #f1f3f5',
    padding: '1.5rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
    flexWrap: 'wrap',
    gap: '1rem',
  },
  bloodAvatar: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    background: '#c0392b',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: '800',
    fontSize: '1.3rem',
    boxShadow: '0 2px 6px rgba(192,57,43,0.3)',
  },
  donorName: { margin: 0, fontSize: '1.65rem', color: '#1a202c', fontWeight: 800 },
  donorMeta: { margin: '0.3rem 0 0 0', color: '#6c757d', fontSize: '0.9rem' },
  splitLayout: {
    display: 'flex',
    gap: '1.5rem',
    alignItems: 'flex-start',
    flexWrap: 'wrap',
  },
  leftCol: {
    flex: '1 1 360px',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    minWidth: '320px',
  },
  rightCol: {
    flex: '2 1 640px',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    minWidth: '340px',
  },
  card: {
    background: '#fff',
    borderRadius: '10px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
    border: '1px solid #f1f3f5',
    padding: '1.5rem',
  },
  detailList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  detailRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: '0.5rem',
    borderBottom: '1px solid #f8f9fa',
    fontSize: '0.9rem',
  },
  grid2: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' },
  selectInput: { width: '100%', padding: '0.6rem', borderRadius: '6px', border: '1px solid #ced4da' },
  infoLabel: { fontSize: '0.8rem', color: '#6c757d', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' },
  infoValue: { fontSize: '0.92rem', color: '#2c3e50', fontWeight: 500, textAlign: 'right' },
  tableWrapper: { overflowX: 'auto', WebkitOverflowScrolling: 'touch' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  tableHead: { background: '#f8f9fa', color: '#495057', textAlign: 'left', borderBottom: '2px solid #dee2e6' },
  tableRow: { borderBottom: '1px solid #f1f3f5' },
  bloodBadge: { background: '#fdecea', color: '#c0392b', padding: '3px 9px', borderRadius: '8px', fontWeight: '800', fontSize: '0.85rem' },
  emptyCard: { background: '#fafafa', padding: '2.5rem 1.5rem', textAlign: 'center', borderRadius: '8px', border: '1px dashed #ced4da' },
}
