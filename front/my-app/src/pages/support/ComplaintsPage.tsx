/**
 * Support > Complaints – list by status. View detail, Assign, Resolve, Close, Escalate, Comments.
 */

import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { complaintsAPI, usersAPI } from '../../services/api'
import { getApiErrorMessage } from '../../services/api'
import type { ComplaintDto, PageDto } from '../../types/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import { SearchableSelect, type SelectOption } from '../../components/SearchableSelect'
import { statusLabel, priorityLabel } from '../../i18n/enumLabels'
import { usePermission } from '../../hooks/usePermission'

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
  const canWrite = usePermission('complaints:write')
  const [complaints, setComplaints] = useState<ComplaintDto[]>([])
  const [filter, setFilter] = useState<string | null>(null)
  const [priorityFilter, setPriorityFilter] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [assignId, setAssignId] = useState<string | null>(null)
  const [assignAgent, setAssignAgent] = useState<SelectOption | null>(null)
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
  const [escalateUser, setEscalateUser] = useState<SelectOption | null>(null)
  const [escalateLoading, setEscalateLoading] = useState(false)

  const fetchStaffOptions = useCallback(async (query: string) => {
    try {
      const res = await usersAPI.list({ size: 50, role: 'support' })
      const items = (Array.isArray(res.data) ? res.data : ((res.data as { content?: unknown[] })?.content ?? [])) as Array<{ id?: string; firstName?: string; lastName?: string; email?: string }>
      const q = query.toLowerCase()
      return items
        .filter((u) => {
          const name = `${u.firstName ?? ''} ${u.lastName ?? ''}`.toLowerCase()
          return !q || name.includes(q) || (u.email ?? '').toLowerCase().includes(q)
        })
        .map((u) => ({
          value: u.id ?? '',
          label: `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || (u.email ?? ''),
          sublabel: u.email,
        }))
    } catch {
      return []
    }
  }, [])

  const fetchAllStaffOptions = useCallback(async (query: string) => {
    try {
      const res = await usersAPI.list({ size: 100 })
      const items = (Array.isArray(res.data) ? res.data : ((res.data as { content?: unknown[] })?.content ?? [])) as Array<{ id?: string; firstName?: string; lastName?: string; email?: string }>
      const q = query.toLowerCase()
      return items
        .filter((u) => {
          const name = `${u.firstName ?? ''} ${u.lastName ?? ''}`.toLowerCase()
          return !q || name.includes(q) || (u.email ?? '').toLowerCase().includes(q)
        })
        .map((u) => ({
          value: u.id ?? '',
          label: `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || (u.email ?? ''),
          sublabel: u.email,
        }))
    } catch {
      return []
    }
  }, [])

  const load = () => {
    setLoading(true)
    setError(null)
    complaintsAPI
      .list({ ...(filter ? { status: filter } : {}), ...(priorityFilter ? { priority: priorityFilter } : {}) })
      .then((res) => setComplaints(getList(res.data)))
      .catch((e: unknown) => {
        const status = (e as { response?: { status?: number } })?.response?.status
        setError(status === 403 ? t('ui.accessDenied') : getApiErrorMessage(e))
        setComplaints([])
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [filter, priorityFilter])

  const handleAssign = async () => {
    if (!assignId || !assignAgent?.value) return
    setError(null)
    try {
      await complaintsAPI.assign(assignId, { agentId: assignAgent.value })
      setAssignId(null)
      setAssignAgent(null)
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
      setEscalateUser(null)
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
    if (!detailId || !escalateUser?.value) return
    setError(null)
    setEscalateLoading(true)
    try {
      await complaintsAPI.escalate(detailId, { escalateToId: escalateUser.value })
      setEscalateModalOpen(false)
      setEscalateUser(null)
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
      <div className="mt-2 flex flex-wrap gap-2">
        <span className="self-center text-xs text-slate-500 dark:text-slate-400">{t('complaintsPage.colPriority')}:</span>
        <button
          type="button"
          onClick={() => setPriorityFilter(null)}
          className={`text-xs ${priorityFilter === null ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}`}
        >
          {t('ui.all')}
        </button>
        {(['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const).map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => setPriorityFilter(p)}
            className={`text-xs ${priorityFilter === p ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}`}
          >
            {priorityLabel(t, p)}
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
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{priorityLabel(t, c.priority)}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{statusLabel(t, c.status)}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <button
                      type="button"
                      onClick={() => setDetailId(c.id)}
                      className="text-slate-600 hover:underline dark:text-slate-300"
                    >
                      {t('complaintsPage.view')}
                    </button>
                    {canWrite && ((c.status ?? '').toUpperCase() === 'OPEN' || (c.status ?? '').toUpperCase() === 'IN_PROGRESS') ? (
                      <>
                        <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                        <button
                          type="button"
                          onClick={() => { setAssignId(c.id); setAssignAgent(null); }}
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
                    ) : canWrite && (c.status ?? '').toUpperCase() === 'RESOLVED' ? (
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

      {/* Assign modal */}
      <Modal
        open={!!assignId}
        onClose={() => { setAssignId(null); setAssignAgent(null) }}
        title={t('complaintsPage.assignModalTitle')}
        description={t('complaintsPage.assignModalHint')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setAssignId(null); setAssignAgent(null) }}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="button"
              disabled={!assignAgent?.value}
              onClick={handleAssign}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('complaintsPage.assign')}
            </button>
          </>
        }
      >
        <FormField label={t('complaintsPage.agentUuidPlaceholder')} required>
          <SearchableSelect
            selectedOption={assignAgent}
            onSelect={setAssignAgent}
            fetchOptions={fetchStaffOptions}
            placeholder={t('complaintsPage.agentUuidPlaceholder')}
          />
        </FormField>
      </Modal>

      {/* Escalate modal */}
      <Modal
        open={escalateModalOpen && !!detailId}
        onClose={() => { setEscalateModalOpen(false); setEscalateUser(null) }}
        title={t('complaintsPage.escalateModalTitle')}
        description={t('complaintsPage.escalateModalHint')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setEscalateModalOpen(false); setEscalateUser(null) }}
              disabled={escalateLoading}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="button"
              disabled={!escalateUser?.value || escalateLoading}
              onClick={handleEscalate}
              className="rounded-xl bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
            >
              {t('complaintsPage.escalate')}
            </button>
          </>
        }
      >
        <FormField label={t('complaintsPage.escalateTargetPlaceholder')} required>
          <SearchableSelect
            selectedOption={escalateUser}
            onSelect={setEscalateUser}
            fetchOptions={fetchAllStaffOptions}
            placeholder={t('complaintsPage.escalateTargetPlaceholder')}
          />
        </FormField>
      </Modal>

      {/* Resolve modal */}
      <Modal
        open={!!resolveId}
        onClose={() => { setResolveId(null); setResolveNotes('') }}
        title={t('complaintsPage.resolveModalTitle')}
        description={t('complaintsPage.resolveModalHint')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setResolveId(null); setResolveNotes('') }}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="complaint-resolve-form"
              className="rounded-xl bg-green-600 px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
            >
              {t('complaintsPage.resolve')}
            </button>
          </>
        }
      >
        <form id="complaint-resolve-form" onSubmit={(e) => { e.preventDefault(); handleResolve() }}>
          <FormField label={t('complaintsPage.notesPlaceholder')}>
            <textarea
              placeholder={t('complaintsPage.notesPlaceholder')}
              value={resolveNotes}
              onChange={(e) => setResolveNotes(e.target.value)}
              rows={3}
              className="modal-textarea"
            />
          </FormField>
        </form>
      </Modal>

      {/* Detail modal */}
      <Modal
        open={!!detailId}
        onClose={() => setDetailId(null)}
        title={t('complaintsPage.detailTitle')}
        size="md"
        footer={
          <button type="button" onClick={() => setDetailId(null)} className="dashboard-btn-secondary">
            {t('ui.close')}
          </button>
        }
      >
        {detailLoading ? (
          <div className="py-8 text-center text-slate-500 dark:text-slate-400">{t('complaintsPage.loading')}</div>
        ) : detail ? (
          <div className="space-y-4">
            <dl className="space-y-2 rounded-xl border border-slate-100 p-3.5 text-sm dark:border-white/[0.05]">
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colTicket')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{detail.ticketNumber?.trim() || t('ticketDetailPage.noNumberTitle')}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colSubject')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{detail.subject}</dd>
              </div>
              {detail.description ? (
                <div className="flex flex-col gap-1">
                  <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colDescription')}</dt>
                  <dd className="whitespace-pre-wrap font-medium text-slate-900 dark:text-slate-100">{detail.description}</dd>
                </div>
              ) : null}
              {detail.category ? (
                <div className="flex justify-between gap-4">
                  <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colCategory')}</dt>
                  <dd className="font-medium text-slate-900 dark:text-slate-100">{detail.category}</dd>
                </div>
              ) : null}
              {detail.bookingReference?.trim() ? (
                <div className="flex justify-between gap-4">
                  <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colBooking')}</dt>
                  <dd className="font-medium text-slate-900 dark:text-slate-100">{detail.bookingReference}</dd>
                </div>
              ) : detail.bookingId ? (
                <div className="flex justify-between gap-4">
                  <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colBooking')}</dt>
                  <dd className="font-medium text-slate-900 dark:text-slate-100">{t('paymentsPage.linkedBooking')}</dd>
                </div>
              ) : null}
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colPriority')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{priorityLabel(t, detail.priority)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500 dark:text-slate-400">{t('complaintsPage.colStatus')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{statusLabel(t, detail.status)}</dd>
              </div>
            </dl>

            {(detail.status ?? '').toUpperCase() !== 'CLOSED' && (
              <button
                type="button"
                onClick={() => { setEscalateModalOpen(true); setEscalateUser(null) }}
                className="rounded-xl bg-amber-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-amber-700"
              >
                {t('complaintsPage.escalate')}
              </button>
            )}

            <div>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('complaintsPage.comments')}</h3>
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
              <ul className="mt-2 max-h-40 space-y-2 overflow-y-auto">
                {detailComments.length === 0 ? (
                  <li className="text-sm text-slate-500 dark:text-slate-400">{t('complaintsPage.noComments')}</li>
                ) : (
                  detailComments.map((com, i) => (
                    <li key={com.id ?? i} className="rounded-lg border border-slate-100 p-2.5 text-sm dark:border-white/[0.05]">
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
            </div>

            <div className="space-y-2 rounded-xl border border-slate-100 p-3.5 dark:border-white/[0.05]">
              <label className="flex cursor-pointer items-center gap-2 text-xs text-slate-600 dark:text-slate-400">
                <input
                  type="checkbox"
                  checked={commentInternal}
                  onChange={(e) => setCommentInternal(e.target.checked)}
                  className="rounded border-slate-300"
                />
                {t('complaintsPage.markCommentInternal')}
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder={t('complaintsPage.addCommentPlaceholder')}
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  className="modal-input flex-1"
                />
                <button
                  type="button"
                  onClick={handleAddComment}
                  disabled={!newComment.trim()}
                  className="dashboard-btn-primary shrink-0 disabled:opacity-50"
                >
                  {t('complaintsPage.add')}
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>
    </>
  )
}
