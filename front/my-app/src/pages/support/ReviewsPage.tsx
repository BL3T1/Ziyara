/**
 * Support > Reviews — admin list (GET /reviews) and moderate (POST .../moderate).
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, reviewsAPI } from '../../services/api'
import type { PageDto, ReviewAdminRowDto } from '../../types/api'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { statusLabel } from '../../i18n/enumLabels'
import { usePermission } from '../../hooks/usePermission'

const STATUS_FILTER_OPTIONS: { value: string; labelKey: string }[] = [
  { value: '', labelKey: 'reviewsPage.allStatuses' },
  { value: 'PENDING', labelKey: 'reviewsPage.stPending' },
  { value: 'PUBLISHED', labelKey: 'reviewsPage.stPublished' },
  { value: 'REJECTED', labelKey: 'reviewsPage.stRejected' },
  { value: 'HIDDEN', labelKey: 'reviewsPage.stHidden' },
  { value: 'REPORTED', labelKey: 'reviewsPage.stReported' },
]

const MODERATE_ACTIONS: { status: string; labelKey: string; variant: 'danger' | 'default' }[] = [
  { status: 'PUBLISHED', labelKey: 'reviewsPage.publish', variant: 'default' },
  { status: 'REJECTED', labelKey: 'reviewsPage.reject', variant: 'danger' },
  { status: 'HIDDEN', labelKey: 'reviewsPage.hide', variant: 'danger' },
]

export function ReviewsPage() {
  const { t } = useLanguage()
  const canModerate = usePermission('reviews:moderate')
  const [rows, setRows] = useState<ReviewAdminRowDto[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [pending, setPending] = useState<{ id: string; status: string; label: string; variant: 'danger' | 'default' } | null>(null)
  const size = 15

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    const params: { page: number; size: number; status?: string } = { page, size }
    if (statusFilter) params.status = statusFilter
    reviewsAPI
      .listAdmin(params)
      .then((res) => {
        const data = res.data as PageDto<ReviewAdminRowDto>
        setRows(Array.isArray(data?.content) ? data.content : [])
        setTotalPages(typeof data?.totalPages === 'number' ? data.totalPages : 0)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setRows([])
      })
      .finally(() => setLoading(false))
  }, [page, size, statusFilter])

  useEffect(() => {
    load()
  }, [load])

  const moderate = async (id: string, status: string) => {
    setBusyId(id)
    setError(null)
    try {
      await reviewsAPI.moderate(id, { status })
      await load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('reviewsPage.title')}</h1>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <label className="text-sm text-slate-600 dark:text-slate-400">{t('reviewsPage.filterStatus')}</label>
          <select
            value={statusFilter}
            onChange={(e) => {
              setPage(0)
              setStatusFilter(e.target.value)
            }}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {STATUS_FILTER_OPTIONS.map((o) => (
              <option key={o.value || 'all'} value={o.value}>
                {t(o.labelKey)}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : rows.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('reviewsPage.empty')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('reviewsPage.colRating')}</th>
                <th className="px-4 py-3.5">{t('reviewsPage.colComment')}</th>
                <th className="px-4 py-3.5">{t('reviewsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('reviewsPage.colService')}</th>
                <th className="px-4 py-3.5">{t('reviewsPage.colCreated')}</th>
                <th className="px-4 py-3.5 text-end">{t('reviewsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => {
                const isExpanded = expandedId === r.id
                return (
                  <tr key={r.id}>
                    <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">
                      {r.rating ?? '—'}
                    </td>
                    <td className="max-w-xs px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {r.comment ? (
                        <div>
                          <p className={isExpanded ? '' : 'line-clamp-2'}>{r.comment}</p>
                          {r.comment.length > 80 && (
                            <button
                              type="button"
                              onClick={() => setExpandedId(isExpanded ? null : r.id)}
                              className="mt-0.5 text-xs text-primary hover:underline dark:text-[#60b4f8]"
                            >
                              {isExpanded ? t('reviewsPage.collapse') : t('reviewsPage.expand')}
                            </button>
                          )}
                        </div>
                      ) : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {statusLabel(t, r.status)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {r.serviceName?.trim() || t('ui.emDash')}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                      {r.createdAt ? String(r.createdAt).replace('T', ' ').slice(0, 16) : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm">
                      {canModerate && (
                        <div className="flex flex-wrap justify-end gap-1">
                          {MODERATE_ACTIONS.map((a) => (
                            <button
                              key={a.status}
                              type="button"
                              disabled={busyId === r.id}
                              onClick={() => setPending({ id: r.id, status: a.status, label: t(a.labelKey), variant: a.variant })}
                              className="rounded-lg border border-slate-200 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-700"
                            >
                              {t(a.labelKey)}
                            </button>
                          ))}
                        </div>
                      )}
                    </td>
                  </tr>
                )
              })}
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
            {t('reviewsPage.prev')}
          </button>
          <span className="text-sm text-slate-600 dark:text-slate-400">
            {t('reviewsPage.pageOf', { n: page + 1, total: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-slate-600"
          >
            {t('reviewsPage.next')}
          </button>
        </div>
      )}

      <ConfirmDialog
        open={!!pending}
        onClose={() => setPending(null)}
        title={pending?.label ?? ''}
        description={t('reviewsPage.moderateConfirmDesc')}
        confirmLabel={pending?.label}
        variant={pending?.variant ?? 'default'}
        onConfirm={() => moderate(pending!.id, pending!.status)}
      />
    </>
  )
}
