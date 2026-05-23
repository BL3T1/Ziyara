/**
 * Super admin > Deleted items — search soft-deleted users & services, restore.
 */

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { adminSuperAPI, getApiErrorMessage } from '../../services/api'
import type { DeletedItemDto } from '../../types/api'
import { isUuid } from '../../utils/isUuid'

export function DeletedItemsPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isSuperAdmin = user?.role === 'super_admin'
  const [q, setQ] = useState('')
  const [customerOnly, setCustomerOnly] = useState(false)
  const [rows, setRows] = useState<DeletedItemDto[]>([])
  const [loading, setLoading] = useState(false)
  const [restoring, setRestoring] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [listSource, setListSource] = useState<'recent' | 'search'>('recent')

  useEffect(() => {
    if (!user || user.role !== 'super_admin') return
    setError(null)
    setLoading(true)
    setListSource('recent')
    adminSuperAPI
      .listRecentDeleted(50)
      .then((res) => {
        const data = res.data
        setRows(Array.isArray(data) ? (data as DeletedItemDto[]) : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, 'Failed to load recent deletions'))
        setRows([])
      })
      .finally(() => setLoading(false))
  }, [user?.id, user?.role])

  if (!user) return null

  if (!isSuperAdmin) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">
          {t('access.superAdminDeletedWithRole', { role: user.role })}
        </p>
        <button
          type="button"
          onClick={() => navigate('/dashboard')}
          className="mt-4 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          {t('access.backToDashboard')}
        </button>
      </div>
    )
  }

  const search = () => {
    const term = q.trim()
    if (!term) {
      setError(t('deletedItemsPage.enterQuery'))
      return
    }
    if (isUuid(term)) {
      setError(t('deletedItemsPage.noUuidSearch'))
      return
    }
    setError(null)
    setInfo(null)
    setLoading(true)
    setListSource('search')
    adminSuperAPI
      .searchDeleted(term)
      .then((res) => {
        const data = res.data
        setRows(Array.isArray(data) ? (data as DeletedItemDto[]) : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, 'Search failed'))
        setRows([])
      })
      .finally(() => setLoading(false))
  }

  const permanentDelete = (row: DeletedItemDto) => {
    if (!window.confirm(`Permanently delete ${row.entityType} "${row.label}"? This cannot be undone.`)) return
    const key = `${row.entityType}:${row.id}`
    setDeleting(key)
    setError(null)
    setInfo(null)
    adminSuperAPI
      .permanentDelete({ entityType: row.entityType, id: row.id })
      .then(() => {
        setInfo(`Permanently deleted ${row.entityType}: ${row.label ?? row.id}`)
        setRows((prev) => prev.filter((r) => r.id !== row.id))
      })
      .catch((e) => setError(getApiErrorMessage(e, 'Permanent delete failed')))
      .finally(() => setDeleting(null))
  }

  const restore = (row: DeletedItemDto) => {
    const key = `${row.entityType}:${row.id}`
    setRestoring(key)
    setError(null)
    setInfo(null)
    adminSuperAPI
      .restoreDeleted({ entityType: row.entityType, id: row.id })
      .then(() => {
        setInfo(t('deletedItemsPage.restored', { entityType: row.entityType, label: row.label ?? '' }))
        setRows((prev) => prev.filter((r) => r.id !== row.id))
      })
      .catch((e) => setError(getApiErrorMessage(e, 'Restore failed')))
      .finally(() => setRestoring(null))
  }

  const dash = t('ui.emDash')
  const emptyMessage =
    listSource === 'search' ? t('deletedItemsPage.noSearchResults') : t('deletedItemsPage.noRecent')
  const displayRows = customerOnly ? rows.filter((r) => r.entityType?.toUpperCase() === 'USER') : rows

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('deletedItemsPage.title')}</h1>
      <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400">{t('deletedItemsPage.intro')}</p>
      <p className="mt-1 max-w-3xl text-sm text-slate-600 dark:text-slate-400">{t('deletedItemsPage.recentAutoLoad')}</p>
      <div className="mt-6 space-y-3">
        <div className="flex flex-wrap gap-2">
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && search()}
            placeholder={t('deletedItemsPage.placeholder')}
            className="min-w-[16rem] flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <button
            type="button"
            onClick={search}
            disabled={loading}
            className="dashboard-btn-primary"
          >
            {loading && listSource === 'search'
              ? t('ui.searching')
              : loading
                ? t('ui.loading')
                : t('ui.searchAction')}
          </button>
        </div>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
          <input
            type="checkbox"
            checked={customerOnly}
            onChange={(e) => setCustomerOnly(e.target.checked)}
            className="rounded border-slate-300"
          />
          Show customers only
        </label>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {info && (
        <div className="mt-4 rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-800 dark:border-green-800 dark:bg-green-900/20 dark:text-green-200">
          {info}
        </div>
      )}

      <div className="mt-6 table-shell">
        {displayRows.length === 0 && !loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{emptyMessage}</div>
        ) : loading && displayRows.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('deletedItemsPage.colType')}</th>
                <th className="px-4 py-3.5">{t('deletedItemsPage.colLabel')}</th>
                <th className="px-4 py-3.5">{t('deletedItemsPage.colDeleted')}</th>
                <th className="px-4 py-3.5">{t('deletedItemsPage.colAction')}</th>
              </tr>
            </thead>
            <tbody>
              {displayRows.map((r) => (
                <tr key={`${r.entityType}-${r.id}`}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-800 dark:text-slate-200">{r.entityType}</td>
                  <td className="px-4 py-3 text-sm text-slate-700 dark:text-slate-300">
                    <div>{r.label}</div>
                    {r.detail && <div className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{r.detail}</div>}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">
                    {r.deletedAt ? new Date(r.deletedAt).toLocaleString() : dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => restore(r)}
                        disabled={restoring !== null || deleting !== null}
                        className="rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-50"
                      >
                        {restoring === `${r.entityType}:${r.id}` ? t('ui.restoring') : t('ui.restore')}
                      </button>
                      <button
                        type="button"
                        onClick={() => permanentDelete(r)}
                        disabled={restoring !== null || deleting !== null}
                        className="rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-50"
                      >
                        {deleting === `${r.entityType}:${r.id}` ? 'Deleting…' : 'Delete permanently'}
                      </button>
                    </div>
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
