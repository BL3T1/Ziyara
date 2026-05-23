/**
 * Provider dashboard overview (command center).
 * KPIs scoped to the provider: upcoming bookings, revenue, pending tasks.
 * KPIs from GET /portal/dashboard (provider-scoped).
 */

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { useLanguage } from '../../context/LanguageContext'
import { portalAPI } from '../../services/api'
import type { PortalDashboardDto } from '../../types/api'
import { Card } from '../../components/Card'

function StatSkeleton() {
  return (
    <Card>
      <div className="h-4 w-28 animate-pulse rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
      <div className="mt-3 h-8 w-20 animate-pulse rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
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
      .then((res) => {
        if (!cancelled) setKpis(res.data as PortalDashboardDto)
      })
      .catch(() => {
        if (!cancelled) setKpis(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <>
      <header className="pb-1">
        <h1 className="app-page-title">{t('portalHome.welcomeTitle')}</h1>
      </header>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4" aria-busy={loading}>
        {loading ? (
          <>
            <StatSkeleton />
            <StatSkeleton />
            <StatSkeleton />
            <StatSkeleton />
          </>
        ) : (
          <>
            <Card>
              <div className="[container-type:inline-size]">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('portalHome.activeBookings')}</p>
              <p className="mt-1 font-bold leading-tight text-slate-900 [font-size:clamp(1.125rem,14cqw,1.5rem)] [overflow-wrap:anywhere] dark:text-slate-100">
                {kpis?.activeBookings ?? '—'}
              </p>
              </div>
            </Card>
            <Card>
              <div className="[container-type:inline-size]">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('portalHome.revenueCompleted')}</p>
              <p className="mt-1 font-bold leading-tight text-slate-900 [font-size:clamp(1.125rem,14cqw,1.5rem)] [overflow-wrap:anywhere] dark:text-slate-100">
                {kpis != null ? `${kpis.revenueCurrency ?? 'USD'} ${Number(kpis.totalRevenue ?? 0).toLocaleString()}` : '—'}
              </p>
              </div>
            </Card>
            <Card>
              <div className="[container-type:inline-size]">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('portalHome.totalBookings')}</p>
              <p className="mt-1 font-bold leading-tight text-slate-900 [font-size:clamp(1.125rem,14cqw,1.5rem)] [overflow-wrap:anywhere] dark:text-slate-100">
                {kpis?.totalBookings ?? '—'}
              </p>
              </div>
            </Card>
            <Card>
              <div className="[container-type:inline-size]">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('portalHome.myListings')}</p>
              <p className="mt-1 font-bold leading-tight text-slate-900 [font-size:clamp(1.125rem,14cqw,1.5rem)] [overflow-wrap:anywhere] dark:text-slate-100">
                {kpis?.serviceCount ?? '—'}
              </p>
              </div>
            </Card>
          </>
        )}
      </div>

      {/* Weekly Revenue Chart */}
      {kpis?.weeklyRevenue && kpis.weeklyRevenue.length > 0 && (
        <Card className="p-5">
          <h2 className="mb-4 text-sm font-semibold text-slate-700 dark:text-slate-200">
            {t('portalHome.weeklyRevenue')}
          </h2>
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={kpis.weeklyRevenue} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
              <XAxis
                dataKey="week"
                tickFormatter={(v: string) => {
                  const d = new Date(v + 'T00:00:00')
                  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
                }}
                tick={{ fontSize: 11, fill: 'var(--color-text-muted, #94a3b8)' }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tickFormatter={(v: number) => (v >= 1000 ? `${(v / 1000).toFixed(1)}k` : String(v))}
                tick={{ fontSize: 11, fill: 'var(--color-text-muted, #94a3b8)' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(value) =>
                  [`${kpis.revenueCurrency ?? 'USD'} ${Number(value ?? 0).toLocaleString()}`, t('portalHome.revenueCompleted')]
                }
                labelFormatter={(label) => {
                  const d = new Date(String(label) + 'T00:00:00')
                  return `Week of ${d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}`
                }}
                contentStyle={{
                  borderRadius: '10px',
                  border: '1px solid rgba(148,163,184,0.2)',
                  fontSize: '12px',
                }}
              />
              <Bar dataKey="amount" fill="var(--color-primary, #1e4d6b)" radius={[4, 4, 0, 0]} maxBarSize={40} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      )}

      <div className="flex flex-wrap gap-3 sm:gap-4">
        <Link
          to="/portal/listings"
          className="dashboard-btn-primary shrink-0 shadow-primary/20"
        >
          {t('nav.listings')}
        </Link>
        <Link
          to="/portal/bookings"
          className="rounded-xl bg-primary/10 px-4 py-2.5 text-sm font-semibold text-primary outline-none transition hover:bg-primary/20 focus-visible:ring-2 focus-visible:ring-primary/35 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-50 dark:focus-visible:ring-offset-slate-950"
        >
          {t('nav.bookings')}
        </Link>
        <Link
          to="/portal/support"
          className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 outline-none transition hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-primary/30 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800 dark:focus-visible:ring-offset-slate-950"
        >
          {t('nav.support')}
        </Link>
      </div>
    </>
  )
}
