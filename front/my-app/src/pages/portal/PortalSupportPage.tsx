/**
 * Provider support hub: submit requests to Ziyara (Phase 5), quick links, FAQ.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { resolveCompanyDashboardUrl } from '../../config/appSurface'
import { getApiErrorMessage, portalSupportAPI } from '../../services/api'
import type { PortalSupportRequestDto } from '../../types/api'

const FAQ_KEYS: { q: string; a: string }[] = [
  { q: 'portalSupportPage.faq1q', a: 'portalSupportPage.faq1a' },
  { q: 'portalSupportPage.faq2q', a: 'portalSupportPage.faq2a' },
  { q: 'portalSupportPage.faq3q', a: 'portalSupportPage.faq3a' },
  { q: 'portalSupportPage.faq4q', a: 'portalSupportPage.faq4a' },
]

function fmtWhen(iso: string | undefined, locale: string) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString(locale === 'ar' ? 'ar' : undefined, {
    dateStyle: 'short',
    timeStyle: 'short',
  })
}

export function PortalSupportPage() {
  const { t, locale } = useLanguage()
  const [rows, setRows] = useState<PortalSupportRequestDto[]>([])
  const [loadingList, setLoadingList] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [subject, setSubject] = useState('')
  const [body, setBody] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sentOk, setSentOk] = useState(false)

  const loadList = useCallback(() => {
    setLoadingList(true)
    setError(null)
    portalSupportAPI
      .list()
      .then((r) => {
        const data = r.data
        setRows(Array.isArray(data) ? data : [])
      })
      .catch((e) => {
        setError(getApiErrorMessage(e))
        setRows([])
      })
      .finally(() => setLoadingList(false))
  }, [])

  useEffect(() => {
    loadList()
  }, [loadList])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const sub = subject.trim()
    const msg = body.trim()
    if (!sub || !msg) return
    setSubmitting(true)
    setSentOk(false)
    setError(null)
    try {
      await portalSupportAPI.create({ subject: sub, body: msg })
      setSubject('')
      setBody('')
      setSentOk(true)
      loadList()
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <h1 className="app-page-title">{t('title.portalSupport')}</h1>

      <section className="mt-8 max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800/50">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
          {t('portalSupportPage.requestSectionTitle')}
        </h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{t('portalSupportPage.requestSectionHint')}</p>
        {sentOk && (
          <p className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-200">
            {t('portalSupportPage.sentOk')}
          </p>
        )}
        {error && (
          <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
            {error}
          </p>
        )}
        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <div>
            <label htmlFor="portal-support-subject" className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalSupportPage.subject')}
            </label>
            <input
              id="portal-support-subject"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              maxLength={500}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700"
              required
            />
          </div>
          <div>
            <label htmlFor="portal-support-body" className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('portalSupportPage.message')}
            </label>
            <textarea
              id="portal-support-body"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={5}
              maxLength={8000}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700"
              required
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {submitting ? t('portalSupportPage.submitting') : t('portalSupportPage.submit')}
          </button>
        </form>
      </section>

      <section className="mt-10 max-w-3xl">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('portalSupportPage.recentTitle')}</h2>
        {loadingList ? (
          <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : rows.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('portalSupportPage.emptyRequests')}</p>
        ) : (
          <ul className="mt-4 space-y-3">
            {rows.map((row) => (
              <li
                key={row.id}
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-800/50"
              >
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <span className="font-medium text-slate-900 dark:text-slate-100">{row.subject}</span>
                  <span className="text-xs text-slate-500 dark:text-slate-400">{fmtWhen(row.createdAt, locale)}</span>
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm text-slate-600 dark:text-slate-300">{row.body}</p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="mt-8 flex flex-wrap gap-3">
        <Link
          to="/portal/bookings"
          className="dashboard-btn-primary inline-flex shrink-0"
        >
          {t('portalSupportPage.linkBookings')}
        </Link>
        <Link
          to="/portal/listings"
          className="inline-flex rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
        >
          {t('portalSupportPage.linkListings')}
        </Link>
        <Link
          to="/portal/profile"
          className="inline-flex rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
        >
          {t('portalSupportPage.linkProfile')}
        </Link>
        <a
          href={resolveCompanyDashboardUrl()}
          className="inline-flex rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
        >
          {t('portal.companyDashboard')}
        </a>
      </div>

      <h2 className="mt-12 text-lg font-semibold text-slate-900 dark:text-slate-100">{t('portalSupportPage.faqTitle')}</h2>
      <div className="mt-4 max-w-2xl space-y-2">
        {FAQ_KEYS.map((item, idx) => (
          <details
            key={idx}
            className="group rounded-xl border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-800/50"
          >
            <summary className="cursor-pointer text-sm font-medium text-slate-900 dark:text-slate-100">{t(item.q)}</summary>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{t(item.a)}</p>
          </details>
        ))}
      </div>
    </>
  )
}
