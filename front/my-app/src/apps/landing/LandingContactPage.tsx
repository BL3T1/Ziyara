import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Card } from '../../components/Card'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, publicAPI } from '../../services/api'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

export function LandingContactPage() {
  const { t } = useLanguage()
  const { totalServices, totalCities } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('contact')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [company, setCompany] = useState('')
  const [message, setMessage] = useState('')
  const [showSuccess, setShowSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const isFormValid = useMemo(
    () => name.trim().length > 1 && email.includes('@') && company.trim().length > 1 && message.trim().length > 10,
    [name, email, company, message]
  )

  const handleContactSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!isFormValid) return
    setSubmitting(true)
    setSubmitError(null)
    setShowSuccess(false)
    try {
      await publicAPI.submitContact({
        name: name.trim(),
        email: email.trim(),
        company: company.trim() || undefined,
        message: message.trim(),
      })
      setShowSuccess(true)
      setName('')
      setEmail('')
      setCompany('')
      setMessage('')
    } catch (err) {
      const msg = getApiErrorMessage(err, t('landingBusiness.contactError'))
      setSubmitError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="landing-3d-stage landing-scroll-parallax relative grid gap-6 lg:grid-cols-[1.1fr_1fr]">
      <Card className="landing-3d-card h-full bg-gradient-to-br from-white/90 via-white to-primary/[0.08] dark:from-slate-900/80 dark:via-slate-900/75 dark:to-primary/[0.16]">
        <p className="landing-fade-up text-xs font-semibold uppercase tracking-[0.12em] text-secondary">
          {pageText('eyebrow', t('landingBusiness.contactEyebrow'))}
        </p>
        <h1 className="landing-fade-up landing-fade-up-delay-1 font-display mt-3 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          {pageText('title', t('landingBusiness.contactTitle'))}
        </h1>
        <p className="landing-fade-up landing-fade-up-delay-2 mt-3 text-sm leading-relaxed text-slate-600 dark:text-slate-400">
          {pageText('body', t('landingBusiness.contactBody'))}
        </p>
        <div className="mt-8 space-y-3 text-sm">
          <div className="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 dark:border-slate-700/70 dark:bg-slate-800/60">
            <p className="font-semibold text-slate-800 dark:text-slate-200">Response time</p>
            <p className="text-slate-600 dark:text-slate-400">Usually within one business day.</p>
          </div>
          <div className="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 dark:border-slate-700/70 dark:bg-slate-800/60">
            <p className="font-semibold text-slate-800 dark:text-slate-200">Coverage</p>
            <p className="text-slate-600 dark:text-slate-400">
              {totalCities ? `${totalCities} active cities with ${totalServices} listings in the database.` : 'Makkah, Madinah, Jeddah and nearby cities.'}
            </p>
          </div>
        </div>
      </Card>
      <Card className="landing-3d-card bg-white/95 dark:bg-slate-900/70">
        <form onSubmit={handleContactSubmit} className="space-y-3">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder={t('landingBusiness.contactName')}
            className="w-full rounded-xl border border-slate-300/90 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none ring-primary/30 transition focus:ring-2 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          />
          <input
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder={t('landingBusiness.contactEmail')}
            className="w-full rounded-xl border border-slate-300/90 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none ring-primary/30 transition focus:ring-2 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          />
          <input
            value={company}
            onChange={(event) => setCompany(event.target.value)}
            placeholder={t('landingBusiness.contactCompany')}
            className="w-full rounded-xl border border-slate-300/90 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none ring-primary/30 transition focus:ring-2 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          />
          <textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            placeholder={t('landingBusiness.contactMessage')}
            rows={4}
            className="w-full rounded-xl border border-slate-300/90 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none ring-primary/30 transition focus:ring-2 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          />
          <button
            type="submit"
            disabled={!isFormValid || submitting}
            className="inline-flex rounded-2xl bg-gradient-to-br from-primary to-primary/90 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-primary/25 ring-1 ring-white/10 transition hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? t('landingBusiness.contactSending') : t('landingBusiness.contactSubmit')}
          </button>
          {submitError ? (
            <p className="text-sm text-red-600 dark:text-red-400" role="alert">
              {submitError}
            </p>
          ) : null}
          {showSuccess ? <p className="text-sm text-emerald-700 dark:text-emerald-400">{t('landingBusiness.contactSuccess')}</p> : null}
        </form>
      </Card>
    </section>
  )
}
