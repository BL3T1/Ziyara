/**
 * Support > Ticket detail – comments, resolve, close.
 */

import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { ticketsAPI, getApiErrorMessage } from '../../services/api'
import type { TicketDto } from '../../types/api'

interface CommentDto {
  id?: string
  content?: string
  authorId?: string
  createdAt?: string
}

export function TicketDetailPage() {
  const { t } = useLanguage()
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [ticket, setTicket] = useState<TicketDto | null>(null)
  const [comments, setComments] = useState<CommentDto[]>([])
  const [newComment, setNewComment] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError(null)
    Promise.all([
      ticketsAPI.get(id).then((r) => r.data as TicketDto),
      ticketsAPI.getComments(id).then((r) => (Array.isArray(r.data) ? r.data : []) as CommentDto[]),
    ])
      .then(([tk, c]) => {
        setTicket(tk ?? null)
        setComments(Array.isArray(c) ? c : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setTicket(null)
        setComments([])
      })
      .finally(() => setLoading(false))
  }, [id])

  const handleAddComment = async () => {
    if (!id || !newComment.trim()) return
    setError(null)
    setActionLoading(true)
    try {
      await ticketsAPI.addComment(id, { comment: newComment.trim() })
      const res = await ticketsAPI.getComments(id)
      setComments(Array.isArray(res.data) ? (res.data as CommentDto[]) : [])
      setNewComment('')
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setActionLoading(false)
    }
  }

  const handleResolve = async () => {
    if (!id) return
    setError(null)
    setActionLoading(true)
    try {
      await ticketsAPI.resolve(id)
      const res = await ticketsAPI.get(id)
      setTicket((res.data as TicketDto) ?? null)
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setActionLoading(false)
    }
  }

  const handleClose = async () => {
    if (!id) return
    setError(null)
    setActionLoading(true)
    try {
      await ticketsAPI.close(id)
      navigate('/support/tickets')
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setActionLoading(false)
    }
  }

  if (loading && !ticket) {
    return <div className="text-slate-500 dark:text-slate-400">{t('ticketDetailPage.loading')}</div>
  }
  if (!ticket) {
    return (
      <div>
        <p className="text-slate-600 dark:text-slate-300">{error ?? t('ticketDetailPage.notFound')}</p>
        <Link to="/support/tickets" className="mt-4 inline-block text-primary hover:underline">
          {t('ticketDetailPage.backToTickets')}
        </Link>
      </div>
    )
  }

  return (
    <>
      <div className="flex items-center gap-4">
        <Link to="/support/tickets" className="text-primary hover:underline">
          {t('ticketDetailPage.backTickets')}
        </Link>
        <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">
          {ticket.ticketNumber?.trim() || t('ticketDetailPage.noNumberTitle')}
        </h1>
      </div>
      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800">
        <p className="font-medium text-slate-900 dark:text-slate-100">{ticket.subject}</p>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
          {t('ticketDetailPage.priorityStatus', { priority: String(ticket.priority), status: String(ticket.status) })}
        </p>
        {ticket.createdAt && (
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {t('ui.created')}: {new Date(ticket.createdAt).toLocaleString()}
          </p>
        )}
      </div>

      <div className="mt-6 flex flex-wrap gap-2">
        {ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' && (
          <button
            type="button"
            onClick={handleResolve}
            disabled={actionLoading}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {t('ui.resolve')}
          </button>
        )}
        {ticket.status !== 'CLOSED' && (
          <button
            type="button"
            onClick={handleClose}
            disabled={actionLoading}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200 disabled:opacity-50"
          >
            {t('ticketDetailPage.closeTicket')}
          </button>
        )}
      </div>

      <div className="mt-8">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('ticketDetailPage.comments')}</h2>
        <ul className="mt-3 space-y-3">
          {comments.map((c, i) => (
            <li key={c.id ?? i} className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-600 dark:bg-slate-700/50">
              <p className="text-sm text-slate-700 dark:text-slate-200">{c.content ?? t('ui.emDash')}</p>
              {c.createdAt && (
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{new Date(c.createdAt).toLocaleString()}</p>
              )}
            </li>
          ))}
        </ul>
        {ticket.status !== 'CLOSED' && (
          <div className="mt-4 flex gap-2">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              rows={2}
              placeholder={t('ticketDetailPage.addCommentPlaceholder')}
              className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
            <button
              type="button"
              onClick={handleAddComment}
              disabled={!newComment.trim() || actionLoading}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('ui.addComment')}
            </button>
          </div>
        )}
      </div>
    </>
  )
}
