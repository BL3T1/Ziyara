/**
 * Provider portal: own service listings (paginated), links to create/edit.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { usePermission } from '../../hooks/usePermission'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import { statusLabel } from '../../i18n/enumLabels'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { StatusBadge } from '../../components/StatusBadge'
import type { PageDto, ServiceDto } from '../../types/api'

function asPage(data: unknown): PageDto<ServiceDto> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<ServiceDto>).content)) {
    return data as PageDto<ServiceDto>
  }
  return null
}

export function PortalListingsPage() {
  const { t } = useLanguage()
  useDocumentMeta({ title: `${t('title.listings')} — Ziyara` })
  const canManage = usePermission('portal:manage')
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<ServiceDto[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [pendingDelete, setPendingDelete] = useState<{ id: string; name: string } | null>(null)

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

  const handleDelete = async (id: string) => {
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
        {canManage && (
          <Link to="/portal/listings/new" className="dashboard-btn-primary shrink-0">
            {t('portalPages.addListing')}
          </Link>
        )}
      </div>

      <div role="status" aria-live="polite" aria-atomic="true">
        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
            {error}
          </div>
        )}
      </div>

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
                <th>{t('portalPages.colStatus')}</th>
                <th>{t('portalPages.colPrice')}</th>
                <th>{t('portalPages.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>
                    <StatusBadge status={s.status} label={statusLabel(t, s.status ?? '')} />
                    {s.status === 'REJECTED' && s.rejectionReason && (
                      <p className="mt-1 max-w-xs text-xs text-red-600 dark:text-red-400">
                        {s.rejectionReason}
                      </p>
                    )}
                  </td>
                  <td className="tabular-nums text-slate-600 dark:text-slate-300">
                    {s.basePrice != null ? `${s.currency ?? 'USD'} ${s.basePrice}` : '—'}
                  </td>
                  <td>
                    {canManage && (
                      <div className="flex flex-wrap gap-3">
                        <Link
                          to={`/portal/listings/${s.id}`}
                          className="text-sm font-semibold text-primary hover:underline dark:text-secondary"
                        >
                          {t('portalPages.edit')}
                        </Link>
                        <button
                          type="button"
                          disabled={deletingId === s.id}
                          onClick={() => setPendingDelete({ id: s.id, name: s.name })}
                          className="text-sm font-semibold text-red-600 hover:underline disabled:opacity-40 dark:text-red-400"
                        >
                          {t('portalPages.delete')}
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <ConfirmDialog
        open={!!pendingDelete}
        onClose={() => setPendingDelete(null)}
        title={t('portalPages.delete')}
        description={t('portalPages.confirmDeleteListing', { name: pendingDelete?.name ?? '' })}
        confirmLabel={t('portalPages.delete')}
        variant="danger"
        onConfirm={() => handleDelete(pendingDelete!.id)}
      />

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
