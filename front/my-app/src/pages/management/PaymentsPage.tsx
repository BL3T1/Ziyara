/**
 * Management > Payments – group-first list view.
 * Step 1: Status or method cards. Step 2: Table with refund action.
 */

import { useEffect, useMemo, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { paymentsAPI } from '../../services/api'
import type { PageDto, PaymentDto } from '../../types/api'

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

      {refundModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl dark:bg-slate-800">
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('paymentsPage.refundTitle')}</h3>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              {t('paymentsPage.transactionLine', { ref: refundModal.ref ?? refundModal.id })}
            </p>
            <label className="mt-4 block text-sm font-medium text-slate-700 dark:text-slate-300">
              {t('ui.reason')} <span className="text-red-500">*</span>
            </label>
            <textarea
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              rows={3}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              placeholder={t('paymentsPage.refundReasonPlaceholder')}
            />
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setRefundModal(null)
                  setRefundReason('')
                }}
                className="dashboard-btn-secondary"
              >
                {t('ui.cancel')}
              </button>
              <button
                type="button"
                onClick={handleRefund}
                disabled={!refundReason.trim() || refundSubmitting}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {refundSubmitting ? t('ui.submitting') : t('ui.confirmRefund')}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
