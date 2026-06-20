import { useEffect, useMemo, useState } from 'react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts'
import { Card, StatCard } from '../components'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { dashboardAPI, bookingsAPI, getApiErrorMessage } from '../services/api'
import type { DashboardBootstrapDto, BookingDto } from '../types/api'
import { useDisplayCurrency } from '../context/DisplayCurrencyContext'

const DollarIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="12" x2="12" y1="2" y2="22" />
    <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
  </svg>
)
const CalendarIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="18" height="18" x="3" y="4" rx="2" ry="2" />
    <line x1="16" x2="16" y1="2" y2="6" />
    <line x1="8" x2="8" y1="2" y2="6" />
    <line x1="3" x2="21" y1="10" y2="10" />
  </svg>
)
const UsersIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
  </svg>
)
const TicketIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4a2 2 0 0 0 0 4v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4a2 2 0 0 0 0-4V7z" />
  </svg>
)
const SearchIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8" />
    <path d="m21 21-4.3-4.3" />
  </svg>
)

const SERVICE_TYPE_COLORS: Record<string, string> = {
  HOTEL: '#1e4d6b',
  RESORT: '#2e6b8f',
  RESTAURANT: '#ac9e78',
  TAXI: '#3b82f6',
  TRIP: '#10b981',
}

const BOOKING_STATUS_COLORS: Record<string, string> = {
  CONFIRMED: 'bg-emerald-100 text-emerald-900 ring-1 ring-emerald-200/80 dark:bg-emerald-950/60 dark:text-emerald-300 dark:ring-emerald-800/50',
  ACTIVE: 'bg-sky-100 text-sky-900 ring-1 ring-sky-200/80 dark:bg-sky-950/50 dark:text-sky-300 dark:ring-sky-800/50',
  PENDING: 'bg-amber-100 text-amber-900 ring-1 ring-amber-200/80 dark:bg-amber-950/50 dark:text-amber-300 dark:ring-amber-800/50',
  PENDING_CONFIRMATION: 'bg-amber-100 text-amber-900 ring-1 ring-amber-200/80 dark:bg-amber-950/50 dark:text-amber-300 dark:ring-amber-800/50',
  CANCELLED: 'bg-red-100 text-red-900 ring-1 ring-red-200/80 dark:bg-red-950/50 dark:text-red-300 dark:ring-red-900/40',
  COMPLETED: 'bg-emerald-100 text-emerald-900 ring-1 ring-emerald-200/80 dark:bg-emerald-950/60 dark:text-emerald-300 dark:ring-emerald-800/50',
}

function StatusBadge({ status }: { status: string }) {
  const cls = BOOKING_STATUS_COLORS[status] ?? 'bg-slate-100 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:ring-slate-600'
  return (
    <span className={`inline-flex rounded-full px-3 py-1.5 text-sm font-medium ${cls}`}>
      {status.replace(/_/g, ' ')}
    </span>
  )
}

function fmtDate(iso: string | undefined): string {
  if (!iso) return '—'
  try {
    return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(iso))
  } catch {
    return iso
  }
}

function fmtCurrency(amount: number | undefined, currency: string | undefined): string {
  if (amount == null) return '—'
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency ?? 'USD', maximumFractionDigits: 0 }).format(amount)
  } catch {
    return `${amount}`
  }
}

