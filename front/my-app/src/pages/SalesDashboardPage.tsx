import { useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
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
const StarIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polygon points="12 2 15 8.5 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 9 8.5 12 2" />
  </svg>
)
const SearchIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8" />
    <path d="m21 21-4.3-4.3" />
  </svg>
)
const FilterIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
  </svg>
)
const ExportIcon = () => (
  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
    <polyline points="7 10 12 15 17 10" />
    <line x1="12" x2="12" y1="15" y2="3" />
  </svg>
)

const REVENUE_META = [
  { nameKey: 'salesDemo.pieHotels' as const, value: 45, color: '#1e4d6b' },
  { nameKey: 'salesDemo.pieRestaurants' as const, value: 25, color: '#ac9e78' },
  { nameKey: 'salesDemo.pieTaxis' as const, value: 18, color: '#3b82f6' },
  { nameKey: 'salesDemo.pieTrips' as const, value: 12, color: '#10b981' },
]

const BOOKINGS_BY_REGION_KEYS = [
  { nameKey: 'salesDemo.regionSaudi' as const, bookings: 1200 },
  { nameKey: 'salesDemo.regionUae' as const, bookings: 950 },
  { nameKey: 'salesDemo.regionQatar' as const, bookings: 620 },
  { nameKey: 'salesDemo.regionKuwait' as const, bookings: 480 },
  { nameKey: 'salesDemo.regionBahrain' as const, bookings: 310 },
]

const RECENT_BOOKINGS = [
  { reference: 'BK-2845', customer: 'Ahmed Al-Sayed', service: 'Luxury Hotel', date: '2026-02-17', amount: '$850', status: 'Completed' },
  { reference: 'BK-2844', customer: 'Fatima Hassan', service: 'Restaurant Booking', date: '2026-02-17', amount: '$120', status: 'Active' },
  { reference: 'BK-2843', customer: 'Omar Ibrahim', service: 'City Tour', date: '2026-02-16', amount: '$450', status: 'Pending' },
  { reference: 'BK-2842', customer: 'Layla Mohammed', service: 'Taxi Service', date: '2026-02-16', amount: '$35', status: 'Completed' },
  { reference: 'BK-2841', customer: 'Yusuf Ali', service: 'Hotel Package', date: '2026-02-16', amount: '$1,200', status: 'Cancelled' },
]

const STATUS_LABEL_KEYS: Record<string, string> = {
  Completed: 'salesDemo.statusCompleted',
  Active: 'salesDemo.statusActive',
  Pending: 'salesDemo.statusPending',
  Cancelled: 'salesDemo.statusCancelled',
}

function StatusBadge({ status }: { status: string }) {
  const { t } = useLanguage()
  const label = STATUS_LABEL_KEYS[status] ? t(STATUS_LABEL_KEYS[status]) : status
  const styles: Record<string, string> = {
    Completed:
      'bg-emerald-100 text-emerald-900 ring-1 ring-emerald-200/80 dark:bg-emerald-950/60 dark:text-emerald-300 dark:ring-emerald-800/50',
    Active:
      'bg-sky-100 text-sky-900 ring-1 ring-sky-200/80 dark:bg-sky-950/50 dark:text-sky-300 dark:ring-sky-800/50',
    Pending:
      'bg-amber-100 text-amber-900 ring-1 ring-amber-200/80 dark:bg-amber-950/50 dark:text-amber-300 dark:ring-amber-800/50',
    Cancelled:
      'bg-red-100 text-red-900 ring-1 ring-red-200/80 dark:bg-red-950/50 dark:text-red-300 dark:ring-red-900/40',
  }
  return (
    <span
      className={`inline-flex rounded-full px-3 py-1.5 text-sm font-medium ${
        styles[status] ?? 'bg-slate-100 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:ring-slate-600'
      }`}
    >
      {label}
    </span>
  )
}

