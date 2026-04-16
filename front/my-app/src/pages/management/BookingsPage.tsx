/**
 * Management > Bookings – group-first list view.
 * Step 1: Status cards. Step 2: Table with View, Confirm, Cancel actions.
 */

import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { bookingsAPI } from '../../services/api'
import { getApiErrorMessage } from '../../services/api'
import type { BookingDto, PageDto } from '../../types/api'

function asPage<T>(data: unknown): PageDto<T> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<T>).content)) {
    return data as PageDto<T>
  }
  return null
}

const STATUS_IDS = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'] as const

const PAGE_SIZE = 20

export function BookingsPage() {
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const [searchParams, setSearchParams] = useSearchParams()
  const [bookings, setBookings] = useState<BookingDto[]>([])
  const [selectedStatus, setSelectedStatus] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [detail, setDetail] = useState<BookingDto | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [voucherData, setVoucherData] = useState<Record<string, unknown> | null>(null)

  const load = () => {
    setLoading(true)
    setError(null)
    bookingsAPI
      .listAdmin({
        status: selectedStatus ?? undefined,
        page,
        size: PAGE_SIZE,
      })
      .then((res) => {
        const p = asPage<BookingDto>(res.data)
        setBookings(p?.content ?? [])
        setTotalPages(p?.totalPages ?? 0)
      })
      .catch(() => {
        setBookings([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [selectedStatus, page])

  useEffect(() => {
    const bid = searchParams.get('bookingId')
    if (!bid) return
    setDetailId(bid)
    setDetail(null)
    setVoucherData(null)
    bookingsAPI
      .get(bid)
      .then((res) => setDetail(res.data as BookingDto))
      .catch(() => setDetail(null))
    setSearchParams(
      (sp) => {
        const n = new URLSearchParams(sp)
        n.delete('bookingId')
        return n
      },
      { replace: true },
    )
  }, [searchParams, setSearchParams])

  const openDetail = (id: string) => {
    setDetailId(id)
    setDetail(null)
    setVoucherData(null)
    bookingsAPI
      .get(id)
      .then((res) => setDetail(res.data as BookingDto))
      .catch(() => setDetail(null))
  }

  const openVoucher = (id: string) => {
    bookingsAPI
      .getVoucher(id)
      .then((res) => setVoucherData((res.data as Record<string, unknown>) ?? null))
      .catch(() => setVoucherData(null))
  }

  const handleConfirm = async (id: string) => {
    setError(null)
    try {
      await bookingsAPI.confirm(id)
      setBookings((prev) =>
        prev.map((b) => (b.id === id ? { ...b, status: 'CONFIRMED' } : b))
      )
      if (detailId === id) setDetail((d) => (d ? { ...d, status: 'CONFIRMED' } : d))
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleCancel = async (id: string) => {
    setError(null)
    try {
      await bookingsAPI.cancel(id, { reason: cancelReason || t('bookingsPage.cancelledByAdmin') })
      setBookings((prev) =>
        prev.map((b) => (b.id === id ? { ...b, status: 'CANCELLED' } : b))
      )
      setDetailId(null)
      setDetail(null)
      setCancelReason('')
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('bookingsPage.title')}</h1>
      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 flex flex-wrap gap-4">
        <button
          type="button"
          onClick={() => {
            setSelectedStatus(null)
            setPage(0)
          }}
          className={selectedStatus === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
        >
          {t('ui.all')}
        </button>
        {STATUS_IDS.map((id) => (
          <button
            key={id}
            type="button"
            onClick={() => {
              setSelectedStatus(id)
              setPage(0)
            }}
            className={selectedStatus === id ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {t(`bookingsPage.status_${id}`)}
          </button>
        ))}
      </div>

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : bookings.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('bookingsPage.noBookings')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('bookingsPage.reference')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.customer')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.checkIn')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.amount')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.status')}</th>
                <th className="px-4 py-3.5">{t('bookingsPage.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">{b.bookingReference}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {b.customerEmail?.trim() ||
                      b.customerName?.trim() ||
                      t('ui.emDash')}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{b.checkInDate ?? t('ui.emDash')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {b.totalAmount != null ? displayInDefault(Number(b.totalAmount), b.currency) : t('ui.emDash')}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{b.status ?? t('ui.emDash')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <button
                      type="button"
                      onClick={() => openDetail(b.id)}
                      className="text-primary hover:underline"
                    >
                      {t('ui.view')}
                    </button>
                    <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                    <button
                      type="button"
                      onClick={() => { openDetail(b.id); openVoucher(b.id); }}
                      className="text-slate-600 hover:underline dark:text-slate-300"
                    >
                      {t('ui.voucher')}
                    </button>
                    {(b.status ?? '').toUpperCase() === 'PENDING' && (
                      <>
                        <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                        <button
                          type="button"
                          onClick={() => handleConfirm(b.id)}
                          className="text-green-600 hover:underline dark:text-green-400"
                        >
                          {t('ui.confirm')}
                        </button>
                        <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                        <button
                          type="button"
                          onClick={() => openDetail(b.id)}
                          className="text-amber-600 hover:underline dark:text-amber-400"
                        >
                          {t('ui.cancel')}
                        </button>
                      </>
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
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-slate-600"
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
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-slate-600"
          >
            {t('ui.next')}
          </button>
        </div>
      )}

      {detailId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('bookingsPage.bookingDetail')}</h2>
            {detail ? (
              <div className="mt-4 space-y-2 text-sm">
                <p><span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.reference')}:</span> {detail.bookingReference}</p>
                <p>
                  <span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.customer')}:</span>{' '}
                  {detail.customerEmail?.trim() || detail.customerName?.trim() || t('ui.emDash')}
                </p>
                <p>
                  <span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.service')}:</span>{' '}
                  {detail.serviceName?.trim() || t('ui.emDash')}
                </p>
                <p><span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.checkIn')}:</span> {detail.checkInDate ?? t('ui.emDash')}</p>
                <p><span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.checkOut')}:</span> {detail.checkOutDate ?? t('ui.emDash')}</p>
                <p><span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.amount')}:</span>{' '}
                {displayInDefault(Number(detail.totalAmount ?? 0), detail.currency)}</p>
                <p><span className="font-medium text-slate-700 dark:text-slate-300">{t('bookingsPage.status')}:</span> {detail.status}</p>
                {detailId === detail.id && (
                  <div className="mt-4">
                    <button
                      type="button"
                      onClick={() => openVoucher(detail.id)}
                      className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700"
                    >
                      {voucherData ? t('ui.refreshVoucher') : t('ui.viewVoucher')}
                    </button>
                    {voucherData && (
                      <div className="mt-2 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm dark:border-slate-600 dark:bg-slate-700/50">
                        <p className="font-medium text-slate-900 dark:text-slate-100">{t('ui.voucher')}</p>
                        <pre className="mt-1 overflow-x-auto text-xs text-slate-700 dark:text-slate-200">
                          {JSON.stringify(voucherData, null, 2)}
                        </pre>
                      </div>
                    )}
                  </div>
                )}
                {(detail.status ?? '').toUpperCase() === 'PENDING' && (
                  <div className="mt-4 flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => handleConfirm(detail.id)}
                      className="rounded-xl bg-green-600 px-3 py-1.5 text-sm text-white hover:opacity-90"
                    >
                      {t('ui.confirm')}
                    </button>
                    <input
                      type="text"
                      placeholder={t('ui.cancelReasonPlaceholder')}
                      value={cancelReason}
                      onChange={(e) => setCancelReason(e.target.value)}
                      className="rounded border border-slate-300 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                    />
                    <button
                      type="button"
                      onClick={() => handleCancel(detail.id)}
                      className="rounded-xl bg-amber-600 px-3 py-1.5 text-sm text-white hover:opacity-90"
                    >
                      {t('ui.cancelBooking')}
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <p className="mt-4 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
            )}
            <button
              type="button"
              onClick={() => { setDetailId(null); setDetail(null); setCancelReason(''); setVoucherData(null); }}
              className="mt-4 rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
            >
              {t('ui.close')}
            </button>
          </div>
        </div>
      )}
    </>
  )
}
