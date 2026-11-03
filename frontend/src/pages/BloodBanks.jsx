import React, { useContext, useEffect, useState } from 'react'
import { AuthContext } from '../context/AuthContext'
import { bloodBankService } from '../services/bloodBankService'
import { adminService } from '../services/adminService'

export default function BloodBanks() {
  const { user } = useContext(AuthContext)
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const isBloodBankStaff = user?.role === 'BLOOD_BANK_STAFF'

  const [bloodBanks, setBloodBanks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Blood bank form (Create / Edit)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ name: '', address: '', contactPhone: '' })
  const [saving, setSaving] = useState(false)

  // Staff management panel
  const [managingStaffBloodBank, setManagingStaffBloodBank] = useState(null)
  const [staffList, setStaffList] = useState([])
  const [pendingStaffList, setPendingStaffList] = useState([])
  const [allUsers, setAllUsers] = useState([])
  const [selectedUserId, setSelectedUserId] = useState('')
  const [staffLoading, setStaffLoading] = useState(false)

  const fetchBloodBanks = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await bloodBankService.getAll()
      setBloodBanks(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load blood banks')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchBloodBanks()
  }, [])

  const openCreate = () => {
    setEditing(null)
    setForm({ name: '', address: '', contactPhone: '' })
    setManagingStaffBloodBank(null)
    setShowForm(true)
  }

  const openEdit = (b) => {
    setEditing(b)
    setForm({ name: b.name, address: b.address || '', contactPhone: b.contactPhone || '' })
    setManagingStaffBloodBank(null)
    setShowForm(true)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      if (editing) {
        await bloodBankService.update(editing.id, form)
        setSuccess('Blood bank updated successfully.')
      } else {
        await bloodBankService.create(form)
        setSuccess('Blood bank created successfully.')
      }
      setShowForm(false)
      fetchBloodBanks()
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Deactivate this blood bank?')) return
    setError('')
    setSuccess('')
    try {
      await bloodBankService.remove(id)
      setSuccess('Blood bank deactivated.')
      fetchBloodBanks()
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed')
    }
  }

  // Manage Staff
  const openStaffManager = async (bloodBank) => {
    setShowForm(false)
    setManagingStaffBloodBank(bloodBank)
    setStaffLoading(true)
    setError('')
    setSuccess('')
    try {
      const [staffData, pendingData, usersData] = await Promise.all([
        bloodBankService.getStaff(bloodBank.id),
        bloodBankService.getPendingStaff(bloodBank.id),
        adminService.listUsers(),
      ])
      setStaffList(staffData)
      setPendingStaffList(pendingData)
      setAllUsers(usersData.filter((u) => u.role === 'BLOOD_BANK_STAFF'))
      setSelectedUserId('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load staff')
    } finally {
      setStaffLoading(false)
    }
  }

  const handleApproveStaff = async (userId) => {
    if (!managingStaffBloodBank) return
    setError('')
    setSuccess('')
    try {
      await bloodBankService.approveStaff(managingStaffBloodBank.id, userId)
      setSuccess('Staff association approved successfully. User now has full blood bank access.')
      const [updatedStaff, updatedPending] = await Promise.all([
        bloodBankService.getStaff(managingStaffBloodBank.id),
        bloodBankService.getPendingStaff(managingStaffBloodBank.id),
      ])
      setStaffList(updatedStaff)
      setPendingStaffList(updatedPending)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to approve staff association')
    }
  }

  const handleRejectStaff = async (userId) => {
    if (!managingStaffBloodBank) return
    if (!window.confirm('Reject this staff association request?')) return
    setError('')
    setSuccess('')
    try {
      await bloodBankService.rejectStaff(managingStaffBloodBank.id, userId)
      setSuccess('Staff association request rejected.')
      const updatedPending = await bloodBankService.getPendingStaff(managingStaffBloodBank.id)
      setPendingStaffList(updatedPending)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reject staff association')
    }
  }

  const handleAssignStaff = async (e) => {
    e.preventDefault()
    if (!selectedUserId || !managingStaffBloodBank) return
    setError('')
    setSuccess('')
    try {
      await bloodBankService.assignStaff(managingStaffBloodBank.id, selectedUserId)
      setSuccess('Staff member assigned successfully.')
      const [updatedStaff, updatedPending] = await Promise.all([
        bloodBankService.getStaff(managingStaffBloodBank.id),
        bloodBankService.getPendingStaff(managingStaffBloodBank.id),
      ])
      setStaffList(updatedStaff)
      setPendingStaffList(updatedPending)
      setSelectedUserId('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to assign staff')
    }
  }

  const handleUnassignStaff = async (userId) => {
    if (!managingStaffBloodBank) return
    setError('')
    setSuccess('')
    try {
      await bloodBankService.unassignStaff(managingStaffBloodBank.id, userId)
      setSuccess('Staff member unassigned.')
      const [updatedStaff, updatedPending] = await Promise.all([
        bloodBankService.getStaff(managingStaffBloodBank.id),
        bloodBankService.getPendingStaff(managingStaffBloodBank.id),
      ])
      setStaffList(updatedStaff)
      setPendingStaffList(updatedPending)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to unassign staff')
    }
  }

  const totalBloodBanks = bloodBanks.length
  const activeBloodBanks = bloodBanks.filter((b) => b.active).length
  const inactiveBloodBanks = totalBloodBanks - activeBloodBanks

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <div>
          <h1 style={styles.title}>🏦 Blood Banks</h1>
          <p style={styles.subtitle}>Manage regional blood banks, cold storage centers, and authorized personnel</p>
        </div>
        {isAdmin && !showForm && (
          <button className="btn btn-primary" onClick={openCreate}>+ Add Blood Bank</button>
        )}
      </div>

      {/* Top Metrics Row for Desktop Balance */}
      <div style={styles.metricsRow}>
        <div style={styles.metricCard}>
          <span style={styles.metricLabel}>Total Blood Banks</span>
          <span style={styles.metricVal}>{totalBloodBanks}</span>
        </div>
        <div style={styles.metricCard}>
          <span style={styles.metricLabel}>Active Centers</span>
          <span style={{ ...styles.metricVal, color: '#27ae60' }}>{activeBloodBanks}</span>
        </div>
        <div style={styles.metricCard}>
          <span style={styles.metricLabel}>Inactive / Pending</span>
          <span style={{ ...styles.metricVal, color: inactiveBloodBanks > 0 ? '#e67e22' : '#7f8c8d' }}>
            {inactiveBloodBanks}
          </span>
        </div>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '1rem' }}>{success}</div>}

      {/* Create / Edit Form */}
      {showForm && (isAdmin || isBloodBankStaff) && (
        <div style={styles.formCard}>
          <h3 style={styles.formTitle}>{editing ? 'Edit Blood Bank' : 'New Blood Bank'}</h3>
          <form onSubmit={handleSave}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label>Name *</label>
                <input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                  maxLength={150}
                />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label>Address</label>
                <input
                  value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })}
                  maxLength={300}
                />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label>Contact Phone</label>
                <input
                  value={form.contactPhone}
                  onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
                  maxLength={20}
                />
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.25rem' }}>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button type="button" className="btn" onClick={() => setShowForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Staff Management Panel */}
      {managingStaffBloodBank && isAdmin && (
        <div style={styles.formCard}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>
              Staff for: <em>{managingStaffBloodBank.name}</em>
            </h3>
            <button className="btn" onClick={() => setManagingStaffBloodBank(null)}>Close</button>
          </div>

          {staffLoading ? (
            <p style={{ color: '#6c757d' }}>Loading staff members…</p>
          ) : (
            <>
              {/* 1. Pending Staff Association Requests (Admin Approval/Rejection Workflow) */}
              <div style={styles.pendingSection}>
                <h4 style={styles.pendingSectionTitle}>
                  ⏳ Pending Association Requests ({pendingStaffList.length})
                </h4>
                {pendingStaffList.length === 0 ? (
                  <p style={styles.pendingEmptyText}>
                    No pending staff requests for this blood bank.
                  </p>
                ) : (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                    {pendingStaffList.map((s) => (
                      <li key={s.id} style={styles.pendingItem}>
                        <div>
                          <strong>{s.username}</strong>
                          <span style={{ color: '#6c757d', fontSize: '0.85rem', marginLeft: '0.4rem' }}>
                            ({s.email})
                          </span>
                          <span style={styles.pendingBadge}>PENDING APPROVAL</span>
                        </div>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                          <button
                            type="button"
                            className="btn"
                            style={styles.approveBtn}
                            onClick={() => handleApproveStaff(s.id)}
                            title="Approve blood bank association and grant access"
                          >
                            ✓ Approve
                          </button>
                          <button
                            type="button"
                            className="btn"
                            style={styles.rejectBtn}
                            onClick={() => handleRejectStaff(s.id)}
                            title="Reject blood bank association"
                          >
                            ✕ Reject
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* 2. Currently Assigned Staff */}
              <div style={{ marginBottom: '1.2rem' }}>
                <h4 style={{ fontSize: '0.95rem', color: '#495057', marginBottom: '0.5rem' }}>
                  Approved & Assigned Staff ({staffList.length}):
                </h4>
                {staffList.length === 0 ? (
                  <p style={{ color: '#888', fontStyle: 'italic', fontSize: '0.9rem' }}>No staff assigned to this blood bank yet.</p>
                ) : (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                    {staffList.map((s) => (
                      <li key={s.id} style={styles.staffItem}>
                        <span>
                          <strong>{s.username}</strong> ({s.email})
                        </span>
                        <button
                          className="btn"
                          style={styles.unassignBtn}
                          onClick={() => handleUnassignStaff(s.id)}
                        >
                          Unassign
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* 3. Assign new staff form */}
              <form onSubmit={handleAssignStaff} style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end' }}>
                <div className="form-group" style={{ margin: 0, flex: 1 }}>
                  <label style={{ fontSize: '0.85rem' }}>Directly Assign Blood Bank Staff User:</label>
                  <select
                    value={selectedUserId}
                    onChange={(e) => setSelectedUserId(e.target.value)}
                    style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #ced4da' }}
                    required
                  >
                    <option value="">-- Select a BLOOD_BANK_STAFF user --</option>
                    {allUsers.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.username} ({u.email}) {u.assignedBloodBankName ? `[Assigned: ${u.assignedBloodBankName}]` : u.pendingBloodBankName ? `[Pending: ${u.pendingBloodBankName}]` : '[Unassigned]'}
                      </option>
                    ))}
                  </select>
                </div>
                <button type="submit" className="btn btn-primary" style={{ height: '38px' }}>
                  Assign
                </button>
              </form>
            </>
          )}
        </div>
      )}

      {/* Blood Banks Table */}
      {loading ? (
        <p style={{ color: '#6c757d' }}>Loading blood banks…</p>
      ) : bloodBanks.length === 0 ? (
        <p style={{ color: '#aaa', fontStyle: 'italic' }}>No blood banks found.</p>
      ) : (
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.tableHead}>
                <th>ID</th>
                <th>Name</th>
                <th>Address</th>
                <th>Phone</th>
                <th>Status</th>
                {(isAdmin || isBloodBankStaff) && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {bloodBanks.map((b) => (
                <tr key={b.id} style={styles.tableRow}>
                  <td>{b.id}</td>
                  <td><strong>{b.name}</strong></td>
                  <td>{b.address || '—'}</td>
                  <td>{b.contactPhone || '—'}</td>
                  <td>
                    <span style={{ color: b.active ? '#27ae60' : '#e74c3c', fontWeight: 600 }}>
                      {b.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {(isAdmin || isBloodBankStaff) && (
                    <td>
                      <button className="btn" style={styles.editBtn} onClick={() => openEdit(b)}>
                        Edit
                      </button>
                      {isAdmin && (
                        <>
                          <button
                            className="btn"
                            style={styles.staffBtn}
                            onClick={() => openStaffManager(b)}
                          >
                            Staff
                          </button>
                          {b.active && (
                            <button
                              className="btn"
                              style={styles.deleteBtn}
                              onClick={() => handleDelete(b.id)}
                            >
                              Deactivate
                            </button>
                          )}
                        </>
                      )}
                    </td>
                  )}
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
  headerRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '1rem' },
  title: { fontSize: '1.8rem', fontWeight: '800', color: '#2c3e50', margin: 0 },
  subtitle: { color: '#6c757d', margin: '0.25rem 0 0 0', fontSize: '0.95rem' },
  metricsRow: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1.5rem' },
  metricCard: { background: '#fff', borderRadius: '10px', padding: '1.25rem 1.5rem', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', display: 'flex', flexDirection: 'column', gap: '0.35rem', border: '1px solid #eef0f2' },
  metricLabel: { fontSize: '0.85rem', color: '#6c757d', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.5px' },
  metricVal: { fontSize: '1.8rem', fontWeight: '800', color: '#2c3e50' },
  formCard: { background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', padding: '1.5rem', marginBottom: '1.5rem', border: '1px solid #eef0f2' },
  formTitle: { marginBottom: '1rem', color: '#2c3e50' },
  tableWrapper: { overflowX: 'auto', background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', padding: '1.25rem', border: '1px solid #eef0f2' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.95rem' },
  tableHead: { background: '#f8f9fa', color: '#495057', textAlign: 'left' },
  tableRow: { borderBottom: '1px solid #dee2e6' },
  editBtn: { marginRight: '0.4rem', fontSize: '0.8rem', padding: '6px 12px', background: '#3498db', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 },
  staffBtn: { marginRight: '0.4rem', fontSize: '0.8rem', padding: '6px 12px', background: '#2ecc71', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 },
  deleteBtn: { fontSize: '0.8rem', padding: '6px 12px', background: '#e74c3c', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 },
  staffItem: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.6rem 0.8rem', borderBottom: '1px solid #eee', fontSize: '0.9rem' },
  unassignBtn: { fontSize: '0.75rem', padding: '4px 10px', background: '#f39c12', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 },
  pendingSection: { marginBottom: '1.5rem', background: '#fffdf5', border: '1px solid #fce8b2', borderRadius: '8px', padding: '1.1rem' },
  pendingSectionTitle: { fontSize: '0.95rem', color: '#856404', margin: '0 0 0.75rem 0', fontWeight: '700' },
  pendingEmptyText: { color: '#856404', fontStyle: 'italic', fontSize: '0.88rem', margin: 0 },
  pendingItem: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.7rem 0.9rem', background: '#fff', border: '1px solid #f0e0c0', borderRadius: '6px', marginBottom: '0.5rem', fontSize: '0.9rem', flexWrap: 'wrap', gap: '0.5rem' },
  pendingBadge: { marginLeft: '0.6rem', fontSize: '0.72rem', background: '#fff3cd', color: '#856404', padding: '3px 8px', borderRadius: '4px', fontWeight: 700, border: '1px solid #ffeeba' },
  approveBtn: { fontSize: '0.8rem', padding: '6px 14px', background: '#27ae60', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 },
  rejectBtn: { fontSize: '0.8rem', padding: '6px 14px', background: '#e74c3c', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 },
}