export function SalesDashboardPage() {
  const { theme } = useLayout()
  const { t } = useLanguage()
  const [search, setSearch] = useState('')

  const salesStats = useMemo(
    () => [
      { icon: <DollarIcon />, label: t('salesDemo.statTotalRevenue'), value: '$458.2K', trend: '+18.5%', trendPositive: true },
      { icon: <CalendarIcon />, label: t('salesDemo.statBookingsMtd'), value: '3,842', trend: '+12.3%', trendPositive: true },
      { icon: <UsersIcon />, label: t('salesDemo.statActiveProviders'), value: '284', trend: '+8 new', trendPositive: true },
      { icon: <StarIcon />, label: t('salesDemo.statAvgRating'), value: '4.8', trend: undefined, trendPositive: true },
    ],
    [t]
  )

  const revenueByService = useMemo(
    () => REVENUE_META.map((row) => ({ ...row, name: t(row.nameKey) })),
    [t]
  )

  const bookingsByRegion = useMemo(
    () => BOOKINGS_BY_REGION_KEYS.map((row) => ({ ...row, name: t(row.nameKey) })),
    [t]
  )

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

  const filteredBookings = RECENT_BOOKINGS.filter(
    (b) =>
      b.reference.toLowerCase().includes(search.toLowerCase()) ||
      b.customer.toLowerCase().includes(search.toLowerCase()) ||
      b.service.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <>
      <header className="pb-1">
        <h1 className="app-page-title">{t('salesDashboardPage.pageTitle')}</h1>
      </header>

      <div className="dashboard-toolbar-surface border-primary/20 bg-primary/[0.06] dark:border-primary/25 dark:bg-primary/10">
        <p className="max-w-2xl text-sm font-medium text-slate-800 dark:text-slate-100">{t('salesDashboardPage.ctaTitle')}</p>
        <Link to="/management/providers" className="dashboard-btn-primary w-full shrink-0 sm:w-auto">
          {t('salesDashboardPage.ctaPrimary')}
        </Link>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4">
        {salesStats.map((stat) => (
          <StatCard
            key={stat.label}
            icon={stat.icon}
            label={stat.label}
            value={stat.value}
            trend={stat.trend}
            trendPositive={stat.trendPositive}
          />
        ))}
      </div>

      {/* Charts */}
      <div className="mt-8 grid gap-5 [grid-template-columns:repeat(auto-fill,min(30rem,100%))] [justify-content:center] sm:gap-6 [&_.recharts-text]:fill-slate-600 dark:[&_.recharts-text]:fill-slate-400 [&_.recharts-legend-item-text]:!fill-current">
        <Card>
          <h3 className="dashboard-card-title mb-4">{t('salesDemo.revenueByService')}</h3>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={revenueByService}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={2}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                >
                  {revenueByService.map((entry, i) => (
                    <Cell key={i} fill={entry.color} stroke={theme === 'dark' ? 'rgb(15 23 42)' : '#fff'} strokeWidth={2} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => [`${value}%`, t('salesDemo.share')]} contentStyle={chartChrome.tooltip} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <h3 className="dashboard-card-title mb-4">{t('salesDemo.bookingsByRegion')}</h3>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={bookingsByRegion} layout="vertical" margin={{ left: 20, right: 20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={chartChrome.grid} />
                <XAxis type="number" stroke={chartChrome.axis} fontSize={12} tickLine={false} />
                <YAxis type="category" dataKey="name" width={100} stroke={chartChrome.axis} fontSize={12} tickLine={false} />
                <Tooltip contentStyle={chartChrome.tooltip} formatter={(value) => [value, t('salesDemo.bookings')]} />
                <Bar dataKey="bookings" fill="#1e4d6b" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

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
              <div className="flex w-full flex-wrap gap-2 sm:w-auto sm:justify-end">
                <button type="button" className="dashboard-btn-secondary flex-1 sm:flex-initial">
                  <FilterIcon />
                  {t('ui.filter')}
                </button>
                <button type="button" className="dashboard-btn-secondary flex-1 sm:flex-initial">
                  <ExportIcon />
                  {t('ui.export')}
                </button>
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
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
                  <tr key={booking.reference}>
                    <td className="px-5 py-3.5 text-sm font-medium text-slate-900 dark:text-slate-100">{booking.reference}</td>
                    <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">{booking.customer}</td>
                    <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">{booking.service}</td>
                    <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">{booking.date}</td>
                    <td className="px-5 py-3.5 text-sm text-slate-800 dark:text-slate-200">{booking.amount}</td>
                    <td className="px-5 py-3.5">
                      <StatusBadge status={booking.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </>
  )
}
