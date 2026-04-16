/**
 * Sales workspace: submit new partners (pending until Super Admin / CEO approves).
 */

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CreateProviderModal } from '../../components/CreateProviderModal'
import { useLanguage } from '../../context/LanguageContext'
import { providersAPI } from '../../services/api'
import type { PageDto, ServiceProviderDto } from '../../types/api'

function asPage<T>(data: unknown): PageDto<T> | null {
  if (data && typeof data === 'object' && Array.isArray((data as PageDto<T>).content)) {
    return data as PageDto<T>
  }
  return null
}

const PAGE_SIZE = 20

export function SalesProvidersPage() {
  const { t } = useLanguage()
  const [modalOpen, setModalOpen] = useState(false)
  const [rows, setRows] = useState<ServiceProviderDto[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [filter, setFilter] = useState<'PENDING_APPROVAL' | null>('PENDING_APPROVAL')

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
        setRows(p?.content ?? [])
        setTotalPages(p?.totalPages ?? 0)
      })
      .catch(() => {
        setRows([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [filter, page])

  return (
    <>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">
            {t('salesProvidersPage.title')}
          </h1>
        </div>
        <button type="button" onClick={() => setModalOpen(true)} className="dashboard-btn-primary shrink-0">
          {t('salesProvidersPage.addPartner')}
        </button>
      </div>

      <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
        {t('salesProvidersPage.hintQueue')}{' '}
        <Link to="/management/providers" className="text-primary hover:underline">
          {t('salesProvidersPage.linkManagement')}
        </Link>
      </p>

      <div className="mt-6 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => {
            setFilter('PENDING_APPROVAL')
            setPage(0)
          }}
          className={
            filter === 'PENDING_APPROVAL'
              ? 'dashboard-pill dashboard-pill--active px-3 py-1.5 text-sm'
              : 'dashboard-pill px-3 py-1.5 text-sm'
          }
        >
          {t('salesProvidersPage.tabPending')}
        </button>
        <button
          type="button"
          onClick={() => {
            setFilter(null)
            setPage(0)
          }}
          className={filter === null ? 'dashboard-pill dashboard-pill--active px-3 py-1.5 text-sm' : 'dashboard-pill px-3 py-1.5 text-sm'}
        >
          {t('ui.all')}
        </button>
      </div>

      <div className="mt-4 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500">{t('ui.loading')}</div>
        ) : rows.length === 0 ? (
          <div className="p-8 text-center text-slate-500">{t('salesProvidersPage.empty')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('providersPage.colName')}</th>
                <th className="px-4 py-3.5">{t('providersPage.colType')}</th>
                <th className="px-4 py-3.5">{t('providersPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('providersPage.colEmail')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">{p.name}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.type ?? t('ui.emDash')}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.status}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{p.email ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex justify-center gap-2">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((x) => Math.max(0, x - 1))}
            className="rounded border px-3 py-1 text-sm disabled:opacity-50"
          >
            {t('ui.previous')}
          </button>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((x) => x + 1)}
            className="rounded border px-3 py-1 text-sm disabled:opacity-50"
          >
            {t('ui.next')}
          </button>
        </div>
      )}

      <CreateProviderModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        variant="sales"
        onCreated={() => load()}
      />
    </>
  )
}
