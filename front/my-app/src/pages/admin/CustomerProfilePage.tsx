/**
 * Super admin > Customer profile — account details, bookings, payments.
 */

import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { useLanguage } from '../../context/LanguageContext'
import { adminSuperAPI, getApiErrorMessage, usersAPI } from '../../services/api'
import type { BookingDto, PageDto, PaymentDto, UserDto } from '../../types/api'

function asPage<T>(data: unknown): PageDto<T> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<T>).content)) {
    return data as PageDto<T>
  }
  return null
}

function isCustomerRole(role: string | undefined): boolean {
  return (role || '').toUpperCase() === 'CUSTOMER'
}

const PAGE_SIZE = 10

export function CustomerProfilePage() {
  const { userId } = useParams<{ userId: string }>()
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const { user } = useAuth()
  const isSuperAdmin = user?.role === 'super_admin'

  const [profile, setProfile] = useState<UserDto | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  const [loadingProfile, setLoadingProfile] = useState(true)

  const [bookings, setBookings] = useState<BookingDto[]>([])
  const [bookingsPage, setBookingsPage] = useState(0)
  const [bookingsTotalPages, setBookingsTotalPages] = useState(0)
  const [loadingBookings, setLoadingBookings] = useState(false)

  const [payments, setPayments] = useState<PaymentDto[]>([])
  const [paymentsPage, setPaymentsPage] = useState(0)
  const [paymentsTotalPages, setPaymentsTotalPages] = useState(0)
  const [loadingPayments, setLoadingPayments] = useState(false)

  useEffect(() => {
    setBookingsPage(0)
    setPaymentsPage(0)
  }, [userId])

  useEffect(() => {
    if (!userId) return
    setLoadingProfile(true)
    setProfileError(null)
    usersAPI
      .get(userId)
      .then((res) => {
        const u = res.data as UserDto
        setProfile(u)
        if (!isCustomerRole(u.role)) {
          setProfileError(t('customerProfilePage.notACustomer'))
        }
      })
      .catch((e) => {
        setProfile(null)
        setProfileError(getApiErrorMessage(e, t('customerProfilePage.loadFailed')))
      })
      .finally(() => setLoadingProfile(false))
  }, [userId, t])

  const customerOk = profile != null && isCustomerRole(profile.role)

  useEffect(() => {
    if (!userId || !customerOk) return
    setLoadingBookings(true)
    adminSuperAPI
      .customerBookings(userId, { page: bookingsPage, size: PAGE_SIZE })
      .then((res) => {
        const p = asPage<BookingDto>(res.data)
        setBookings(p?.content ?? [])
        setBookingsTotalPages(p?.totalPages ?? 0)
      })
      .catch(() => {
        setBookings([])
        setBookingsTotalPages(0)
      })
      .finally(() => setLoadingBookings(false))
  }, [userId, customerOk, bookingsPage])

  useEffect(() => {
    if (!userId || !customerOk) return
    setLoadingPayments(true)
    adminSuperAPI
      .customerPayments(userId, { page: paymentsPage, size: PAGE_SIZE })
      .then((res) => {
        const p = asPage<PaymentDto>(res.data)
        setPayments(p?.content ?? [])
        setPaymentsTotalPages(p?.totalPages ?? 0)
      })
      .catch(() => {
        setPayments([])
        setPaymentsTotalPages(0)
      })
      .finally(() => setLoadingPayments(false))
  }, [userId, customerOk, paymentsPage])

  if (!user) return null

  if (!isSuperAdmin) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">
          {t('access.superAdminCustomersWithRole', { role: user.role })}
        </p>
        <Link
          to="/dashboard"
          className="mt-4 inline-block rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          {t('access.backToDashboard')}
        </Link>
      </div>
    )
  }

  if (!userId) return null

  const dash = t('ui.emDash')
  const dateRange = (b: BookingDto) => {
    const a = b.checkInDate ? new Date(b.checkInDate).toLocaleDateString() : dash
    const c = b.checkOutDate ? new Date(b.checkOutDate).toLocaleDateString() : dash
    return `${a} – ${c}`
  }

  return (
    <>
      <div className="flex flex-wrap items-center gap-3">
        <Link
          to="/admin/find-customer"
          className="text-sm font-medium text-primary hover:underline"
        >
          {t('customerProfilePage.backToSearch')}
        </Link>
      </div>
      <h1 className="mt-4 text-2xl font-bold text-slate-800 dark:text-slate-100">{t('customerProfilePage.title')}</h1>

      {loadingProfile ? (
        <p className="mt-6 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : profileError ? (
        <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {profileError}
        </div>
      ) : profile ? (
        <div className="mt-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-900">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('customerProfilePage.detailsTitle')}</h2>
          <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerSearchPage.colName')}</dt>
              <dd className="font-medium text-slate-900 dark:text-slate-100">
                {profile.fullName?.trim() || dash}
              </dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerSearchPage.colEmail')}</dt>
              <dd className="text-slate-800 dark:text-slate-200">{profile.email}</dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerSearchPage.colPhone')}</dt>
              <dd className="text-slate-800 dark:text-slate-200">{profile.phone ?? dash}</dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerSearchPage.colStatus')}</dt>
              <dd className="text-slate-800 dark:text-slate-200">{profile.status ?? dash}</dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerSearchPage.colUserId')}</dt>
              <dd className="font-mono text-xs text-slate-700 dark:text-slate-300">{profile.id}</dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerSearchPage.colCreated')}</dt>
              <dd className="text-slate-800 dark:text-slate-200">
                {profile.createdAt ? new Date(profile.createdAt).toLocaleString() : dash}
              </dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerProfilePage.labelEmailVerified')}</dt>
              <dd className="text-slate-800 dark:text-slate-200">
                {profile.emailVerified === true ? t('ui.yes') : profile.emailVerified === false ? t('ui.no') : dash}
              </dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">{t('customerProfilePage.labelPhoneVerified')}</dt>
              <dd className="text-slate-800 dark:text-slate-200">
                {profile.phoneVerified === true ? t('ui.yes') : profile.phoneVerified === false ? t('ui.no') : dash}
              </dd>
            </div>
          </dl>
        </div>
      ) : null}

      {customerOk && (
        <>
          <h2 className="mt-10 text-lg font-semibold text-slate-900 dark:text-slate-100">{t('customerProfilePage.bookingsTitle')}</h2>
          <div className="mt-4 table-shell">
            {loadingBookings ? (
              <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
            ) : bookings.length === 0 ? (
              <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('customerProfilePage.noBookings')}</div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th className="px-4 py-3.5">{t('customerProfilePage.bookingColRef')}</th>
                    <th className="px-4 py-3.5">{t('customerProfilePage.bookingColStatus')}</th>
                    <th className="px-4 py-3.5">{t('customerProfilePage.bookingColDates')}</th>
                    <th className="px-4 py-3.5">{t('customerProfilePage.bookingColAmount')}</th>
                    <th className="px-4 py-3.5">{t('customerSearchPage.colProfile')}</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((b) => (
                    <tr key={b.id}>
                      <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-800 dark:text-slate-200">
                        {b.bookingReference ?? dash}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{b.status}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{dateRange(b)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">
                        {displayInDefault(Number(b.totalAmount ?? 0), b.currency)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm">
                        <Link
                          to={`/management/bookings?bookingId=${encodeURIComponent(b.id)}`}
                          className="text-primary hover:underline"
                        >
                          {t('customerProfilePage.openBooking')}
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          {bookingsTotalPages > 1 && (
            <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
              <button
                type="button"
                disabled={bookingsPage <= 0 || loadingBookings}
                onClick={() => setBookingsPage((p) => Math.max(0, p - 1))}
                className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                {t('ui.previous')}
              </button>
              <span className="text-sm text-slate-600 dark:text-slate-400">
                {t('ui.pageOf', { current: bookingsPage + 1, total: bookingsTotalPages })}
              </span>
              <button
                type="button"
                disabled={bookingsPage >= bookingsTotalPages - 1 || loadingBookings}
                onClick={() => setBookingsPage((p) => p + 1)}
                className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                {t('ui.next')}
              </button>
            </div>
          )}

          <h2 className="mt-10 text-lg font-semibold text-slate-900 dark:text-slate-100">{t('customerProfilePage.paymentsTitle')}</h2>
          <div className="mt-4 table-shell">
            {loadingPayments ? (
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
                    <th className="px-4 py-3.5">{t('paymentsPage.colDate')}</th>
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
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                        {p.processedAt ? new Date(p.processedAt).toLocaleDateString() : dash}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          {paymentsTotalPages > 1 && (
            <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
              <button
                type="button"
                disabled={paymentsPage <= 0 || loadingPayments}
                onClick={() => setPaymentsPage((p) => Math.max(0, p - 1))}
                className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                {t('ui.previous')}
              </button>
              <span className="text-sm text-slate-600 dark:text-slate-400">
                {t('ui.pageOf', { current: paymentsPage + 1, total: paymentsTotalPages })}
              </span>
              <button
                type="button"
                disabled={paymentsPage >= paymentsTotalPages - 1 || loadingPayments}
                onClick={() => setPaymentsPage((p) => p + 1)}
                className="rounded-lg border-2 border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                {t('ui.next')}
              </button>
            </div>
          )}
        </>
      )}
    </>
  )
}
