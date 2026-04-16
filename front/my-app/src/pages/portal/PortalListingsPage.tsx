/**
 * Provider portal: own service listings (paginated), links to create/edit.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { PageDto, ServiceDto } from '../../types/api'
import { Card } from '../../components/Card'

function asPage(data: unknown): PageDto<ServiceDto> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<ServiceDto>).content)) {
    return data as PageDto<ServiceDto>
  }
  return null
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

  useEffect(() => {
    load()
  }, [load])

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
        <div>
          <h1 className="app-page-title">{t('title.listings')}</h1>
        </div>
        <Link
          to="/portal/listings/new"
          className="dashboard-btn-primary inline-flex shrink-0"
        >
          {t('portalPages.addListing')}
        </Link>
      </div>

      {error && (
        <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </p>
      )}

      <Card className="mt-6 overflow-x-auto p-0">
        {loading ? (
          <p className="p-6 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : rows.length === 0 ? (
          <p className="p-6 text-slate-600 dark:text-slate-300">{t('portalPages.noListings')}</p>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50/80 dark:border-slate-700 dark:bg-slate-900/50">
              <tr>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('portalPages.colName')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('portalPages.colType')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('portalPages.colStatus')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('portalPages.colPrice')}</th>
                <th className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200">{t('portalPages.colActions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
              {rows.map((s) => (
                <tr key={s.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40">
                  <td className="px-4 py-3 font-medium text-slate-900 dark:text-slate-100">{s.name}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{s.type}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{s.status ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                    {s.basePrice != null ? `${s.currency ?? 'USD'} ${s.basePrice}` : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Link
                        to={`/portal/listings/${s.id}`}
                        className="text-sm font-medium text-primary hover:underline"
                      >
                        {t('portalPages.edit')}
                      </Link>
                      <button
                        type="button"
                        disabled={deletingId === s.id}
                        onClick={() => handleDelete(s.id, s.name)}
                        className="text-sm font-medium text-red-600 hover:underline disabled:opacity-50 dark:text-red-400"
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
      </Card>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center gap-3">
          <button
            type="button"
            disabled={page <= 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-slate-600"
          >
            {t('portalPages.prev')}
          </button>
          <span className="text-sm text-slate-600 dark:text-slate-300">
            {t('portalPages.pageOf', { current: page + 1, total: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-slate-600"
          >
            {t('portalPages.next')}
          </button>
        </div>
      )}
    </>
  )
}
