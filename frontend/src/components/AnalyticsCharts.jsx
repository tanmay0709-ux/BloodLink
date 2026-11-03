import React from 'react'

/**
 * Clean, lightweight, dependency-free CSS/SVG visual chart components
 * Polished for BloodLink Module 8 dashboard UI.
 */

// ─── 1. Horizontal Bar Distribution Widget ───────────────────────────────────
export function DistributionBar({ items = [], max = 0 }) {
  if (!items || items.length === 0) return null
  const calculatedMax = max > 0 ? max : Math.max(...items.map((i) => i.value), 1)

  return (
    <div style={styles.distList}>
      {items.map((item, index) => {
        const pct = Math.round((item.value / calculatedMax) * 100)
        return (
          <div key={index} style={styles.distRow}>
            <div style={styles.distHeader}>
              <span style={styles.distLabel}>{item.label}</span>
              <span style={styles.distValue}>
                {item.value} {item.unit || ''}
              </span>
            </div>
            <div style={styles.distTrack}>
              <div
                style={{
                  ...styles.distFill,
                  width: `${Math.max(pct, item.value > 0 ? 2 : 0)}%`,
                  backgroundColor: item.color || '#e74c3c',
                }}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ─── 2. Monthly Trend Bar Chart ───────────────────────────────────────────────
export function MonthlyTrendChart({ trends = [] }) {
  if (!trends || trends.length === 0) {
    return (
      <div style={styles.emptyChart}>
        <span style={styles.emptyIcon}>📊</span>
        <p style={styles.emptyText}>No trend data recorded yet.</p>
      </div>
    )
  }

  const maxCount = Math.max(...trends.map((t) => t.donationCount || 0), 1)

  // Format label: "Nov 2026" → "Nov '26"
  const formatLabel = (label) => {
    if (!label) return ''
    const parts = label.split(' ')
    if (parts.length === 2) {
      return `${parts[0]} '${parts[1].slice(-2)}`
    }
    return parts[0]
  }

  return (
    <div style={styles.trendWrapper}>
      <div style={styles.trendYLabel}>Donations</div>
      <div style={styles.trendContent}>
        <div style={styles.trendBarsRow}>
          {trends.map((t, idx) => {
            const heightPct = t.donationCount > 0
              ? Math.max(Math.round((t.donationCount / maxCount) * 100), 6)
              : 3
            return (
              <div key={idx} style={styles.trendCol}>
                {t.donationCount > 0 && (
                  <div style={styles.trendValue}>{t.donationCount}</div>
                )}
                <div style={styles.trendBarTrack}>
                  <div
                    style={{
                      ...styles.trendBarFill,
                      height: `${heightPct}%`,
                      backgroundColor: t.donationCount > 0 ? '#3498db' : '#e9ecef',
                    }}
                    title={`${t.label}: ${t.donationCount} donation${t.donationCount !== 1 ? 's' : ''} (${t.totalVolumeMl ?? 0} ml)`}
                  />
                </div>
                <div style={styles.trendLabel}>{formatLabel(t.label)}</div>
              </div>
            )
          })}
        </div>
        <div style={styles.trendBaseline} />
      </div>
    </div>
  )
}

// ─── 3. Status Badge Pill ─────────────────────────────────────────────────────
export function StatusPill({ status, count }) {
  const colors = {
    PENDING:    { bg: '#fff3cd', color: '#856404', border: '#ffc107' },
    REVIEWED:   { bg: '#cce5ff', color: '#004085', border: '#b8daff' },
    ALLOCATED:  { bg: '#e2e3e5', color: '#383d41', border: '#d6d8db' },
    DISPATCHED: { bg: '#d4edda', color: '#155724', border: '#c3e6cb' },
    FULFILLED:  { bg: '#d1e7dd', color: '#0f5132', border: '#badbcc' },
    CANCELLED:  { bg: '#f8d7da', color: '#721c24', border: '#f5c6cb' },
    REJECTED:   { bg: '#f8d7da', color: '#842029', border: '#f5c6cb' },
    AVAILABLE:  { bg: '#d1e7dd', color: '#0f5132', border: '#badbcc' },
    RESERVED:   { bg: '#fff3cd', color: '#856404', border: '#ffc107' },
    ISSUED:     { bg: '#e2e3e5', color: '#383d41', border: '#d6d8db' },
    EXPIRED:    { bg: '#f8d7da', color: '#721c24', border: '#f5c6cb' },
    COMPLETED:  { bg: '#d1e7dd', color: '#0f5132', border: '#badbcc' },
  }

  const c = colors[status] || { bg: '#e9ecef', color: '#495057', border: '#dee2e6' }

  return (
    <div style={{
      ...styles.pill,
      backgroundColor: c.bg,
      color: c.color,
      border: `1px solid ${c.border}`,
    }}>
      <span style={{ fontWeight: 700, fontSize: '0.8rem', letterSpacing: '0.02em' }}>{status}</span>
      {count !== '' && count !== undefined && count !== null && (
        <span style={{
          fontWeight: 800,
          fontSize: '0.95rem',
          marginLeft: '6px',
          minWidth: '20px',
          textAlign: 'center',
        }}>{count}</span>
      )}
    </div>
  )
}

// ─── Styles ──────────────────────────────────────────────────────────────────
const styles = {
  // DistributionBar
  distList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.9rem',
  },
  distRow: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.3rem',
  },
  distHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '0.88rem',
    fontWeight: 600,
    color: '#34495e',
  },
  distLabel: {
    color: '#2c3e50',
    fontWeight: 600,
  },
  distValue: {
    color: '#7f8c8d',
    fontWeight: 500,
  },
  distTrack: {
    height: '12px',
    backgroundColor: '#ecf0f1',
    borderRadius: '6px',
    overflow: 'hidden',
    border: '1px solid #e2e8f0',
  },
  distFill: {
    height: '100%',
    borderRadius: '6px',
    transition: 'width 0.45s ease',
    minWidth: '4px',
  },

  // MonthlyTrendChart
  emptyChart: {
    textAlign: 'center',
    padding: '2rem 1rem',
    color: '#adb5bd',
  },
  emptyIcon: {
    fontSize: '2rem',
    display: 'block',
    marginBottom: '0.5rem',
  },
  emptyText: {
    fontSize: '0.85rem',
    color: '#868e96',
  },
  trendWrapper: {
    display: 'flex',
    alignItems: 'stretch',
    gap: '0.5rem',
    paddingTop: '0.75rem',
  },
  trendYLabel: {
    writingMode: 'vertical-rl',
    transform: 'rotate(180deg)',
    fontSize: '0.72rem',
    fontWeight: 600,
    color: '#95a5a6',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    alignSelf: 'center',
    marginBottom: '1.5rem',
  },
  trendContent: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
  },
  trendBarsRow: {
    display: 'flex',
    alignItems: 'flex-end',
    justifyContent: 'space-around',
    height: '180px',
    paddingBottom: '0.25rem',
  },
  trendBaseline: {
    height: '2px',
    backgroundColor: '#ecf0f1',
    borderRadius: '1px',
    marginBottom: '0.25rem',
  },
  trendCol: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '0.2rem',
    height: '100%',
    justifyContent: 'flex-end',
    flex: 1,
    minWidth: 0,
  },
  trendValue: {
    fontSize: '0.72rem',
    fontWeight: 700,
    color: '#3498db',
    lineHeight: 1,
  },
  trendBarTrack: {
    width: 'clamp(24px, 70%, 44px)',
    height: '150px',
    display: 'flex',
    alignItems: 'flex-end',
    justifyContent: 'center',
  },
  trendBarFill: {
    width: '100%',
    borderRadius: '4px 4px 0 0',
    transition: 'height 0.45s ease',
    minHeight: '4px',
  },
  trendLabel: {
    fontSize: '0.72rem',
    fontWeight: 600,
    color: '#34495e',
    marginTop: '0.35rem',
    textAlign: 'center',
    whiteSpace: 'nowrap',
  },

  // StatusPill
  pill: {
    padding: '0.35rem 0.85rem',
    borderRadius: '20px',
    display: 'inline-flex',
    alignItems: 'center',
    gap: '2px',
  },
}
