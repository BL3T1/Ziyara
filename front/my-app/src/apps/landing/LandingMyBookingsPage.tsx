import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { bookingsAPI, getApiErrorMessage } from '../../services/api'
import type { BookingDto, VoucherDto } from '../../types/api'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { downloadVoucherPdf } from './voucherPdf'
import { useToast } from './LandingToast'

const CANCELLABLE = new Set(['PENDING', 'CONFIRMED', 'ACTIVE'])

const STATUS_STYLE: Record<string, { bg: string; color: string }> = {
  CONFIRMED:  { bg: 'rgba(34,160,107,0.12)', color: '#16855a' },
  ACTIVE:     { bg: 'rgba(61,112,128,0.12)', color: 'var(--accent-teal)' },
  COMPLETED:  { bg: 'rgba(90,100,110,0.1)',  color: 'var(--ink-muted)' },
  PENDING:    { bg: 'rgba(184,150,110,0.15)', color: 'var(--accent-tan-dark)' },
  CANCELLED:  { bg: 'rgba(192,57,43,0.1)',   color: '#c0392b' },
  EXPIRED:    { bg: 'rgba(90,100,110,0.08)', color: 'var(--ink-faint)' },
}

function StatusBadge({ status }: { status: string }) {
  const key = status.toUpperCase()
  const style = STATUS_STYLE[key] ?? { bg: 'rgba(90,100,110,0.1)', color: 'var(--ink-muted)' }
  return (
    <span
      className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold"
      style={{ background: style.bg, color: style.color }}
    >
      {status.charAt(0) + status.slice(1).toLowerCase().replace('_', ' ')}
    </span>
  )
}

