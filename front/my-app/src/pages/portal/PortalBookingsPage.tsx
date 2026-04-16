/**
 * Provider portal: bookings for the signed-in provider's listings (read-only).
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { BookingDto } from '../../types/api'
import { Card } from '../../components/Card'

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
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <>
      <h1 className="app-page-title">{t('title.bookings')}</h1>
      {error && (
        <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}

      <Card className="mt-6 overflow-x-auto p-0">
        {loading ? (
          <p className="p-6 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : rows.length === 0 ? (
          <p className="p-6 text-slate-600 dark:text-slate-300">{t('portalPages.noBookings')}</p>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50/80 dark:border-slate-700 dark:bg-slate-900/50">
              <tr>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('bookingsPage.reference')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('bookingsPage.service')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('bookingsPage.checkIn')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('bookingsPage.checkOut')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('bookingsPage.amount')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('bookingsPage.status')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
              {rows.map((b) => (
                <tr key={b.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40">
                  <td className="px-4 py-3 font-mono text-xs text-slate-800 dark:text-slate-100">{b.bookingReference}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                    {b.serviceName?.trim() || t('ui.emDash')}
                  </td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{b.checkInDate ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{b.checkOutDate ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                    {b.totalAmount != null ? `${b.currency ?? ''} ${b.totalAmount}`.trim() : '—'}
                  </td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{b.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
