/**
 * Notification bell with dropdown for provider portal.
 * Polls unread count every 30 seconds.
 */

import { useEffect, useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { notificationsAPI } from '../services/api'
import { useLanguage } from '../context/LanguageContext'

interface Notification {
  id: string
  title: string
  message: string
  type: string
  referenceId?: string
  read: boolean
  createdAt: string
}

function getPortalNotificationLink(type: string): string | null {
  switch (type) {
    case 'BOOKING_CONFIRMATION':
    case 'BOOKING_CANCELLED':
    case 'BOOKING_CONFIRMED_STAFF':
    case 'PAYMENT_SUCCESS':
    case 'PAYMENT_FAILED':
      return '/portal/bookings'
    case 'PORTAL_SUPPORT_REQUEST':
      return '/portal/support'
    default:
      return null
  }
}

export function NotificationBell() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  const fetchCount = useCallback(() => {
    notificationsAPI.getUnreadCount()
      .then((res: { data: unknown }) => setUnread(typeof res.data === 'number' ? res.data : 0))
      .catch(() => {})
  }, [])

  useEffect(() => {
    fetchCount()
    const interval = setInterval(fetchCount, 30_000)
    return () => clearInterval(interval)
  }, [fetchCount])

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  async function toggleOpen() {
    if (!open) {
      setLoading(true)
      try {
        const res = await notificationsAPI.list({ page: 0, size: 20 })
        const data = res.data as { content: Notification[] }
        setNotifications(data.content ?? [])
      } catch {
        setNotifications([])
      } finally {
        setLoading(false)
      }
    }
    setOpen(!open)
  }

  async function markAllRead() {
    try {
      await notificationsAPI.markAllAsRead()
      setUnread(0)
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
    } catch {}
  }

  async function markRead(id: string) {
    try {
      await notificationsAPI.markAsRead(id)
      setUnread((c) => Math.max(0, c - 1))
      setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n))
    } catch {}
  }

  function handleNotificationClick(n: Notification) {
    if (!n.read) markRead(n.id)
    const link = getPortalNotificationLink(n.type)
    if (link) {
      navigate(link)
      setOpen(false)
    }
  }

  return (
    <div ref={ref} className="relative">
      <button
        onClick={toggleOpen}
        className="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-white/[0.06] transition"
        aria-label={t('portalPages.notificationBell')}
      >
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 rounded-xl border border-slate-200 bg-white shadow-lg dark:border-slate-700 dark:bg-slate-800">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3 dark:border-slate-700">
            <h3 className="text-sm font-semibold">{t('portalPages.notificationBell')}</h3>
            {unread > 0 && (
              <button onClick={markAllRead} className="text-xs text-primary hover:underline">
                {t('portalPages.notificationMarkAllRead')}
              </button>
            )}
          </div>
          <div className="max-h-80 overflow-y-auto">
            {loading && <p className="px-4 py-6 text-center text-sm text-slate-400">Loading…</p>}
            {!loading && notifications.length === 0 && (
              <p className="px-4 py-6 text-center text-sm text-slate-400">{t('portalPages.notificationEmpty')}</p>
            )}
            {notifications.map((n) => {
              const link = getPortalNotificationLink(n.type)
              return (
                <button
                  key={n.id}
                  onClick={() => handleNotificationClick(n)}
                  className={`w-full text-left px-4 py-3 border-b border-slate-50 dark:border-slate-700 last:border-0 hover:bg-slate-50 dark:hover:bg-white/[0.03] transition ${
                    !n.read ? 'bg-blue-50/50 dark:bg-blue-900/10' : ''
                  }`}
                >
                  <div className="flex items-start gap-2">
                    {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-500" />}
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium truncate">{n.title}</p>
                      <p className="text-xs text-slate-500 line-clamp-2">{n.message}</p>
                      <div className="mt-0.5 flex items-center gap-1.5">
                        <p className="text-[10px] text-slate-400">
                          {new Date(n.createdAt).toLocaleDateString()}
                        </p>
                        {link && <span className="text-[10px] text-primary opacity-60">→</span>}
                      </div>
                    </div>
                  </div>
                </button>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
