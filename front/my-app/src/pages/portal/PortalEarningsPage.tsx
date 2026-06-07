/**
 * Provider portal: completed-payment earnings summary with optional date range.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import { statusLabel } from '../../i18n/enumLabels'
import type { PortalEarningsDto, PortalServiceEarningRow } from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

interface PayoutRequest {
  amount: number
  notes: string
  currency: string
  date: string
  status: string
  reference?: string
}

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
  const { t } = useLanguage()
  const [amount, setAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const num = parseFloat(amount)
    if (!num || num <= 0) { setError(t('payout.validAmount')); return }
    if (num > maxAmount) { setError(t('payout.maxAmount', { currency, max: maxAmount.toLocaleString() })); return }
    setError('')
    setSubmitting(true)
    try {
      await onSubmit(num, notes)
      setSuccess(true)
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, t('payout.validAmount')))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('payout.requestTitle')}
      size="sm"
      footer={
        success ? (
          <button type="button" onClick={onClose} className="dashboard-btn-primary w-full">
            {t('ui.close')}
          </button>
        ) : (
          <>
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="payout-form"
              disabled={submitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {submitting ? t('payout.submitting') : t('payout.submit')}
            </button>
          </>
        )
      }
    >
      {success ? (
        <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
          {t('payout.success')}
        </p>
      ) : (
        <form id="payout-form" onSubmit={handleSubmit} className="space-y-5">
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200">
            {t('payout.availableBalance')}:{' '}
            <span className="font-semibold text-primary dark:text-blue-300">
              {currency} {maxAmount.toLocaleString()}
            </span>
          </div>
          <FormField label={t('payout.amountLabel', { currency })} required>
            <input
              type="number"
              min="1"
              step="0.01"
              max={maxAmount}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder={t('payout.amountMax', { amount: maxAmount.toLocaleString() })}
              className="modal-input"
              required
            />
          </FormField>
          <FormField label={t('payout.notesLabel')} hint={t('payout.notesHint')}>
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
  useDocumentMeta({ title: `${t('title.earnings')} — Ziyara` })
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [data, setData] = useState<PortalEarningsDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showPayoutModal, setShowPayoutModal] = useState(false)
  const [payoutHistory, setPayoutHistory] = useState<PayoutRequest[]>([])
  const [payoutPage, setPayoutPage] = useState(0)
  const [payoutTotal, setPayoutTotal] = useState(0)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyLoadingMore, setHistoryLoadingMore] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    portalAPI
      .getEarnings({ start: start.trim() || undefined, end: end.trim() || undefined })
      .then((res) => setData(res.data as PortalEarningsDto))
      .catch((e) => { setData(null); setError(getApiErrorMessage(e)) })
      .finally(() => setLoading(false))
  }, [start, end])

  const loadHistory = useCallback(() => {
    setHistoryLoading(true)
    portalAPI
      .listPayouts?.({ page: 0, size: 20 })
      .then((res) => {
        const paged = res?.data as { content: PayoutRequest[]; totalElements: number } | undefined
        setPayoutHistory(paged?.content ?? [])
        setPayoutTotal(paged?.totalElements ?? 0)
        setPayoutPage(0)
      })
      .catch(() => { setPayoutHistory([]); setPayoutTotal(0) })
      .finally(() => setHistoryLoading(false))
  }, [])

  function loadMoreHistory() {
    const nextPage = payoutPage + 1
    setHistoryLoadingMore(true)
    portalAPI
      .listPayouts?.({ page: nextPage, size: 20 })
      .then((res) => {
        const paged = res?.data as { content: PayoutRequest[]; totalElements: number } | undefined
        setPayoutHistory((prev) => [...prev, ...(paged?.content ?? [])])
        setPayoutTotal(paged?.totalElements ?? 0)
        setPayoutPage(nextPage)
      })
      .catch(() => {})
      .finally(() => setHistoryLoadingMore(false))
  }

  useEffect(() => { load() }, [load])
  useEffect(() => { loadHistory() }, [loadHistory])

  const totalEarnings = Number(data?.totalEarnings ?? 0)

  function handleCsvDownload() {
    const params: Record<string, string> = {}
    if (start.trim()) params.start = start.trim()
    if (end.trim()) params.end = end.trim()
    portalAPI.exportEarnings(params).then((res) => {
      const disposition = (res.headers as Record<string, string>)?.['content-disposition'] ?? ''
      const match = disposition.match(/filename="?([^"]+)"?/)
      const filename = match?.[1] ?? 'earnings.csv'
      const blob = new Blob([res.data as BlobPart], { type: 'text/csv' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
    }).catch(() => {})
  }

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

        <div role="status" aria-live="polite" aria-atomic="true">
          {error && (
            <p className="mt-4 text-sm text-red-600 dark:text-red-400">{error}</p>
          )}
        </div>

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
                  {t('payout.requestButton')}
                </button>
              )}
            </div>
          </div>
        )}

        {!data && !loading && !error && (
          <p className="mt-6 text-sm text-slate-500 dark:text-slate-400">{t('portalPages.earningsHint')}</p>
        )}
      </Card>

      {/* ── Profit-share breakdown ───────────────────────────────────────── */}
      {data && !loading && (data.grossRevenue != null || data.platformFee != null) && (
        <>
          {/* Three metric cards */}
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            {(
              [
                { labelKey: 'earningsGross',      value: data.grossRevenue ?? data.totalEarnings, cls: '' },
                { labelKey: 'earningsPlatformFee', value: data.platformFee ?? 0,                  cls: 'text-red-600 dark:text-red-400' },
                { labelKey: 'earningsYourNet',     value: data.providerNet ?? data.totalEarnings,  cls: 'text-primary dark:text-blue-300' },
              ] as { labelKey: string; value: number; cls: string }[]
            ).map(({ labelKey, value, cls }) => (
              <Card key={labelKey} className="p-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                  {t(`portalPages.${labelKey}`)}
                </p>
                <p className={`mt-1 text-2xl font-bold tabular-nums ${cls || 'text-slate-900 dark:text-white'}`}>
                  <span className="text-sm font-medium text-slate-400 mr-1">{data.currency ?? 'USD'}</span>
                  {Number(value).toLocaleString()}
                </p>
              </Card>
            ))}
          </div>

          {data.platformCommissionPct != null && (
            <p className="mt-2 text-xs text-slate-400 dark:text-slate-500">
              {t('portalPages.earningsCommissionNote', { pct: String(data.platformCommissionPct) })}
              {data.bookingCount != null && (
                <span className="ml-3">
                  {t('portalPages.earningsBookingsCount', { n: String(data.bookingCount) })}
                </span>
              )}
            </p>
          )}

          {/* Per-service breakdown table */}
          {data.perServiceBreakdown && data.perServiceBreakdown.length > 0 ? (
            <div className="mt-5">
              <div className="mb-3 flex items-center justify-between">
                <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  {t('portalPages.earningsByService')}
                </p>
                <button
                  type="button"
                  onClick={handleCsvDownload}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
                >
                  ↓ {t('portalPages.earningsExportCsv')}
                </button>
              </div>
              <Card className="overflow-hidden p-0">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
                        {[
                          t('portalPages.earningsColService'),
                          t('portalPages.earningsColBookings'),
                          t('portalPages.earningsColGross'),
                          t('portalPages.earningsColFee'),
                          t('portalPages.earningsColNet'),
                        ].map((h) => (
                          <th key={h} className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                            {h}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-700/60">
                      {(data.perServiceBreakdown as PortalServiceEarningRow[]).map((row) => (
                        <tr key={row.serviceId} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                          <td className="px-4 py-3 font-medium text-slate-800 dark:text-slate-100">{row.serviceName}</td>
                          <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{row.bookingCount}</td>
                          <td className="px-4 py-3 tabular-nums text-slate-700 dark:text-slate-200">{Number(row.grossRevenue).toLocaleString()}</td>
                          <td className="px-4 py-3 tabular-nums text-red-600 dark:text-red-400">{Number(row.platformFee).toLocaleString()}</td>
                          <td className="px-4 py-3 tabular-nums font-semibold text-primary dark:text-blue-300">{Number(row.providerNet).toLocaleString()}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot className="border-t-2 border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
                      {(() => {
                        const rows = data.perServiceBreakdown as PortalServiceEarningRow[]
                        const totBookings = rows.reduce((s, r) => s + Number(r.bookingCount), 0)
                        const totGross = rows.reduce((s, r) => s + Number(r.grossRevenue), 0)
                        const totFee = rows.reduce((s, r) => s + Number(r.platformFee), 0)
                        const totNet = rows.reduce((s, r) => s + Number(r.providerNet), 0)
                        return (
                          <tr>
                            <td className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                              {t('portalPages.earningsTotal')}
                            </td>
                            <td className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{totBookings}</td>
                            <td className="px-4 py-3 tabular-nums font-semibold text-slate-700 dark:text-slate-200">{totGross.toLocaleString()}</td>
                            <td className="px-4 py-3 tabular-nums font-semibold text-red-600 dark:text-red-400">{totFee.toLocaleString()}</td>
                            <td className="px-4 py-3 tabular-nums font-semibold text-primary dark:text-blue-300">{totNet.toLocaleString()}</td>
                          </tr>
                        )
                      })()}
                    </tfoot>
                  </table>
                </div>
              </Card>
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-400 dark:text-slate-500">
              {t('portalPages.earningsNoBreakdown')}
            </p>
          )}
        </>
      )}

      {/* Payout history */}
      <div className="mt-8">
        <h2 className="mb-4 text-lg font-semibold text-slate-800 dark:text-slate-100">{t('payout.historyTitle')}</h2>
        {historyLoading ? (
          <p className="text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : payoutHistory.length === 0 ? (
          <p className="text-sm text-slate-500 dark:text-slate-400">{t('payout.noHistory')}</p>
        ) : (
          <div className="table-shell">
            <table>
              <thead>
                <tr>
                  <th className="px-4 py-3.5">{t('payout.colDate')}</th>
                  <th className="px-4 py-3.5">{t('payout.colAmount')}</th>
                  <th className="px-4 py-3.5">{t('payout.colStatus')}</th>
                  <th className="px-4 py-3.5">{t('payout.colNotes')}</th>
                </tr>
              </thead>
              <tbody>
                {payoutHistory.map((req, i) => (
                  <tr key={req.reference ?? i}>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-700 dark:text-slate-200">
                      {req.date ? String(req.date).slice(0, 10) : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-700 dark:text-slate-200">
                      {req.currency} {Number(req.amount).toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-700 dark:text-slate-200">
                      {statusLabel(t, req.status)}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {req.notes ?? '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {payoutHistory.length < payoutTotal && (
        <div className="mt-4 flex justify-center">
          <button
            type="button"
            disabled={historyLoadingMore}
            onClick={loadMoreHistory}
            className="dashboard-btn-secondary disabled:opacity-50"
          >
            {historyLoadingMore ? t('ui.loading') : t('portalPages.loadMore', { loaded: payoutHistory.length, total: payoutTotal })}
          </button>
        </div>
      )}

      {showPayoutModal && data && (
        <PayoutRequestModal
          currency={data.currency ?? 'USD'}
          maxAmount={totalEarnings}
          onClose={() => { setShowPayoutModal(false); loadHistory() }}
          onSubmit={async (amount, notes) => { await portalAPI.requestPayout({ amount, notes }) }}
        />
      )}
    </>
  )
}
