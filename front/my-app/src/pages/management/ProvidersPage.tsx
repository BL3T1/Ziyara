/**
 * Management > Providers – group-first list view.
 * Step 1: Status or vertical cards. Step 2: Table with commission override, Approve, Suspend.
 */

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { providersAPI } from '../../services/api'
import { getApiErrorMessage } from '../../services/api'
import type { PageDto, ServiceProviderDto } from '../../types/api'
import { BulkActionBar } from '../../components/BulkActionBar'
import {
  canApproveRejectProvider,
  canCreateProvider,
  canViewProviderCommission,
} from '../../types/auth'

function asPage<T>(data: unknown): PageDto<T> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<T>).content)) {
    return data as PageDto<T>
  }
  return null
}

const PAGE_SIZE = 20

const STATUS_FILTERS = [
  { id: 'ACTIVE', labelKey: 'providersPage.statusActive' },
  { id: 'PENDING_APPROVAL', labelKey: 'providersPage.statusPendingApproval' },
  { id: 'SUSPENDED', labelKey: 'providersPage.statusSuspended' },
  { id: 'INACTIVE', labelKey: 'providersPage.statusInactive' },
] as const

export function ProvidersPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const showCommission = user?.role ? canViewProviderCommission(user.role) : false
  const showCreate = user?.role ? canCreateProvider(user.role) : false
  const showApproveReject = user?.role ? canApproveRejectProvider(user.role) : false
  const [providers, setProviders] = useState<ServiceProviderDto[]>([])
  const [filter, setFilter] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [commissionRate, setCommissionRate] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const load = () => {
    setLoading(true)
    providersAPI
      .list({
        page,
        size: PAGE_SIZE,
        status: filter ?? undefined,
      })
      .then((res) => {
        const p = asPage<ServiceProviderDto>(res.data)
        setProviders(p?.content ?? [])
        setTotalPages(p?.totalPages ?? 0)
      })
      .catch(() => {
        setProviders([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [filter, page])

  const handleApprove = async (id: string) => {
    setError(null)
    try {
      await providersAPI.approve(id)
      setProviders((prev) =>
        prev.map((p) => (p.id === id ? { ...p, status: 'ACTIVE' } : p))
      )
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleReject = async (id: string) => {
    setError(null)
    try {
      await providersAPI.reject(id)
      setProviders((prev) =>
        prev.map((p) => (p.id === id ? { ...p, status: 'INACTIVE' } : p))
      )
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleSuspend = async (id: string) => {
    setError(null)
    try {
      await providersAPI.suspend(id)
      setProviders((prev) =>
        prev.map((p) => (p.id === id ? { ...p, status: 'SUSPENDED' } : p))
      )
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const isAllSelected = providers.length > 0 && providers.every((p) => selectedIds.has(p.id))
  const toggleAll = () =>
    setSelectedIds(isAllSelected ? new Set() : new Set(providers.map((p) => p.id)))
  const toggleOne = (id: string) =>
    setSelectedIds((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })

  const bulkApprove = async () => {
    const ids = providers
      .filter((p) => selectedIds.has(p.id) && (p.status ?? '').toUpperCase() === 'PENDING_APPROVAL')
      .map((p) => p.id)
    if (!ids.length) return
    setError(null)
    await Promise.allSettled(ids.map((id) => providersAPI.approve(id)))
    setProviders((prev) => prev.map((p) => (ids.includes(p.id) ? { ...p, status: 'ACTIVE' } : p)))
    setSelectedIds(new Set())
  }

  const bulkSuspend = async () => {
    const ids = providers
      .filter((p) => selectedIds.has(p.id) && (p.status ?? '').toUpperCase() === 'ACTIVE')
      .map((p) => p.id)
    if (!ids.length) return
    setError(null)
    await Promise.allSettled(ids.map((id) => providersAPI.suspend(id)))
    setProviders((prev) => prev.map((p) => (ids.includes(p.id) ? { ...p, status: 'SUSPENDED' } : p)))
    setSelectedIds(new Set())
  }

  const handleSaveCommission = async () => {
    if (!editingId || commissionRate === '') return
    const rate = parseFloat(commissionRate)
    if (Number.isNaN(rate) || rate < 0 || rate > 100) return
    try {
      await providersAPI.updateCommission(editingId, { commissionRate: rate })
      setProviders((prev) =>
        prev.map((p) => (p.id === editingId ? { ...p, commissionRate: rate } : p))
      )
      setEditingId(null)
      setCommissionRate('')
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  return (
    <>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('providersPage.title')}</h1>
        </div>
        {showCreate && (
          <Link to="/management/providers/new" className="dashboard-btn-primary inline-flex shrink-0 items-center justify-center no-underline">
            {t('providersPage.createProvider')}
          </Link>
        )}
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <BulkActionBar
        selectedCount={selectedIds.size}
        onClearSelection={() => setSelectedIds(new Set())}
        actions={[
          {
            label: t('providersPage.bulkApprove'),
            onClick: bulkApprove,
            disabled: ![...selectedIds].some(
              (id) => (providers.find((p) => p.id === id)?.status ?? '').toUpperCase() === 'PENDING_APPROVAL',
            ),
          },
          {
            label: t('providersPage.bulkSuspend'),
            onClick: bulkSuspend,
            variant: 'danger',
            disabled: ![...selectedIds].some(
              (id) => (providers.find((p) => p.id === id)?.status ?? '').toUpperCase() === 'ACTIVE',
            ),
          },
        ]}
      />

      <div className="mt-6 flex flex-wrap gap-4">
        <button
          type="button"
          onClick={() => {
            setFilter(null)
            setPage(0)
          }}
          className={filter === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
        >
          {t('ui.all')}
        </button>
        {STATUS_FILTERS.map((card) => (
          <button
            key={card.id}
            type="button"
            onClick={() => {
              setFilter(card.id)
              setPage(0)
            }}
            className={filter === card.id ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {t(card.labelKey)}
          </button>
        ))}
      </div>

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : providers.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('providersPage.noProviders')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="w-10 px-4 py-3.5">
                  <input
                    type="checkbox"
                    checked={isAllSelected}
                    onChange={toggleAll}
                    className="h-4 w-4 cursor-pointer rounded border-slate-300"
                  />
                </th>
                <th className="px-4 py-3.5">{t('providersPage.colName')}</th>
                <th className="px-4 py-3.5">{t('providersPage.colType')}</th>
                <th className="px-4 py-3.5">{t('providersPage.colStatus')}</th>
                {showCommission && <th className="px-4 py-3.5">{t('providersPage.colCommission')}</th>}
                <th className="px-4 py-3.5">{t('providersPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {providers.map((p) => (
                <tr key={p.id} className={selectedIds.has(p.id) ? 'bg-primary/5 dark:bg-primary/10' : ''}>
                  <td className="whitespace-nowrap px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedIds.has(p.id)}
                      onChange={() => toggleOne(p.id)}
                      className="h-4 w-4 cursor-pointer rounded border-slate-300"
                    />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                    <Link to={`/management/providers/${p.id}`} className="text-primary hover:underline">
                      {p.name}
                    </Link>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.type ?? t('ui.emDash')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{String(p.status ?? t('ui.emDash'))}</td>
                  {showCommission && (
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {editingId === p.id ? (
                        <div className="flex items-center gap-2">
                          <input
                            type="number"
                            min="0"
                            max="100"
                            step="0.5"
                            value={commissionRate}
                            onChange={(e) => setCommissionRate(e.target.value)}
                            className="w-20 rounded border border-slate-300 px-2 py-1 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                          />
                          <button type="button" onClick={handleSaveCommission} className="text-primary text-sm hover:underline">{t('providersPage.save')}</button>
                          <button type="button" onClick={() => { setEditingId(null); setCommissionRate(''); }} className="text-slate-500 text-sm hover:underline">{t('ui.cancel')}</button>
                        </div>
                      ) : (
                        <span>{p.commissionRate != null ? `${Number(p.commissionRate)}%` : t('ui.emDash')}</span>
                      )}
                    </td>
                  )}
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    {editingId !== p.id && (
                      <>
                        <Link
                          to={`/management/providers/${p.id}`}
                          className="text-slate-600 hover:underline dark:text-slate-300"
                        >
                          {t('providersPage.viewEdit')}
                        </Link>
                        {showCommission && (
                          <>
                            <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                            <button
                              type="button"
                              onClick={() => { setEditingId(p.id); setCommissionRate(String(p.commissionRate ?? '10')); }}
                              className="text-primary hover:underline"
                            >
                              {t('providersPage.editCommission')}
                            </button>
                          </>
                        )}
                        {(p.status ?? '').toUpperCase() === 'PENDING_APPROVAL' && showApproveReject && (
                          <>
                            <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                            <button
                              type="button"
                              onClick={() => handleApprove(p.id)}
                              className="text-green-600 hover:underline dark:text-green-400"
                            >
                              {t('providersPage.approve')}
                            </button>
                            <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                            <button
                              type="button"
                              onClick={() => handleReject(p.id)}
                              className="text-red-600 hover:underline dark:text-red-400"
                            >
                              {t('providersPage.reject')}
                            </button>
                          </>
                        )}
                        {(p.status ?? '').toUpperCase() === 'ACTIVE' && (
                          <>
                            <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                            <button
                              type="button"
                              onClick={() => handleSuspend(p.id)}
                              className="text-amber-600 hover:underline dark:text-amber-400"
                            >
                              {t('providersPage.suspend')}
                            </button>
                          </>
                        )}
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
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
