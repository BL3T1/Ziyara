/**
 * Support > Complaints – list by status. View detail, Assign, Resolve, Close, Escalate, Comments.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { complaintsAPI } from '../../services/api'
import { getApiErrorMessage } from '../../services/api'
import type { ComplaintDto, PageDto } from '../../types/api'

interface CommentDto {
  id?: string
  comment?: string
  content?: string
  userId?: string
  authorId?: string
  isInternal?: boolean
  createdAt?: string
}

const STATUS_FILTERS = [
  { id: 'OPEN', labelKey: 'complaintsPage.statusOpen' },
  { id: 'IN_PROGRESS', labelKey: 'complaintsPage.statusInProgress' },
  { id: 'RESOLVED', labelKey: 'complaintsPage.statusResolved' },
  { id: 'CLOSED', labelKey: 'complaintsPage.statusClosed' },
] as const

function getList(data: unknown): ComplaintDto[] {
  if (Array.isArray(data)) return data
  const page = data as PageDto<ComplaintDto> | undefined
  return page?.content ?? []
}

export function ComplaintsPage() {
  const { t } = useLanguage()
  const [complaints, setComplaints] = useState<ComplaintDto[]>([])
  const [filter, setFilter] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [assignId, setAssignId] = useState<string | null>(null)
  const [assignAgentId, setAssignAgentId] = useState('')
  const [resolveId, setResolveId] = useState<string | null>(null)
  const [resolveNotes, setResolveNotes] = useState('')
  const [detailId, setDetailId] = useState<string | null>(null)
  const [detail, setDetail] = useState<ComplaintDto | null>(null)
  const [detailComments, setDetailComments] = useState<CommentDto[]>([])
  const [newComment, setNewComment] = useState('')
  const [commentInternal, setCommentInternal] = useState(false)
  const [includeInternalComments, setIncludeInternalComments] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [escalateModalOpen, setEscalateModalOpen] = useState(false)
  const [escalateToUserId, setEscalateToUserId] = useState('')
  const [escalateLoading, setEscalateLoading] = useState(false)

  const load = () => {
    setLoading(true)
    setError(null)
    complaintsAPI
      .list(filter ? { status: filter } : undefined)
      .then((res) => setComplaints(getList(res.data)))
      .catch(() => setComplaints([]))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [filter])

  const handleAssign = async () => {
    if (!assignId || !assignAgentId.trim()) return
    setError(null)
    try {
      await complaintsAPI.assign(assignId, { agentId: assignAgentId.trim() })
      setAssignId(null)
      setAssignAgentId('')
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleResolve = async () => {
    if (!resolveId) return
    setError(null)
    try {
      await complaintsAPI.resolve(resolveId, { notes: resolveNotes.trim() || undefined })
      setResolveId(null)
      setResolveNotes('')
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  const handleClose = async (id: string) => {
    setError(null)
    try {
      await complaintsAPI.close(id)
      load()
      if (detailId === id) setDetailId(null)
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  useEffect(() => {
    if (!detailId) {
      setDetail(null)
      setDetailComments([])
      setIncludeInternalComments(false)
      setCommentInternal(false)
      setEscalateModalOpen(false)
      setEscalateToUserId('')
      return
    }
    setDetailLoading(true)
    Promise.all([
      complaintsAPI.get(detailId).then((r) => r.data as ComplaintDto),
      complaintsAPI
        .getComments(detailId, includeInternalComments)
        .then((r) => (Array.isArray(r.data) ? r.data : []) as CommentDto[]),
    ])
      .then(([c, comments]) => {
        setDetail(c ?? null)
        setDetailComments(comments ?? [])
      })
      .catch(() => {
        setDetail(null)
        setDetailComments([])
      })
      .finally(() => setDetailLoading(false))
  }, [detailId, includeInternalComments])

  const handleEscalate = async () => {
    if (!detailId || !escalateToUserId.trim()) return
    setError(null)
    setEscalateLoading(true)
    try {
      await complaintsAPI.escalate(detailId, { escalateToId: escalateToUserId.trim() })
      setEscalateModalOpen(false)
      setEscalateToUserId('')
      complaintsAPI.get(detailId).then((r) => setDetail((r.data as ComplaintDto) ?? null))
      load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setEscalateLoading(false)
    }
  }

  const handleAddComment = async () => {
    if (!detailId || !newComment.trim()) return
    setError(null)
    try {
      await complaintsAPI.addComment(detailId, {
        comment: newComment.trim(),
        isInternal: commentInternal,
      })
      setNewComment('')
      setCommentInternal(false)
      complaintsAPI
        .getComments(detailId, includeInternalComments)
        .then((r) => setDetailComments(Array.isArray(r.data) ? (r.data as CommentDto[]) : []))
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('complaintsPage.title')}</h1>
      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 flex flex-wrap gap-4">
        <button
          type="button"
          onClick={() => setFilter(null)}
          className={filter === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
        >
          {t('ui.all')}
        </button>
        {STATUS_FILTERS.map((card) => (
          <button
            key={card.id}
            type="button"
            onClick={() => setFilter(card.id)}
            className={filter === card.id ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {t(card.labelKey)}
          </button>
        ))}
      </div>

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('complaintsPage.loading')}</div>
        ) : complaints.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('complaintsPage.noComplaints')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('complaintsPage.colTicket')}</th>
                <th className="px-4 py-3.5">{t('complaintsPage.colSubject')}</th>
                <th className="px-4 py-3.5">{t('complaintsPage.colPriority')}</th>
                <th className="px-4 py-3.5">{t('complaintsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('complaintsPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {complaints.map((c) => (
                <tr key={c.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                    {c.ticketNumber?.trim() || t('ui.emDash')}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
                    <button
                      type="button"
                      onClick={() => setDetailId(c.id)}
                      className="text-primary hover:underline text-left"
                    >
                      {c.subject}
                    </button>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{c.priority}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{c.status}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <button
                      type="button"
                      onClick={() => setDetailId(c.id)}
                      className="text-slate-600 hover:underline dark:text-slate-300"
                    >
                      {t('complaintsPage.view')}
                    </button>
                    {(c.status ?? '').toUpperCase() === 'OPEN' || (c.status ?? '').toUpperCase() === 'IN_PROGRESS' ? (
                      <>
                        <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                        <button
                          type="button"
                          onClick={() => { setAssignId(c.id); setAssignAgentId(''); }}
                          className="text-primary hover:underline"
                        >
                          {t('complaintsPage.assign')}
                        </button>
                        <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                        <button
                          type="button"
                          onClick={() => { setResolveId(c.id); setResolveNotes(''); }}
                          className="text-green-600 hover:underline dark:text-green-400"
                        >
                          {t('complaintsPage.resolve')}
                        </button>
                        <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                        <button
                          type="button"
                          onClick={() => handleClose(c.id)}
                          className="text-amber-600 hover:underline dark:text-amber-400"
                        >
                          {t('complaintsPage.close')}
                        </button>
                      </>
                    ) : (c.status ?? '').toUpperCase() === 'RESOLVED' ? (
                      <button
                        type="button"
                        onClick={() => handleClose(c.id)}
                        className="text-amber-600 hover:underline dark:text-amber-400"
                      >
                        {t('complaintsPage.close')}
                      </button>
                    ) : (
                      t('ui.emDash')
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {assignId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('complaintsPage.assignModalTitle')}</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('complaintsPage.assignModalHint')}</p>
            <input
              type="text"
              placeholder={t('complaintsPage.agentUuidPlaceholder')}
              value={assignAgentId}
              onChange={(e) => setAssignAgentId(e.target.value)}
              className="mt-3 w-full rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={handleAssign}
                disabled={!assignAgentId.trim()}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {t('complaintsPage.assign')}
              </button>
              <button
                type="button"
                onClick={() => { setAssignId(null); setAssignAgentId(''); }}
                className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
              >
                {t('ui.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {escalateModalOpen && detailId && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('complaintsPage.escalateModalTitle')}</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('complaintsPage.escalateModalHint')}</p>
            <input
              type="text"
              placeholder={t('complaintsPage.escalateTargetPlaceholder')}
              value={escalateToUserId}
              onChange={(e) => setEscalateToUserId(e.target.value)}
              className="mt-3 w-full rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={handleEscalate}
                disabled={!escalateToUserId.trim() || escalateLoading}
                className="rounded-xl bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
              >
                {t('complaintsPage.escalate')}
              </button>
              <button
                type="button"
                onClick={() => { setEscalateModalOpen(false); setEscalateToUserId(''); }}
                className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
              >
                {t('ui.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {resolveId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('complaintsPage.resolveModalTitle')}</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('complaintsPage.resolveModalHint')}</p>
            <textarea
              placeholder={t('complaintsPage.notesPlaceholder')}
              value={resolveNotes}
              onChange={(e) => setResolveNotes(e.target.value)}
              rows={3}
              className="mt-3 w-full rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={handleResolve}
                className="rounded-xl bg-green-600 px-4 py-2 text-sm font-medium text-white hover:opacity-90"
              >
                {t('complaintsPage.resolve')}
              </button>
              <button
                type="button"
                onClick={() => { setResolveId(null); setResolveNotes(''); }}
                className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
              >
                {t('ui.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {detailId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('complaintsPage.detailTitle')}</h2>
            {detailLoading ? (
              <div className="mt-4 py-8 text-center text-slate-500 dark:text-slate-400">{t('complaintsPage.loading')}</div>
            ) : detail ? (
              <>
                <dl className="mt-4 space-y-2 text-sm">
                  <div>
                    <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colTicket')}</dt>
                    <dd className="text-slate-900 dark:text-slate-100">{detail.ticketNumber?.trim() || t('ticketDetailPage.noNumberTitle')}</dd>
                  </div>
                  <div><dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colSubject')}</dt><dd className="text-slate-900 dark:text-slate-100">{detail.subject}</dd></div>
                  {detail.description ? (
                    <div><dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colDescription')}</dt><dd className="whitespace-pre-wrap text-slate-900 dark:text-slate-100">{detail.description}</dd></div>
                  ) : null}
                  {detail.category ? (
                    <div><dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colCategory')}</dt><dd className="text-slate-900 dark:text-slate-100">{detail.category}</dd></div>
                  ) : null}
                  {detail.bookingReference?.trim() ? (
                    <div>
                      <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colBooking')}</dt>
                      <dd className="text-slate-900 dark:text-slate-100">{detail.bookingReference}</dd>
                    </div>
                  ) : detail.bookingId ? (
                    <div>
                      <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colBooking')}</dt>
                      <dd className="text-slate-900 dark:text-slate-100">{t('paymentsPage.linkedBooking')}</dd>
                    </div>
                  ) : null}
                  <div><dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colPriority')}</dt><dd className="text-slate-900 dark:text-slate-100">{detail.priority}</dd></div>
                  <div><dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colStatus')}</dt><dd className="text-slate-900 dark:text-slate-100">{detail.status}</dd></div>
                </dl>
                {(detail.status ?? '').toUpperCase() !== 'CLOSED' && (
                  <div className="mt-4">
                    <button
                      type="button"
                      onClick={() => { setEscalateModalOpen(true); setEscalateToUserId(''); }}
                      className="rounded-xl bg-amber-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-amber-700"
                    >
                      {t('complaintsPage.escalate')}
                    </button>
                  </div>
                )}
                <div className="mt-6 flex flex-wrap items-center justify-between gap-2">
                  <h3 className="text-sm font-medium text-slate-900 dark:text-slate-100">{t('complaintsPage.comments')}</h3>
                  <label className="flex cursor-pointer items-center gap-2 text-xs text-slate-600 dark:text-slate-400">
                    <input
                      type="checkbox"
                      checked={includeInternalComments}
                      onChange={(e) => setIncludeInternalComments(e.target.checked)}
                      className="rounded border-slate-300"
                    />
                    {t('complaintsPage.showInternalComments')}
                  </label>
                </div>
                <ul className="mt-2 max-h-32 overflow-y-auto space-y-2">
                  {detailComments.length === 0 ? (
                    <li className="text-sm text-slate-500 dark:text-slate-400">{t('complaintsPage.noComments')}</li>
                  ) : (
                    detailComments.map((com, i) => (
                      <li key={com.id ?? i} className="rounded border border-slate-100 p-2 text-sm dark:border-slate-600">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-slate-900 dark:text-slate-100">{com.comment ?? com.content}</p>
                          {com.isInternal ? (
                            <span className="rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-900 dark:bg-amber-900/40 dark:text-amber-200">
                              {t('complaintsPage.internalBadge')}
                            </span>
                          ) : null}
                        </div>
                        {com.createdAt && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{com.createdAt}</p>}
                      </li>
                    ))
                  )}
                </ul>
                <label className="mt-3 flex cursor-pointer items-center gap-2 text-xs text-slate-600 dark:text-slate-400">
                  <input
                    type="checkbox"
                    checked={commentInternal}
                    onChange={(e) => setCommentInternal(e.target.checked)}
                    className="rounded border-slate-300"
                  />
                  {t('complaintsPage.markCommentInternal')}
                </label>
                <div className="mt-2 flex gap-2">
                  <input
                    type="text"
                    placeholder={t('complaintsPage.addCommentPlaceholder')}
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    className="flex-1 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                  />
                  <button
                    type="button"
                    onClick={handleAddComment}
                    disabled={!newComment.trim()}
                    className="dashboard-btn-primary px-3 py-2 text-sm disabled:opacity-50"
                  >
                    {t('complaintsPage.add')}
                  </button>
                </div>
              </>
            ) : null}
            <button
              type="button"
              onClick={() => setDetailId(null)}
              className="mt-4 rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
            >
              {t('complaintsPage.close')}
            </button>
          </div>
        </div>
      )}
    </>
  )
}
