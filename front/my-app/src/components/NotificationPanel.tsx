/**
 * Notification dropdown panel – lists notifications (paged), mark as read / mark all read.
 */

import { useEffect, useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useLanguage } from '../context/LanguageContext'
import { notificationsAPI, adminMediaAPI } from '../services/api'
import type { NotificationDto, NotificationInboxDto } from '../types/api'

const PAGE_SIZE = 20

function formatTime(iso: string | null | undefined, t: (key: string, params?: Record<string, string | number>) => string) {
  if (!iso) return t('ui.emDash')
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  if (diffMs < 60_000) return t('notifications.justNow')
  if (diffMs < 3600_000) return t('notifications.minutesAgo', { n: Math.floor(diffMs / 60_000) })
  if (diffMs < 86400_000) return t('notifications.hoursAgo', { n: Math.floor(diffMs / 3600_000) })
  return d.toLocaleDateString()
}

interface NotificationPanelProps {
  isOpen: boolean
  onClose: () => void
  anchorRef: React.RefObject<HTMLButtonElement | null>
  onMarkAllRead?: () => void
  onUnreadCount?: (count: number) => void
}

export function NotificationPanel({ isOpen, onClose, anchorRef, onMarkAllRead, onUnreadCount }: NotificationPanelProps) {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState<NotificationDto[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [mediaProcessing, setMediaProcessing] = useState<string | null>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  const fetchPage = useCallback(
    async (pageIndex: number, append: boolean) => {
      const res = await notificationsAPI.list({ page: pageIndex, size: PAGE_SIZE })
      const inbox = res.data as NotificationInboxDto
      const pg = inbox?.notifications
      const chunk = pg?.content ?? []
      if (append) {
        setNotifications((prev) => [...prev, ...chunk])
      } else {
        setNotifications(chunk)
      }
      const unread = Number(inbox?.unreadCount ?? 0)
      onUnreadCount?.(unread)
      if (pg) {
        setHasMore(pg.number < pg.totalPages - 1)
      } else {
        setHasMore(false)
      }
    },
    [onUnreadCount],
  )

  useEffect(() => {
    if (!isOpen) return
    setLoading(true)
    setPage(0)
    fetchPage(0, false)
      .catch(() => {
        setNotifications([])
        onUnreadCount?.(0)
        setHasMore(false)
      })
      .finally(() => setLoading(false))
  }, [isOpen, fetchPage, onUnreadCount])

  const loadMore = () => {
    if (!hasMore || loadingMore) return
    const next = page + 1
    setLoadingMore(true)
    fetchPage(next, true)
      .then(() => setPage(next))
      .catch(() => {})
      .finally(() => setLoadingMore(false))
  }

  useEffect(() => {
    if (!isOpen) return
    const handleClick = (e: MouseEvent) => {
      if (
        panelRef.current?.contains(e.target as Node) ||
        anchorRef.current?.contains(e.target as Node)
      ) return
      onClose()
    }
    document.addEventListener('click', handleClick)
    return () => document.removeEventListener('click', handleClick)
  }, [isOpen, onClose, anchorRef])

  const handleMarkAsRead = async (id: string) => {
    try {
      await notificationsAPI.markAsRead(id)
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString(), status: 'READ' } : n))
      )
    } catch {
      // ignore
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await notificationsAPI.markAllAsRead()
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, readAt: n.readAt ?? new Date().toISOString(), status: 'READ' }))
      )
      onMarkAllRead?.()
      onUnreadCount?.(0)
    } catch {
      // ignore
    }
  }

  const handleMediaApprove = async (n: NotificationDto) => {
    if (!n.referenceId) return
    setMediaProcessing(n.id)
    try {
      await adminMediaAPI.approve(n.referenceId)
      await handleMarkAsRead(n.id)
      setNotifications((prev) =>
        prev.map((x) => (x.id === n.id ? { ...x, readAt: x.readAt ?? new Date().toISOString() } : x))
      )
    } catch {
      // ignore — full page has detailed error handling
    } finally {
      setMediaProcessing(null)
    }
  }

  const unreadCount = notifications.filter((n) => !n.readAt).length

  if (!isOpen) return null

  return (
    <div
      ref={panelRef}
      className="absolute right-0 top-full z-50 mt-1 w-96 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-600 dark:bg-slate-800"
    >
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-600">
        <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('notifications.title')}</h3>
        {unreadCount > 0 && (
          <button
            type="button"
            onClick={handleMarkAllRead}
            className="text-xs font-medium text-primary hover:underline"
          >
            {t('notifications.markAllRead')}
          </button>
        )}
      </div>
      <div className="max-h-80 overflow-y-auto">
        {loading ? (
          <div className="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
            {t('notifications.loading')}
          </div>
        ) : notifications.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
            {t('notifications.empty')}
          </div>
        ) : (
          <ul className="divide-y divide-slate-200 dark:divide-slate-600">
            {notifications.map((n) => (
              <li
                key={n.id}
                className={`px-4 py-3 transition-colors hover:bg-slate-50 dark:hover:bg-slate-700/50 ${
                  !n.readAt ? 'bg-primary/5 dark:bg-primary/10' : ''
                }`}
              >
                <div className="flex justify-between gap-2">
                  <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                    {n.title ?? t('notifications.fallbackTitle')}
                  </p>
                  {!n.readAt && (
                    <button
                      type="button"
                      onClick={() => handleMarkAsRead(n.id)}
                      className="shrink-0 text-xs text-primary hover:underline"
                    >
                      {t('notifications.markRead')}
                    </button>
                  )}
                </div>
                {n.message && (
                  <p className="mt-0.5 line-clamp-2 text-xs text-slate-600 dark:text-slate-300">
                    {n.message}
                  </p>
                )}
                {n.type === 'MEDIA_SUBMISSION_PENDING' && n.referenceId && (
                  <div className="mt-2 flex items-center gap-3">
                    <button
                      type="button"
                      disabled={mediaProcessing === n.id}
                      onClick={() => handleMediaApprove(n)}
                      className="rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-40 dark:bg-emerald-900/30 dark:text-emerald-300 dark:hover:bg-emerald-900/50"
                    >
                      {t('mediaSubmissionsPage.approve')}
                    </button>
                    <button
                      type="button"
                      onClick={() => { onClose(); navigate('/admin/media-submissions') }}
                      className="text-xs text-primary hover:underline"
                    >
                      {t('notifications.review')}
                    </button>
                  </div>
                )}
                <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                  {formatTime(n.sentAt ?? n.createdAt, t)}
                </p>
              </li>
            ))}
          </ul>
        )}
        {hasMore && !loading && notifications.length > 0 && (
          <div className="border-t border-slate-200 p-2 dark:border-slate-600">
            <button
              type="button"
              onClick={loadMore}
              disabled={loadingMore}
              className="w-full rounded-lg py-2 text-center text-xs font-medium text-primary hover:bg-slate-50 disabled:opacity-50 dark:hover:bg-slate-700/50"
            >
              {loadingMore ? t('notifications.loading') : t('ui.loadMore')}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
