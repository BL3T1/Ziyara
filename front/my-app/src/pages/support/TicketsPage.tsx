/**
 * Provider Messages — support requests submitted by providers via their portal.
 * Staff can view full messages and send responses.
 */

import { useEffect, useRef, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, staffSupportRequestsAPI } from '../../services/api'
import type { PortalSupportRequestDto } from '../../types/api'

function formatDate(raw: string | null | undefined, locale: string): string {
  if (!raw) return '—'
  try {
    return new Intl.DateTimeFormat(locale === 'ar' ? 'ar-SA' : 'en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(raw))
  } catch {
    return raw
  }
}

function ResponseModal({
  msg,
  onClose,
  onSaved,
  locale,
}: {
  msg: PortalSupportRequestDto
  onClose: () => void
  onSaved: (updated: PortalSupportRequestDto) => void
  locale: string
}) {
  const { t } = useLanguage()
  const [text, setText] = useState(msg.staffResponse ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    textareaRef.current?.focus()
  }, [])

  async function handleSend() {
    if (!text.trim()) return
    setSaving(true)
    setError(null)
    try {
      const res = await staffSupportRequestsAPI.respond(msg.id, text)
      onSaved(res.data as PortalSupportRequestDto)
      onClose()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.45)' }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700 dark:bg-slate-900">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-4 dark:border-slate-800">
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
              {msg.providerName ?? t('ui.emDash')}
            </p>
            <h2 className="mt-0.5 truncate text-base font-semibold text-slate-800 dark:text-slate-100">
              {msg.subject}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="shrink-0 rounded-lg p-1 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
          >
            ✕
          </button>
        </div>

        {/* Original message */}
        <div className="px-6 py-4">
          <p className="mb-1 text-xs font-semibold uppercase tracking-wider text-slate-400">
            {t('ticketsPage.colMessage')}
          </p>
          <div className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-700 dark:bg-slate-800 dark:text-slate-300">
            {msg.body}
          </div>
          <p className="mt-1 text-right text-xs text-slate-400">
            {formatDate(msg.createdAt, locale)}
          </p>
        </div>

        {/* Previous response (if any) */}
        {msg.staffResponse && (
          <div className="px-6 pb-2">
            <p className="mb-1 text-xs font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
              {t('ticketsPage.respondedLabel')} — {formatDate(msg.respondedAt, locale)}
            </p>
            <div className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-900/40 dark:bg-emerald-950/30 dark:text-emerald-300">
              {msg.staffResponse}
            </div>
          </div>
        )}

        {/* Response textarea */}
        <div className="px-6 pb-2">
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            {msg.staffResponse ? t('ticketsPage.updateResponse') : t('ticketsPage.respond')}
          </label>
          <textarea
            ref={textareaRef}
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={4}
            placeholder={t('ticketsPage.respondPlaceholder')}
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          {error && (
            <p className="mt-1 text-xs text-red-600 dark:text-red-400">{error}</p>
          )}
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 px-6 pb-5 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
          >
            {t('ticketsPage.close')}
          </button>
          <button
            type="button"
            disabled={!text.trim() || saving}
            onClick={handleSend}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {saving ? t('ticketsPage.sending') : t('ticketsPage.sendResponse')}
          </button>
        </div>
      </div>
    </div>
  )
}

export function TicketsPage() {
  const { t, locale } = useLanguage()
  const [messages, setMessages] = useState<PortalSupportRequestDto[]>([])
  const [loading, setLoading] = useState(true)
  const [activeMsg, setActiveMsg] = useState<PortalSupportRequestDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    staffSupportRequestsAPI
      .listAll()
      .then((res) => setMessages(Array.isArray(res.data) ? (res.data as PortalSupportRequestDto[]) : []))
      .catch((e: unknown) => {
        const status = (e as { response?: { status?: number } })?.response?.status
        setError(status === 403 ? t('ui.accessDenied') : getApiErrorMessage(e))
        setMessages([])
      })
      .finally(() => setLoading(false))
  }, [])

  function handleSaved(updated: PortalSupportRequestDto) {
    setMessages((prev) => prev.map((m) => (m.id === updated.id ? { ...m, ...updated } : m)))
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('title.tickets')}</h1>
      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('ticketsPage.providerMessagesHint')}</p>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : messages.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ticketsPage.noProviderMessages')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('ticketsPage.colProvider')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.colSubject')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.colCreated')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.respondedLabel')}</th>
                <th className="px-4 py-3.5" />
              </tr>
            </thead>
            <tbody>
              {messages.map((msg) => (
                <tr key={msg.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-800 dark:text-slate-100">
                    {msg.providerName ?? t('ui.emDash')}
                  </td>
                  <td className="max-w-xs px-4 py-3 text-sm text-slate-700 dark:text-slate-200">
                    <span className="line-clamp-1 font-medium">{msg.subject}</span>
                    <span className="mt-0.5 line-clamp-1 block text-xs text-slate-400 dark:text-slate-500">
                      {msg.body}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                    {formatDate(msg.createdAt, locale)}
                  </td>
                  <td className="px-4 py-3">
                    {msg.staffResponse ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400">
                        ✓ {formatDate(msg.respondedAt, locale)}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                        {t('ticketsPage.respond')}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => setActiveMsg(msg)}
                      className="text-sm font-semibold text-primary hover:underline dark:text-[#90caff]"
                    >
                      {msg.staffResponse ? t('ticketsPage.updateResponse') : t('ticketsPage.respond')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {activeMsg && (
        <ResponseModal
          msg={activeMsg}
          locale={locale}
          onClose={() => setActiveMsg(null)}
          onSaved={handleSaved}
        />
      )}
    </>
  )
}
