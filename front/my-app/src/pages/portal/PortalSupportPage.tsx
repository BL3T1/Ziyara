/**
 * Provider support hub: submit requests to Ziyara, quick links, FAQ.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { resolveCompanyDashboardUrl } from '../../config/appSurface'
import { getApiErrorMessage, portalSupportAPI } from '../../services/api'
import type { PortalSupportRequestDto } from '../../types/api'
import { Card } from '../../components/Card'

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
  return d.toLocaleString(locale === 'ar' ? 'ar' : undefined, { dateStyle: 'short', timeStyle: 'short' })
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
      .catch((e) => { setError(getApiErrorMessage(e)); setRows([]) })
      .finally(() => setLoadingList(false))
  }, [])

  useEffect(() => { loadList() }, [loadList])

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

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        {/* Submit form */}
        <Card className="!p-5">
          <h2 className="dashboard-card-title">{t('portalSupportPage.requestSectionTitle')}</h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('portalSupportPage.requestSectionHint')}</p>

          {sentOk && (
            <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
              {t('portalSupportPage.sentOk')}
            </div>
          )}
          {error && (
            <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-900/20 dark:text-red-300">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-5 space-y-4">
            <div>
              <label htmlFor="portal-support-subject" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalSupportPage.subject')}
              </label>
              <input
                id="portal-support-subject"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                maxLength={500}
                className="dashboard-date-input w-full"
                required
              />
            </div>
            <div>
              <label htmlFor="portal-support-body" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('portalSupportPage.message')}
              </label>
              <textarea
                id="portal-support-body"
                value={body}
                onChange={(e) => setBody(e.target.value)}
                rows={5}
                maxLength={8000}
                className="dashboard-date-input w-full resize-none"
                required
              />
            </div>
            <button type="submit" disabled={submitting} className="dashboard-btn-primary disabled:opacity-50">
              {submitting ? t('portalSupportPage.submitting') : t('portalSupportPage.submit')}
            </button>
          </form>
        </Card>

        {/* Quick links */}
        <Card className="!p-5 h-fit">
          <h2 className="dashboard-card-title mb-4">{t('portalSupportPage.linkBookings')}</h2>
          <nav className="flex flex-col gap-2">
            <Link to="/portal/bookings" className="dashboard-btn-primary justify-start">
              {t('portalSupportPage.linkBookings')}
            </Link>
            <Link to="/portal/listings" className="dashboard-btn-secondary justify-start">
              {t('portalSupportPage.linkListings')}
            </Link>
            <Link to="/portal/profile" className="dashboard-btn-secondary justify-start">
              {t('portalSupportPage.linkProfile')}
            </Link>
            <a href={resolveCompanyDashboardUrl()} className="dashboard-btn-ghost justify-start">
              {t('portal.companyDashboard')}
            </a>
          </nav>
        </Card>
      </div>

      {/* Recent requests */}
      <section>
        <h2 className="dashboard-section-title">{t('portalSupportPage.recentTitle')}</h2>
        {loadingList ? (
          <div className="mt-3 flex flex-col gap-2">
            {Array.from({ length: 3 }, (_, i) => (
              <div key={i} className="h-16 animate-pulse rounded-xl bg-slate-100 dark:bg-white/[0.04]" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{t('portalSupportPage.emptyRequests')}</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {rows.map((row) => (
              <li
                key={row.id}
                className="rounded-xl border border-slate-100 bg-white px-4 py-3.5 dark:border-white/[0.05] dark:bg-[#0d1117]"
              >
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <span className="font-semibold text-slate-900 dark:text-slate-100">{row.subject}</span>
                  <span className="text-xs text-slate-400 dark:text-slate-500">{fmtWhen(row.createdAt, locale)}</span>
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-slate-600 dark:text-slate-300">
                  {row.body}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* FAQ */}
      <section>
        <h2 className="dashboard-section-title">{t('portalSupportPage.faqTitle')}</h2>
        <div className="mt-3 max-w-2xl space-y-2">
          {FAQ_KEYS.map((item, idx) => (
            <details
              key={idx}
              className="group rounded-xl border border-slate-100 bg-white px-4 py-3 transition-colors dark:border-white/[0.05] dark:bg-[#0d1117] open:border-[#1e4d6b]/20 dark:open:border-[#1e4d6b]/30"
            >
              <summary className="cursor-pointer select-none text-sm font-semibold text-slate-800 dark:text-slate-200 list-none">
                <span className="flex items-center justify-between gap-2">
                  {t(item.q)}
                  <span className="shrink-0 text-slate-400 transition-transform duration-200 group-open:rotate-45">＋</span>
                </span>
              </summary>
              <p className="mt-3 border-t border-slate-100 pt-3 text-sm leading-relaxed text-slate-600 dark:border-white/[0.05] dark:text-slate-300">
                {t(item.a)}
              </p>
            </details>
          ))}
        </div>
      </section>
    </>
  )
}
