import React, { useContext, useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { bloodRequestService } from '../services/bloodRequestService'
import { bloodAllocationService } from '../services/bloodAllocationService'

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

export default function BloodRequestDetail() {
  const { id } = useParams()
  const { user } = useContext(AuthContext)
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const isHospitalStaff = user?.role === 'HOSPITAL_STAFF'
  const isPendingHospitalStaff = isHospitalStaff && (user?.approvalStatus === 'PENDING' || !user?.assignedHospitalId)
  const isApprovedHospitalStaff = isHospitalStaff && !isPendingHospitalStaff

  const isBloodBankStaff = user?.role === 'BLOOD_BANK_STAFF'
  const isPendingBankStaff = isBloodBankStaff && (user?.approvalStatus === 'PENDING' || !user?.assignedBloodBankId)
  const isApprovedBankStaff = isBloodBankStaff && !isPendingBankStaff

  const canManageBankActions = (isApprovedBankStaff || isAdmin)

  const [request, setRequest] = useState(null)
  const [matchingData, setMatchingData] = useState(null)
  const [allocations, setAllocations] = useState([])
  const [dispatchData, setDispatchData] = useState(null)

  const [loading, setLoading] = useState(true)
  const [matchingLoading, setMatchingLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  // Allocation State
  const [selectedUnitIds, setSelectedUnitIds] = useState([])

  // Dispatch Form State
  const [dispatchDate, setDispatchDate] = useState(new Date().toISOString().split('T')[0])
  const [dispatchNotes, setDispatchNotes] = useState('')

  // Receipt Form State
  const [receiptNotes, setReceiptNotes] = useState('')

  useEffect(() => {
    loadAllData()
  }, [id])

  const loadAllData = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await bloodRequestService.getById(id)
      setRequest(data)

      // Load matching inventory if in REVIEWED or PENDING
      if (canManageBankActions && ['PENDING', 'REVIEWED'].includes(data.status)) {
        loadMatchingUnits()
      }

      // Load allocations if request is ALLOCATED, DISPATCHED, or FULFILLED
      if (['ALLOCATED', 'DISPATCHED', 'FULFILLED'].includes(data.status)) {
        loadAllocations()
      }

      // Load dispatch record if DISPATCHED or FULFILLED
      if (['DISPATCHED', 'FULFILLED'].includes(data.status)) {
        loadDispatchRecord()
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load request details')
    } finally {
      setLoading(false)
    }
  }

  const loadMatchingUnits = async () => {
    setMatchingLoading(true)
    try {
      const mData = await bloodRequestService.getMatchingUnits(id)
      setMatchingData(mData)
    } catch (err) {
      console.error('Failed to load matching inventory', err)
    } finally {
      setMatchingLoading(false)
    }
  }

  const loadAllocations = async () => {
    try {
      const allocData = await bloodAllocationService.getAllocations(id)
      setAllocations(allocData)
    } catch (err) {
      console.error('Failed to load allocations', err)
    }
  }

  const loadDispatchRecord = async () => {
    try {
      const dData = await bloodAllocationService.getDispatch(id)
      setDispatchData(dData)
    } catch (err) {
      console.error('Failed to load dispatch details', err)
    }
  }

  const handleUpdateStatus = async (status) => {
    const actionLabel = status === 'REVIEWED' ? 'review and accept this request for fulfillment' : `change request status to ${status}`
    if (!window.confirm(`Are you sure you want to ${actionLabel}?`)) return
    setActionLoading(true)
    setError('')
    setSuccess('')
    try {
      const updated = await bloodRequestService.updateStatus(id, status)
      setRequest(updated)
      setSuccess(`Request status updated to ${status}`)
      if (status === 'REVIEWED' && canManageBankActions) {
        loadMatchingUnits()
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update status')
    } finally {
      setActionLoading(false)
    }
  }

  // Toggle unit selection for allocation
  const toggleSelectUnit = (unitId) => {
    const hasSufficient = (matchingData?.totalAvailableUnits || 0) >= request.quantity
    if (!hasSufficient) {
      setError(`Cannot allocate: Insufficient compatible units in inventory (${matchingData?.totalAvailableUnits || 0} available for ${request.quantity} required).`)
      return
    }
    if (selectedUnitIds.includes(unitId)) {
      setSelectedUnitIds(selectedUnitIds.filter((uid) => uid !== unitId))
    } else {
      if (selectedUnitIds.length >= (request?.quantity || 1)) {
        setError(`You can only select up to ${request.quantity} unit(s) for this request.`)
        return
      }
      setError('')
      setSelectedUnitIds([...selectedUnitIds, unitId])
    }
  }

  // Submit Allocation
  const handleAllocateSubmit = async (e) => {
    e.preventDefault()
    const hasSufficient = (matchingData?.totalAvailableUnits || 0) >= request.quantity
    if (!hasSufficient) {
      setError(`Cannot allocate: Insufficient compatible inventory. Request requires ${request.quantity} unit(s), but only ${matchingData?.totalAvailableUnits || 0} unit(s) are available.`)
      return
    }
    if (selectedUnitIds.length !== request.quantity) {
      setError(`Please select exactly ${request.quantity} unit(s). Currently selected: ${selectedUnitIds.length}`)
      return
    }

    setActionLoading(true)
    setError('')
    setSuccess('')
    try {
      await bloodAllocationService.allocateUnits(id, selectedUnitIds)
      setSuccess(`Successfully allocated ${selectedUnitIds.length} blood unit(s) to Request #${id}!`)
      setSelectedUnitIds([])
      loadAllData()
    } catch (err) {
      setError(err.response?.data?.message || 'Allocation failed')
    } finally {
      setActionLoading(false)
    }
  }

  // Submit Dispatch
  const handleDispatchSubmit = async (e) => {
    e.preventDefault()
    if (!window.confirm('Confirm dispatch of reserved units to requesting hospital?')) return

    setActionLoading(true)
    setError('')
    setSuccess('')
    try {
      const res = await bloodAllocationService.dispatchRequest(id, {
        dispatchDate,
        notes: dispatchNotes,
      })
      setSuccess(`Shipment dispatched! Reference: ${res.dispatchReference}`)
      loadAllData()
    } catch (err) {
      setError(err.response?.data?.message || 'Dispatch failed')
    } finally {
      setActionLoading(false)
    }
  }

  // Confirm Receipt (Hospital Staff)
  const handleConfirmReceipt = async (e) => {
    e.preventDefault()
    if (!dispatchData) return
    if (!window.confirm('Confirm receipt of shipment and verify package integrity?')) return

    setActionLoading(true)
    setError('')
    setSuccess('')
    try {
      await bloodAllocationService.confirmReceipt(dispatchData.id, {
        receiptNotes,
      })
      setSuccess('Shipment delivery and receipt confirmed successfully!')
      loadAllData()
    } catch (err) {
      setError(err.response?.data?.message || 'Confirmation failed')
    } finally {
      setActionLoading(false)
    }
  }

  const getBloodTypeLabel = (val) => {
    const item = BLOOD_TYPES.find((b) => b.value === val)
    return item ? item.label : val
  }

  const getPriorityBadgeStyle = (priority) => {
    switch (priority) {
      case 'EMERGENCY':
        return { bg: '#c0392b', color: '#fff' }
      case 'URGENT':
        return { bg: '#e67e22', color: '#fff' }
      case 'ROUTINE':
      default:
        return { bg: '#2980b9', color: '#fff' }
    }
  }

  const getStatusBadgeStyle = (status) => {
    switch (status) {
      case 'PENDING':
        return { bg: '#fff3cd', color: '#856404' }
      case 'REVIEWED':
        return { bg: '#d1ecf1', color: '#0c5460' }
      case 'ALLOCATED':
        return { bg: '#e8daef', color: '#5b2c6f' }
      case 'DISPATCHED':
        return { bg: '#ffeeba', color: '#856404' }
      case 'FULFILLED':
        return { bg: '#d4edda', color: '#155724' }
      case 'CANCELLED':
        return { bg: '#e2e3e5', color: '#383d41' }
      case 'REJECTED':
        return { bg: '#f8d7da', color: '#721c24' }
      default:
        return { bg: '#eee', color: '#333' }
    }
  }

  if (loading) {
    return (
      <div style={styles.page}>
        <p style={{ color: '#6c757d' }}>Loading blood request #{id}…</p>
      </div>
    )
  }

  if (error && !request) {
    return (
      <div style={styles.page}>
        <div className="alert alert-error">{error}</div>
        <Link to="/blood-requests" className="btn" style={{ marginTop: '1rem', display: 'inline-block' }}>
          ← Back to Blood Requests
        </Link>
      </div>
    )
  }

  const pStyle = getPriorityBadgeStyle(request.priority)
  const sStyle = getStatusBadgeStyle(request.status)

  return (
    <div style={styles.page}>
      <div style={styles.navRow}>
        <Link to="/blood-requests" style={styles.backLink}>← Back to Blood Requests List</Link>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '1rem' }}>{success}</div>}

      {isPendingHospitalStaff && (
        <div style={{ background: '#fff3cd', color: '#856404', border: '1px solid #ffeeba', marginBottom: '1.25rem', padding: '1rem 1.25rem', borderRadius: '8px', fontSize: '0.95rem' }}>
          <strong>⏳ Association Pending Approval:</strong> Your request to associate with <strong>{user?.pendingHospitalName || 'the hospital'}</strong> is awaiting administrator approval. Request management is restricted until approved.
        </div>
      )}

      {isPendingBankStaff && (
        <div style={{ background: '#fff3cd', color: '#856404', border: '1px solid #ffeeba', marginBottom: '1.25rem', padding: '1rem 1.25rem', borderRadius: '8px', fontSize: '0.95rem' }}>
          <strong>⏳ Association Pending Approval:</strong> Your request to associate with <strong>{user?.pendingBloodBankName || 'the blood bank'}</strong> is awaiting administrator approval. Request review, inventory allocation, and dispatch operations are restricted until approved.
        </div>
      )}

      {/* Header Profile */}
      <div style={styles.profileHeader}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
            <h1 style={{ margin: 0, fontSize: '1.6rem', color: '#2c3e50' }}>
              Blood Request #{request.id}
            </h1>
            <span style={{
              background: pStyle.bg,
              color: pStyle.color,
              padding: '3px 8px',
              borderRadius: '4px',
              fontSize: '0.75rem',
              fontWeight: '700',
            }}>
              {request.priority}
            </span>
            <span style={{
              background: sStyle.bg,
              color: sStyle.color,
              padding: '3px 8px',
              borderRadius: '8px',
              fontSize: '0.75rem',
              fontWeight: '600',
            }}>
              {request.status}
            </span>
          </div>
          <p style={{ margin: '0.3rem 0 0 0', color: '#6c757d', fontSize: '0.9rem' }}>
            Submitted by: <strong>{request.hospitalName}</strong> &bull; Address: {request.hospitalAddress || '—'}
          </p>
        </div>

        {/* Action Buttons for Request Status */}
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {isApprovedHospitalStaff && request.status === 'PENDING' && (
            <button
              className="btn"
              style={{ background: '#e74c3c', color: '#fff' }}
              disabled={actionLoading}
              onClick={() => handleUpdateStatus('CANCELLED')}
            >
              {actionLoading ? 'Cancelling…' : 'Cancel Request'}
            </button>
          )}

          {canManageBankActions && request.status === 'PENDING' && (
            <>
              <button
                className="btn btn-primary"
                style={{ background: '#27ae60' }}
                disabled={actionLoading}
                onClick={() => handleUpdateStatus('REVIEWED')}
              >
                {actionLoading ? 'Updating…' : '✓ Review Request'}
              </button>
              <button
                className="btn"
                style={{ background: '#e74c3c', color: '#fff' }}
                disabled={actionLoading}
                onClick={() => handleUpdateStatus('REJECTED')}
              >
                {actionLoading ? 'Rejecting…' : '✕ Reject Request'}
              </button>
            </>
          )}
        </div>
      </div>

      {/* Workflow Progress Steps */}
      <div style={{ ...styles.card, marginBottom: '1.25rem' }}>
        <div style={styles.stepProgressRow}>
          <div style={request.status === 'PENDING' ? styles.stepActive : styles.stepDone}>
            1. PENDING
          </div>
          <div style={styles.stepArrow}>&rarr;</div>
          <div style={request.status === 'REVIEWED' ? styles.stepActive : (['ALLOCATED', 'DISPATCHED', 'FULFILLED'].includes(request.status) ? styles.stepDone : styles.stepInactive)}>
            2. REVIEWED
          </div>
          <div style={styles.stepArrow}>&rarr;</div>
          <div style={request.status === 'ALLOCATED' ? styles.stepActive : (['DISPATCHED', 'FULFILLED'].includes(request.status) ? styles.stepDone : styles.stepInactive)}>
            3. ALLOCATED
          </div>
          <div style={styles.stepArrow}>&rarr;</div>
          <div style={request.status === 'DISPATCHED' ? styles.stepActive : (request.status === 'FULFILLED' ? styles.stepDone : styles.stepInactive)}>
            4. DISPATCHED
          </div>
          <div style={styles.stepArrow}>&rarr;</div>
          <div style={request.status === 'FULFILLED' ? styles.stepActive : styles.stepInactive}>
            5. FULFILLED
          </div>
        </div>
      </div>

      {/* Two-Column Responsive Split Layout */}
      <div style={styles.splitLayout}>
        {/* ── Left Column: Request Details & Hospital Context ── */}
        <div style={styles.leftCol}>
          {/* Request Details Grid */}
          <div style={styles.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f1f3f5', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, color: '#2c3e50', fontSize: '1.05rem', fontWeight: 700 }}>
                📋 Request Information
              </h3>
              <span style={styles.bloodBadge}>{getBloodTypeLabel(request.bloodType)}</span>
            </div>

            <div style={styles.detailList}>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Component Type</span>
                <span style={styles.infoValue}>{request.componentDisplayName || request.componentType}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Quantity Requested</span>
                <span style={{ ...styles.infoValue, fontWeight: 700, color: '#c0392b' }}>{request.quantity} unit(s)</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Required Date</span>
                <span style={styles.infoValue}>{request.requiredDate}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Priority Level</span>
                <span style={{
                  padding: '2px 8px',
                  borderRadius: '6px',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  backgroundColor: request.priority === 'EMERGENCY' ? '#fdecea' : request.priority === 'URGENT' ? '#fff3cd' : '#e2e3e5',
                  color: request.priority === 'EMERGENCY' ? '#c0392b' : request.priority === 'URGENT' ? '#856404' : '#383d41',
                }}>
                  {request.priority}
                </span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Submitted By</span>
                <span style={styles.infoValue}>{request.createdByUsername || 'Hospital Staff'}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Timestamp</span>
                <span style={{ ...styles.infoValue, fontSize: '0.82rem', color: '#6c757d' }}>{new Date(request.createdAt).toLocaleString()}</span>
              </div>
            </div>

            <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid #f8f9fa' }}>
              <span style={styles.infoLabel}>Clinical Reason / Diagnosis</span>
              <p style={{ margin: '0.35rem 0 0 0', fontSize: '0.88rem', color: request.clinicalReason ? '#2c3e50' : '#888', fontStyle: request.clinicalReason ? 'normal' : 'italic', lineHeight: 1.5 }}>
                {request.clinicalReason || 'No specific clinical notes provided.'}
              </p>
            </div>
          </div>

          {/* Hospital Details Card */}
          <div style={styles.card}>
            <h3 style={{ margin: '0 0 0.75rem 0', color: '#2c3e50', fontSize: '1rem', fontWeight: 700, borderBottom: '1px solid #f1f3f5', paddingBottom: '0.5rem' }}>
              🏥 Requesting Facility
            </h3>
            <div style={styles.detailList}>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Hospital</span>
                <span style={{ ...styles.infoValue, fontWeight: 600 }}>{request.hospitalName}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.infoLabel}>Address</span>
                <span style={{ ...styles.infoValue, fontSize: '0.85rem' }}>{request.hospitalAddress || '—'}</span>
              </div>
            </div>
          </div>

          {/* DISPATCH RECORD CARD (Visible when DISPATCHED or FULFILLED) */}
          {dispatchData && (
            <div style={{ ...styles.card, borderLeft: '5px solid #2980b9' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem', flexWrap: 'wrap' }}>
                <h3 style={{ margin: 0, color: '#2980b9', fontSize: '1rem' }}>
                  📦 Shipment Tracking
                </h3>
                <span style={{
                  padding: '3px 10px',
                  borderRadius: '8px',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  background: dispatchData.status === 'DELIVERED' ? '#d4edda' : '#ffeeba',
                  color: dispatchData.status === 'DELIVERED' ? '#155724' : '#856404',
                }}>
                  {dispatchData.status}
                </span>
              </div>

              <div style={styles.detailList}>
                <div style={styles.detailRow}>
                  <span style={styles.infoLabel}>Tracking Reference</span>
                  <strong style={{ color: '#2c3e50', fontSize: '0.9rem' }}>{dispatchData.dispatchReference}</strong>
                </div>
                <div style={styles.detailRow}>
                  <span style={styles.infoLabel}>Dispatched By</span>
                  <span style={styles.infoValue}>{dispatchData.dispatchedByUsername || 'Blood Bank Staff'}</span>
                </div>
                <div style={styles.detailRow}>
                  <span style={styles.infoLabel}>Dispatch Date</span>
                  <span style={styles.infoValue}>{dispatchData.dispatchDate}</span>
                </div>
                <div style={styles.detailRow}>
                  <span style={styles.infoLabel}>Origin Bank</span>
                  <span style={styles.infoValue}>{dispatchData.bloodBankName}</span>
                </div>
                {dispatchData.notes && (
                  <div style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#6c757d' }}>
                    <span style={styles.infoLabel}>Courier Notes:</span> {dispatchData.notes}
                  </div>
                )}
              </div>

              {/* Delivery Receipt Information if already confirmed */}
              {dispatchData.receivedAt && (
                <div style={{ marginTop: '1rem', padding: '0.8rem', background: '#e8f8f5', borderRadius: '6px' }}>
                  <span style={{ fontWeight: 700, color: '#117864', display: 'block', marginBottom: '0.3rem', fontSize: '0.85rem' }}>
                    ✅ Confirmed Delivery Receipt
                  </span>
                  <p style={{ margin: 0, fontSize: '0.85rem', color: '#165b4c' }}>
                    Received by: <strong>{dispatchData.receivedByUsername}</strong> &bull; {new Date(dispatchData.receivedAt).toLocaleString()}
                  </p>
                  {dispatchData.receiptNotes && (
                    <p style={{ margin: '0.3rem 0 0 0', fontSize: '0.82rem', color: '#165b4c', fontStyle: 'italic' }}>
                      &ldquo;{dispatchData.receiptNotes}&rdquo;
                    </p>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* ── Right Column: Interactive Workflow Actions & Tables ── */}
        <div style={styles.rightCol}>
          {/* PENDING State: Role-Based Display */}
          {request.status === 'PENDING' && (
            <>
              {/* Hospital Staff View: Waiting Messaging */}
              {isHospitalStaff && (
                <div style={styles.card}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                    <span style={{ fontSize: '1.8rem' }}>⏳</span>
                    <div>
                      <h3 style={{ margin: 0, color: '#2c3e50', fontSize: '1.1rem' }}>Awaiting Review</h3>
                      <p style={{ margin: '0.2rem 0 0 0', color: '#6c757d', fontSize: '0.88rem' }}>
                        This request has been submitted and is currently pending blood bank review.
                      </p>
                    </div>
                  </div>
                  <p style={{ margin: 0, fontSize: '0.88rem', color: '#495057', lineHeight: 1.5, background: '#f8f9fa', padding: '1rem', borderRadius: '8px' }}>
                    Blood bank staff will review inventory and reserve compatible units for this request.
                  </p>
                </div>
              )}

              {/* Blood Bank Staff & Admin View: Interactive Review & Inventory Availability */}
              {canManageBankActions && (
                <div style={{ ...styles.card, border: '2px solid #2980b9' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <span style={{ fontSize: '1.8rem' }}>🔍</span>
                      <div>
                        <h3 style={{ margin: 0, color: '#2c3e50', fontSize: '1.15rem' }}>Review Blood Request</h3>
                        <p style={{ margin: '0.2rem 0 0 0', color: '#6c757d', fontSize: '0.88rem' }}>
                          Check clinical requirements and local compatible inventory availability before accepting.
                        </p>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button
                        type="button"
                        className="btn btn-primary"
                        style={{ background: '#27ae60', padding: '8px 18px', fontWeight: 700 }}
                        disabled={actionLoading}
                        onClick={() => handleUpdateStatus('REVIEWED')}
                      >
                        {actionLoading ? 'Updating…' : '✓ Review Request'}
                      </button>
                      <button
                        type="button"
                        className="btn"
                        style={{ background: '#e74c3c', color: '#fff', padding: '8px 14px' }}
                        disabled={actionLoading}
                        onClick={() => handleUpdateStatus('REJECTED')}
                      >
                        {actionLoading ? 'Rejecting…' : '✕ Reject'}
                      </button>
                    </div>
                  </div>

                  {/* Inventory Availability Banner */}
                  <div style={{
                    padding: '1rem',
                    borderRadius: '8px',
                    marginBottom: '1rem',
                    background: (matchingData?.totalAvailableUnits || 0) >= request.quantity ? '#eafaf1' : '#fef9e7',
                    border: `1px solid ${(matchingData?.totalAvailableUnits || 0) >= request.quantity ? '#a9dfbf' : '#f9e79f'}`,
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <div>
                        <strong style={{
                          color: (matchingData?.totalAvailableUnits || 0) >= request.quantity ? '#1e8449' : '#b7950b',
                          fontSize: '0.95rem',
                          display: 'block',
                          marginBottom: '0.25rem',
                        }}>
                          {(matchingData?.totalAvailableUnits || 0) >= request.quantity
                            ? `✅ Sufficient Stock Available: ${matchingData?.totalAvailableUnits || 0} compatible unit(s) in your blood bank`
                            : `⚠️ Stock Alert: Only ${matchingData?.totalAvailableUnits || 0} compatible unit(s) available in your blood bank (Requires ${request.quantity})`}
                        </strong>
                        <span style={{ fontSize: '0.85rem', color: '#555' }}>
                          Compatible Donor Blood Groups: <strong>{matchingData?.compatibleBloodTypes?.map(getBloodTypeLabel).join(', ') || getBloodTypeLabel(request.bloodType)}</strong>
                        </span>
                      </div>
                      <span style={{
                        fontSize: '0.85rem',
                        fontWeight: 700,
                        padding: '4px 10px',
                        borderRadius: '12px',
                        background: (matchingData?.totalAvailableUnits || 0) >= request.quantity ? '#27ae60' : '#e67e22',
                        color: '#fff',
                      }}>
                        Stock: {matchingData?.totalAvailableUnits || 0} / {request.quantity} Req
                      </span>
                    </div>
                  </div>

                  {/* Matching Inventory Preview */}
                  <div>
                    <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '0.95rem', color: '#2c3e50', fontWeight: 600 }}>
                      Compatible Inventory Units at Assigned Blood Bank ({matchingData?.matchingUnits?.length || 0}):
                    </h4>
                    {matchingLoading ? (
                      <p style={{ color: '#6c757d', fontStyle: 'italic', fontSize: '0.88rem' }}>Checking compatible inventory…</p>
                    ) : !matchingData || matchingData.matchingUnits?.length === 0 ? (
                      <p style={{ color: '#888', fontStyle: 'italic', fontSize: '0.88rem', margin: 0 }}>
                        No compatible, unexpired units currently available in this blood bank. You may still mark the request as REVIEWED or reject it.
                      </p>
                    ) : (
                      <div style={styles.tableWrapper}>
                        <table style={styles.table}>
                          <thead>
                            <tr style={styles.tableHead}>
                              <th>Unit ID</th>
                              <th>Blood Group</th>
                              <th>Component</th>
                              <th>Volume</th>
                              <th>Collection Date</th>
                              <th>Expiry Date</th>
                              <th>Status</th>
                            </tr>
                          </thead>
                          <tbody>
                            {matchingData.matchingUnits.map((u) => (
                              <tr key={u.id} style={styles.tableRow}>
                                <td><strong>{u.unitId}</strong></td>
                                <td><span style={styles.bloodBadge}>{getBloodTypeLabel(u.bloodType)}</span></td>
                                <td>{u.componentDisplayName || u.componentType}</td>
                                <td>{u.quantity} ml</td>
                                <td>{u.collectionDate}</td>
                                <td style={{ color: '#27ae60', fontWeight: 600 }}>{u.expiryDate}</td>
                                <td>
                                  <span style={{ background: '#d4edda', color: '#155724', padding: '2px 8px', borderRadius: '8px', fontSize: '0.75rem', fontWeight: 600 }}>
                                    {u.status}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          )}

          {/* REVIEWED State: Role-Based Display */}
          {request.status === 'REVIEWED' && (
            <>
              {/* Hospital Staff View: In-Progress Status */}
              {isHospitalStaff && (
                <div style={styles.card}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                    <span style={{ fontSize: '1.8rem' }}>🔬</span>
                    <div>
                      <h3 style={{ margin: 0, color: '#0c5460', fontSize: '1.1rem' }}>Request Under Review</h3>
                      <p style={{ margin: '0.2rem 0 0 0', color: '#6c757d', fontSize: '0.88rem' }}>
                        This request has been reviewed by blood bank personnel.
                      </p>
                    </div>
                  </div>
                  <p style={{ margin: 0, fontSize: '0.88rem', color: '#0c5460', lineHeight: 1.5, background: '#d1ecf1', padding: '1rem', borderRadius: '8px' }}>
                    Blood bank staff are verifying compatible inventory and preparing to allocate units for this request.
                  </p>
                </div>
              )}

              {/* Blood Bank Staff & Admin View: STAGE 1 ALLOCATION SECTION */}
              {canManageBankActions && (
                <div style={{ ...styles.card, border: '2px solid #8e44ad' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <h3 style={{ margin: 0, color: '#6c3483', fontSize: '1.1rem' }}>
                      🎯 Allocate Blood Units to Request
                    </h3>
                    <span style={{ fontSize: '0.9rem', fontWeight: '700', color: selectedUnitIds.length === request.quantity ? '#27ae60' : '#c0392b' }}>
                      Selected: {selectedUnitIds.length} / {request.quantity} required
                    </span>
                  </div>

                  <div style={styles.disclaimerBox}>
                    <span style={{ fontWeight: '700', color: '#b9770e', display: 'block', marginBottom: '0.2rem' }}>
                      ⚠️ ABO/Rh COMPATIBILITY SCREENING
                    </span>
                    <p style={{ margin: 0, fontSize: '0.85rem', color: '#7d6608', lineHeight: 1.4 }}>
                      Select available, unexpired compatible units below. Selected units will be locked and reserved for this request.
                    </p>
                  </div>

                  {/* Insufficient Inventory Warning Banner */}
                  {(matchingData?.totalAvailableUnits || 0) < request.quantity && (
                    <div style={{
                      background: '#fdecea',
                      border: '1px solid #f5b7b1',
                      borderLeft: '5px solid #c0392b',
                      borderRadius: '6px',
                      padding: '1rem',
                      marginBottom: '1rem',
                    }}>
                      <strong style={{ color: '#962d22', display: 'block', marginBottom: '0.25rem', fontSize: '0.95rem' }}>
                        ⚠️ Insufficient Compatible Inventory to Allocate
                      </strong>
                      <p style={{ margin: 0, fontSize: '0.88rem', color: '#78281f', lineHeight: 1.4 }}>
                        This request requires <strong>{request.quantity} unit(s)</strong> of compatible {getBloodTypeLabel(request.bloodType)} {request.componentDisplayName || request.componentType}, but your blood bank currently has only <strong>{matchingData?.totalAvailableUnits || 0} unit(s)</strong> available in stock. Allocation cannot proceed until sufficient compatible units are added to inventory.
                      </p>
                    </div>
                  )}

                  {matchingLoading ? (
                    <p style={{ color: '#6c757d' }}>Loading compatible units…</p>
                  ) : !matchingData || matchingData.matchingUnits?.length === 0 ? (
                    <div style={styles.emptyCard}>
                      <p style={{ color: '#888', fontStyle: 'italic', margin: 0 }}>
                        No available compatible units found in this blood bank.
                      </p>
                    </div>
                  ) : (
                    <form onSubmit={handleAllocateSubmit}>
                      <div style={styles.tableWrapper}>
                        <table style={styles.table}>
                          <thead>
                            <tr style={styles.tableHead}>
                              <th style={{ width: '40px' }}>Select</th>
                              <th>Unit ID</th>
                              <th>Blood Group</th>
                              <th>Component</th>
                              <th>Quantity</th>
                              <th>Collection</th>
                              <th>Expiry</th>
                              <th>Status</th>
                            </tr>
                          </thead>
                          <tbody>
                            {matchingData.matchingUnits.map((u) => {
                              const isChecked = selectedUnitIds.includes(u.id)
                              const hasSufficient = (matchingData?.totalAvailableUnits || 0) >= request.quantity
                              return (
                                <tr key={u.id} style={{ ...styles.tableRow, background: isChecked ? '#f4ecf7' : 'transparent' }}>
                                  <td>
                                    <input
                                      type="checkbox"
                                      checked={isChecked}
                                      disabled={!hasSufficient}
                                      onChange={() => toggleSelectUnit(u.id)}
                                      title={!hasSufficient ? 'Cannot select: Insufficient inventory' : undefined}
                                    />
                                  </td>
                                  <td><strong>{u.unitId}</strong></td>
                                  <td><span style={styles.bloodBadge}>{getBloodTypeLabel(u.bloodType)}</span></td>
                                  <td>{u.componentDisplayName || u.componentType}</td>
                                  <td>{u.quantity} ml</td>
                                  <td>{u.collectionDate}</td>
                                  <td style={{ color: '#27ae60', fontWeight: '600' }}>{u.expiryDate}</td>
                                  <td>
                                    <span style={{ background: '#d4edda', color: '#155724', padding: '2px 8px', borderRadius: '8px', fontSize: '0.75rem', fontWeight: 600 }}>
                                      {u.status}
                                    </span>
                                  </td>
                                </tr>
                              )
                            })}
                          </tbody>
                        </table>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
                        <button
                          type="submit"
                          className="btn btn-primary"
                          style={{ background: '#8e44ad', borderColor: '#7d3c98' }}
                          disabled={
                            actionLoading ||
                            (matchingData?.totalAvailableUnits || 0) < request.quantity ||
                            selectedUnitIds.length !== request.quantity
                          }
                          title={
                            (matchingData?.totalAvailableUnits || 0) < request.quantity
                              ? 'Cannot allocate: Insufficient compatible inventory'
                              : selectedUnitIds.length !== request.quantity
                              ? `Select exactly ${request.quantity} units to confirm`
                              : undefined
                          }
                        >
                          {actionLoading
                            ? 'Allocating…'
                            : (matchingData?.totalAvailableUnits || 0) < request.quantity
                            ? `Insufficient Inventory (${matchingData?.totalAvailableUnits || 0}/${request.quantity})`
                            : `Confirm Allocation (${selectedUnitIds.length}/${request.quantity} units)`}
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              )}
            </>
          )}

          {/* ALLOCATED UNITS DISPLAY (Visible when ALLOCATED, DISPATCHED, or FULFILLED) */}
          {['ALLOCATED', 'DISPATCHED', 'FULFILLED'].includes(request.status) && (
            <div style={styles.card}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f1f3f5', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
                <h3 style={{ margin: 0, color: '#2c3e50', fontSize: '1.1rem', fontWeight: 700 }}>
                  🔒 Allocated Blood Units ({allocations.length})
                </h3>
                <span style={{ fontSize: '0.85rem', color: '#27ae60', fontWeight: 700 }}>
                  ✓ Units Reserved
                </span>
              </div>
              <div style={styles.tableWrapper}>
                <table style={styles.table}>
                  <thead>
                    <tr style={styles.tableHead}>
                      <th>Unit ID</th>
                      <th>Blood Group</th>
                      <th>Component</th>
                      <th>Volume</th>
                      <th>Origin Bank</th>
                      <th>Status</th>
                      <th>Allocated Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {allocations.map((a) => (
                      <tr key={a.id} style={styles.tableRow}>
                        <td><strong>{a.unitId}</strong></td>
                        <td><span style={styles.bloodBadge}>{getBloodTypeLabel(a.bloodType)}</span></td>
                        <td>{a.componentType}</td>
                        <td>{a.quantity} ml</td>
                        <td style={{ color: '#495057' }}>{a.bloodBankName}</td>
                        <td>
                          <span style={{
                            padding: '2px 8px',
                            borderRadius: '8px',
                            fontSize: '0.75rem',
                            fontWeight: '600',
                            background: a.status === 'COMPLETED' ? '#d4edda' : '#fff3cd',
                            color: a.status === 'COMPLETED' ? '#155724' : '#856404',
                          }}>
                            {a.status}
                          </span>
                        </td>
                        <td style={{ color: '#6c757d', fontSize: '0.85rem' }}>{new Date(a.allocatedAt).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ALLOCATED Status Waiting Card for Hospital Staff */}
          {isHospitalStaff && request.status === 'ALLOCATED' && (
            <div style={{ ...styles.card, background: '#fcf3cf', border: '1px solid #f9e79f' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span style={{ fontSize: '1.5rem' }}>📦</span>
                <div>
                  <h4 style={{ margin: 0, color: '#7d6608', fontSize: '1rem' }}>Units Allocated &amp; Awaiting Dispatch</h4>
                  <p style={{ margin: '0.2rem 0 0 0', color: '#7d6608', fontSize: '0.85rem' }}>
                    Compatible blood units have been reserved and are currently being prepared for dispatch shipment by the blood bank.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* STAGE 2: DISPATCH SECTION (Visible for Blood Bank Staff / Admin when status is ALLOCATED) */}
          {canManageBankActions && request.status === 'ALLOCATED' && (
            <div style={{ ...styles.card, border: '2px solid #e67e22' }}>
              <h3 style={{ margin: '0 0 0.5rem 0', color: '#d35400', fontSize: '1.1rem' }}>
                🚚 Dispatch Blood Units to Hospital
              </h3>
              <p style={{ fontSize: '0.88rem', color: '#666', margin: '0 0 1rem 0' }}>
                Dispatches the {allocations.length} reserved unit(s) to <strong>{request.hospitalName}</strong>. Units will transition to <strong>ISSUED</strong> status.
              </p>

              <form onSubmit={handleDispatchSubmit}>
                <div style={styles.grid2}>
                  <div className="form-group">
                    <label>Dispatch Date *</label>
                    <input
                      type="date"
                      required
                      value={dispatchDate}
                      onChange={(e) => setDispatchDate(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label>Shipping / Courier Notes</label>
                    <input
                      type="text"
                      maxLength={500}
                      placeholder="e.g. Courier: MedExpress #402, 4°C verified"
                      value={dispatchNotes}
                      onChange={(e) => setDispatchNotes(e.target.value)}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    style={{ background: '#e67e22', borderColor: '#d35400' }}
                    disabled={actionLoading}
                  >
                    {actionLoading ? 'Dispatching…' : 'Confirm Dispatch Shipment'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* DISPATCHED Status in Transit Card for Blood Bank Staff */}
          {canManageBankActions && request.status === 'DISPATCHED' && (
            <div style={{ ...styles.card, background: '#eaf2f8', border: '1px solid #aed6f1' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span style={{ fontSize: '1.5rem' }}>🚚</span>
                <div>
                  <h4 style={{ margin: 0, color: '#1b4f72', fontSize: '1rem' }}>Shipment in Transit</h4>
                  <p style={{ margin: '0.2rem 0 0 0', color: '#2874a6', fontSize: '0.85rem' }}>
                    Units have been dispatched and are in transit to <strong>{request.hospitalName}</strong>. Awaiting confirmation of delivery receipt by hospital staff.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* STAGE 3: RECEIPT CONFIRMATION FORM (Visible for Hospital Staff & Admin when DISPATCHED) */}
          {(isApprovedHospitalStaff || isAdmin) && dispatchData?.status === 'DISPATCHED' && (
            <div style={{ ...styles.card, background: '#fef9e7', border: '2px solid #f39c12' }}>
              <h4 style={{ margin: '0 0 0.5rem 0', color: '#7d6608', fontSize: '1.05rem' }}>
                📝 Confirm Delivery &amp; Receipt of Blood Units
              </h4>
              <p style={{ fontSize: '0.85rem', color: '#7d6608', margin: '0 0 1rem 0' }}>
                Please verify cold chain integrity and container seals before confirming receipt.
              </p>

              <form onSubmit={handleConfirmReceipt}>
                <div className="form-group">
                  <label>Inspection / Receipt Notes</label>
                  <input
                    type="text"
                    maxLength={500}
                    placeholder="e.g. Temperature verified (3.8°C), seal intact, units received into storage"
                    value={receiptNotes}
                    onChange={(e) => setReceiptNotes(e.target.value)}
                  />
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    style={{ background: '#27ae60', borderColor: '#229954' }}
                    disabled={actionLoading}
                  >
                    {actionLoading ? 'Confirming…' : 'Confirm Receipt & Complete Request'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* FULFILLED Success State Banner */}
          {request.status === 'FULFILLED' && (
            <div style={{ ...styles.card, background: '#e8f8f5', border: '1px solid #a3e4d7', textAlign: 'center', padding: '2rem' }}>
              <span style={{ fontSize: '2.5rem', display: 'block', marginBottom: '0.5rem' }}>🎉</span>
              <h3 style={{ margin: '0 0 0.5rem 0', color: '#117864' }}>Request Fully Completed</h3>
              <p style={{ margin: 0, fontSize: '0.9rem', color: '#165b4c' }}>
                Blood units have been successfully allocated, dispatched, and confirmed received by the hospital.
              </p>
            </div>
          )}

          {/* CANCELLED State Banner */}
          {request.status === 'CANCELLED' && (
            <div style={{ ...styles.card, background: '#f8d7da', border: '1px solid #f5c6cb' }}>
              <h4 style={{ margin: '0 0 0.3rem 0', color: '#721c24' }}>Request Cancelled</h4>
              <p style={{ margin: 0, fontSize: '0.88rem', color: '#721c24' }}>
                This blood request has been cancelled and will not be processed.
              </p>
            </div>
          )}

          {/* REJECTED State Banner */}
          {request.status === 'REJECTED' && (
            <div style={{ ...styles.card, background: '#f8d7da', border: '1px solid #f5c6cb' }}>
              <h4 style={{ margin: '0 0 0.3rem 0', color: '#721c24' }}>Request Rejected</h4>
              <p style={{ margin: 0, fontSize: '0.88rem', color: '#721c24' }}>
                This request was rejected by blood bank personnel or administration.
              </p>
            </div>
          )}
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
    marginBottom: '1.25rem',
    flexWrap: 'wrap',
    gap: '1rem',
  },
  splitLayout: {
    display: 'flex',
    gap: '1.5rem',
    alignItems: 'flex-start',
    flexWrap: 'wrap',
  },
  leftCol: {
    flex: '1 1 380px',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    minWidth: '340px',
  },
  rightCol: {
    flex: '2 1 650px',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    minWidth: '360px',
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
  infoGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginTop: '0.5rem' },
  infoLabel: { fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.5px', color: '#888', fontWeight: 600 },
  infoValue: { margin: '0.2rem 0 0 0', fontSize: '0.95rem', color: '#2c3e50', fontWeight: 500 },
  bloodBadge: { background: '#fdecea', color: '#c0392b', padding: '3px 8px', borderRadius: '6px', fontWeight: '700', fontSize: '0.85rem' },
  disclaimerBox: {
    background: '#fef9e7',
    border: '1px solid #f9e79f',
    borderLeft: '4px solid #f39c12',
    padding: '0.75rem 1rem',
    borderRadius: '6px',
    marginBottom: '1rem',
  },
  tableWrapper: { overflowX: 'auto', WebkitOverflowScrolling: 'touch' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  tableHead: { background: '#f8f9fa', color: '#495057', textAlign: 'left', borderBottom: '2px solid #dee2e6' },
  tableRow: { borderBottom: '1px solid #f1f3f5' },
  emptyCard: { background: '#fafafa', padding: '2rem', textAlign: 'center', borderRadius: '8px', border: '1px dashed #ced4da' },
  stepProgressRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: '0.5rem',
    fontSize: '0.85rem',
    fontWeight: '700',
  },
  stepActive: {
    padding: '6px 14px',
    borderRadius: '20px',
    background: '#2980b9',
    color: '#fff',
    boxShadow: '0 2px 4px rgba(41,128,185,0.3)',
  },
  stepDone: {
    padding: '6px 14px',
    borderRadius: '20px',
    background: '#d4edda',
    color: '#155724',
    border: '1px solid #c3e6cb',
  },
  stepInactive: {
    padding: '6px 14px',
    borderRadius: '20px',
    background: '#f1f2f6',
    color: '#a4b0be',
  },
  stepArrow: {
    color: '#a4b0be',
    fontSize: '1rem',
  },
  grid2: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' },
}
