import React, { useContext, useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import notificationService from '../services/notificationService'

export default function Navbar() {
  const { user, logout } = useContext(AuthContext)
  const navigate = useNavigate()

  const [unreadCount, setUnreadCount] = useState(0)
  const [notifications, setNotifications] = useState([])
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 820)
  const dropdownRef = useRef(null)

  // Track viewport width for responsive hamburger nav
  useEffect(() => {
    const onResize = () => {
      const mobile = window.innerWidth <= 820
      setIsMobile(mobile)
      if (!mobile) setMenuOpen(false)
    }
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  // Poll unread count every 30 seconds when user is authenticated
  useEffect(() => {
    if (!user) return

    const fetchCount = async () => {
      try {
        const res = await notificationService.getUnreadCount()
        if (res && res.data && typeof res.data.unreadCount === 'number') {
          setUnreadCount(res.data.unreadCount)
        }
      } catch (err) {
        // Silently catch polling errors to avoid interrupting user workflow
      }
    }

    fetchCount()
    const interval = setInterval(fetchCount, 30000)
    return () => clearInterval(interval)
  }, [user])

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const toggleDropdown = async () => {
    if (!dropdownOpen) {
      setLoading(true)
      try {
        const res = await notificationService.getNotifications(false, 5)
        if (res && res.data) {
          setNotifications(res.data)
        }
      } catch (err) {
        console.error('Failed to load notifications', err)
      } finally {
        setLoading(false)
      }
    }
    setDropdownOpen(!dropdownOpen)
  }

  const handleNotificationClick = async (notif) => {
    try {
      if (!notif.isRead) {
        await notificationService.markAsRead(notif.id)
        setNotifications((prev) =>
          prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n))
        )
        setUnreadCount((prev) => Math.max(0, prev - 1))
      }
    } catch (err) {
      console.error('Failed to mark notification as read', err)
    }

    setDropdownOpen(false)
    if (notif.referenceType === 'BLOOD_REQUEST' && notif.referenceId) {
      navigate(`/blood-requests/${notif.referenceId}`)
    } else if (notif.referenceType === 'DISPATCH') {
      navigate('/dispatches')
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead()
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
      setUnreadCount(0)
    } catch (err) {
      console.error('Failed to mark all as read', err)
    }
  }

  const formatTimestamp = (dateStr) => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' })
  }

  return (
    <nav style={styles.nav}>
      {/* Top bar: always visible */}
      <div style={styles.navTop}>
        <Link to="/" style={styles.brandLink}>🩸 BloodLink</Link>

        {/* Hamburger — mobile only */}
        {isMobile && (
          <button
            onClick={() => setMenuOpen((o) => !o)}
            style={styles.hamburger}
            aria-label="Toggle navigation"
          >
            {menuOpen ? '✕' : '☰'}
          </button>
        )}

        {/* Desktop link bar — inline with brand */}
        {!isMobile && (
          <ul style={styles.links}>
            <li><Link to="/" style={styles.link}>Home</Link></li>
            {user ? (
              <>
                <li><Link to="/dashboard" style={styles.link}>Dashboard</Link></li>
                <li><Link to="/hospitals" style={styles.link}>Hospitals</Link></li>
                <li><Link to="/blood-banks" style={styles.link}>Blood Banks</Link></li>
                <li><Link to="/blood-requests" style={styles.link}>Requests</Link></li>
                <li><Link to="/dispatches" style={styles.link}>Dispatches</Link></li>
                {(user.role === 'BLOOD_BANK_STAFF' || user.role === 'SYSTEM_ADMIN') && (
                  <>
                    <li><Link to="/donors" style={styles.link}>Donors</Link></li>
                    <li><Link to="/blood-units" style={styles.link}>Blood Units</Link></li>
                  </>
                )}
                <li ref={dropdownRef} style={{ position: 'relative' }}>
                  <button onClick={toggleDropdown} style={styles.bellBtn} title="Notifications" aria-label="Notifications">
                    🔔
                    {unreadCount > 0 && (
                      <span style={styles.badge}>{unreadCount > 9 ? '9+' : unreadCount}</span>
                    )}
                  </button>
                  {dropdownOpen && (
                    <div style={styles.dropdown}>
                      <div style={styles.dropdownHeader}>
                        <span style={{ fontWeight: 700, color: '#2c3e50', fontSize: '0.9rem' }}>
                          Notifications {unreadCount > 0 && `(${unreadCount} unread)`}
                        </span>
                        {unreadCount > 0 && (
                          <button onClick={handleMarkAllRead} style={styles.markAllBtn}>Mark all as read</button>
                        )}
                      </div>
                      <div style={styles.notificationList}>
                        {loading ? (
                          <div style={styles.emptyMsg}>Loading...</div>
                        ) : notifications.length === 0 ? (
                          <div style={styles.emptyMsg}>No notifications yet.</div>
                        ) : (
                          notifications.map((n) => (
                            <div
                              key={n.id}
                              onClick={() => handleNotificationClick(n)}
                              style={{ ...styles.notifItem, backgroundColor: n.isRead ? '#ffffff' : '#f0f7ff' }}
                            >
                              <div style={styles.notifHeader}>
                                <span style={{ ...styles.notifTitle, fontWeight: n.isRead ? 600 : 700, color: n.isRead ? '#495057' : '#0d6efd' }}>
                                  {!n.isRead && '● '}{n.title}
                                </span>
                                <span style={styles.notifTime}>{formatTimestamp(n.createdAt)}</span>
                              </div>
                              <div style={styles.notifBody}>{n.message}</div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </li>
                <li>
                  <span style={styles.username}>
                    👤 {user.username}
                    <span style={styles.roleBadge}>{user.role?.replace('_', ' ')}</span>
                  </span>
                </li>
                <li><button onClick={handleLogout} style={styles.logoutBtn}>Logout</button></li>
              </>
            ) : (
              <>
                <li><Link to="/login" style={styles.link}>Login</Link></li>
                <li><Link to="/register" style={styles.registerLink}>Register</Link></li>
              </>
            )}
          </ul>
        )}
      </div>

      {/* Mobile dropdown menu */}
      {isMobile && menuOpen && (
        <ul style={styles.mobileLinks}>
          <li><Link to="/" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Home</Link></li>
          {user ? (
            <>
              <li><Link to="/dashboard" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Dashboard</Link></li>
              <li><Link to="/hospitals" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Hospitals</Link></li>
              <li><Link to="/blood-banks" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Blood Banks</Link></li>
              <li><Link to="/blood-requests" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Requests</Link></li>
              <li><Link to="/dispatches" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Dispatches</Link></li>
              {(user.role === 'BLOOD_BANK_STAFF' || user.role === 'SYSTEM_ADMIN') && (
                <>
                  <li><Link to="/donors" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Donors</Link></li>
                  <li><Link to="/blood-units" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Blood Units</Link></li>
                </>
              )}
              <li style={{ paddingTop: '0.5rem', borderTop: '1px solid rgba(255,255,255,0.2)' }}>
                <span style={{ color: 'rgba(255,255,255,0.85)', fontSize: '0.88rem' }}>
                  👤 {user.username} · {user.role?.replace(/_/g, ' ')}
                </span>
              </li>
              <li>
                <button onClick={handleLogout} style={{ ...styles.logoutBtn, width: '100%' }}>Logout</button>
              </li>
            </>
          ) : (
            <>
              <li><Link to="/login" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Login</Link></li>
              <li><Link to="/register" style={styles.mobileLink} onClick={() => setMenuOpen(false)}>Register</Link></li>
            </>
          )}
        </ul>
      )}
    </nav>
  )
}

const styles = {
  nav: {
    background: '#c0392b',
    color: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
    position: 'sticky',
    top: 0,
    zIndex: 1000,
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },
  navTop: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 0.5rem',
    maxWidth: '1440px',
    width: '94%',
    margin: '0 auto',
    height: '64px',
  },
  brand: {
    fontSize: '1.4rem',
    fontWeight: '700',
  },
  brandLink: {
    color: '#fff',
    textDecoration: 'none',
    letterSpacing: '0.5px',
    fontSize: '1.35rem',
    fontWeight: 700,
  },
  // Desktop horizontal link list (rendered inside navTop via !isMobile)
  links: {
    display: 'flex',
    alignItems: 'center',
    gap: '1.25rem',
    listStyle: 'none',
    margin: 0,
    padding: 0,
    flexWrap: 'nowrap',
  },
  // Mobile full-width dropdown list
  mobileLinks: {
    display: 'flex',
    flexDirection: 'column',
    listStyle: 'none',
    margin: 0,
    padding: '0.5rem 1.5rem 1rem',
    gap: '0.5rem',
    borderTop: '1px solid rgba(255,255,255,0.15)',
    backgroundColor: '#a93226',
  },
  mobileLink: {
    color: 'rgba(255,255,255,0.92)',
    textDecoration: 'none',
    fontWeight: 500,
    fontSize: '0.95rem',
    display: 'block',
    padding: '0.35rem 0',
  },
  hamburger: {
    background: 'rgba(255,255,255,0.15)',
    border: '1.5px solid rgba(255,255,255,0.4)',
    color: '#fff',
    borderRadius: '6px',
    width: '38px',
    height: '38px',
    fontSize: '1.15rem',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: 'inherit',
  },
  link: {
    color: 'rgba(255,255,255,0.9)',
    textDecoration: 'none',
    fontWeight: 500,
    fontSize: '0.92rem',
    whiteSpace: 'nowrap',
    transition: 'color 0.2s',
  },
  registerLink: {
    color: '#fff',
    background: 'rgba(255,255,255,0.2)',
    padding: '0.35rem 0.9rem',
    borderRadius: '6px',
    textDecoration: 'none',
    fontWeight: '600',
    fontSize: '0.9rem',
  },
  username: {
    color: 'rgba(255,255,255,0.85)',
    fontSize: '0.9rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  roleBadge: {
    background: 'rgba(255,255,255,0.2)',
    padding: '2px 8px',
    borderRadius: '10px',
    fontSize: '0.75rem',
    fontWeight: '600',
    letterSpacing: '0.3px',
  },
  logoutBtn: {
    background: 'rgba(255,255,255,0.15)',
    border: '1.5px solid rgba(255,255,255,0.5)',
    color: '#fff',
    padding: '0.35rem 0.9rem',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: '600',
    fontSize: '0.9rem',
    transition: 'background 0.2s',
  },
  bellBtn: {
    background: 'rgba(255,255,255,0.15)',
    border: 'none',
    borderRadius: '50%',
    width: '38px',
    height: '38px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1.1rem',
    position: 'relative',
    color: '#fff',
    transition: 'background 0.2s',
  },
  badge: {
    position: 'absolute',
    top: '-3px',
    right: '-3px',
    backgroundColor: '#ff4757',
    color: '#fff',
    borderRadius: '10px',
    padding: '1px 5px',
    fontSize: '0.7rem',
    fontWeight: 'bold',
    minWidth: '16px',
    textAlign: 'center',
    boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
  },
  dropdown: {
    position: 'absolute',
    top: '46px',
    right: 0,
    width: '340px',
    backgroundColor: '#fff',
    borderRadius: '8px',
    boxShadow: '0 8px 24px rgba(0,0,0,0.25)',
    color: '#333',
    zIndex: 2000,
    overflow: 'hidden',
    border: '1px solid #e9ecef',
  },
  dropdownHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0.75rem 1rem',
    borderBottom: '1px solid #e9ecef',
    backgroundColor: '#f8f9fa',
  },
  markAllBtn: {
    background: 'none',
    border: 'none',
    color: '#0d6efd',
    fontSize: '0.78rem',
    cursor: 'pointer',
    fontWeight: '600',
    padding: 0,
  },
  notificationList: {
    maxHeight: '360px',
    overflowY: 'auto',
  },
  notifItem: {
    padding: '0.75rem 1rem',
    borderBottom: '1px solid #f1f3f5',
    cursor: 'pointer',
    transition: 'background-color 0.15s',
  },
  notifHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'baseline',
    marginBottom: '0.25rem',
  },
  notifTitle: {
    fontSize: '0.85rem',
  },
  notifTime: {
    fontSize: '0.72rem',
    color: '#868e96',
  },
  notifBody: {
    fontSize: '0.8rem',
    color: '#495057',
    lineHeight: '1.35',
  },
  emptyMsg: {
    padding: '1.5rem',
    textAlign: 'center',
    color: '#868e96',
    fontSize: '0.85rem',
  },
}
