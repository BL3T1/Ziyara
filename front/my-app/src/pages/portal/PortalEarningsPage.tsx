/**
 * Provider portal: completed-payment earnings summary with optional date range.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { PortalEarningsDto } from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

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
    <Modal
      open
      onClose={onClose}
      title="Request Payout"
      size="sm"
      footer={
        success ? (
          <button type="button" onClick={onClose} className="dashboard-btn-primary w-full">
            Close
          </button>
        ) : (
          <>
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="dashboard-btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              form="payout-form"
              disabled={submitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {submitting ? 'Submitting…' : 'Submit Request'}
            </button>
          </>
        )
      }
    >
      {success ? (
        <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
          Payout request submitted. Our team will process it within 3–5 business days.
        </p>
      ) : (
        <form id="payout-form" onSubmit={handleSubmit} className="space-y-5">
          <FormField label={`Amount (${currency})`} required>
            <input
              type="number"
              min="1"
              step="0.01"
              max={maxAmount}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder={`Max ${maxAmount.toLocaleString()}`}
              className="modal-input"
              required
            />
          </FormField>
          <FormField label="Notes" hint="Bank account, reference, or any note for our team">
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              className="modal-textarea"
            />
          </FormField>
          {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
        </form>
      )}
    </Modal>
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
      .getEarnings({ start: start.trim() || undefined, end: end.trim() || undefined })
      .then((res) => setData(res.data as PortalEarningsDto))
      .catch((e) => { setData(null); setError(getApiErrorMessage(e)) })
      .finally(() => setLoading(false))
  }, [start, end])

  useEffect(() => { load() }, [])

  const totalEarnings = Number(data?.totalEarnings ?? 0)

  return (
    <>
      <h1 className="app-page-title">{t('title.earnings')}</h1>

      <Card className="!p-5">
        {/* Date filter */}
        <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.earningsStart')}
            </label>
            <input
              type="date"
              value={start}
              onChange={(e) => setStart(e.target.value)}
              className="dashboard-date-input"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalPages.earningsEnd')}
            </label>
            <input
              type="date"
              value={end}
              onChange={(e) => setEnd(e.target.value)}
              className="dashboard-date-input"
            />
          </div>
          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="dashboard-btn-primary disabled:opacity-50 self-end"
          >
            {loading ? t('ui.loading') : t('portalPages.applyRange')}
          </button>
        </div>

        {error && (
          <p className="mt-4 text-sm text-red-600 dark:text-red-400">{error}</p>
        )}

        {data && !loading && (
          <div className="mt-8 border-t border-slate-100 pt-6 dark:border-white/[0.05]">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400 dark:text-slate-500">
              {data.start && data.end
                ? t('portalPages.earningsPeriod', { start: data.start, end: data.end })
                : t('portalPages.earningsAllTime')}
            </p>
            <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
              <p className="font-bold tabular-nums leading-none tracking-tight text-slate-900 dark:text-white" style={{ fontSize: 'clamp(1.75rem, 5vw, 2.75rem)' }}>
                <span className="text-base font-semibold text-slate-400 dark:text-slate-500 mr-1">{data.currency ?? 'USD'}</span>
                {totalEarnings.toLocaleString()}
              </p>
              {totalEarnings > 0 && (
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
          maxAmount={totalEarnings}
          onClose={() => setShowPayoutModal(false)}
          onSubmit={async (amount, notes) => { await portalAPI.requestPayout({ amount, notes }) }}
        />
      )}
    </>
  )
}
