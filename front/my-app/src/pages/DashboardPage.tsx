import { useMemo } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useDisplayCurrency } from '../context/DisplayCurrencyContext'
import { useLanguage } from '../context/LanguageContext'
import { Card, StatCard, StatCardIcons } from '../components'
import { dashboardAPI } from '../services/api'
import type {
  DashboardBootstrapDto,
  DashboardLiveDto,
  DashboardKpiDto,
  ActivityFeedItemDto,
  ServiceHealthDto,
  CommissionAnalysisDto,
  PayoutsResponseDto,
} from '../types/api'

function defaultDateRange(): { start: string; end: string } {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - 30)
  return {
    start: start.toISOString().slice(0, 10),
    end: end.toISOString().slice(0, 10),
  }
}

function SkeletonRow() {
  return (
    <div
      className="h-4 animate-pulse rounded-md bg-slate-200/90 dark:bg-slate-700/80"
      aria-hidden
    />
  )
}

function StatCardSkeleton() {
  return (
    <Card>
      <div className="flex flex-col gap-3 animate-pulse [container-type:inline-size]">
        <div className="h-12 w-12 rounded-2xl bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
        <div className="h-3 w-24 rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
        <div className="h-8 w-28 rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
        <div className="h-3 w-full max-w-[12rem] rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
      </div>
    </Card>
  )
}

const DASHBOARD_LIVE_POLL_MS = 45_000

function HealthLiveDot() {
  return (
    <span className="relative inline-flex h-2 w-2 shrink-0" aria-hidden>
      <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400/45 opacity-80 motion-reduce:hidden" />
      <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-500 dark:bg-emerald-400" />
    </span>
  )
}

const DASHBOARD_ACTIVITY_LIMIT = 15

