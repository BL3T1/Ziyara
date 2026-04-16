/**
 * Admin > Audit logs – search and table.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { auditLogsAPI } from '../../services/api'
import type { AuditLogDto } from '../../types/api'

export function AuditLogsPage() {
  const { t } = useLanguage()
  const [logs, setLogs] = useState<AuditLogDto[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    auditLogsAPI
      .getRecent({ limit: 50, search: search || undefined })
      .then((res) => setLogs(Array.isArray(res.data) ? (res.data as AuditLogDto[]) : []))
      .catch(() => setLogs([]))
      .finally(() => setLoading(false))
  }, [search])

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('auditLogsPage.title')}</h1>
      <div className="mt-6">
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t('ui.searchPlaceholder')}
          className="w-full max-w-md rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
        />
      </div>

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
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{log.createdAt}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">{log.action}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {log.entityType ?? t('ui.emDash')}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{t('ui.emDash')}</td>
                  <td className="max-w-xs px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    {log.oldValue != null || log.newValue != null ? (
                      <span title={`Old: ${log.oldValue ?? '—'}\nNew: ${log.newValue ?? '—'}`}>
                        {log.oldValue != null ? String(log.oldValue).slice(0, 20) : t('ui.emDash')}
                        {' → '}
                        {log.newValue != null ? String(log.newValue).slice(0, 20) : t('ui.emDash')}
                      </span>
                    ) : (
                      t('ui.emDash')
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
