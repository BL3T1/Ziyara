/**
 * Management > Payments – group-first list view.
 * Step 1: Status or method cards. Step 2: Table with refund action.
 */

import { useEffect, useMemo, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { paymentsAPI } from '../../services/api'
import type { PageDto, PaymentDto } from '../../types/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'

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
  const [refundModal, setRefundModal] = useState<{ id: string; ref?: string } | null>(null)
  const [refundReason, setRefundReason] = useState('')
  const [refundSubmitting, setRefundSubmitting] = useState(false)

  const summary = useMemo(() => {
    const completed = payments.filter((p) => p.status === 'COMPLETED')
    const pending = payments.filter((p) => p.status === 'PENDING')
    const refunded = payments.filter((p) => p.status === 'REFUNDED')
    const sum = (arr: PaymentDto[]) => arr.reduce((s, p) => s + Number(p.amount ?? 0), 0)
    return { completed: sum(completed), pending: sum(pending), refunded: sum(refunded), currency: payments[0]?.currency ?? 'USD' }
  }, [payments])

  useEffect(() => {
    setLoading(true)
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
      .catch(() => {
        setPayments([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }, [filter, page])

  const handleRefund = async () => {
    if (!refundModal || !refundReason.trim()) return
    setRefundSubmitting(true)
    try {
      await paymentsAPI.refund(refundModal.id, { reason: refundReason.trim() })
      setPayments((prev) =>
        prev.map((p) => (p.id === refundModal.id ? { ...p, status: 'REFUNDED' } : p))
      )
      setRefundModal(null)
      setRefundReason('')
    } catch {
      // keep modal open
    } finally {
      setRefundSubmitting(false)
    }
  }

  const dash = t('ui.emDash')

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('title.payments')}</h1>
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
          { label: 'Collected', amount: summary.completed, color: 'text-green-700 dark:text-green-400' },
          { label: 'Pending', amount: summary.pending, color: 'text-amber-700 dark:text-amber-400' },
          { label: 'Refunded', amount: summary.refunded, color: 'text-red-700 dark:text-red-400' },
        ].map(({ label, amount, color }) => (
          <div key={label} className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</p>
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
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.method ?? dash}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.status ?? dash}</td>
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
                    {p.status === 'COMPLETED' && (
                      <button
                        type="button"
                        onClick={() => setRefundModal({ id: p.id, ref: p.transactionReference })}
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
              type="submit"
              form="refund-form"
              disabled={!refundReason.trim() || refundSubmitting}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {refundSubmitting ? t('ui.submitting') : t('ui.confirmRefund')}
            </button>
          </>
        }
      >
        <form id="refund-form" onSubmit={(e) => { e.preventDefault(); handleRefund() }}>
          <FormField label={t('ui.reason')} required>
            <textarea
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              rows={4}
              className="modal-textarea"
              placeholder={t('paymentsPage.refundReasonPlaceholder')}
              required
            />
          </FormField>
        </form>
      </Modal>
    </>
  )
}
