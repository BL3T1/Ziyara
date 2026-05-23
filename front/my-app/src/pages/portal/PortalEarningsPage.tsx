/**
 * Provider portal: completed-payment earnings summary with optional date range.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { PortalEarningsDto } from '../../types/api'
import { Card } from '../../components/Card'

function PayoutRequestModal({
  currency,
  maxAmount,
  onClose,
  onSubmit,
}: {
  currency: string
  maxAmount: number
  onClose: () => void
  onSubmit: (amount: number, notes: string) => Promise<void>
}) {
  const [amount, setAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const num = parseFloat(amount)
    if (!num || num <= 0) { setError('Enter a valid amount'); return }
    if (num > maxAmount) { setError(`Amount cannot exceed ${currency} ${maxAmount.toLocaleString()}`); return }
    setError('')
    setSubmitting(true)
    try {
      await onSubmit(num, notes)
      setSuccess(true)
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Payout request failed'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
      <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-slate-700 dark:bg-slate-900">
        <h3 className="mb-4 text-base font-semibold text-slate-900 dark:text-slate-50">
          Request Payout
        </h3>
        {success ? (
          <div className="space-y-4">
            <p className="text-sm text-emerald-600 dark:text-emerald-400">
              Payout request submitted successfully. Our team will process it within 3–5 business days.
            </p>
            <button type="button" onClick={onClose} className="dashboard-btn-primary w-full">
              Close
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
                Amount ({currency})
              </label>
              <input
                type="number"
                min="1"
                step="0.01"
                max={maxAmount}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder={`Max ${maxAmount.toLocaleString()}`}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
                Notes (optional)
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={2}
                placeholder="Bank account, reference, or any note for our team"
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              />
            </div>
            {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
            <div className="flex gap-3">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="flex-1 dashboard-btn-primary disabled:opacity-50"
              >
                {submitting ? 'Submitting…' : 'Submit Request'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export function PortalEarningsPage() {
  const { t } = useLanguage()
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [data, setData] = useState<PortalEarningsDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showPayoutModal, setShowPayoutModal] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    portalAPI
      .getEarnings({
        start: start.trim() || undefined,
        end: end.trim() || undefined,
      })
      .then((res) => setData(res.data as PortalEarningsDto))
      .catch((e) => {
        setData(null)
        setError(getApiErrorMessage(e))
      })
      .finally(() => setLoading(false))
  }, [start, end])

  useEffect(() => {
    load()
  }, [])

  return (
    <>
      <h1 className="app-page-title">{t('title.earnings')}</h1>
      <Card className="mt-6 p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:flex-wrap sm:items-end">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.earningsStart')}</label>
            <input
              type="date"
              value={start}
              onChange={(e) => setStart(e.target.value)}
              className="mt-1 rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.earningsEnd')}</label>
            <input
              type="date"
              value={end}
              onChange={(e) => setEnd(e.target.value)}
              className="mt-1 rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
            />
          </div>
          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {loading ? t('ui.loading') : t('portalPages.applyRange')}
          </button>
        </div>

        {error && (
          <p className="mt-4 text-sm text-red-600 dark:text-red-400">{error}</p>
        )}

        {data && !loading && (
          <div className="mt-8">
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {data.start && data.end
                ? t('portalPages.earningsPeriod', { start: data.start, end: data.end })
                : t('portalPages.earningsAllTime')}
            </p>
            <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
              <p className="text-3xl font-bold text-slate-900 dark:text-slate-50">
                {data.currency ?? 'USD'}{' '}
                {typeof data.totalEarnings === 'number'
                  ? data.totalEarnings.toLocaleString()
                  : Number(data.totalEarnings ?? 0).toLocaleString()}
              </p>
              {Number(data.totalEarnings ?? 0) > 0 && (
                <button
                  type="button"
                  onClick={() => setShowPayoutModal(true)}
                  className="dashboard-btn-primary shrink-0"
                >
                  Request Payout
                </button>
              )}
            </div>
          </div>
        )}

        {!data && !loading && !error && (
          <p className="mt-6 text-sm text-slate-500 dark:text-slate-400">{t('portalPages.earningsHint')}</p>
        )}
      </Card>

      {showPayoutModal && data && (
        <PayoutRequestModal
          currency={data.currency ?? 'USD'}
          maxAmount={Number(data.totalEarnings ?? 0)}
          onClose={() => setShowPayoutModal(false)}
          onSubmit={async (amount, notes) => {
            await portalAPI.requestPayout({ amount, notes })
          }}
        />
      )}
    </>
  )
}
