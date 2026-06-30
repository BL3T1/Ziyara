/**
 * Super admin / deleted_items:read — search soft-deleted users & services, restore.
 * Three tabs: Company (staff), Providers (services), App Users (customers).
 */

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { adminSuperAPI, getApiErrorMessage } from '../../services/api'
import type { DeletedItemDto } from '../../types/api'
import { isUuid } from '../../utils/isUuid'
import { ConfirmDialog } from '../../components/ConfirmDialog'

type TabId = 'all' | 'company' | 'providers' | 'app_users'

function classifyRow(r: DeletedItemDto): Exclude<TabId, 'all'> {
  if (r.entityType === 'SERVICE' || r.entityType === 'PROVIDER') return 'providers'
  if (r.detail?.includes('role=CUSTOMER')) return 'app_users'
  return 'company'
}

export function DeletedItemsPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { user } = useAuth()
  const canRestore = usePermission('deleted_items:company:restore')
  const canAccess = usePermission('deleted_items:company:read')

  const [tab, setTab] = useState<TabId>('all')
  const [q, setQ] = useState('')
  const [rows, setRows] = useState<DeletedItemDto[]>([])
  const [loading, setLoading] = useState(false)
  const [restoring, setRestoring] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<DeletedItemDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [listSource, setListSource] = useState<'recent' | 'search'>('recent')

  useEffect(() => {
    if (!canAccess) return
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
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id])

  if (!user) return null

  if (!canAccess) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">
          {t('access.needPermission', { permission: 'deleted_items:company:read' })}
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
    if (!term) { setError(t('deletedItemsPage.enterQuery')); return }
    if (isUuid(term)) { setError(t('deletedItemsPage.noUuidSearch')); return }
    setError(null); setInfo(null); setLoading(true); setListSource('search')
    adminSuperAPI
      .searchDeleted(term)
      .then((res) => setRows(Array.isArray(res.data) ? (res.data as DeletedItemDto[]) : []))
      .catch((e) => { setError(getApiErrorMessage(e, 'Search failed')); setRows([]) })
      .finally(() => setLoading(false))
  }

  const permanentDelete = (row: DeletedItemDto) => {
    setDeleteTarget(row)
  }

  const doDelete = async () => {
    if (!deleteTarget) return
    const row = deleteTarget
    const key = `${row.entityType}:${row.id}`
    setDeleting(key); setError(null); setInfo(null)
    try {
      await adminSuperAPI.permanentDelete({ entityType: row.entityType, id: row.id })
      setInfo(`Permanently deleted ${row.entityType}: ${row.label ?? row.id}`)
      setRows((p) => p.filter((r) => r.id !== row.id))
    } catch (e) {
      setError(getApiErrorMessage(e, 'Permanent delete failed'))
    } finally {
      setDeleting(null)
      setDeleteTarget(null)
    }
  }

  const restore = (row: DeletedItemDto) => {
    const key = `${row.entityType}:${row.id}`
    setRestoring(key); setError(null); setInfo(null)
    adminSuperAPI
      .restoreDeleted({ entityType: row.entityType, id: row.id })
      .then(() => { setInfo(t('deletedItemsPage.restored', { entityType: row.entityType, label: row.label ?? '' })); setRows((p) => p.filter((r) => r.id !== row.id)) })
      .catch((e) => setError(getApiErrorMessage(e, 'Restore failed')))
      .finally(() => setRestoring(null))
  }

  const dash = t('ui.emDash')
  const emptyMessage = listSource === 'search' ? t('deletedItemsPage.noSearchResults') : t('deletedItemsPage.noRecent')

  const filteredRows = tab === 'all' ? rows : rows.filter((r) => classifyRow(r) === tab)

  const TABS: { id: TabId; label: string }[] = [
    { id: 'all',       label: t('deletedItemsPage.tabAll') },
    { id: 'company',   label: t('deletedItemsPage.tabCompany') },
    { id: 'providers', label: t('deletedItemsPage.tabProviders') },
    { id: 'app_users', label: t('deletedItemsPage.tabAppUsers') },
  ]

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('deletedItemsPage.title')}</h1>
      <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400">{t('deletedItemsPage.intro')}</p>
      <p className="mt-1 max-w-3xl text-sm text-slate-600 dark:text-slate-400">{t('deletedItemsPage.recentAutoLoad')}</p>

      {/* Search bar */}
      <div className="mt-6 flex flex-wrap gap-2">
        <input
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && search()}
          placeholder={t('deletedItemsPage.placeholder')}
          className="min-w-[16rem] flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
        />
        <button type="button" onClick={search} disabled={loading} className="dashboard-btn-primary">
          {loading && listSource === 'search' ? t('ui.searching') : loading ? t('ui.loading') : t('ui.searchAction')}
        </button>
      </div>

      {/* Tab bar */}
      <div className="mt-5 flex gap-1 border-b border-slate-200 dark:border-slate-700">
        {TABS.map((tb) => {
          const count = tb.id === 'all' ? rows.length : rows.filter((r) => classifyRow(r) === tb.id).length
          return (
            <button
              key={tb.id}
              type="button"
              onClick={() => setTab(tb.id)}
              className={`rounded-t-lg px-4 py-2 text-sm font-medium transition-colors ${
                tab === tb.id
                  ? 'border-b-2 border-primary text-primary dark:text-secondary'
                  : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {tb.label}
              {count > 0 && (
                <span className="ms-1.5 rounded-full bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-700 dark:text-slate-300">
                  {count}
                </span>
              )}
            </button>
          )
        })}
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

      <div className="mt-4 table-shell">
        {filteredRows.length === 0 && !loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{emptyMessage}</div>
        ) : loading && filteredRows.length === 0 ? (
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
              {filteredRows.map((r) => (
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
                      {canRestore && (
                        <button
                          type="button"
                          onClick={() => restore(r)}
                          disabled={restoring !== null || deleting !== null}
                          className="rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-50"
                        >
                          {restoring === `${r.entityType}:${r.id}` ? t('ui.restoring') : t('ui.restore')}
                        </button>
                      )}
                      {canRestore && (
                        <button
                          type="button"
                          onClick={() => permanentDelete(r)}
                          disabled={restoring !== null || deleting !== null}
                          className="rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-50"
                        >
                          {deleting === `${r.entityType}:${r.id}` ? 'Deleting…' : 'Delete permanently'}
                        </button>
                      )}
                      {!canRestore && (
                        <span className="text-xs text-slate-400">{t('ui.viewOnly')}</span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title={`Permanently delete ${deleteTarget?.entityType ?? ''} "${deleteTarget?.label ?? ''}"?`}
        description={t('deletedItemsPage.permanentDeleteWarning')}
        confirmLabel="Delete permanently"
        variant="danger"
        onConfirm={doDelete}
      />
    </>
  )
}
