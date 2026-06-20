/**
 * Management > Payments – group-first list view.
 * Step 1: Status or method cards. Step 2: Table with refund action.
 */

import { useEffect, useMemo, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { paymentsAPI, getApiErrorMessage } from '../../services/api'
import { statusLabel, paymentMethodLabel } from '../../i18n/enumLabels'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import type { PageDto, PaymentDto } from '../../types/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import { usePermission } from '../../hooks/usePermission'

function asPage<T>(data: unknown): PageDto<T> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<T>).content)) {
    return data as PageDto<T>
  }
  return null
}

const PAGE_SIZE = 20

export function PaymentsPage() {
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const canWrite = usePermission('payments:write')
  const STATUS_CARDS = useMemo(
    () => [
      { id: 'COMPLETED', label: t('paymentsPage.statusCompleted') },
      { id: 'PENDING', label: t('paymentsPage.statusPending') },
      { id: 'FAILED', label: t('paymentsPage.statusFailed') },
      { id: 'REFUNDED', label: t('paymentsPage.statusRefunded') },
    ],
    [t],
  )

  const [payments, setPayments] = useState<PaymentDto[]>([])
  const [filter, setFilter] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [refundModal, setRefundModal] = useState<{ id: string; ref?: string; amount?: number } | null>(null)
  const [refundReason, setRefundReason] = useState('')
  const [pendingRefundConfirm, setPendingRefundConfirm] = useState(false)
  const [summary, setSummary] = useState({ totalCollected: 0, totalPending: 0, totalRefunded: 0, currency: 'USD' })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    paymentsAPI.summary().then((res) => setSummary(res.data as typeof summary)).catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    setError(null)
    paymentsAPI
      .list({
        page,
        size: PAGE_SIZE,
        status: filter ?? undefined,
      })
      .then((res) => {
        const p = asPage<PaymentDto>(res.data)
        setPayments(p?.content ?? [])
        setTotalPages(p?.totalPages ?? 0)
      })
      .catch((e: unknown) => {
        const status = (e as { response?: { status?: number } })?.response?.status
        setError(status === 403 ? t('ui.accessDenied') : getApiErrorMessage(e))
        setPayments([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }, [filter, page])

  const handleRefund = async () => {
    if (!refundModal || !refundReason.trim()) return
    await paymentsAPI.refund(refundModal.id, { reason: refundReason.trim() })
    setPayments((prev) =>
      prev.map((p) => (p.id === refundModal.id ? { ...p, status: 'REFUNDED' } : p))
    )
    setRefundModal(null)
    setRefundReason('')
    // Refresh aggregates after a refund
    paymentsAPI.summary().then((res) => setSummary(res.data)).catch(() => {})
  }

  const dash = t('ui.emDash')

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('title.payments')}</h1>
      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      <div className="mt-6 flex flex-wrap gap-4">
        <button
          type="button"
          onClick={() => {
            setFilter(null)
            setPage(0)
          }}
          className={filter === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
        >
          {t('ui.all')}
        </button>
        {STATUS_CARDS.map((card) => (
          <button
            key={card.id}
            type="button"
            onClick={() => {
              setFilter(card.id)
              setPage(0)
            }}
            className={filter === card.id ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {card.label}
          </button>
        ))}
      </div>

      <div className="mt-6 grid grid-cols-3 gap-4">
        {[
          { labelKey: 'paymentsPage.summaryCollected', amount: summary.totalCollected, color: 'text-green-700 dark:text-green-400' },
          { labelKey: 'paymentsPage.summaryPending', amount: summary.totalPending, color: 'text-amber-700 dark:text-amber-400' },
          { labelKey: 'paymentsPage.summaryRefunded', amount: summary.totalRefunded, color: 'text-red-700 dark:text-red-400' },
        ].map(({ labelKey, amount, color }) => (
          <div key={labelKey} className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{t(labelKey)}</p>
            <p className={`mt-1 text-xl font-bold ${color}`}>{displayInDefault(amount, summary.currency)}</p>
          </div>
        ))}
      </div>

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : payments.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('paymentsPage.noPayments')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('paymentsPage.colBookingRef')}</th>
                <th className="px-4 py-3.5">{t('paymentsPage.colAmount')}</th>
                <th className="px-4 py-3.5">{t('paymentsPage.colMethod')}</th>
                <th className="px-4 py-3.5">{t('paymentsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('paymentsPage.col3ds')}</th>
                <th className="px-4 py-3.5">Entity</th>
                <th className="px-4 py-3.5">Category</th>
                <th className="px-4 py-3.5">{t('paymentsPage.colGatewayRef')}</th>
                <th className="px-4 py-3.5">{t('paymentsPage.colDate')}</th>
                <th className="px-4 py-3.5">{t('paymentsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {p.bookingReference?.trim()
                      ? p.bookingReference
                      : p.bookingId
                        ? t('paymentsPage.linkedBooking')
                        : dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {displayInDefault(Number(p.amount ?? 0), p.currency)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{paymentMethodLabel(t, p.method ?? '')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{statusLabel(t, p.status)}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.threeDsStatus ?? dash}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {p.entityType ? <span className="rounded bg-slate-100 px-1.5 py-0.5 text-xs dark:bg-slate-700">{p.entityType}</span> : dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.category ?? dash}</td>
                  <td className="max-w-[8rem] truncate whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300" title={p.gatewayReference ?? undefined}>
                    {p.gatewayReference ?? dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {p.processedAt ? new Date(p.processedAt).toLocaleDateString() : dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    {canWrite && p.status === 'COMPLETED' && (
                      <button
                        type="button"
                        onClick={() => setRefundModal({ id: p.id, ref: p.transactionReference, amount: Number(p.amount ?? 0) })}
                        className="text-primary hover:underline"
                      >
                        {t('ui.refund')}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
          <button
            type="button"
            disabled={page <= 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {t('ui.previous')}
          </button>
          <span className="text-sm text-slate-600 dark:text-slate-400">
            {t('ui.pageOf', { current: page + 1, total: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {t('ui.next')}
          </button>
        </div>
      )}

      <Modal
        open={!!refundModal}
        onClose={() => { setRefundModal(null); setRefundReason('') }}
        title={t('paymentsPage.refundTitle')}
        description={refundModal ? t('paymentsPage.transactionLine', { ref: refundModal.ref ?? refundModal.id }) : undefined}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setRefundModal(null); setRefundReason('') }}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="button"
              disabled={refundReason.trim().length < 5}
              onClick={() => setPendingRefundConfirm(true)}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('ui.confirmRefund')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <FormField label={t('ui.reason')} hint={t('paymentsPage.refundReasonMinLength')} required>
            <textarea
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              rows={4}
              className="modal-textarea"
              placeholder={t('paymentsPage.refundReasonPlaceholder')}
            />
          </FormField>
          {refundModal?.amount != null && (
            <p className="text-sm text-slate-600 dark:text-slate-300">
              {t('paymentsPage.refundAmountNote', {
                amount: displayInDefault(refundModal.amount, summary.currency),
                ref: refundModal.ref ?? refundModal.id,
              })}
            </p>
          )}
        </div>
      </Modal>

      <ConfirmDialog
        open={pendingRefundConfirm}
        onClose={() => setPendingRefundConfirm(false)}
        title={t('ui.confirmRefund')}
        description={refundModal?.amount != null
          ? t('paymentsPage.refundAmountNote', {
              amount: displayInDefault(refundModal.amount, summary.currency),
              ref: refundModal?.ref ?? refundModal?.id ?? '',
            })
          : t('confirm.cannotUndo')}
        confirmLabel={t('ui.confirmRefund')}
        variant="danger"
        onConfirm={async () => { await handleRefund() }}
      />
    </>
  )
}