export function DashboardPage() {
  const dateRange = useMemo(() => defaultDateRange(), [])
  const bootstrapQueryKey = useMemo(
    () => ['dashboard', 'bootstrap', dateRange.start, dateRange.end] as const,
    [dateRange.start, dateRange.end],
  )
  const queryClient = useQueryClient()
  const { t } = useLanguage()
  const { displayInDefault, defaultCurrency } = useDisplayCurrency()

  const bootstrapQuery = useQuery({
    queryKey: bootstrapQueryKey,
    queryFn: async () => {
      const res = await dashboardAPI.getBootstrap({
        start: dateRange.start,
        end: dateRange.end,
        activityLimit: DASHBOARD_ACTIVITY_LIMIT,
      })
      return res.data as DashboardBootstrapDto
    },
    staleTime: 60_000,
  })

  useQuery({
    queryKey: ['dashboard', 'live', dateRange.start, dateRange.end, DASHBOARD_ACTIVITY_LIMIT],
    queryFn: async () => {
      const res = await dashboardAPI.getLive({
        start: dateRange.start,
        end: dateRange.end,
        activityLimit: DASHBOARD_ACTIVITY_LIMIT,
      })
      const live = res.data as DashboardLiveDto
      queryClient.setQueryData<DashboardBootstrapDto | undefined>(bootstrapQueryKey, (old) => {
        if (!old) return old
        return {
          ...old,
          kpis: live.kpis,
          activity: live.activity,
          serviceHealth: live.serviceHealth,
        }
      })
      return live
    },
    enabled: bootstrapQuery.isSuccess,
    staleTime: 60_000,
    refetchInterval: () =>
      typeof document !== 'undefined' && document.visibilityState === 'hidden' ? false : DASHBOARD_LIVE_POLL_MS,
  })

  const data = bootstrapQuery.data
  const isPending = bootstrapQuery.isPending

  const kpis: DashboardKpiDto | null = data?.kpis ?? null
  const activity: ActivityFeedItemDto[] = data?.activity ?? []
  const serviceHealth: ServiceHealthDto | null = data?.serviceHealth ?? null
  const commission: CommissionAnalysisDto | null = data?.commissionAnalysis ?? null
  const payouts: PayoutsResponseDto | null = data?.payouts ?? null

  const sectionPending = isPending

  const revenue = kpis != null ? Number(kpis.totalRevenue ?? 0) : 0

  const stats = [
    {
      icon: <StatCardIcons.ActivityIcon />,
      label: t('home.totalRevenue'),
      value: displayInDefault(revenue, kpis?.revenueCurrency ?? defaultCurrency),
      trend: kpis ? t('home.trendBookings', { count: kpis.totalBookings }) : t('ui.emDash'),
      trendPositive: true,
    },
    {
      icon: <StatCardIcons.UserIcon />,
      label: t('home.activeBookings'),
      value: String(kpis?.activeBookings ?? t('ui.emDash')),
      trend: t('home.trendCurrent'),
      trendPositive: true,
    },
    {
      icon: <StatCardIcons.ServerIcon />,
      label: t('home.totalProviders'),
      value: String(kpis?.totalProviders ?? t('ui.emDash')),
      trend: t('home.trendRegistered'),
      trendPositive: true,
    },
    {
      icon: <StatCardIcons.BellIcon />,
      label: t('home.pendingComplaints'),
      value: String(kpis?.pendingComplaints ?? t('ui.emDash')),
      trend: t('home.trendCurrent'),
      trendPositive: (kpis?.pendingComplaints ?? 0) === 0,
    },
  ]

  return (
    <>
      <header className="pb-1">
        <h1 className="app-page-title">{t('home.welcomeTitle')}</h1>
      </header>

      <div
        className="grid grid-cols-1 gap-5 sm:gap-6 sm:grid-cols-[repeat(auto-fit,minmax(min(100%,13.5rem),1fr))] [&>*]:min-w-0"
        aria-busy={sectionPending}
      >
        {sectionPending
          ? Array.from({ length: 4 }, (_, i) => <StatCardSkeleton key={i} />)
          : stats.map((stat) => (
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

      <div className="grid gap-5 lg:grid-cols-2 lg:gap-6">
        <Card className="!p-5">
          <h2 className="dashboard-card-title">{t('home.activityFeed')}</h2>
          <ul className="mt-3 max-h-64 space-y-2 overflow-y-auto">
            {sectionPending ? (
              <>
                <li className="space-y-2 py-1">
                  <SkeletonRow />
                </li>
                <li className="space-y-2 py-1">
                  <SkeletonRow />
                </li>
                <li className="space-y-2 py-1">
                  <SkeletonRow />
                </li>
              </>
            ) : activity.length === 0 ? (
              <li className="text-sm text-slate-500 dark:text-slate-400">{t('home.noRecentActivity')}</li>
            ) : (
              activity.map((item, i) => (
                <li
                  key={item.id ?? i}
                  className="flex justify-between gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors duration-200 hover:bg-slate-100/90 dark:hover:bg-slate-800/50"
                >
                  <span className="text-slate-700 dark:text-slate-200">
                    {item.title ??
                      item.action ??
                      item.entityType ??
                      item.type ??
                      t('home.activityFallback')}
                  </span>
                  <span className="shrink-0 text-slate-500 dark:text-slate-400">{item.timestamp ?? ''}</span>
                </li>
              ))
            )}
          </ul>
        </Card>

        <Card className="!p-5">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="dashboard-card-title">{t('home.serviceHealth')}</h2>
            {!sectionPending &&
              serviceHealth &&
              (Object.keys(serviceHealth.serviceCountByType ?? {}).length > 0 ||
                Object.keys(serviceHealth.activeBookingCountByType ?? {}).length > 0) && (
                <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2 py-0.5 text-[0.65rem] font-semibold tracking-wide text-emerald-700 dark:border-emerald-400/20 dark:bg-emerald-400/10 dark:text-emerald-300">
                  <HealthLiveDot />
                  {t('home.serviceHealthLive')}
                </span>
              )}
          </div>
          {sectionPending ? (
            <div className="mt-3 space-y-2">
              <SkeletonRow />
              <SkeletonRow />
            </div>
          ) : serviceHealth &&
            (Object.keys(serviceHealth.serviceCountByType ?? {}).length > 0 ||
              Object.keys(serviceHealth.activeBookingCountByType ?? {}).length > 0) ? (
            <div className="mt-3 space-y-2 text-sm">
              {Object.entries(serviceHealth.serviceCountByType ?? {}).map(([type, count]) => (
                <div key={type} className="flex justify-between">
                  <span className="text-slate-600 dark:text-slate-300">{type}</span>
                  <span className="font-medium text-slate-900 dark:text-slate-100">{t('home.servicesCount', { count })}</span>
                </div>
              ))}
              {Object.entries(serviceHealth.activeBookingCountByType ?? {}).map(([type, count]) => (
                <div
                  key={`active-${type}`}
                  className="flex items-center justify-between gap-2 rounded-md py-0.5 text-slate-500 transition-colors duration-150 dark:text-slate-400"
                >
                  <span className="flex min-w-0 items-center gap-2">
                    <span
                      className="h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500 motion-safe:animate-pulse dark:bg-emerald-400"
                      aria-hidden
                    />
                    <span className="truncate">{t('home.activeBookingsForType', { type })}</span>
                  </span>
                  <span className="shrink-0 font-medium tabular-nums">{count}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('home.noServiceHealth')}</p>
          )}
        </Card>
      </div>

      {/* Commission analysis & Payout summary */}
      <div className="grid gap-5 lg:grid-cols-2 lg:gap-6">
        <Card className="!p-5">
          <h2 className="dashboard-card-title">{t('home.commissionAnalysis')}</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {dateRange.start} – {dateRange.end}
          </p>
          {sectionPending ? (
            <div className="mt-3 space-y-2">
              <SkeletonRow />
              <SkeletonRow />
            </div>
          ) : commission ? (
            <div className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-600 dark:text-slate-300">{t('home.baseCollected')}</span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {displayInDefault(Number(commission.totalBaseAmount ?? 0), commission.currency ?? defaultCurrency)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600 dark:text-slate-300">{t('home.commissionDelta')}</span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {displayInDefault(Number(commission.totalCommissionAmount ?? 0), commission.currency ?? defaultCurrency)}
                </span>
              </div>
            </div>
          ) : (
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('home.noCommissionPeriod')}</p>
          )}
          <div className="dashboard-card-actions">
            <Link to="/management/payments" className="dashboard-btn-secondary">
              {t('home.viewTransactionLedger')}
            </Link>
          </div>
        </Card>

        <Card className="!p-5">
          <h2 className="dashboard-card-title">{t('home.payoutSummary')}</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {dateRange.start} – {dateRange.end}
          </p>
          {sectionPending ? (
            <div className="mt-3 space-y-2">
              <SkeletonRow />
              <SkeletonRow />
            </div>
          ) : payouts?.payouts && payouts.payouts.length > 0 ? (
            <ul className="mt-3 max-h-40 space-y-1.5 overflow-y-auto text-sm">
              {payouts.payouts.slice(0, 5).map((p, i) => (
                <li key={p.providerId ?? i} className="flex justify-between">
                  <span className="text-slate-600 dark:text-slate-300 truncate mr-2">{p.providerName ?? p.providerId ?? '—'}</span>
                  <span className="font-medium text-slate-900 dark:text-slate-100 shrink-0">
                    {displayInDefault(Number(p.amount ?? 0), p.currency ?? defaultCurrency)}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('home.noPayoutsPeriod')}</p>
          )}
          <div className="dashboard-card-actions">
            <Link to="/management/payments" className="dashboard-btn-secondary">
              {t('home.viewPayments')}
            </Link>
          </div>
        </Card>
      </div>
    </>
  )
}
