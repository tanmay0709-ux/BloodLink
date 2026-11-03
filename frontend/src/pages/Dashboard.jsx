import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import analyticsService from '../services/analyticsService'
import { DistributionBar, MonthlyTrendChart, StatusPill } from '../components/AnalyticsCharts'

export default function Dashboard() {
  const { user } = useContext(AuthContext)
  const navigate = useNavigate()

  const [analytics, setAnalytics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) {
      navigate('/login')
    }
  }, [user, navigate])

  const fetchDashboardData = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await analyticsService.getDashboardAnalytics()
      if (res && res.data) {
        setAnalytics(res.data)
      }
    } catch (err) {
      console.error(err)
      setError('Failed to load dashboard analytics. Please ensure the backend is running.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (user) {
      fetchDashboardData()
    }
  }, [user])

  if (!user) return null

  const isHospital = user.role === 'HOSPITAL_STAFF'
  const isBloodBank = user.role === 'BLOOD_BANK_STAFF'
  const isAdmin = user.role === 'SYSTEM_ADMIN'

  return (
    <div style={styles.page}>
      {/* ── Page Header ── */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>
            {analytics?.facilityName || 'BloodLink Dashboard'}
          </h1>
          <p style={styles.subtitle}>
            Welcome back, <strong>{user.username}</strong>
            <span style={styles.roleBadge}>{user.role?.replace(/_/g, ' ')}</span>
            {analytics?.facilityType && analytics?.facilityType !== 'ALL' && (
              <span style={styles.facilityBadge}>{analytics.facilityName}</span>
            )}
          </p>
        </div>
        <button
          onClick={fetchDashboardData}
          style={styles.refreshBtn}
          title="Refresh Analytics"
        >
          🔄 Refresh
        </button>
      </div>

      {error && (
        <div style={styles.alertBox}>{error}</div>
      )}

      {user.role === 'HOSPITAL_STAFF' && (user.approvalStatus === 'PENDING' || !user.assignedHospitalId) && (
        <div style={{ ...styles.alertBox, background: '#fff3cd', color: '#856404', borderColor: '#ffeeba', marginBottom: '1.25rem' }}>
          ⏳ <strong>Account Pending Approval:</strong> Your association with <strong>{user.pendingHospitalName || 'your requested hospital'}</strong> is awaiting administrator approval. Blood request submissions and sensitive hospital operations are restricted until approved.
        </div>
      )}

      {user.role === 'BLOOD_BANK_STAFF' && (user.approvalStatus === 'PENDING' || !user.assignedBloodBankId) && (
        <div style={{ ...styles.alertBox, background: '#fff3cd', color: '#856404', borderColor: '#ffeeba', marginBottom: '1.25rem' }}>
          ⏳ <strong>Account Pending Approval:</strong> Your association with <strong>{user.pendingBloodBankName || 'your requested blood bank'}</strong> is awaiting administrator approval. Blood bank operations, inventory management, and requests are restricted until approved.
        </div>
      )}

      {loading ? (
        <div style={styles.loadingBox}>
          <div style={styles.spinner} />
          <p style={{ marginTop: '1rem', color: '#6c757d', fontSize: '0.9rem' }}>
            Compiling live analytics…
          </p>
        </div>
      ) : !analytics ? (
        <div style={styles.emptyBox}>No analytics data available.</div>
      ) : (
        <>
          {/* ── 1. Overview Metric Cards ── */}
          <div style={styles.cardGrid}>
            {(isBloodBank || isAdmin) && (
              <>
                <MetricCard
                  icon="🩸"
                  label="Available Units"
                  value={analytics.overview?.availableUnits ?? 0}
                  subtext="Ready for allocation"
                  color="#27ae60"
                  linkTo="/blood-units"
                />
                <MetricCard
                  icon="📦"
                  label="Total Units"
                  value={analytics.overview?.totalUnits ?? 0}
                  subtext={`${analytics.overview?.reservedUnits ?? 0} reserved · ${analytics.overview?.expiredUnits ?? 0} expired`}
                  color="#c0392b"
                  linkTo="/blood-units"
                />
                <MetricCard
                  icon="👥"
                  label="Registered Donors"
                  value={analytics.overview?.totalDonors ?? 0}
                  subtext={`${analytics.overview?.totalDonations ?? 0} total donations`}
                  color="#2980b9"
                  linkTo="/donors"
                />
              </>
            )}

            <MetricCard
              icon="📋"
              label="Blood Requests"
              value={analytics.overview?.totalRequests ?? 0}
              subtext={`${analytics.overview?.pendingRequests ?? 0} pending · ${analytics.overview?.fulfilledRequests ?? 0} fulfilled`}
              color="#e67e22"
              linkTo="/blood-requests"
            />

            <MetricCard
              icon="🚚"
              label="Active Dispatches"
              value={analytics.overview?.activeDispatches ?? 0}
              subtext="Shipments in transit"
              color="#8e44ad"
              linkTo="/dispatches"
            />

            {isHospital && (
              <MetricCard
                icon="🎯"
                label="Fulfillment Rate"
                value={`${analytics.requestAnalytics?.fulfillmentRatePercent ?? 0}%`}
                subtext={`${analytics.requestAnalytics?.fulfilledRequestsCount ?? 0} of ${analytics.requestAnalytics?.eligibleRequestsCount ?? 0} eligible`}
                color="#16a085"
                linkTo="/blood-requests"
              />
            )}
          </div>

          {/* ── 2. Main Analytics Grid ── */}
          <div style={styles.twoColGrid}>
            {/* ── Left Column: Requests + Inventory ── */}
            <div style={styles.col}>

              {/* Request Status Breakdown */}
              <div style={styles.panel}>
                <div style={styles.panelHeader}>
                  <h2 style={styles.panelTitle}>📋 Request Status Breakdown</h2>
                  {analytics.requestAnalytics?.fulfillmentRatePercent !== undefined && (
                    <span style={styles.rateBadge}>
                      Fulfillment: <strong>{analytics.requestAnalytics.fulfillmentRatePercent}%</strong>
                    </span>
                  )}
                </div>

                {analytics.requestAnalytics?.byStatus?.length > 0 ? (
                  <div style={styles.pillRow}>
                    {analytics.requestAnalytics.byStatus.map((st) => (
                      <StatusPill key={st.status} status={st.status} count={st.count} />
                    ))}
                  </div>
                ) : (
                  <p style={styles.emptyText}>No request status data yet.</p>
                )}

                {analytics.requestAnalytics?.byPriority?.length > 0 && (
                  <div style={{ marginTop: '1.25rem' }}>
                    <h3 style={styles.subHeading}>By Priority</h3>
                    <DistributionBar
                      items={analytics.requestAnalytics.byPriority.map((p) => ({
                        label: p.priority.charAt(0) + p.priority.slice(1).toLowerCase(),
                        value: p.count,
                        unit: p.count === 1 ? 'request' : 'requests',
                        color:
                          p.priority === 'EMERGENCY'
                            ? '#c0392b'
                            : p.priority === 'URGENT'
                            ? '#e67e22'
                            : '#2980b9',
                      }))}
                    />
                  </div>
                )}
              </div>

              {/* Inventory by Component (Blood Bank & Admin only) */}
              {(isBloodBank || isAdmin) && analytics.inventoryAnalytics && (
                <div style={styles.panel}>
                  <h2 style={styles.panelTitle}>🧪 Available Inventory by Component</h2>
                  {analytics.inventoryAnalytics.byComponentType?.length > 0 ? (
                    <DistributionBar
                      items={analytics.inventoryAnalytics.byComponentType.map((c) => ({
                        label: c.displayName,
                        value: c.count,
                        unit: `unit${c.count !== 1 ? 's' : ''} · ${c.totalVolumeMl} ml`,
                        color: '#3498db',
                      }))}
                    />
                  ) : (
                    <div style={styles.emptyState}>
                      <span style={styles.emptyStateIcon}>🩸</span>
                      <p style={styles.emptyStateTitle}>No available inventory</p>
                      <p style={styles.emptyStateSubtext}>
                        Available stock will appear here when blood units are marked available.
                      </p>
                    </div>
                  )}
                </div>
              )}

              {/* Inventory by Blood Group (Blood Bank & Admin only) */}
              {(isBloodBank || isAdmin) && analytics.inventoryAnalytics && (
                <div style={styles.panel}>
                  <h2 style={styles.panelTitle}>🩸 Available Stock by Blood Group</h2>
                  {analytics.inventoryAnalytics.byBloodGroup?.length > 0 ? (
                    <DistributionBar
                      items={analytics.inventoryAnalytics.byBloodGroup.map((b) => ({
                        label: b.bloodType.replace('_', ' '),
                        value: b.count,
                        unit: `bag${b.count !== 1 ? 's' : ''} · ${b.totalVolumeMl} ml`,
                        color: '#e74c3c',
                      }))}
                    />
                  ) : (
                    <div style={styles.emptyState}>
                      <span style={styles.emptyStateIcon}>🩸</span>
                      <p style={styles.emptyStateTitle}>No available inventory</p>
                      <p style={styles.emptyStateSubtext}>
                        Available stock by blood group will appear here when units are added.
                      </p>
                    </div>
                  )}
                </div>
              )}

              {/* Units Nearing Expiry Warning (Blood Bank & Admin) */}
              {(isBloodBank || isAdmin) && analytics.inventoryAnalytics?.nearExpiryUnits?.length > 0 && (
                <div style={{ ...styles.panel, borderLeft: '4px solid #e74c3c' }}>
                  <div style={styles.panelHeader}>
                    <h2 style={{ ...styles.panelTitle, color: '#c0392b' }}>
                      ⚠️ Units Nearing Expiry
                    </h2>
                    <span style={styles.expiryBadge}>
                      {analytics.inventoryAnalytics.nearExpiryUnits.length} bag(s) · next 7 days
                    </span>
                  </div>
                  <div style={styles.tableScroll}>
                    <table style={styles.miniTable}>
                      <thead>
                        <tr>
                          <th>Unit ID</th>
                          <th>Group</th>
                          <th>Component</th>
                          <th>Expiry</th>
                          <th>Days Left</th>
                        </tr>
                      </thead>
                      <tbody>
                        {analytics.inventoryAnalytics.nearExpiryUnits.map((u, idx) => (
                          <tr key={u.id} style={{ backgroundColor: idx % 2 === 0 ? '#fff' : '#fff5f5' }}>
                            <td style={{ fontWeight: 600, color: '#2c3e50' }}>{u.unitId}</td>
                            <td>{u.bloodType.replace('_', ' ')}</td>
                            <td>{u.componentType}</td>
                            <td>{u.expiryDate}</td>
                            <td style={{
                              color: u.daysRemaining <= 2 ? '#c0392b' : '#e67e22',
                              fontWeight: 700,
                            }}>
                              {u.daysRemaining}d
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>

            {/* ── Right Column: Donations + Dispatches ── */}
            <div style={styles.col}>

              {/* Monthly Donations Trend (Blood Bank & Admin only) */}
              {(isBloodBank || isAdmin) && analytics.donationAnalytics && (
                <div style={styles.panel}>
                  <div style={styles.panelHeader}>
                    <h2 style={styles.panelTitle}>📊 Donation Trends (Latest 6 Months)</h2>
                  </div>
                  <p style={styles.donationSummary}>
                    <strong>{analytics.donationAnalytics.totalCompletedDonations}</strong> completed donations
                    &nbsp;·&nbsp;
                    <strong>{analytics.donationAnalytics.totalDonatedVolumeMl} ml</strong> total volume
                  </p>
                  <MonthlyTrendChart trends={analytics.donationAnalytics.monthlyTrends} />
                </div>
              )}

              {/* Recent Donations (Blood Bank & Admin only) */}
              {(isBloodBank || isAdmin) && analytics.donationAnalytics && (
                <div style={styles.panel}>
                  <div style={styles.panelHeader}>
                    <h2 style={styles.panelTitle}>💉 Recent Donations</h2>
                    <Link to="/donors" style={styles.viewAllLink}>View Donors →</Link>
                  </div>
                  {analytics.donationAnalytics.recentDonations?.length > 0 ? (
                    <div style={styles.tableScroll}>
                      <table style={styles.miniTable}>
                        <thead>
                          <tr>
                            <th>Donor</th>
                            <th>Blood Type</th>
                            <th>Volume</th>
                            <th>Date</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {analytics.donationAnalytics.recentDonations.slice(0, 5).map((d, idx) => (
                            <tr key={d.id} style={{ backgroundColor: idx % 2 === 0 ? '#fff' : '#f8f9fa' }}>
                              <td style={{ fontWeight: 600 }}>{d.donorName}</td>
                              <td>
                                <span style={styles.bloodTypeBadge}>
                                  {d.bloodType?.replace('_', ' ')}
                                </span>
                              </td>
                              <td>{d.quantity} ml</td>
                              <td style={{ color: '#6c757d' }}>{d.donationDate}</td>
                              <td>
                                <StatusPill status={d.status} count="" />
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p style={styles.emptyText}>No recent donations recorded.</p>
                  )}
                </div>
              )}

              {/* Recent Dispatches (All roles) */}
              <div style={styles.panel}>
                <div style={styles.panelHeader}>
                  <h2 style={styles.panelTitle}>🚚 Recent Dispatches</h2>
                  <Link to="/dispatches" style={styles.viewAllLink}>View All →</Link>
                </div>

                {analytics.operationalAnalytics && (
                  <p style={styles.dispatchSummary}>
                    <span style={styles.summaryChip}>
                      {analytics.operationalAnalytics.activeInTransit ?? 0} in transit
                    </span>
                    <span style={{ ...styles.summaryChip, backgroundColor: '#d1e7dd', color: '#0f5132' }}>
                      {analytics.operationalAnalytics.deliveredCount ?? 0} delivered
                    </span>
                  </p>
                )}

                {(!analytics.operationalAnalytics?.recentDispatches ||
                  analytics.operationalAnalytics.recentDispatches.length === 0) ? (
                  <p style={styles.emptyText}>No recent dispatches logged.</p>
                ) : (
                  <div style={styles.tableScroll}>
                    <table style={styles.miniTable}>
                      <thead>
                        <tr>
                          <th>Reference</th>
                          <th>Destination</th>
                          <th>Units</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {analytics.operationalAnalytics.recentDispatches.map((d, idx) => (
                          <tr key={d.id} style={{ backgroundColor: idx % 2 === 0 ? '#fff' : '#f8f9fa' }}>
                            <td>
                              <strong style={{ color: '#2c3e50', fontSize: '0.85rem' }}>
                                {d.dispatchReference}
                              </strong>
                            </td>
                            <td style={{ color: '#495057' }}>{d.hospitalName}</td>
                            <td style={{ color: '#495057' }}>{d.unitsCount} unit{d.unitsCount !== 1 ? 's' : ''}</td>
                            <td>
                              <span style={{
                                padding: '2px 9px',
                                borderRadius: '12px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                backgroundColor:
                                  d.status === 'DELIVERED' ? '#d1e7dd'
                                  : d.status === 'CANCELLED' ? '#f8d7da'
                                  : '#fff3cd',
                                color:
                                  d.status === 'DELIVERED' ? '#0f5132'
                                  : d.status === 'CANCELLED' ? '#721c24'
                                  : '#856404',
                              }}>
                                {d.status}
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
          </div>

          {/* ── 3. Bottom Full-width: Recent Blood Requests ── */}
          <div style={styles.panel}>
            <div style={styles.panelHeader}>
              <h2 style={styles.panelTitle}>🕒 Recent Blood Requests</h2>
              <Link to="/blood-requests" style={styles.viewAllLink}>View All Requests →</Link>
            </div>
            {(!analytics.requestAnalytics?.recentRequests ||
              analytics.requestAnalytics.recentRequests.length === 0) ? (
              <p style={{ ...styles.emptyText, padding: '1rem 0' }}>No recent requests found.</p>
            ) : (
              <div style={styles.tableScroll}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th>Req #</th>
                      <th>Hospital</th>
                      <th>Blood Group</th>
                      <th>Component</th>
                      <th>Qty</th>
                      <th>Priority</th>
                      <th>Required By</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.requestAnalytics.recentRequests.map((req, idx) => (
                      <tr key={req.id} style={{ backgroundColor: idx % 2 === 0 ? '#fff' : '#f8f9fa' }}>
                        <td style={{ fontWeight: 600, color: '#c0392b' }}>#{req.id}</td>
                        <td>{req.hospitalName}</td>
                        <td>
                          <span style={styles.bloodTypeBadge}>
                            {req.bloodType?.replace('_', ' ')}
                          </span>
                        </td>
                        <td style={{ color: '#495057' }}>{req.componentType}</td>
                        <td>{req.quantity} unit{req.quantity !== 1 ? 's' : ''}</td>
                        <td>
                          <span style={{
                            padding: '2px 8px',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor:
                              req.priority === 'EMERGENCY' ? '#f8d7da'
                              : req.priority === 'URGENT' ? '#fff3cd'
                              : '#e2e3e5',
                            color:
                              req.priority === 'EMERGENCY' ? '#721c24'
                              : req.priority === 'URGENT' ? '#856404'
                              : '#383d41',
                          }}>
                            {req.priority}
                          </span>
                        </td>
                        <td style={{ color: '#6c757d' }}>{req.requiredDate}</td>
                        <td>
                          <StatusPill status={req.status} count="" />
                        </td>
                        <td>
                          <Link to={`/blood-requests/${req.id}`} style={styles.tableActionBtn}>
                            View →
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}

// ─── MetricCard Component ─────────────────────────────────────────────────────
function MetricCard({ icon, label, value, subtext, color, linkTo }) {
  const [hovered, setHovered] = useState(false)

  const cardStyle = {
    ...styles.card,
    borderTop: `4px solid ${color}`,
    transform: hovered ? 'translateY(-3px)' : 'translateY(0)',
    boxShadow: hovered
      ? '0 6px 20px rgba(0,0,0,0.12)'
      : '0 2px 8px rgba(0,0,0,0.07)',
  }

  const content = (
    <div
      style={cardStyle}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <div style={styles.cardIconRow}>
        <span style={styles.cardIcon}>{icon}</span>
      </div>
      <div style={{ ...styles.cardValue, color }}>{value}</div>
      <div style={styles.cardLabel}>{label}</div>
      {subtext && <div style={styles.cardSubtext}>{subtext}</div>}
    </div>
  )

  return linkTo ? (
    <Link to={linkTo} style={{ textDecoration: 'none' }}>
      {content}
    </Link>
  ) : (
    content
  )
}

// ─── Styles ───────────────────────────────────────────────────────────────────
const styles = {
  page: {
    width: '94%',
    maxWidth: '1400px',
    margin: '0 auto',
    padding: '2rem 0',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.5rem',
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },

  // Header
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '1rem',
    paddingBottom: '1rem',
    borderBottom: '1px solid #f1f3f5',
  },
  title: {
    margin: 0,
    fontSize: '1.75rem',
    color: '#1a202c',
    fontWeight: 800,
    letterSpacing: '-0.01em',
  },
  subtitle: {
    margin: '0.4rem 0 0',
    color: '#6c757d',
    fontSize: '0.9rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    flexWrap: 'wrap',
  },
  roleBadge: {
    backgroundColor: '#2c3e50',
    color: '#fff',
    padding: '2px 9px',
    borderRadius: '10px',
    fontSize: '0.75rem',
    fontWeight: 600,
    letterSpacing: '0.02em',
  },
  facilityBadge: {
    backgroundColor: '#e8f4fd',
    color: '#2980b9',
    padding: '2px 9px',
    borderRadius: '10px',
    fontSize: '0.75rem',
    fontWeight: 600,
  },
  refreshBtn: {
    background: '#fff',
    border: '1.5px solid #dee2e6',
    padding: '0.5rem 1.1rem',
    borderRadius: '8px',
    cursor: 'pointer',
    fontWeight: 600,
    fontSize: '0.88rem',
    color: '#2c3e50',
    transition: 'border-color 0.15s, box-shadow 0.15s',
    fontFamily: 'inherit',
  },

  // States
  alertBox: {
    padding: '1rem 1.25rem',
    backgroundColor: '#fdecea',
    color: '#c0392b',
    borderRadius: '8px',
    border: '1px solid #f5c6cb',
    fontSize: '0.9rem',
    fontWeight: 500,
  },
  loadingBox: {
    textAlign: 'center',
    padding: '4rem 2rem',
    backgroundColor: '#fff',
    borderRadius: '10px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
  },
  spinner: {
    width: '36px',
    height: '36px',
    border: '3px solid #f1f3f5',
    borderTop: '3px solid #c0392b',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
    margin: '0 auto',
  },
  emptyBox: {
    textAlign: 'center',
    padding: '3rem',
    color: '#6c757d',
    fontSize: '0.95rem',
  },

  // Metric Cards
  cardGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
    gap: '1.25rem',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: '10px',
    padding: '1.4rem 1.5rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.2rem',
    transition: 'transform 0.18s ease, box-shadow 0.18s ease',
    cursor: 'default',
  },
  cardIconRow: {
    marginBottom: '0.3rem',
  },
  cardIcon: {
    fontSize: '1.6rem',
    lineHeight: 1,
  },
  cardValue: {
    fontSize: '2rem',
    fontWeight: 800,
    letterSpacing: '-0.02em',
    lineHeight: 1.1,
  },
  cardLabel: {
    fontSize: '0.88rem',
    fontWeight: 600,
    color: '#2c3e50',
    marginTop: '0.1rem',
  },
  cardSubtext: {
    fontSize: '0.77rem',
    color: '#9aa5b1',
    marginTop: '0.2rem',
  },

  // Two-col grid — collapses to single column below ~800px
  twoColGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
    gap: '1.5rem',
    alignItems: 'start',
  },
  col: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1.5rem',
  },

  // Panels
  panel: {
    backgroundColor: '#fff',
    borderRadius: '10px',
    padding: '1.5rem',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
    border: '1px solid #f1f3f5',
  },
  panelHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.1rem',
    flexWrap: 'wrap',
    gap: '0.5rem',
  },
  panelTitle: {
    margin: 0,
    fontSize: '1rem',
    color: '#1a202c',
    fontWeight: 700,
    letterSpacing: '-0.01em',
  },
  subHeading: {
    fontSize: '0.88rem',
    color: '#495057',
    marginBottom: '0.75rem',
    fontWeight: 600,
  },
  rateBadge: {
    fontSize: '0.8rem',
    backgroundColor: '#d1e7dd',
    color: '#0f5132',
    padding: '4px 10px',
    borderRadius: '12px',
    fontWeight: 500,
  },
  expiryBadge: {
    fontSize: '0.78rem',
    fontWeight: 700,
    color: '#c0392b',
    backgroundColor: '#fdecea',
    padding: '3px 10px',
    borderRadius: '12px',
  },
  pillRow: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '0.5rem',
  },
  viewAllLink: {
    fontSize: '0.82rem',
    color: '#3498db',
    textDecoration: 'none',
    fontWeight: 600,
    whiteSpace: 'nowrap',
  },

  // Empty states
  emptyState: {
    textAlign: 'center',
    padding: '1.75rem 1rem',
  },
  emptyStateIcon: {
    fontSize: '2rem',
    display: 'block',
    marginBottom: '0.6rem',
    opacity: 0.5,
  },
  emptyStateTitle: {
    fontSize: '0.9rem',
    fontWeight: 600,
    color: '#495057',
    margin: '0 0 0.35rem',
  },
  emptyStateSubtext: {
    fontSize: '0.8rem',
    color: '#adb5bd',
    maxWidth: '280px',
    margin: '0 auto',
    lineHeight: 1.5,
  },
  emptyText: {
    color: '#9aa5b1',
    fontSize: '0.85rem',
    margin: 0,
  },

  // Donation summary line
  donationSummary: {
    fontSize: '0.82rem',
    color: '#6c757d',
    marginBottom: '0.25rem',
  },

  // Dispatch summary chips
  dispatchSummary: {
    display: 'flex',
    gap: '0.5rem',
    flexWrap: 'wrap',
    marginBottom: '1rem',
  },
  summaryChip: {
    backgroundColor: '#fff3cd',
    color: '#856404',
    padding: '3px 10px',
    borderRadius: '12px',
    fontSize: '0.78rem',
    fontWeight: 600,
  },

  // Tables
  tableScroll: {
    overflowX: 'auto',
    WebkitOverflowScrolling: 'touch',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    textAlign: 'left',
    fontSize: '0.9rem',
    minWidth: '600px',
  },
  miniTable: {
    width: '100%',
    borderCollapse: 'collapse',
    textAlign: 'left',
    fontSize: '0.85rem',
    minWidth: '340px',
  },
  tableActionBtn: {
    color: '#2980b9',
    textDecoration: 'none',
    fontWeight: 700,
    fontSize: '0.82rem',
  },

  // Blood type badge
  bloodTypeBadge: {
    backgroundColor: '#fdecea',
    color: '#c0392b',
    padding: '2px 8px',
    borderRadius: '10px',
    fontSize: '0.78rem',
    fontWeight: 700,
    whiteSpace: 'nowrap',
  },
}
