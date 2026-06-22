/**
 * Provider portal: bookings for the signed-in provider's listings.
 * Includes cash-payment approval, payment history per booking, and manual payment recording.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { getApiErrorMessage, portalAPI, portalCashAPI } from '../../services/api'
import { statusLabel } from '../../i18n/enumLabels'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import type { BookingDto, PaymentDto, AddPaymentPayload } from '../../types/api'

function StatusBadge({ status, t }: { status?: string | null; t: (k: string) => string }) {
  if (!status) return <span className="badge badge-neutral">—</span>
  const label = statusLabel(t, status)
  const s = status.toLowerCase()
  if (s === 'confirmed' || s === 'completed') return <span className="badge badge-success">{label}</span>
  if (s === 'pending') return <span className="badge badge-warning">{label}</span>
  if (s === 'cancelled' || s === 'rejected') return <span className="badge badge-danger">{label}</span>
  return <span className="badge badge-neutral">{label}</span>
}

function PaymentStatusBadge({ status, t }: { status?: string | null; t: (k: string) => string }) {
  if (!status) return null
  const s = status.toUpperCase()
  if (s === 'PAID') return <span className="badge badge-success">{t('portalBookings.status.paid')}</span>
  if (s === 'PARTIAL' || s === 'PARTIALLY_PAID') return <span className="badge badge-warning">{t('portalBookings.status.partial')}</span>
  return <span className="badge badge-neutral">{t('portalBookings.status.pending')}</span>
}

const STATUS_FILTERS = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'] as const

const PAYMENT_METHODS: AddPaymentPayload['method'][] = ['CASH', 'BANK_TRANSFER', 'CHEQUE', 'OTHER']

function methodLabel(t: (k: string) => string, m: string) {
  const key = `portalBookings.method.${m.toLowerCase().replace('_', '')}`
  return t(key)
}

export function PortalBookingsPage() {
  const { t } = useLanguage()
  const canFinance = usePermission('portal:finance')
  const [rows, setRows] = useState<BookingDto[]>([])
  const [statusFilter, setStatusFilter] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [detail, setDetail] = useState<BookingDto | null>(null)

  // payment history in detail modal
  const [payments, setPayments] = useState<PaymentDto[]>([])
  const [paymentsLoading, setPaymentsLoading] = useState(false)

  // cash approval
  const [approvingId, setApprovingId] = useState<string | null>(null)
  const [approveCashBooking, setApproveCashBooking] = useState<BookingDto | null>(null)
  const [approveNotes, setApproveNotes] = useState('')
  const [approveSaving, setApproveSaving] = useState(false)

  // record payment modal
  const [recordPaymentBooking, setRecordPaymentBooking] = useState<BookingDto | null>(null)
  const [rpAmount, setRpAmount] = useState('')
  const [rpCurrency, setRpCurrency] = useState('USD')
  const [rpMethod, setRpMethod] = useState<AddPaymentPayload['method']>('CASH')
  const [rpRef, setRpRef] = useState('')
  const [rpNotes, setRpNotes] = useState('')
  const [rpSaving, setRpSaving] = useState(false)
  const [rpSuccess, setRpSuccess] = useState(false)

  const load = useCallback(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    portalAPI
      .listBookings()
      .then((res) => {
        if (!cancelled) {
          const data = Array.isArray(res.data) ? (res.data as BookingDto[]) : []
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

  const loadPayments = useCallback((bookingId: string) => {
    setPaymentsLoading(true)
    setPayments([])
    portalAPI
      .listBookingPayments(bookingId)
      .then((r) => setPayments(Array.isArray(r.data) ? r.data : []))
      .catch(() => setPayments([]))
      .finally(() => setPaymentsLoading(false))
  }, [])

  const openDetail = (b: BookingDto) => {
    setDetail(b)
    loadPayments(b.id)
  }

  const handleApproveCash = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!approveCashBooking) return
    setApproveSaving(true)
    setError(null)
    try {
      await portalCashAPI.recordCollection(approveCashBooking.id, {
        amount: approveCashBooking.totalAmount ?? 0,
        notes: approveNotes.trim() || undefined,
      })
      setApproveCashBooking(null)
      setApproveNotes('')
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setApproveSaving(false)
      setApprovingId(null)
    }
  }

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!recordPaymentBooking) return
    const amt = parseFloat(rpAmount)
    if (!Number.isFinite(amt) || amt <= 0) return
    setRpSaving(true)
    setRpSuccess(false)
    setError(null)
    try {
      await portalAPI.addPayment(recordPaymentBooking.id, {
        amount: amt,
        currency: rpCurrency.trim() || 'USD',
        method: rpMethod,
        transactionReference: rpRef.trim() || undefined,
        notes: rpNotes.trim() || undefined,
      })
      setRpSuccess(true)
      setRpAmount('')
      setRpRef('')
      setRpNotes('')
      load()
      if (detail?.id === recordPaymentBooking.id) loadPayments(recordPaymentBooking.id)
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setRpSaving(false)
    }
  }

  const filtered = statusFilter
    ? rows.filter((b) => (b.status ?? '').toUpperCase() === statusFilter)
    : rows

  const inputCls = 'w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100'

  return (
    <>
      <h1 className="app-page-title">{t('title.bookings')}</h1>

      {/* Status filter pills */}
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setStatusFilter(null)}
          className={statusFilter === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
        >
          {t('ui.all')}
        </button>
        {STATUS_FILTERS.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setStatusFilter(s)}
            className={statusFilter === s ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {statusLabel(t, s)}
          </button>
        ))}
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-4 table-shell">
        {loading ? (
          <div className="flex flex-col gap-2 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <div key={i} className="h-10 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
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
                <th className="px-4 py-3.5">{t('bookingsPage.reference')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.service')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.checkIn')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.checkOut')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.amount')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.status')}</th>
                <th className="px-4 py-3.5">{t('portalBookings.paymentMethod')}</th>
                <th className="px-4 py-3.5">{t('portalBookings.paymentStatus')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((b) => {
                const isCashUnpaid =
                  (b.paymentMethod ?? '').toUpperCase() === 'CASH' &&
                  (b.paymentStatus ?? '').toUpperCase() !== 'PAID'
                return (
                  <tr key={b.id}>
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">{b.bookingReference ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{b.serviceName?.trim() || '—'}</td>
                    <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm text-slate-600 dark:text-slate-300">{b.checkInDate ?? '—'}</td>
                    <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm text-slate-600 dark:text-slate-300">{b.checkOutDate ?? '—'}</td>
                    <td className="whitespace-nowrap px-4 py-3 tabular-nums text-sm text-slate-600 dark:text-slate-300">
                      {b.totalAmount != null ? `${b.currency ?? ''} ${b.totalAmount}`.trim() : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <StatusBadge status={b.status} t={t} />
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {b.paymentMethod ? methodLabel(t, b.paymentMethod) : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <PaymentStatusBadge status={b.paymentStatus} t={t} />
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm">
                      <div className="flex flex-wrap gap-3">
                        <button
                          type="button"
                          onClick={() => openDetail(b)}
                          className="text-primary hover:underline dark:text-[#60b4f8]"
                        >
                          {t('ui.view')}
                        </button>
                        {canFinance && isCashUnpaid && (
                          <button
                            type="button"
                            disabled={approvingId === b.id}
                            onClick={() => {
                              setApprovingId(b.id)
                              setApproveNotes('')
                              setApproveCashBooking(b)
                            }}
                            className="font-semibold text-emerald-600 hover:underline disabled:opacity-40 dark:text-emerald-400"
                          >
                            {t('portalBookings.approveCash')}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Booking detail modal */}
      <Modal
        open={!!detail}
        onClose={() => setDetail(null)}
        title={t('bookingsPage.bookingDetail')}
        size="md"
        footer={
          <div className="flex flex-wrap gap-2">
            {canFinance && detail && (
              <button
                type="button"
                onClick={() => {
                  setRecordPaymentBooking(detail)
                  setRpCurrency(detail.currency ?? 'USD')
                  setRpAmount(detail.totalAmount != null ? String(detail.totalAmount) : '')
                  setRpMethod('CASH')
                  setRpRef('')
                  setRpNotes('')
                  setRpSuccess(false)
                }}
                className="dashboard-btn-primary"
              >
                {t('portalBookings.addPayment')}
              </button>
            )}
            <button type="button" onClick={() => setDetail(null)} className="dashboard-btn-secondary">
              {t('ui.close')}
            </button>
          </div>
        }
      >
        {detail && (
          <div className="space-y-4">
            <dl className="space-y-3 text-sm">
              {[
                [t('bookingsPage.reference'), detail.bookingReference],
                [t('bookingsPage.service'), detail.serviceName],
                [t('bookingsPage.checkIn'), detail.checkInDate],
                [t('bookingsPage.checkOut'), detail.checkOutDate],
                [t('bookingsPage.amount'), detail.totalAmount != null ? `${detail.currency ?? ''} ${detail.totalAmount}`.trim() : null],
                [t('bookingsPage.status'), statusLabel(t, detail.status)],
                [t('portalBookings.paymentMethod'), detail.paymentMethod ? methodLabel(t, detail.paymentMethod) : null],
                [t('portalBookings.paymentStatus'), detail.paymentStatus ?? null],
                [t('portalPages.bookingCustomer'), detail.customerName ?? detail.customerId],
                [t('portalPages.bookingNotes'), (detail as unknown as Record<string, unknown>).specialRequests as string ?? (detail as unknown as Record<string, unknown>).notes as string],
              ]
                .filter(([, v]) => v != null && v !== '')
                .map(([label, value]) => (
                  <div key={label as string} className="flex justify-between gap-4 rounded-lg border border-slate-100 px-3 py-2 dark:border-white/[0.05]">
                    <dt className="text-slate-500 dark:text-slate-400">{label as string}</dt>
                    <dd className="font-medium text-slate-900 dark:text-slate-100 text-right">{value as string}</dd>
                  </div>
                ))}
            </dl>

            {/* Payment history */}
            <div className="border-t border-slate-100 pt-4 dark:border-white/[0.06]">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                {t('portalBookings.paymentsHistory')}
              </p>
              {paymentsLoading ? (
                <div className="h-8 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
              ) : payments.length === 0 ? (
                <p className="text-sm text-slate-400 dark:text-slate-500">{t('portalBookings.noPayments')}</p>
              ) : (
                <ul className="space-y-2">
                  {payments.map((p) => (
                    <li key={p.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-100 px-3 py-2 text-sm dark:border-white/[0.05]">
                      <span className="font-medium text-slate-800 dark:text-slate-200">
                        {p.currency} {p.amount}
                      </span>
                      <span className="text-slate-500 dark:text-slate-400">{methodLabel(t, p.method)}</span>
                      <span className="badge badge-neutral text-xs">{p.status}</span>
                      {p.processedAt && (
                        <span className="text-xs text-slate-400 dark:text-slate-500">
                          {new Date(p.processedAt).toLocaleDateString()}
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Cash approve confirm modal */}
      <Modal
        open={!!approveCashBooking}
        onClose={() => { setApproveCashBooking(null); setApproveNotes('') }}
        title={t('portalBookings.approveCash')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setApproveCashBooking(null); setApproveNotes('') }}
              disabled={approveSaving}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="approve-cash-form"
              disabled={approveSaving}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {approveSaving ? t('ui.saving') : t('portalBookings.approveCash')}
            </button>
          </>
        }
      >
        {approveCashBooking && (
          <form id="approve-cash-form" onSubmit={handleApproveCash} className="space-y-3">
            <p className="text-sm text-slate-600 dark:text-slate-300">
              {t('portalBookings.approveCashConfirm', {
                ref: approveCashBooking.bookingReference ?? approveCashBooking.id,
                amount: `${approveCashBooking.currency ?? ''} ${approveCashBooking.totalAmount ?? ''}`.trim(),
              })}
            </p>
            <FormField label={t('portalBookings.notes')}>
              <input
                type="text"
                value={approveNotes}
                onChange={(e) => setApproveNotes(e.target.value)}
                className={inputCls}
                placeholder={t('portalBookings.notesPlaceholder')}
              />
            </FormField>
          </form>
        )}
      </Modal>

      {/* Record payment modal */}
      <Modal
        open={!!recordPaymentBooking}
        onClose={() => { setRecordPaymentBooking(null); setRpSuccess(false) }}
        title={t('portalBookings.addPayment')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setRecordPaymentBooking(null); setRpSuccess(false) }}
              disabled={rpSaving}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="record-payment-form"
              disabled={rpSaving}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {rpSaving ? t('ui.saving') : t('portalBookings.recordPayment')}
            </button>
          </>
        }
      >
        <form id="record-payment-form" onSubmit={handleRecordPayment} className="space-y-3">
          {rpSuccess && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
              {t('portalBookings.recordPaymentSuccess')}
            </div>
          )}
          <div className="grid grid-cols-2 gap-3">
            <FormField label={t('bookingsPage.amount')}>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                value={rpAmount}
                onChange={(e) => setRpAmount(e.target.value)}
                className={inputCls}
              />
            </FormField>
            <FormField label={t('portalPages.fieldCurrency')}>
              <input
                type="text"
                required
                value={rpCurrency}
                onChange={(e) => setRpCurrency(e.target.value)}
                className={inputCls}
              />
            </FormField>
          </div>
          <FormField label={t('portalBookings.paymentMethod')}>
            <select
              value={rpMethod}
              onChange={(e) => setRpMethod(e.target.value as AddPaymentPayload['method'])}
              className={inputCls}
            >
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>{methodLabel(t, m)}</option>
              ))}
            </select>
          </FormField>
          <FormField label={t('portalBookings.transactionRef')}>
            <input
              type="text"
              value={rpRef}
              onChange={(e) => setRpRef(e.target.value)}
              className={inputCls}
              placeholder={t('portalBookings.transactionRefPlaceholder')}
            />
          </FormField>
          <FormField label={t('portalBookings.notes')}>
            <input
              type="text"
              value={rpNotes}
              onChange={(e) => setRpNotes(e.target.value)}
              className={inputCls}
              placeholder={t('portalBookings.notesPlaceholder')}
            />
          </FormField>
        </form>
      </Modal>
    </>
  )
}
