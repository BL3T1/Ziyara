/**
 * Management > Reports – revenue and bookings by date range, scope, and display currency.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDisplayCurrency } from '../../context/DisplayCurrencyContext'
import { Card } from '../../components/Card'
import { getApiErrorMessage, providersAPI, reportsAPI } from '../../services/api'
import type { PageDto, ServiceProviderDto, UserDto } from '../../types/api'

interface DayTotal {
  date: string
  amount: number
}

interface DayCount {
  date: string
  count: number
}

interface RevenueReport {
  start: string
  end: string
  totalRevenue: number
  currency: string
  byDay: DayTotal[]
}

interface BookingReport {
  start: string
  end: string
  totalBookings: number
  byDay: DayCount[]
}

type ReportScope = 'ALL' | 'PROVIDER' | 'CUSTOMER'

const SCOPE_OPTIONS: { id: ReportScope; labelKey: string }[] = [
  { id: 'ALL', labelKey: 'reportsPage.scopeAll' },
  { id: 'PROVIDER', labelKey: 'reportsPage.scopeProvider' },
  { id: 'CUSTOMER', labelKey: 'reportsPage.scopeCustomer' },
]

export function ReportsPage() {
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 1)
  const [dateRange, setDateRange] = useState({
    start: start.toISOString().slice(0, 10),
    end: end.toISOString().slice(0, 10),
  })
  const [reportScope, setReportScope] = useState<ReportScope>('ALL')
  const [providerId, setProviderId] = useState('')
  const [providers, setProviders] = useState<ServiceProviderDto[]>([])
  const [customerId, setCustomerId] = useState('')
  const [customerQuery, setCustomerQuery] = useState('')
  const [customerHits, setCustomerHits] = useState<UserDto[]>([])
  const [customerSearchBusy, setCustomerSearchBusy] = useState(false)

  const [revenue, setRevenue] = useState<RevenueReport | null>(null)
  const [bookings, setBookings] = useState<BookingReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (reportScope !== 'PROVIDER') return
    providersAPI
      .list({ page: 0, size: 200 })
      .then((r) => {
        const p = r.data as PageDto<ServiceProviderDto>
        setProviders(Array.isArray(p?.content) ? p.content : [])
      })
      .catch(() => setProviders([]))
  }, [reportScope])

  const reportOpts = () => {
    if (reportScope === 'PROVIDER' && providerId) {
      return { scope: 'PROVIDER', providerId }
    }
    if (reportScope === 'CUSTOMER' && customerId) {
      return { scope: 'CUSTOMER', customerId }
    }
    return { scope: 'ALL' }
  }

  const runReports = () => {
    if (reportScope === 'PROVIDER' && !providerId) {
      setError(t('reportsPage.errProviderRequired'))
      return
    }
    if (reportScope === 'CUSTOMER' && !customerId) {
      setError(t('reportsPage.errCustomerRequired'))
      return
    }
    setLoading(true)
    setError(null)
    setRevenue(null)
    setBookings(null)
    const opts = reportOpts()
    Promise.all([
      reportsAPI.getRevenueReport(dateRange.start, dateRange.end, opts).then((r) => r.data as RevenueReport),
      reportsAPI.getBookingReport(dateRange.start, dateRange.end, opts).then((r) => r.data as BookingReport),
    ])
      .then(([rev, book]) => {
        setRevenue(rev ?? null)
        setBookings(book ?? null)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
      })
      .finally(() => setLoading(false))
  }

  const searchCustomers = () => {
    const q = customerQuery.trim()
    if (!q) {
      setCustomerHits([])
      return
    }
    setCustomerSearchBusy(true)
    reportsAPI
      .searchReportCustomers(q)
      .then((res) => {
        const data = res.data
        setCustomerHits(Array.isArray(data) ? (data as UserDto[]) : [])
      })
      .catch(() => setCustomerHits([]))
      .finally(() => setCustomerSearchBusy(false))
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('title.reports')}</h1>
      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 space-y-4 rounded-xl border border-slate-200 bg-slate-50/80 p-4 dark:border-slate-700 dark:bg-slate-800/40">
        <div className="flex flex-wrap gap-4">
          <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <span>{t('ui.from')}</span>
            <input
              type="date"
              value={dateRange.start}
              onChange={(e) => setDateRange((d) => ({ ...d, start: e.target.value }))}
              className="rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <span>{t('ui.to')}</span>
            <input
              type="date"
              value={dateRange.end}
              onChange={(e) => setDateRange((d) => ({ ...d, end: e.target.value }))}
              className="rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </label>
        </div>

        <div>
          <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{t('reportsPage.scopeLabel')}</p>
          <div className="mt-2 flex flex-wrap gap-3">
            {SCOPE_OPTIONS.map(({ id: s, labelKey }) => (
              <label key={s} className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                <input
                  type="radio"
                  name="reportScope"
                  checked={reportScope === s}
                  onChange={() => {
                    setReportScope(s)
                    setError(null)
                    if (s !== 'CUSTOMER') {
                      setCustomerId('')
                      setCustomerHits([])
                    }
                    if (s !== 'PROVIDER') setProviderId('')
                  }}
                />
                {t(labelKey)}
              </label>
            ))}
          </div>
        </div>

        {reportScope === 'PROVIDER' && (
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('reportsPage.providerLabel')}</label>
            <select
              value={providerId}
              onChange={(e) => setProviderId(e.target.value)}
              className="mt-1 w-full max-w-md rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            >
              <option value="">{t('reportsPage.providerPlaceholder')}</option>
              {providers.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name ?? p.id}
                </option>
              ))}
            </select>
          </div>
        )}

        {reportScope === 'CUSTOMER' && (
          <div className="space-y-2">
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('reportsPage.customerLabel')}</label>
            <div className="flex flex-wrap gap-2">
              <input
                type="search"
                value={customerQuery}
                onChange={(e) => setCustomerQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && searchCustomers()}
                placeholder={t('reportsPage.customerSearchPlaceholder')}
                className="min-w-[12rem] flex-1 rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              />
              <button
                type="button"
                onClick={searchCustomers}
                disabled={customerSearchBusy}
                className="dashboard-btn-secondary shrink-0 disabled:opacity-50"
              >
                {customerSearchBusy ? t('ui.loading') : t('reportsPage.customerSearch')}
              </button>
            </div>
            {customerHits.length > 0 && (
              <ul className="max-h-40 overflow-y-auto rounded-lg border border-slate-200 dark:border-slate-600">
                {customerHits.map((u) => (
                  <li key={u.id}>
                    <button
                      type="button"
                      onClick={() => setCustomerId(u.id)}
                      className={`w-full px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-slate-700 ${
                        customerId === u.id ? 'bg-primary/10 font-medium text-primary' : 'text-slate-800 dark:text-slate-200'
                      }`}
                    >
                      {u.email} {u.id === customerId ? `(${t('reportsPage.customerSelected')})` : ''}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <button
          type="button"
          onClick={runReports}
          disabled={loading}
          className="dashboard-btn-primary shrink-0 disabled:opacity-50"
        >
          {loading ? t('ui.loading') : t('ui.runReports')}
        </button>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-2">
        <Card className="p-5 sm:p-6">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('reportsPage.revenueReport')}</h2>
          {revenue ? (
            <>
              <p className="mt-2 text-2xl font-bold text-slate-800 dark:text-slate-100">
                {displayInDefault(Number(revenue.totalRevenue ?? 0), revenue.currency)}
              </p>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                {revenue.start} – {revenue.end}
              </p>
              {(revenue.byDay?.length ?? 0) > 0 && (
                <div className="mt-4 max-h-64 overflow-y-auto rounded-xl ring-1 ring-slate-200/80 dark:ring-slate-600/60">
                  <table className="data-table text-sm">
                    <thead>
                      <tr>
                        <th className="px-3 py-2.5">{t('ui.dateCol')}</th>
                        <th className="px-3 py-2.5 text-end">{t('bookingsPage.amount')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {revenue.byDay.map((row) => (
                        <tr key={row.date}>
                          <td className="px-3 py-2 text-slate-600 dark:text-slate-300">{row.date}</td>
                          <td className="px-3 py-2 text-end font-medium text-slate-900 dark:text-slate-100">
                            {displayInDefault(Number(row.amount ?? 0), revenue.currency)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          ) : (
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{t('reportsPage.selectRunRevenue')}</p>
          )}
        </Card>

        <Card className="p-5 sm:p-6">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('reportsPage.bookingsReport')}</h2>
          {bookings ? (
            <>
              <p className="mt-2 text-2xl font-bold text-slate-800 dark:text-slate-100">
                {t('reportsPage.bookingsTotal', { count: Number(bookings.totalBookings ?? 0).toLocaleString() })}
              </p>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                {bookings.start} – {bookings.end}
              </p>
              {(bookings.byDay?.length ?? 0) > 0 && (
                <div className="mt-4 max-h-64 overflow-y-auto rounded-xl ring-1 ring-slate-200/80 dark:ring-slate-600/60">
                  <table className="data-table text-sm">
                    <thead>
                      <tr>
                        <th className="px-3 py-2.5">{t('ui.dateCol')}</th>
                        <th className="px-3 py-2.5 text-end">{t('ui.countCol')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bookings.byDay.map((row) => (
                        <tr key={row.date}>
                          <td className="px-3 py-2 text-slate-600 dark:text-slate-300">{row.date}</td>
                          <td className="px-3 py-2 text-end font-medium text-slate-900 dark:text-slate-100">{row.count}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          ) : (
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{t('reportsPage.selectRunBookings')}</p>
          )}
        </Card>
      </div>
    </>
  )
}
