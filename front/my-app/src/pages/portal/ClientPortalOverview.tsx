/**
 * Provider dashboard overview (command center).
 * KPIs scoped to the provider: upcoming bookings, revenue, pending tasks.
 */

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { useLanguage } from '../../context/LanguageContext'
import { portalAPI } from '../../services/api'
import type { PortalDashboardDto } from '../../types/api'
import { Card, StatCard, StatCardIcons } from '../../components'

const CalendarIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2" />
    <path d="M16 2v4M8 2v4M3 10h18" />
  </svg>
)

const ListingsIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    <polyline points="9 22 9 12 15 12 15 22" />
  </svg>
)

function StatSkeleton() {
  return (
    <Card>
      <div className="flex flex-col gap-4 animate-pulse [container-type:inline-size]">
        <div className="h-11 w-11 rounded-xl bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
        <div className="flex flex-col gap-1">
          <div className="h-3 w-20 rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
          <div className="h-8 w-24 rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
        </div>
      </div>
    </Card>
  )
}

export function ClientPortalOverview() {
  const { t } = useLanguage()
  const [kpis, setKpis] = useState<PortalDashboardDto | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    portalAPI
      .getDashboard()
      .then((res) => { if (!cancelled) setKpis(res.data as PortalDashboardDto) })
      .catch(() => { if (!cancelled) setKpis(null) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  const revenueDisplay = kpis != null
    ? `${kpis.revenueCurrency ?? 'USD'} ${Number(kpis.totalRevenue ?? 0).toLocaleString()}`
    : '—'

  function pctDelta(current?: number, previous?: number): { label: string; positive: boolean } | null {
    if (current == null || previous == null) return null
    if (previous === 0 && current === 0) return null
    if (previous === 0) return { label: t('portalHome.trendNew'), positive: true }
    const delta = Math.round(((current - previous) / previous) * 100)
    const sign = delta >= 0 ? '+' : ''
    return { label: `${sign}${delta}% ${t('portalHome.trendVsLast30')}`, positive: delta >= 0 }
  }

  const bookingDelta = pctDelta(kpis?.bookingsLast30Days, kpis?.bookingsPrev30Days)
  const revenueDelta = pctDelta(
    kpis?.revenueLast30Days != null ? Number(kpis.revenueLast30Days) : undefined,
    kpis?.revenuePrev30Days != null ? Number(kpis.revenuePrev30Days) : undefined,
  )

  return (
    <>
      <header className="pb-1">
        <h1 className="app-page-title">{t('portalHome.welcomeTitle')}</h1>
      </header>

      {/* KPI grid */}
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4" aria-busy={loading}>
        {loading ? (
          Array.from({ length: 4 }, (_, i) => <StatSkeleton key={i} />)
        ) : (
          <>
            <StatCard
              icon={<StatCardIcons.ActivityIcon />}
              label={t('portalHome.activeBookings')}
              value={String(kpis?.activeBookings ?? '—')}
              trend={t('portalHome.activeLabel')}
              trendPositive
            />
            <StatCard
              icon={<StatCardIcons.ServerIcon />}
              label={t('portalHome.revenueCompleted')}
              value={revenueDisplay}
              trend={revenueDelta?.label ?? t('portalHome.revenueLabel')}
              trendPositive={revenueDelta?.positive ?? true}
            />
            <StatCard
              icon={<CalendarIcon />}
              label={t('portalHome.totalBookings')}
              value={String(kpis?.totalBookings ?? '—')}
              trend={bookingDelta?.label ?? t('portalHome.allTimeLabel')}
              trendPositive={bookingDelta?.positive ?? true}
            />
            <StatCard
              icon={<ListingsIcon />}
              label={t('portalHome.myListings')}
              value={String(kpis?.serviceCount ?? '—')}
              trend={t('portalHome.activeLabel')}
              trendPositive
            />
          </>
        )}
      </div>

      {/* Weekly revenue chart */}
      <Card className="!p-5">
        <h2 className="dashboard-card-title mb-4">{t('portalHome.weeklyRevenue')}</h2>
        {kpis?.weeklyRevenue && kpis.weeklyRevenue.length > 0 ? (
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={kpis.weeklyRevenue} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.12)" />
              <XAxis
                dataKey="week"
                tickFormatter={(v: string) => {
                  const d = new Date(v + 'T00:00:00')
                  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
                }}
                tick={{ fontSize: 11, fill: '#64748b' }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tickFormatter={(v: number) => (v >= 1000 ? `${(v / 1000).toFixed(1)}k` : String(v))}
                tick={{ fontSize: 11, fill: '#64748b' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(value) =>
                  [`${kpis.revenueCurrency ?? 'USD'} ${Number(value ?? 0).toLocaleString()}`, t('portalHome.revenueCompleted')]
                }
                labelFormatter={(label) => {
                  const d = new Date(String(label) + 'T00:00:00')
                  return `${t('portalHome.weekOf')} ${d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}`
                }}
                contentStyle={{
                  borderRadius: '12px',
                  border: '1px solid rgba(148,163,184,0.15)',
                  background: 'rgba(15, 23, 42, 0.92)',
                  color: '#e2e8f0',
                  fontSize: '12px',
                  backdropFilter: 'blur(8px)',
                }}
                itemStyle={{ color: '#90caff' }}
                labelStyle={{ color: '#94a3b8', marginBottom: 4 }}
              />
              <Bar dataKey="amount" fill="#1e4d6b" radius={[4, 4, 0, 0]} maxBarSize={36} />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex h-[180px] items-center justify-center">
            <p className="text-sm text-slate-500 dark:text-slate-400">{t('portalHome.noRevenueData')}</p>
          </div>
        )}
      </Card>

      {/* Quick actions */}
      <div className="flex flex-wrap gap-3">
        <Link to="/portal/listings" className="dashboard-btn-primary shrink-0">
          {t('nav.listings')}
        </Link>
        <Link to="/portal/bookings" className="dashboard-btn-secondary shrink-0">
          {t('nav.bookings')}
        </Link>
        <Link to="/portal/support" className="dashboard-btn-ghost shrink-0">
          {t('nav.support')}
        </Link>
      </div>
    </>
  )
}
