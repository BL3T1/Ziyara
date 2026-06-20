/**
 * Provider portal: daily cash collection sheet.
 * Shows today's CASH collections recorded by this provider with running totals.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalCashAPI } from '../../services/api'

interface CashCollectionRow {
  id: string
  receiptNumber?: string | null
  bookingId?: string | null
  bookingReference?: string | null
  amount: number
  currency?: string | null
  notes?: string | null
  status: string
  collectedAt?: string | null
}

function statusBadge(status: string, t: (k: string) => string) {
  const s = status.toUpperCase()
  if (s === 'RECONCILED') return <span className="badge badge-success">{t('cashSheet.statusReconciled')}</span>
  if (s === 'DISPUTED') return <span className="badge badge-danger">{t('cashSheet.statusDisputed')}</span>
  return <span className="badge badge-warning">{t('cashSheet.statusOpen')}</span>
}

export function PortalCashSheetPage() {
  const { t } = useLanguage()
  const [rows, setRows] = useState<CashCollectionRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    portalCashAPI
      .getDailySheet()
      .then((res) => {
        if (!cancelled) {
          const data = Array.isArray(res.data) ? (res.data as CashCollectionRow[]) : []
          setRows(data)
        }
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

  useEffect(load, [load])

  const total = rows.reduce((sum, r) => sum + (r.amount ?? 0), 0)
  const currency = rows[0]?.currency ?? 'USD'

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="app-page-title">{t('cashSheet.title')}</h1>
        <button
          type="button"
          onClick={() => window.print()}
          className="dashboard-btn-secondary print:hidden"
        >
          {t('cashSheet.printSheet')}
        </button>
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-4 table-shell">
        {loading ? (
          <div className="flex flex-col gap-2 p-6">
            {Array.from({ length: 4 }, (_, i) => (
              <div key={i} className="h-10 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="dashboard-empty-state">
            <div className="dashboard-empty-state__icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <rect x="2" y="5" width="20" height="14" rx="2" />
                <path d="M2 10h20" />
              </svg>
            </div>
            <p className="dashboard-empty-state__title">{t('cashSheet.noCollections')}</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('cashSheet.colReceipt')}</th>
                <th className="px-4 py-3.5">{t('cashSheet.colBooking')}</th>
                <th className="px-4 py-3.5">{t('cashSheet.colAmount')}</th>
                <th className="px-4 py-3.5">{t('cashSheet.colDate')}</th>
                <th className="px-4 py-3.5">{t('cashSheet.colStatus')}</th>
                <th className="px-4 py-3.5">{t('cashSheet.colNotes')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">
                    {row.receiptNumber ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                    {row.bookingReference ?? row.bookingId ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm text-slate-800 dark:text-slate-100">
                    {row.currency ?? currency} {row.amount?.toFixed(2)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {row.collectedAt ? new Date(row.collectedAt).toLocaleString() : '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    {statusBadge(row.status, t)}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                    {row.notes ?? '—'}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="border-t-2 border-slate-200 dark:border-white/10">
                <td colSpan={2} className="px-4 py-3 text-sm font-semibold text-slate-700 dark:text-slate-200">
                  {t('cashSheet.weekTotal')}
                </td>
                <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm font-bold text-slate-900 dark:text-slate-100">
                  {currency} {total.toFixed(2)}
                </td>
                <td colSpan={3} />
              </tr>
            </tfoot>
          </table>
        )}
      </div>
    </>
  )
}
