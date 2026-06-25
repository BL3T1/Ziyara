/**
 * Admin: Media Approvals + ID Verifications — tabbed approval queue.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { adminIdentityAPI, adminMediaAPI, getApiErrorMessage } from '../../services/api'
import { statusLabel } from '../../i18n/enumLabels'
import type { IdentityVerificationEntryDto, ProviderMediaSubmissionDto } from '../../types/api'
import { usePermission } from '../../hooks/usePermission'
import { Link } from 'react-router-dom'

// ─── helpers ────────────────────────────────────────────────────────────────

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

// ─── Media tab ──────────────────────────────────────────────────────────────

function MediaTab() {
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

// ─── ID Verifications tab ────────────────────────────────────────────────────

type StatusFilter = 'ALL' | 'PENDING' | 'VERIFIED'

function IdentityTab() {
  const { t } = useLanguage()
  const canWrite = usePermission('customers:write')
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
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {success && (
        <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300">
          {success}
        </div>
      )}

      {loading && <p className="mt-6 text-sm text-slate-400">{t('ui.loading')}</p>}
      {!loading && entries.length === 0 && (
        <p className="mt-6 text-sm text-slate-400">{t('identityVerificationsPage.empty')}</p>
      )}

      {!loading && entries.length > 0 && (
        <div className="mt-4 table-shell">
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
                      <a href={entry.documentUrl} target="_blank" rel="noreferrer" className="text-primary hover:underline">
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
                    {canWrite && entry.status === 'PENDING' && (
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
                              className="text-sm font-semibold text-green-600 hover:underline disabled:opacity-40 dark:text-green-400"
                            >
                              {acting === entry.userId ? t('ui.submitting') : t('identityVerificationsPage.approve')}
                            </button>
                            <button
                              type="button"
                              onClick={() => setRejectId(entry.userId)}
                              className="text-sm font-semibold text-red-600 hover:underline dark:text-red-400"
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

// ─── Page shell ─────────────────────────────────────────────────────────────

type Tab = 'media' | 'identity'

export function MediaSubmissionsPage() {
  const { t } = useLanguage()
  const [activeTab, setActiveTab] = useState<Tab>('media')

  const tabs: { id: Tab; label: string }[] = [
    { id: 'media', label: t('mediaSubmissionsPage.title') },
    { id: 'identity', label: t('identityVerificationsPage.title') },
  ]

  return (
    <>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('mediaSubmissionsPage.pageTitle')}</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('mediaSubmissionsPage.intro')}</p>
        </div>
      </div>

      <div className="mt-5 border-b border-slate-200 dark:border-slate-700">
        <nav className="-mb-px flex gap-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`whitespace-nowrap border-b-2 pb-3 text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? 'border-primary text-primary'
                  : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      <div className="mt-4">
        {activeTab === 'media' ? <MediaTab /> : <IdentityTab />}
      </div>
    </>
  )
}
