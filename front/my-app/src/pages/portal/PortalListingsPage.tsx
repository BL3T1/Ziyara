/**
 * Provider portal: own service listings (paginated), links to create/edit.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { PageDto, ServiceDto } from '../../types/api'

function asPage(data: unknown): PageDto<ServiceDto> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<ServiceDto>).content)) {
    return data as PageDto<ServiceDto>
  }
  return null
}

function statusBadge(status?: string | null) {
  if (!status) return <span className="badge badge-neutral">—</span>
  const s = status.toLowerCase()
  if (s === 'active' || s === 'approved') return <span className="badge badge-success">{status}</span>
  if (s === 'pending') return <span className="badge badge-warning">{status}</span>
  if (s === 'inactive' || s === 'rejected') return <span className="badge badge-danger">{status}</span>
  return <span className="badge badge-neutral">{status}</span>
}

export function PortalListingsPage() {
  const { t } = useLanguage()
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<ServiceDto[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    portalAPI
      .listServices({ page, size: 20 })
      .then((res) => {
        const p = asPage(res.data)
        setRows(p?.content ?? [])
        setTotalPages(p?.totalPages ?? 0)
      })
      .catch((e) => {
        setRows([])
        setTotalPages(0)
        setError(getApiErrorMessage(e))
      })
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { load() }, [load])

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(t('portalPages.confirmDeleteListing', { name }))) return
    setDeletingId(id)
    setError(null)
    try {
      await portalAPI.deleteService(id)
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="app-page-title">{t('title.listings')}</h1>
        <Link to="/portal/listings/new" className="dashboard-btn-primary shrink-0">
          {t('portalPages.addListing')}
        </Link>
      </div>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="table-shell">
        {loading ? (
          <div className="flex flex-col gap-2 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <div key={i} className="h-10 animate-pulse rounded-lg bg-slate-100 dark:bg-white/[0.04]" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="dashboard-empty-state">
            <div className="dashboard-empty-state__icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
            </div>
            <p className="dashboard-empty-state__title">{t('portalPages.noListings')}</p>
            <p className="dashboard-empty-state__body">{t('portalPages.addListing')}</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('portalPages.colName')}</th>
                <th>{t('portalPages.colType')}</th>
                <th>{t('portalPages.colStatus')}</th>
                <th>{t('portalPages.colPrice')}</th>
                <th>{t('portalPages.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td className="text-slate-600 dark:text-slate-300">{s.type}</td>
                  <td>{statusBadge(s.status)}</td>
                  <td className="tabular-nums text-slate-600 dark:text-slate-300">
                    {s.basePrice != null ? `${s.currency ?? 'USD'} ${s.basePrice}` : '—'}
                  </td>
                  <td>
                    <div className="flex flex-wrap gap-3">
                      <Link
                        to={`/portal/listings/${s.id}`}
                        className="text-sm font-semibold text-[#1e4d6b] hover:underline dark:text-[#90caff]"
                      >
                        {t('portalPages.edit')}
                      </Link>
                      <button
                        type="button"
                        disabled={deletingId === s.id}
                        onClick={() => handleDelete(s.id, s.name)}
                        className="text-sm font-semibold text-red-600 hover:underline disabled:opacity-40 dark:text-red-400"
                      >
                        {t('portalPages.delete')}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-3">
          <button
            type="button"
            disabled={page <= 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="dashboard-btn-secondary disabled:opacity-40"
          >
            {t('portalPages.prev')}
          </button>
          <span className="text-sm text-slate-500 dark:text-slate-400">
            {t('portalPages.pageOf', { current: page + 1, total: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            className="dashboard-btn-secondary disabled:opacity-40"
          >
            {t('portalPages.next')}
          </button>
        </div>
      )}
    </>
  )
}