export function LandingMyBookingsPage() {
  useDocumentMeta({ title: 'My Bookings · Ziyara', description: 'View and manage all your Ziyara bookings.' })
  const { t } = useLanguage()
  const { user } = useAuth()
  const { toast } = useToast()
  const [rows, setRows] = useState<BookingDto[]>([])
  const [detailBooking, setDetailBooking] = useState<BookingDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [cancelTarget, setCancelTarget] = useState<BookingDto | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)
  const [cancelError, setCancelError] = useState('')
  const [voucherLoading, setVoucherLoading] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    setLoading(true)
    bookingsAPI
      .listMy()
      .then((r) => setRows(Array.isArray(r.data) ? r.data : []))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [user])

  const confirmCancel = async () => {
    if (!cancelTarget) return
    setCancelling(true)
    setCancelError('')
    try {
      await bookingsAPI.cancel(cancelTarget.id, cancelReason.trim() ? { reason: cancelReason.trim() } : undefined)
      setRows((prev) => prev.map((b) => b.id === cancelTarget.id ? { ...b, status: 'CANCELLED' } : b))
      toast(t('landingMyBookings.cancelSuccess') || 'Booking cancelled.', 'info')
      setCancelTarget(null)
      setCancelReason('')
    } catch (err) {
      setCancelError(getApiErrorMessage(err, 'Failed to cancel booking.'))
    } finally {
      setCancelling(false)
    }
  }

  const handleDownloadVoucher = async (bookingId: string) => {
    setVoucherLoading(bookingId)
    try {
      const res = await bookingsAPI.getVoucher(bookingId)
      downloadVoucherPdf(res.data as VoucherDto)
      toast(t('landingMyBookings.voucherSuccess') || 'Voucher downloaded.', 'success')
    } catch {
      toast(t('landingMyBookings.voucherError') || 'Failed to download voucher.', 'error')
    } finally {
      setVoucherLoading(null)
    }
  }

  if (!user) {
    return (
      <div className="lp-sheet mx-auto max-w-lg text-center mt-12">
        <p className="lp-muted mb-4 text-base">{t('landingMyBookings.loginRequired')}</p>
        <Link to="/login" className="lp-btn lp-btn-primary lp-btn-sm">
          {t('landingMyBookings.signIn')}
        </Link>
      </div>
    )
  }

  return (
    <div className="lp-sheet">
      <h1 className="lp-h1 mb-6">{t('landingMyBookings.title')}</h1>

      {error && (
        <div className="mb-4 rounded-2xl border px-4 py-3 text-sm" style={{ borderColor: 'rgba(192,57,43,0.25)', background: 'rgba(192,57,43,0.06)', color: '#c0392b' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div className="animate-pulse space-y-3 py-8">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-14 rounded-xl lp-skeleton" />
          ))}
        </div>
      ) : rows.length === 0 ? (
        <div className="rounded-2xl border py-16 text-center lp-bg-glass-card">
          <p className="lp-muted">{t('landingMyBookings.empty')}</p>
          <Link to="/services" className="lp-btn lp-btn-primary lp-btn-sm mt-4 inline-block">
            {t('landingTraveler.ctaBrowse')}
          </Link>
        </div>
      ) : (
        <>
          {/* Desktop: scrollable table */}
          <div className="hidden sm:block overflow-x-auto rounded-2xl border" style={{ borderColor: 'rgba(255,255,255,0.7)', background: 'rgba(255,255,255,0.45)', backdropFilter: 'blur(14px)' }}>
            <table className="w-full min-w-[640px] text-sm">
              <thead>
                <tr className="border-b" style={{ borderColor: 'rgba(90,100,110,0.1)' }}>
                  {[t('landingMyBookings.colRef'), t('landingMyBookings.colService'), t('landingMyBookings.colDates'), t('landingMyBookings.colStatus'), t('landingMyBookings.colTotal'), t('landingMyBookings.colActions')].map((col) => (
                    <th key={col} className="px-4 py-3 text-start font-semibold" style={{ color: 'var(--ink-muted)', fontSize: 11, letterSpacing: '0.07em', textTransform: 'uppercase' }}>
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((b) => (
                  <tr
                    key={b.id}
                    className="border-b last:border-0 cursor-pointer transition-colors hover:bg-white/30 lp-border-xfaint"
                    onClick={() => setDetailBooking(b)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs lp-text-accent">{b.bookingReference}</td>
                    <td className="px-4 py-3 lp-text-heading">
                      {b.serviceName ?? '—'}
                      {b.serviceType && <span className="ms-1.5 text-xs lp-text-faint">{b.serviceType}</span>}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 lp-text-muted">
                      {b.checkInDate ?? '—'}{b.checkOutDate ? ` → ${b.checkOutDate}` : ''}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={b.status} /></td>
                    <td className="whitespace-nowrap px-4 py-3 text-end font-semibold lp-text-heading">
                      {b.totalAmount != null ? `${b.currency ?? 'USD'} ${Number(b.totalAmount).toFixed(2)}` : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-end" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center justify-end gap-2">
                        <button type="button" disabled={voucherLoading === b.id} onClick={() => handleDownloadVoucher(b.id)}
                          className="rounded-xl border px-2.5 py-1 text-xs font-medium transition-colors disabled:opacity-50"
                          style={{ borderColor: 'rgba(61,112,128,0.3)', color: 'var(--accent-teal)', background: 'rgba(61,112,128,0.05)' }}>
                          {voucherLoading === b.id ? '…' : t('landingMyBookings.actionDownloadVoucher')}
                        </button>
                        {CANCELLABLE.has(b.status.toUpperCase()) && (
                          <button type="button" onClick={() => { setCancelTarget(b); setCancelReason(''); setCancelError('') }}
                            className="rounded-xl border px-2.5 py-1 text-xs font-medium transition-colors"
                            style={{ borderColor: 'rgba(192,57,43,0.25)', color: '#c0392b', background: 'rgba(192,57,43,0.05)' }}>
                            {t('landingMyBookings.actionCancel')}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile: stacked cards */}
          <div className="space-y-3 sm:hidden">
            {rows.map((b) => (
              <button
                key={b.id}
                type="button"
                className="lp-glass-card w-full !p-4 text-start"
                onClick={() => setDetailBooking(b)}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-semibold truncate lp-text-heading">{b.serviceName ?? '—'}</p>
                    <p className="mt-0.5 font-mono text-xs lp-text-accent">{b.bookingReference}</p>
                  </div>
                  <StatusBadge status={b.status} />
                </div>
                <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm lp-text-muted">
                  {b.checkInDate && <span>{b.checkInDate}{b.checkOutDate ? ` → ${b.checkOutDate}` : ''}</span>}
                  {b.totalAmount != null && (
                    <span className="font-semibold lp-text-heading">
                      {b.currency ?? 'USD'} {Number(b.totalAmount).toFixed(2)}
                    </span>
                  )}
                </div>
                <div className="mt-3 flex gap-2" onClick={(e) => e.stopPropagation()}>
                  <button type="button" disabled={voucherLoading === b.id} onClick={() => handleDownloadVoucher(b.id)}
                    className="rounded-xl border px-2.5 py-1 text-xs font-medium disabled:opacity-50"
                    style={{ borderColor: 'rgba(61,112,128,0.3)', color: 'var(--accent-teal)', background: 'rgba(61,112,128,0.05)' }}>
                    {voucherLoading === b.id ? '…' : t('landingMyBookings.actionDownloadVoucher')}
                  </button>
                  {CANCELLABLE.has(b.status.toUpperCase()) && (
                    <button type="button" onClick={() => { setCancelTarget(b); setCancelReason(''); setCancelError('') }}
                      className="rounded-xl border px-2.5 py-1 text-xs font-medium"
                      style={{ borderColor: 'rgba(192,57,43,0.25)', color: '#c0392b', background: 'rgba(192,57,43,0.05)' }}>
                      {t('landingMyBookings.actionCancel')}
                    </button>
                  )}
                </div>
              </button>
            ))}
          </div>
        </>
      )}

      {/* Booking detail modal */}
      {detailBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(10,18,30,0.45)', backdropFilter: 'blur(6px)' }}>
          <div className="lp-auth-card w-full max-w-lg" style={{ animation: 'lp-card-enter 0.25s var(--ease-out) both' }}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="lp-h1 !text-xl">{t('landingMyBookings.detailTitle')}</h2>
              <button type="button" onClick={() => setDetailBooking(null)} style={{ color: 'var(--ink-faint)', background: 'none', border: 'none', cursor: 'pointer', fontSize: 20, lineHeight: 1 }}>✕</button>
            </div>
            <dl className="space-y-3 text-sm">
              <div className="flex justify-between">
                <dt className="lp-text-muted">{t('landingMyBookings.colRef')}</dt>
                <dd className="font-mono font-semibold lp-text-accent">{detailBooking.bookingReference}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="lp-text-muted">{t('landingMyBookings.colService')}</dt>
                <dd className="lp-text-heading">{detailBooking.serviceName ?? '—'}</dd>
              </div>
              {(detailBooking.checkInDate || detailBooking.checkOutDate) && (
                <div className="flex justify-between">
                  <dt className="lp-text-muted">{t('landingMyBookings.colDates')}</dt>
                  <dd className="lp-text-heading">
                    {detailBooking.checkInDate ?? '—'}{detailBooking.checkOutDate ? ` → ${detailBooking.checkOutDate}` : ''}
                  </dd>
                </div>
              )}
              <div className="flex justify-between">
                <dt className="lp-text-muted">{t('landingMyBookings.colStatus')}</dt>
                <dd><StatusBadge status={detailBooking.status} /></dd>
              </div>
              {detailBooking.totalAmount != null && (
                <div className="flex justify-between border-t pt-3 lp-border-xsoft">
                  <dt className="font-semibold lp-text-heading">{t('landingMyBookings.colTotal')}</dt>
                  <dd className="font-bold lp-text-heading">
                    {detailBooking.currency ?? 'USD'} {Number(detailBooking.totalAmount).toFixed(2)}
                  </dd>
                </div>
              )}
            </dl>
            <div className="mt-5 flex flex-wrap justify-end gap-2">
              <button type="button" disabled={voucherLoading === detailBooking.id} onClick={() => handleDownloadVoucher(detailBooking.id)}
                className="lp-btn lp-btn-outline lp-btn-sm disabled:opacity-50">
                {voucherLoading === detailBooking.id ? '…' : t('landingMyBookings.actionDownloadVoucher')}
              </button>
              {CANCELLABLE.has(detailBooking.status.toUpperCase()) && (
                <button type="button" onClick={() => { setCancelTarget(detailBooking); setDetailBooking(null); setCancelReason(''); setCancelError('') }}
                  className="lp-btn lp-btn-sm lp-btn-danger">
                  {t('landingMyBookings.actionCancel')}
                </button>
              )}
              <button type="button" onClick={() => setDetailBooking(null)} className="lp-btn lp-btn-outline lp-btn-sm">
                {t('ui.close')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel confirmation modal — matches landing glassmorphism */}
      {cancelTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(10,18,30,0.45)', backdropFilter: 'blur(6px)' }}>
          <div className="lp-auth-card w-full max-w-md" style={{ animation: 'lp-card-enter 0.25s var(--ease-out) both' }}>
            <h2 className="lp-h1 !text-xl mb-1">
              {t('landingMyBookings.cancelConfirmTitle')}
            </h2>
            <p className="lp-muted mb-4 text-sm">
              {cancelTarget.bookingReference} — {cancelTarget.serviceName}
            </p>
            <label className="lp-field-label">{t('landingMyBookings.cancelReasonLabel')}</label>
            <textarea
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              rows={3}
              className="lp-input mb-4 w-full resize-none"
              placeholder={t('landingMyBookings.cancelReasonPlaceholder')}
            />
            {cancelError && (
              <p className="mb-3 rounded-xl px-3 py-2 text-sm" style={{ background: 'rgba(192,57,43,0.08)', color: '#c0392b' }}>
                {cancelError}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                disabled={cancelling}
                onClick={() => setCancelTarget(null)}
                className="lp-btn lp-btn-outline lp-btn-sm"
              >
                {t('landingMyBookings.cancelKeepBooking')}
              </button>
              <button
                type="button"
                disabled={cancelling}
                onClick={confirmCancel}
                className="lp-btn lp-btn-sm lp-btn-danger"
              >
                {cancelling ? '…' : t('landingMyBookings.cancelConfirm')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
