/**
 * Provider portal: completed-payment earnings summary with optional date range.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { PortalEarningsDto } from '../../types/api'
import { Card } from '../../components/Card'

export function PortalEarningsPage() {
  const { t } = useLanguage()
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [data, setData] = useState<PortalEarningsDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    portalAPI
      .getEarnings({
        start: start.trim() || undefined,
        end: end.trim() || undefined,
      })
      .then((res) => setData(res.data as PortalEarningsDto))
      .catch((e) => {
        setData(null)
        setError(getApiErrorMessage(e))
      })
      .finally(() => setLoading(false))
  }, [start, end])

  useEffect(() => {
    load()
  }, [])

  return (
    <>
      <h1 className="app-page-title">{t('title.earnings')}</h1>
      <Card className="mt-6 p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:flex-wrap sm:items-end">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.earningsStart')}</label>
            <input
              type="date"
              value={start}
              onChange={(e) => setStart(e.target.value)}
              className="mt-1 rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">{t('portalPages.earningsEnd')}</label>
            <input
              type="date"
              value={end}
              onChange={(e) => setEnd(e.target.value)}
              className="mt-1 rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
            />
          </div>
          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {loading ? t('ui.loading') : t('portalPages.applyRange')}
          </button>
        </div>

        {error && (
          <p className="mt-4 text-sm text-red-600 dark:text-red-400">{error}</p>
        )}

        {data && !loading && (
          <div className="mt-8">
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {data.start && data.end
                ? t('portalPages.earningsPeriod', { start: data.start, end: data.end })
                : t('portalPages.earningsAllTime')}
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-900 dark:text-slate-50">
              {data.currency ?? 'USD'}{' '}
              {typeof data.totalEarnings === 'number'
                ? data.totalEarnings.toLocaleString()
                : Number(data.totalEarnings ?? 0).toLocaleString()}
            </p>
            {(data.providerNet !== undefined || data.availableForPayout !== undefined) && (
              <div className="mt-4 flex flex-col gap-1">
                {data.providerNet !== undefined && (
                  <p className="text-sm text-slate-600 dark:text-slate-300">
                    {t('portalPages.providerNet')}: {data.currency ?? 'USD'} {Number(data.providerNet).toLocaleString()}
                  </p>
                )}
                {data.availableForPayout !== undefined && (
                  <div className="flex flex-wrap items-center gap-4">
                    <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                      {t('portalPages.availableForPayout')}: {data.currency ?? 'USD'} {Number(data.availableForPayout).toLocaleString()}
                    </p>
                    <Link
                      to="/portal/payouts"
                      className="text-sm font-semibold text-primary hover:underline"
                    >
                      {t('portalPages.requestPayoutLink')} →
                    </Link>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {!data && !loading && !error && (
          <p className="mt-6 text-sm text-slate-500 dark:text-slate-400">{t('portalPages.earningsHint')}</p>
        )}
      </Card>
    </>
  )
}
