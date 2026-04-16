/**
 * Analytics – system-wide metrics for super admin.
 * KPIs, revenue, bookings, providers, complaints, service health,
 * activity feed, commission analysis, and payouts.
 */

import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useDisplayCurrency } from '../context/DisplayCurrencyContext'
import { useLanguage } from '../context/LanguageContext'
import { Card } from '../components/Card'
import { dashboardAPI } from '../services/api'
import type { DashboardBootstrapDto, DashboardLiveDto } from '../types/api'

const ANALYTICS_ACTIVITY_LIMIT = 30
const ANALYTICS_POLL_MS = 45_000

function initialDateRange() {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 1)
  return { start: start.toISOString().slice(0, 10), end: end.toISOString().slice(0, 10) }
}

function SkeletonRow() {
  return <div className="h-4 animate-pulse rounded-md bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
}

export function AnalyticsPage() {
  const { t } = useLanguage()
  const { displayInDefault, defaultCurrency } = useDisplayCurrency()
  const [dateRange, setDateRange] = useState(initialDateRange)
  const queryClient = useQueryClient()
  const bootstrapQueryKey = useMemo(
    () => ['analytics', 'bootstrap', dateRange.start, dateRange.end] as const,
    [dateRange.start, dateRange.end],
  )

  const bootstrapQuery = useQuery({
    queryKey: bootstrapQueryKey,
    queryFn: async () => {
      const res = await dashboardAPI.getBootstrap({
        start: dateRange.start,
        end: dateRange.end,
        activityLimit: ANALYTICS_ACTIVITY_LIMIT,
      })
      return res.data as DashboardBootstrapDto
    },
    staleTime: 60_000,
  })

  useQuery({
    queryKey: ['analytics', 'live', dateRange.start, dateRange.end, ANALYTICS_ACTIVITY_LIMIT],
    queryFn: async () => {
      const res = await dashboardAPI.getLive({
        start: dateRange.start,
        end: dateRange.end,
        activityLimit: ANALYTICS_ACTIVITY_LIMIT,
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
      typeof document !== 'undefined' && document.visibilityState === 'hidden' ? false : ANALYTICS_POLL_MS,
  })

  const data = bootstrapQuery.data
  const kpis = data?.kpis ?? null
  const activity = data?.activity ?? []
  const serviceHealth = data?.serviceHealth ?? null
  const commission = data?.commissionAnalysis ?? null
  const payouts = data?.payouts ?? null

  const sectionPending = bootstrapQuery.isPending

  return (
    <>
      <header className="pb-1">
        <h1 className="app-page-title">{t('analyticsPage.title')}</h1>
      </header>

      <div className="dashboard-toolbar-surface">
        <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{t('analyticsPage.dateRangeLabel')}</span>
        <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:flex-wrap sm:items-center">
          <label className="flex items-center gap-2 text-sm font-medium text-slate-600 dark:text-slate-300">
            <span className="shrink-0">{t('ui.from')}</span>
            <input
              type="date"
              value={dateRange.start}
              onChange={(e) => setDateRange((prev) => ({ ...prev, start: e.target.value }))}
              className="dashboard-date-input min-w-0"
            />
          </label>
          <label className="flex items-center gap-2 text-sm font-medium text-slate-600 dark:text-slate-300">
            <span className="shrink-0">{t('ui.to')}</span>
            <input
              type="date"
              value={dateRange.end}
              onChange={(e) => setDateRange((prev) => ({ ...prev, end: e.target.value }))}
              className="dashboard-date-input min-w-0"
            />
          </label>
        </div>
      </div>

      {/* KPIs */}
      <section aria-busy={sectionPending}>
        <h2 className="dashboard-section-title">{t('analyticsPage.keyMetrics')}</h2>
        <div className="mt-4 grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4 xl:grid-cols-5">
          {sectionPending ? (
            <>
              {[0, 1, 2, 3, 4].map((i) => (
                <Card key={i} className="p-4">
                  <SkeletonRow />
                  <div className="mt-3 h-8 w-24 animate-pulse rounded bg-slate-200/90 dark:bg-slate-700/80" aria-hidden />
                </Card>
              ))}
            </>
          ) : (
            <>
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.totalRevenue')}</p>
                <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">
                  {displayInDefault(Number(kpis?.totalRevenue ?? 0), kpis?.revenueCurrency ?? defaultCurrency)}
                </p>
              </Card>
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.activeBookings')}</p>
                <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{kpis?.activeBookings ?? '—'}</p>
              </Card>
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.totalBookings')}</p>
                <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{kpis?.totalBookings ?? '—'}</p>
              </Card>
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.totalProviders')}</p>
                <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{kpis?.totalProviders ?? '—'}</p>
              </Card>
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.pendingComplaints')}</p>
                <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{kpis?.pendingComplaints ?? '—'}</p>
              </Card>
            </>
          )}
        </div>
      </section>

      {/* Service health */}
      {(sectionPending ||
        (serviceHealth &&
          (Object.keys(serviceHealth.serviceCountByType ?? {}).length > 0 ||
            Object.keys(serviceHealth.activeBookingCountByType ?? {}).length > 0))) && (
        <section className="mt-8">
          <h2 className="dashboard-section-title">{t('home.serviceHealth')}</h2>
          {sectionPending ? (
            <div className="mt-4 grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6">
              <Card className="space-y-2 p-4">
                <SkeletonRow />
                <SkeletonRow />
              </Card>
              <Card className="space-y-2 p-4">
                <SkeletonRow />
                <SkeletonRow />
              </Card>
            </div>
          ) : (
            <div className="mt-4 grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6">
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('analyticsPage.listingsByType')}</p>
                <ul className="mt-2 space-y-1 text-sm text-slate-900 dark:text-slate-100">
                  {Object.entries(serviceHealth!.serviceCountByType ?? {}).map(([type, count]) => (
                    <li key={type}>
                      {type}: {count}
                    </li>
                  ))}
                </ul>
              </Card>
              <Card className="p-4">
                <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('analyticsPage.activeBookingsByType')}</p>
                <ul className="mt-2 space-y-1 text-sm text-slate-900 dark:text-slate-100">
                  {Object.entries(serviceHealth!.activeBookingCountByType ?? {}).map(([type, count]) => (
                    <li key={type}>
                      {type}: {count}
                    </li>
                  ))}
                </ul>
              </Card>
            </div>
          )}
        </section>
      )}

      {/* Commission analysis */}
      {(sectionPending || commission) && (
        <section className="mt-8">
          <h2 className="dashboard-section-title">{t('home.commissionAnalysis')}</h2>
          <Card className="mt-4 p-4">
            {sectionPending ? (
              <div className="grid gap-2 sm:grid-cols-2">
                <SkeletonRow />
                <SkeletonRow />
                <SkeletonRow />
              </div>
            ) : commission ? (
              <dl className="grid gap-2 sm:grid-cols-2">
                <div>
                  <dt className="text-sm text-slate-500 dark:text-slate-400">{t('analyticsPage.period')}</dt>
                  <dd className="text-slate-900 dark:text-slate-100">
                    {t('analyticsPage.dateTo', {
                      start: commission.start ?? '—',
                      end: commission.end ?? '—',
                    })}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500 dark:text-slate-400">{t('analyticsPage.totalBaseAmount')}</dt>
                  <dd className="text-slate-900 dark:text-slate-100">
                    {displayInDefault(Number(commission.totalBaseAmount ?? 0), commission.currency ?? defaultCurrency)}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500 dark:text-slate-400">{t('analyticsPage.totalCommission')}</dt>
                  <dd className="text-slate-900 dark:text-slate-100">
                    {displayInDefault(
                      Number(commission.totalCommissionAmount ?? 0),
                      commission.currency ?? defaultCurrency,
                    )}
                  </dd>
                </div>
              </dl>
            ) : null}
          </Card>
        </section>
      )}

      {/* Payouts */}
      {(sectionPending || (payouts && (payouts.payouts?.length ?? 0) > 0)) && (
        <section className="mt-8">
          <h2 className="dashboard-section-title">{t('analyticsPage.providerPayouts')}</h2>
          {sectionPending ? (
            <Card className="mt-4 space-y-2 p-4">
              <SkeletonRow />
              <SkeletonRow />
              <SkeletonRow />
            </Card>
          ) : (
            <Card className="mt-4 overflow-hidden p-0">
              <table className="data-table">
                <thead>
                  <tr>
                    <th className="px-4 py-3.5">{t('analyticsPage.tableProvider')}</th>
                    <th className="px-4 py-3.5">{t('analyticsPage.tableAmount')}</th>
                    <th className="px-4 py-3.5">{t('analyticsPage.tablePeriod')}</th>
                  </tr>
                </thead>
                <tbody>
                  {payouts!.payouts!.map((p, i) => (
                    <tr key={i}>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                        {p.providerName ?? p.providerId ?? '—'}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                        {displayInDefault(Number(p.amount ?? 0), p.currency ?? defaultCurrency)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                        {t('analyticsPage.dateTo', {
                          start: p.periodStart ?? '—',
                          end: p.periodEnd ?? '—',
                        })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          )}
        </section>
      )}

      {/* Activity feed */}
      <section className="mt-8">
        <h2 className="dashboard-section-title">{t('home.activityFeed')}</h2>
        <Card className="mt-4 p-4">
          {sectionPending ? (
            <div className="space-y-3 py-1">
              <SkeletonRow />
              <SkeletonRow />
              <SkeletonRow />
            </div>
          ) : activity.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">{t('home.noRecentActivity')}</p>
          ) : (
            <ul className="space-y-2">
              {activity.slice(0, 20).map((item, i) => (
                <li key={i} className="flex flex-wrap items-center gap-2 border-b border-slate-100 py-2 last:border-0 dark:border-slate-700">
                  <span className="text-sm font-medium text-slate-900 dark:text-slate-100">
                    {item.title ??
                      item.action ??
                      item.entityType ??
                      item.type ??
                      t('home.activityFallback')}
                  </span>
                  {item.description && <span className="text-sm text-slate-600 dark:text-slate-300">{item.description}</span>}
                  {item.timestamp && <span className="text-xs text-slate-500 dark:text-slate-400">{item.timestamp}</span>}
                </li>
              ))}
            </ul>
          )}
        </Card>
      </section>
    </>
  )
}