export function SalesDashboardPage() {
  const { theme } = useLayout()
  const { t } = useLanguage()
  const { displayInDefault } = useDisplayCurrency()
  const [search, setSearch] = useState('')
  const [bootstrap, setBootstrap] = useState<DashboardBootstrapDto | null>(null)
  const [bookings, setBookings] = useState<BookingDto[]>([])
  const [loadingKpis, setLoadingKpis] = useState(true)
  const [loadingBookings, setLoadingBookings] = useState(true)
  const [kpiError, setKpiError] = useState<string | null>(null)
  const [bookingsError, setBookingsError] = useState<string | null>(null)

  useEffect(() => {
    dashboardAPI
      .getBootstrap()
      .then((res) => setBootstrap(res.data as DashboardBootstrapDto))
      .catch((e: unknown) => {
        const status = (e as { response?: { status?: number } })?.response?.status
        setKpiError(status === 403 ? t('ui.accessDenied') : getApiErrorMessage(e))
        setBootstrap(null)
      })
      .finally(() => setLoadingKpis(false))
  }, [])

  useEffect(() => {
    bookingsAPI
      .listAdmin({ page: 0, size: 10 })
      .then((res) => {
        const data = res.data as { content?: BookingDto[] } | BookingDto[]
        if (Array.isArray(data)) setBookings(data)
        else if (data && 'content' in data && Array.isArray(data.content)) setBookings(data.content)
        else setBookings([])
      })
      .catch((e: unknown) => {
        const status = (e as { response?: { status?: number } })?.response?.status
        setBookingsError(status === 403 ? t('ui.accessDenied') : getApiErrorMessage(e))
        setBookings([])
      })
      .finally(() => setLoadingBookings(false))
  }, [])

  const kpis = bootstrap?.kpis

  const salesStats = useMemo(() => {
    const revenue = kpis ? displayInDefault(kpis.totalRevenue, kpis.revenueCurrency) : '—'
    return [
      { icon: <DollarIcon />, label: t('salesDemo.statTotalRevenue'), value: revenue, trendPositive: true },
      { icon: <CalendarIcon />, label: t('salesDemo.statBookingsMtd'), value: kpis ? String(kpis.totalBookings) : '—', trendPositive: true },
      { icon: <UsersIcon />, label: t('salesDemo.statActiveProviders'), value: kpis ? String(kpis.totalProviders) : '—', trendPositive: true },
      { icon: <TicketIcon />, label: t('salesDemo.statOpenTickets'), value: kpis ? String(kpis.openTickets) : '—', trendPositive: false },
    ]
  }, [kpis, t, displayInDefault])

  const bookingsByServiceType = useMemo(() => {
    const counts = bootstrap?.serviceHealth?.activeBookingCountByType ?? {}
    const colors = ['#1e4d6b', '#ac9e78', '#3b82f6', '#10b981', '#f59e0b']
    return Object.entries(counts).map(([type, count], i) => ({
      name: type,
      value: count as number,
      color: SERVICE_TYPE_COLORS[type] ?? colors[i % colors.length],
    }))
  }, [bootstrap])

  const servicesByType = useMemo(() => {
    const counts = bootstrap?.serviceHealth?.serviceCountByType ?? {}
    return Object.entries(counts).map(([type, count]) => ({
      name: type,
      count: count as number,
    }))
  }, [bootstrap])

  const chartChrome = useMemo(() => {
    const dark = theme === 'dark'
    return {
      grid: dark ? '#334155' : '#e2e8f0',
      axis: dark ? '#94a3b8' : '#64748b',
      tooltip: dark
        ? {
            borderRadius: 10,
            border: '1px solid rgb(51 65 85)',
            background: 'rgb(15 23 42 / 0.96)',
            color: 'rgb(241 245 249)',
            boxShadow: '0 12px 40px -12px rgb(0 0 0 / 0.5)',
          }
        : {
            borderRadius: 10,
            border: '1px solid rgb(226 232 240)',
            background: 'rgb(255 255 255 / 0.98)',
            color: 'rgb(15 23 42)',
            boxShadow: '0 10px 40px -15px rgb(15 23 42 / 0.12)',
          },
    }
  }, [theme])

  const filteredBookings = bookings.filter(
    (b) =>
      (b.bookingReference ?? '').toLowerCase().includes(search.toLowerCase()) ||
      (b.customerName ?? '').toLowerCase().includes(search.toLowerCase()) ||
      (b.serviceName ?? '').toLowerCase().includes(search.toLowerCase())
  )

  return (
    <>
      <header className="pb-1">
        <h1 className="app-page-title">{t('salesDashboardPage.pageTitle')}</h1>
      </header>

      {(kpiError || bookingsError) && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {kpiError ?? bookingsError}
        </div>
      )}

      {/* Stat cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4">
        {salesStats.map((stat) => (
          <StatCard
            key={stat.label}
            icon={stat.icon}
            label={stat.label}
            value={loadingKpis ? '…' : stat.value}
            trendPositive={stat.trendPositive}
          />
        ))}
      </div>

      {/* Charts */}
      {(bookingsByServiceType.length > 0 || servicesByType.length > 0) && (
        <div className="mt-8 grid gap-5 [grid-template-columns:repeat(auto-fill,min(30rem,100%))] [justify-content:center] sm:gap-6 [&_.recharts-text]:fill-slate-600 dark:[&_.recharts-text]:fill-slate-400 [&_.recharts-legend-item-text]:!fill-current">
          {bookingsByServiceType.length > 0 && (
            <Card>
              <h3 className="dashboard-card-title mb-4">{t('salesDemo.revenueByService')}</h3>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={bookingsByServiceType}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={2}
                      dataKey="value"
                      label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                    >
                      {bookingsByServiceType.map((entry, i) => (
                        <Cell key={i} fill={entry.color} stroke={theme === 'dark' ? 'rgb(15 23 42)' : '#fff'} strokeWidth={2} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => [value, t('salesDemo.bookings')]} contentStyle={chartChrome.tooltip} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </Card>
          )}

          {servicesByType.length > 0 && (
            <Card>
              <h3 className="dashboard-card-title mb-4">{t('salesDemo.servicesByType')}</h3>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={servicesByType} layout="vertical" margin={{ left: 20, right: 20 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke={chartChrome.grid} />
                    <XAxis type="number" stroke={chartChrome.axis} fontSize={12} tickLine={false} />
                    <YAxis type="category" dataKey="name" width={100} stroke={chartChrome.axis} fontSize={12} tickLine={false} />
                    <Tooltip contentStyle={chartChrome.tooltip} formatter={(value) => [value, t('salesDemo.services')]} />
                    <Bar dataKey="count" fill="#1e4d6b" radius={[0, 6, 6, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>
          )}
        </div>
      )}

      {/* Recent Bookings */}
      <div className="mt-8">
        <h3 className="dashboard-section-title mb-4">{t('salesDemo.recentBookings')}</h3>
        <Card className="overflow-hidden p-0">
          <div className="border-b border-slate-200/90 bg-slate-50/50 dark:border-slate-700/80 dark:bg-slate-900/40">
            <div className="dashboard-toolbar-surface rounded-none border-0 bg-transparent px-4 py-4 dark:bg-transparent">
              <div className="relative min-w-0 flex-1">
                <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                  <SearchIcon />
                </span>
                <input
                  type="search"
                  placeholder={t('ui.searchPlaceholder')}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="dashboard-date-input w-full pl-10 placeholder:text-slate-400 dark:placeholder:text-slate-500"
                />
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            {loadingBookings ? (
              <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
            ) : filteredBookings.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">{t('bookingsPage.noBookings')}</div>
            ) : (
              <table className="data-table w-full">
                <thead>
                  <tr>
                    <th className="px-5 py-3.5">{t('salesDemo.bookingReference')}</th>
                    <th className="px-5 py-3.5">{t('salesDemo.customer')}</th>
                    <th className="px-5 py-3.5">{t('salesDemo.service')}</th>
                    <th className="px-5 py-3.5">{t('salesDemo.date')}</th>
                    <th className="px-5 py-3.5">{t('salesDemo.amount')}</th>
                    <th className="px-5 py-3.5">{t('salesDemo.status')}</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredBookings.map((booking) => (
                    <tr key={booking.id}>
                      <td className="px-5 py-3.5 text-sm font-medium text-slate-900 dark:text-slate-100">
                        {booking.bookingReference ?? booking.id.slice(0, 8)}
                      </td>
                      <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">
                        {booking.customerName ?? booking.customerEmail ?? '—'}
                      </td>
                      <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">
                        {booking.serviceName ?? '—'}
                      </td>
                      <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">
                        {fmtDate(booking.createdAt)}
                      </td>
                      <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">
                        {fmtCurrency(booking.totalAmount, booking.currency)}
                      </td>
                      <td className="px-5 py-3.5">
                        <StatusBadge status={booking.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </Card>
      </div>
    </>
  )
}
