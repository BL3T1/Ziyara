/**
 * Admin: review and approve/reject provider media submissions.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { adminMediaAPI, getApiErrorMessage } from '../../services/api'
import { statusLabel } from '../../i18n/enumLabels'
import type { ProviderMediaSubmissionDto } from '../../types/api'
import { usePermission } from '../../hooks/usePermission'

function asSubs(data: unknown): ProviderMediaSubmissionDto[] {
  if (Array.isArray(data)) return data as ProviderMediaSubmissionDto[]
  return []
}

function StatusBadge({ status, t }: { status: string; t: (k: string) => string }) {
  const label = statusLabel(t, status)
  if (status === 'APPROVED') return <span className="badge badge-success">{label}</span>
  if (status === 'REJECTED') return <span className="badge badge-danger">{label}</span>
  return <span className="badge badge-warning">{label}</span>
}

export function MediaSubmissionsPage() {
  const { t } = useLanguage()
  const canApprove = usePermission('media_submissions:approve')
  const [submissions, setSubmissions] = useState<ProviderMediaSubmissionDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionMsg, setActionMsg] = useState<string | null>(null)
  const [rejectingId, setRejectingId] = useState<string | null>(null)
  const [rejectNote, setRejectNote] = useState('')
  const [processing, setProcessing] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminMediaAPI
      .list()
      .then((res) => setSubmissions(asSubs(res.data)))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const handleApprove = async (id: string) => {
    setProcessing(id)
    setActionMsg(null)
    setError(null)
    try {
      await adminMediaAPI.approve(id)
      setActionMsg(t('mediaSubmissionsPage.approveSuccess'))
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setProcessing(null)
    }
  }

  const handleRejectSubmit = async (id: string) => {
    setProcessing(id)
    setActionMsg(null)
    setError(null)
    try {
      await adminMediaAPI.reject(id, rejectNote.trim() || undefined)
      setActionMsg(t('mediaSubmissionsPage.rejectSuccess'))
      setRejectingId(null)
      setRejectNote('')
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setProcessing(null)
    }
  }

  return (
    <>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('mediaSubmissionsPage.title')}</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('mediaSubmissionsPage.intro')}</p>
        </div>
      </div>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {actionMsg && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300">
          {actionMsg}
        </div>
      )}

      <div className="mt-4 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('mediaSubmissionsPage.loading')}</div>
        ) : submissions.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('mediaSubmissionsPage.noPending')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('mediaSubmissionsPage.colPreview')}</th>
                <th>{t('mediaSubmissionsPage.colProvider')}</th>
                <th>{t('mediaSubmissionsPage.colType')}</th>
                <th>{t('mediaSubmissionsPage.colStatus')}</th>
                <th>{t('mediaSubmissionsPage.colDate')}</th>
                <th>{t('mediaSubmissionsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {submissions.map((s) => (
                <tr key={s.id}>
                  <td className="whitespace-nowrap px-4 py-3">
                    {s.fileUrl ? (
                      <a href={s.fileUrl} target="_blank" rel="noopener noreferrer">
                        <img
                          src={s.fileUrl}
                          alt={s.altText ?? ''}
                          className="h-14 w-20 rounded-lg object-cover border border-slate-200 dark:border-white/[0.06] hover:opacity-80 transition-opacity"
                        />
                      </a>
                    ) : '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-700 dark:text-slate-200 font-mono text-xs">
                    {s.providerId?.slice(0, 8)}…
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm capitalize text-slate-600 dark:text-slate-300">
                    {s.imageType?.toLowerCase()}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <StatusBadge status={s.status} t={t} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500 dark:text-slate-400">
                    {s.submittedAt ? new Date(s.submittedAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {canApprove && s.status === 'PENDING' && (
                      rejectingId === s.id ? (
                        <div className="flex flex-col gap-2">
                          <input
                            value={rejectNote}
                            onChange={(e) => setRejectNote(e.target.value)}
                            placeholder={t('mediaSubmissionsPage.rejectNote')}
                            className="rounded border border-slate-300 px-2 py-1 text-xs dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                          />
                          <div className="flex gap-2">
                            <button
                              type="button"
                              disabled={processing === s.id}
                              onClick={() => handleRejectSubmit(s.id)}
                              className="text-xs font-semibold text-red-600 hover:underline disabled:opacity-40 dark:text-red-400"
                            >
                              {t('mediaSubmissionsPage.reject')}
                            </button>
                            <button
                              type="button"
                              onClick={() => { setRejectingId(null); setRejectNote('') }}
                              className="text-xs text-slate-500 hover:underline"
                            >
                              {t('ui.cancel')}
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex flex-wrap gap-3">
                          <button
                            type="button"
                            disabled={processing === s.id}
                            onClick={() => handleApprove(s.id)}
                            className="text-sm font-semibold text-green-600 hover:underline disabled:opacity-40 dark:text-green-400"
                          >
                            {t('mediaSubmissionsPage.approve')}
                          </button>
                          <button
                            type="button"
                            onClick={() => setRejectingId(s.id)}
                            className="text-sm font-semibold text-red-600 hover:underline dark:text-red-400"
                          >
                            {t('mediaSubmissionsPage.reject')}
                          </button>
                        </div>
                      )
                    )}
                    {s.status !== 'PENDING' && (
                      <span className="text-xs text-slate-400 dark:text-slate-500">
                        {s.reviewNote ?? '—'}
                      </span>
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
