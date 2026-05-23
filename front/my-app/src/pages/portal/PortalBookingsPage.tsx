/**
 * Provider portal: bookings for the signed-in provider's listings (read-only).
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { BookingDto } from '../../types/api'

function statusBadge(status?: string | null) {
  if (!status) return <span className="badge badge-neutral">—</span>
  const s = status.toLowerCase()
  if (s === 'confirmed' || s === 'completed') return <span className="badge badge-success">{status}</span>
  if (s === 'pending') return <span className="badge badge-warning">{status}</span>
  if (s === 'cancelled' || s === 'rejected') return <span className="badge badge-danger">{status}</span>
  return <span className="badge badge-neutral">{status}</span>
}

export function PortalBookingsPage() {
  const { t } = useLanguage()
  const [rows, setRows] = useState<BookingDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    portalAPI
      .listBookings()
      .then((res) => {
        const data = res.data
        if (!cancelled) setRows(Array.isArray(data) ? data : [])
      })
      .catch((e) => {
        if (!cancelled) {
          setRows([])
          setError(getApiErrorMessage(e))
        }
      })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  return (
    <>
      <h1 className="app-page-title">{t('title.bookings')}</h1>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="table-shell">
        {loading ? (
          <div className="flex flex-col gap-2 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <div key={i} className="h-10 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="dashboard-empty-state">
            <div className="dashboard-empty-state__icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <path d="M16 2v4M8 2v4M3 10h18" />
              </svg>
            </div>
            <p className="dashboard-empty-state__title">{t('portalPages.noBookings')}</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('bookingsPage.reference')}</th>
                <th>{t('bookingsPage.service')}</th>
                <th>{t('bookingsPage.checkIn')}</th>
                <th>{t('bookingsPage.checkOut')}</th>
                <th>{t('bookingsPage.amount')}</th>
                <th>{t('bookingsPage.status')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((b) => (
                <tr key={b.id}>
                  <td className="font-mono text-xs">{b.bookingReference}</td>
                  <td className="text-slate-600 dark:text-slate-300">{b.serviceName?.trim() || '—'}</td>
                  <td className="tabular-nums text-slate-600 dark:text-slate-300">{b.checkInDate ?? '—'}</td>
                  <td className="tabular-nums text-slate-600 dark:text-slate-300">{b.checkOutDate ?? '—'}</td>
                  <td className="tabular-nums text-slate-600 dark:text-slate-300">
                    {b.totalAmount != null ? `${b.currency ?? ''} ${b.totalAmount}`.trim() : '—'}
                  </td>
                  <td>{statusBadge(b.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
