import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { adminIdentityAPI, getApiErrorMessage } from '../../services/api'
import type { IdentityVerificationEntryDto } from '../../types/api'

type StatusFilter = 'ALL' | 'PENDING' | 'VERIFIED'

export function IdentityVerificationsPage() {
  const { t } = useLanguage()
  const [entries, setEntries] = useState<IdentityVerificationEntryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('PENDING')
  const [acting, setActing] = useState<string | null>(null)
  const [rejectId, setRejectId] = useState<string | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    setSuccess('')
    adminIdentityAPI
      .list(statusFilter === 'ALL' ? undefined : statusFilter)
      .then((res) => setEntries((res.data as IdentityVerificationEntryDto[]) ?? []))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [statusFilter])

  async function handleApprove(userId: string) {
    setActing(userId)
    setError('')
    try {
      await adminIdentityAPI.verify(userId, { approved: true })
      setSuccess(t('identityVerificationsPage.approveSuccess'))
      setEntries((prev) => prev.filter((e) => e.userId !== userId))
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setActing(null)
    }
  }

  async function handleReject() {
    if (!rejectId) return
    setActing(rejectId)
    setError('')
    try {
      await adminIdentityAPI.verify(rejectId, { approved: false, reason: rejectReason || undefined })
      setSuccess(t('identityVerificationsPage.rejectSuccess'))
      setEntries((prev) => prev.filter((e) => e.userId !== rejectId))
      setRejectId(null)
      setRejectReason('')
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setActing(null)
    }
  }

  const filters: StatusFilter[] = ['PENDING', 'VERIFIED', 'ALL']

  return (
    <>
      <h1 className="app-page-title">{t('identityVerificationsPage.title')}</h1>

      <div className="mt-4 flex gap-2">
        {filters.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setStatusFilter(f)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
              statusFilter === f
                ? 'bg-primary text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
            }`}
          >
            {t(`identityVerificationsPage.filter${f}`)}
          </button>
        ))}
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      )}
      {success && (
        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/60 dark:bg-emerald-900/20 dark:text-emerald-300">
          {success}
        </div>
      )}

      {loading && <p className="mt-6 text-sm text-slate-400">{t('ui.loading')}</p>}

      {!loading && entries.length === 0 && (
        <p className="mt-6 text-sm text-slate-400">{t('identityVerificationsPage.empty')}</p>
      )}

      {!loading && entries.length > 0 && (
        <div className="mt-6 table-shell">
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('identityVerificationsPage.colName')}</th>
                <th className="px-4 py-3.5">{t('identityVerificationsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('identityVerificationsPage.colDocument')}</th>
                <th className="px-4 py-3.5">{t('identityVerificationsPage.colReviewedAt')}</th>
                <th className="px-4 py-3.5">{t('identityVerificationsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr key={entry.userId}>
                  <td className="px-4 py-3 text-sm">
                    <Link
                      to={`/admin/customers/${entry.userId}`}
                      className="font-medium text-primary hover:underline"
                    >
                      {entry.firstName} {entry.lastName}
                    </Link>
                    <div className="font-mono text-xs text-slate-400">{entry.userId}</div>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <span
                      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                        entry.status === 'VERIFIED'
                          ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
                          : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
                      }`}
                    >
                      {entry.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {entry.documentUrl ? (
                      <a
                        href={entry.documentUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-primary hover:underline"
                      >
                        {t('identityVerificationsPage.viewDocument')}
                      </a>
                    ) : (
                      <span className="text-slate-400">—</span>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                    {entry.reviewedAt ? new Date(entry.reviewedAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {entry.status === 'PENDING' && (
                      <>
                        {rejectId === entry.userId ? (
                          <div className="flex flex-col gap-2">
                            <input
                              className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm dark:border-slate-700 dark:bg-slate-800"
                              placeholder={t('identityVerificationsPage.rejectReasonPlaceholder')}
                              value={rejectReason}
                              onChange={(e) => setRejectReason(e.target.value)}
                            />
                            <div className="flex gap-2">
                              <button
                                type="button"
                                onClick={() => { setRejectId(null); setRejectReason('') }}
                                className="rounded-lg border border-slate-300 px-3 py-1 text-xs dark:border-slate-600"
                              >
                                {t('ui.cancel')}
                              </button>
                              <button
                                type="button"
                                onClick={handleReject}
                                disabled={acting === entry.userId}
                                className="rounded-lg bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                              >
                                {t('identityVerificationsPage.confirmReject')}
                              </button>
                            </div>
                          </div>
                        ) : (
                          <div className="flex gap-2">
                            <button
                              type="button"
                              onClick={() => handleApprove(entry.userId)}
                              disabled={acting === entry.userId}
                              className="dashboard-btn-primary py-1 text-xs disabled:opacity-50"
                            >
                              {acting === entry.userId ? t('ui.submitting') : t('identityVerificationsPage.approve')}
                            </button>
                            <button
                              type="button"
                              onClick={() => setRejectId(entry.userId)}
                              className="rounded-lg border border-red-300 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 dark:border-red-700 dark:text-red-400"
                            >
                              {t('identityVerificationsPage.reject')}
                            </button>
                          </div>
                        )}
                      </>
                    )}
                    {entry.status === 'VERIFIED' && (
                      <span className="text-xs text-emerald-600 dark:text-emerald-400">{t('identityVerificationsPage.verified')}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
