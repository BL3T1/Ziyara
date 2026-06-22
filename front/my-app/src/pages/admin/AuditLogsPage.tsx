/**
 * Admin > Audit logs – paginated, filterable, with human-readable diff output.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { auditLogsAPI, getApiErrorMessage } from '../../services/api'
import type { AuditLogDto, PageDto } from '../../types/api'
import { actionLabel } from '../../i18n/enumLabels'
import { formatDateTime } from '../../utils/formatDate'

function JsonDiff({ oldVal, newVal }: { oldVal?: unknown; newVal?: unknown }) {
  if (oldVal == null && newVal == null) return <span className="text-slate-400">—</span>

  let oldParsed: Record<string, unknown> | null = null
  let newParsed: Record<string, unknown> | null = null
  try {
    if (typeof oldVal === 'string') oldParsed = JSON.parse(oldVal) as Record<string, unknown>
    else if (typeof oldVal === 'object' && oldVal !== null) oldParsed = oldVal as Record<string, unknown>
  } catch { /* use raw */ }
  try {
    if (typeof newVal === 'string') newParsed = JSON.parse(newVal) as Record<string, unknown>
    else if (typeof newVal === 'object' && newVal !== null) newParsed = newVal as Record<string, unknown>
  } catch { /* use raw */ }

  if (!oldParsed && !newParsed) {
    const o = oldVal != null ? String(oldVal).slice(0, 40) : null
    const n = newVal != null ? String(newVal).slice(0, 40) : null
    return (
      <span className="text-xs">
        {o && <span className="text-red-500">{o}</span>}
        {o && n && ' → '}
        {n && <span className="text-emerald-500">{n}</span>}
      </span>
    )
  }

  const keys = Array.from(new Set([
    ...Object.keys(oldParsed ?? {}),
    ...Object.keys(newParsed ?? {}),
  ]))

  if (keys.length === 0) return <span className="text-slate-400">—</span>

  return (
    <ul className="space-y-0.5 text-xs">
      {keys.slice(0, 5).map((k) => {
        const o = oldParsed?.[k]
        const n = newParsed?.[k]
        if (o === n) return null
        return (
          <li key={k}>
            <span className="text-slate-500">{k}: </span>
            {o != null && <span className="text-red-500 line-through">{String(o)}</span>}
            {o != null && n != null && ' '}
            {n != null && <span className="text-emerald-500">{String(n)}</span>}
          </li>
        )
      })}
      {keys.length > 5 && <li className="text-slate-400">+{keys.length - 5} more</li>}
    </ul>
  )
}

const PAGE_SIZE = 20

export function AuditLogsPage() {
  const { t, locale } = useLanguage()
  const [logs, setLogs] = useState<AuditLogDto[]>([])
  const [search, setSearch] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    auditLogsAPI
      .getFiltered({
        page,
        size: PAGE_SIZE,
        action: search.trim() || undefined,
        dateFrom: dateFrom ? `${dateFrom}T00:00:00` : undefined,
        dateTo: dateTo ? `${dateTo}T23:59:59` : undefined,
      })
      .then((res) => {
        const data = res.data as PageDto<AuditLogDto> | AuditLogDto[] | null
        if (data && typeof data === 'object' && Array.isArray((data as PageDto<AuditLogDto>).content)) {
          const p = data as PageDto<AuditLogDto>
          setLogs(p.content ?? [])
          setTotalPages(p.totalPages ?? 0)
        } else {
          setLogs(Array.isArray(data) ? data : [])
          setTotalPages(0)
        }
      })
      .catch((e: unknown) => {
        const status = (e as { response?: { status?: number } })?.response?.status
        setError(status === 403 ? t('ui.accessDenied') : getApiErrorMessage(e))
        setLogs([])
      })
      .finally(() => setLoading(false))
  }, [search, dateFrom, dateTo, page])

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('auditLogsPage.title')}</h1>

      <div className="mt-4 flex flex-wrap gap-3">
        <input
          type="search"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          placeholder={t('ui.searchPlaceholder')}
          className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
        />
        <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
          {t('ui.from')}
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => { setDateFrom(e.target.value); setPage(0) }}
            className="dashboard-date-input"
          />
        </label>
        <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
          {t('ui.to')}
          <input
            type="date"
            value={dateTo}
            onChange={(e) => { setDateTo(e.target.value); setPage(0) }}
            className="dashboard-date-input"
          />
        </label>
        {(search || dateFrom || dateTo) && (
          <button
            type="button"
            onClick={() => { setSearch(''); setDateFrom(''); setDateTo(''); setPage(0) }}
            className="dashboard-btn-secondary text-sm"
          >
            {t('auditLogsPage.clearFilters')}
          </button>
        )}
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : logs.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('auditLogsPage.noEntries')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('auditLogsPage.colTime')}</th>
                <th className="px-4 py-3.5">{t('auditLogsPage.colAction')}</th>
                <th className="px-4 py-3.5">{t('auditLogsPage.colEntity')}</th>
                <th className="px-4 py-3.5">{t('auditLogsPage.colUser')}</th>
                <th className="px-4 py-3.5">{t('auditLogsPage.colOldNew')}</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {formatDateTime(log.createdAt, locale)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">
                    {actionLabel(t, log.action)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {log.entityType ?? t('ui.emDash')}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {log.userDisplay ?? t('ui.emDash')}
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <JsonDiff oldVal={log.oldValue} newVal={log.newValue} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-3">
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
    </>
  )
}
