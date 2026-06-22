/**
 * Provider portal: payout request history + submit new withdrawal request.
 * Route: /portal/payouts — gated on portal:finance.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { portalAPI, getApiErrorMessage } from '../../services/api'
import type { PortalEarningsDto, PortalPayoutRequestDto } from '../../types/api'
import { Card } from '../../components/Card'

const STATUS_BADGE: Record<string, string> = {
  PENDING:    'badge badge-warning',
  PROCESSING: 'badge badge-info',
  COMPLETED:  'badge badge-success',
  REJECTED:   'badge badge-danger',
  CANCELLED:  'badge badge-danger',
  FAILED:     'badge badge-danger',
  ON_HOLD:    'bg-slate-200 text-slate-700 rounded-full px-2.5 py-0.5 text-xs font-semibold dark:bg-slate-700 dark:text-slate-300',
  SCHEDULED:  'bg-sky-100 text-sky-800 rounded-full px-2.5 py-0.5 text-xs font-semibold dark:bg-sky-900/30 dark:text-sky-300',
}

function StatusBadge({ status, t }: { status: string; t: (k: string) => string }) {
  const cls = STATUS_BADGE[status.toUpperCase()] ?? 'badge'
  const labelKey = `portalPayouts.status${status.charAt(0) + status.slice(1).toLowerCase()}`
  return <span className={cls}>{t(labelKey) !== labelKey ? t(labelKey) : status}</span>
}

export function PortalPayoutsPage() {
  const { t } = useLanguage()

  const [earnings, setEarnings] = useState<PortalEarningsDto | null>(null)
  const [history, setHistory] = useState<PortalPayoutRequestDto[]>([])
  const [historyLoading, setHistoryLoading] = useState(true)
  const [earningsLoading, setEarningsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  const [amount, setAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const loadHistory = useCallback(() => {
    setHistoryLoading(true)
    portalAPI
      .listPayouts({ page: 0, size: 50 })
      .then((r) => {
        const data = r.data as { content?: PortalPayoutRequestDto[] } | PortalPayoutRequestDto[]
        setHistory(Array.isArray(data) ? data : ((data as { content?: PortalPayoutRequestDto[] }).content ?? []))
      })
      .catch(() => setHistory([]))
      .finally(() => setHistoryLoading(false))
  }, [])

  useEffect(() => {
    setEarningsLoading(true)
    portalAPI
      .getEarnings()
      .then((r) => setEarnings(r.data as PortalEarningsDto))
      .catch(() => setEarnings(null))
      .finally(() => setEarningsLoading(false))
    loadHistory()
  }, [loadHistory])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const parsed = parseFloat(amount)
    if (!parsed || parsed <= 0) return
    setSubmitting(true)
    setError(null)
    setSuccessMsg(null)
    try {
      await portalAPI.requestPayout({ amount: parsed, notes: notes.trim() || undefined })
      setSuccessMsg(t('portalPayouts.submitSuccess'))
      setAmount('')
      setNotes('')
      setEarningsLoading(true)
      portalAPI.getEarnings().then((r) => setEarnings(r.data as PortalEarningsDto)).catch(() => {}).finally(() => setEarningsLoading(false))
      loadHistory()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  const available = earnings?.availableForPayout ?? 0
  const currency = earnings?.currency ?? 'USD'

  return (
    <>
      <h1 className="app-page-title">{t('portalPayouts.title')}</h1>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {successMsg && (
        <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300">
          {successMsg}
        </div>
      )}

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        {/* Balance card */}
        <Card className="lg:col-span-1">
          <p className="text-sm text-slate-500 dark:text-slate-400">{t('portalPayouts.availableLabel')}</p>
          {earningsLoading ? (
            <div className="mt-2 h-8 w-32 animate-pulse rounded bg-slate-100 dark:bg-white/[0.05]" />
          ) : (
            <p className="mt-1 text-3xl font-bold text-emerald-600 dark:text-emerald-400">
              {currency} {Number(available).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </p>
          )}
          {earnings && (
            <div className="mt-3 space-y-1 border-t border-slate-100 pt-3 dark:border-slate-700">
              <div className="flex justify-between text-xs text-slate-500 dark:text-slate-400">
                <span>{t('portalPayouts.grossRevenue')}</span>
                <span className="font-medium tabular-nums">{currency} {Number(earnings.grossRevenue ?? earnings.totalEarnings ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-500 dark:text-slate-400">
                <span>{t('portalPayouts.platformFee')} ({earnings.platformCommissionPct ?? 10}%)</span>
                <span className="font-medium tabular-nums text-red-500 dark:text-red-400">−{currency} {Number(earnings.platformFee ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
              <div className="flex justify-between text-xs font-semibold text-slate-700 dark:text-slate-200">
                <span>{t('portalPayouts.yourNet')}</span>
                <span className="tabular-nums">{currency} {Number(earnings.providerNet ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-6 space-y-3">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('portalPayouts.requestTitle')}</h3>
            <div>
              <label className="block text-xs text-slate-600 dark:text-slate-400">{t('portalPayouts.amountLabel')}</label>
              <input
                type="number"
                required
                min="0.01"
                step="0.01"
                max={available > 0 ? available : undefined}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              />
            </div>
            <div>
              <label className="block text-xs text-slate-600 dark:text-slate-400">{t('portalPayouts.notesLabel')}</label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder={t('portalPayouts.notesPlaceholder')}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              />
            </div>
            <button
              type="submit"
              disabled={submitting || !amount || parseFloat(amount) <= 0}
              className="dashboard-btn-primary w-full disabled:opacity-50"
            >
              {submitting ? t('ui.saving') : t('portalPayouts.submitBtn')}
            </button>
          </form>
        </Card>

        {/* History */}
        <Card className="lg:col-span-2">
          <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">{t('portalPayouts.historyTitle')}</h2>
          <div className="mt-4 table-shell overflow-x-auto">
            {historyLoading ? (
              <div className="flex flex-col gap-2 p-4">
                {Array.from({ length: 4 }, (_, i) => (
                  <div key={i} className="h-9 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
                ))}
              </div>
            ) : history.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
                {t('portalPayouts.empty')}
              </p>
            ) : (
              <table className="min-w-full">
                <thead>
                  <tr>
                    <th className="px-4 py-3.5 text-left">{t('portalPayouts.colDate')}</th>
                    <th className="px-4 py-3.5 text-left">{t('portalPayouts.colAmount')}</th>
                    <th className="px-4 py-3.5 text-left">{t('portalPayouts.colStatus')}</th>
                    <th className="px-4 py-3.5 text-left">{t('portalPayouts.colNotes')}</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((row) => (
                    <tr key={row.id}>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                        {row.requestedAt ? new Date(row.requestedAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : '—'}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm font-medium text-slate-800 dark:text-slate-100">
                        {row.currency ?? currency} {Number(row.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3">
                        <StatusBadge status={row.status} t={t} />
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                        {row.notes ?? '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </Card>
      </div>
    </>
  )
}
