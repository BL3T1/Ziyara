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
    [name, email, company, message],
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
    <section className="landing-scroll-parallax grid gap-6 lg:grid-cols-[1.1fr_1fr]">
      <Card surface="landing" className="landing-fade-up h-full !rounded-[28px] !p-8">
        <p className="lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.contactEyebrow'))}</p>
        <h1 className="lp-h1" style={{ marginTop: 12 }}>
          {pageText('title', t('landingBusiness.contactTitle'))}
        </h1>
        <p className="lp-body" style={{ marginTop: 14 }}>
          {pageText('body', t('landingBusiness.contactBody'))}
        </p>
        <div className="space-y-3" style={{ marginTop: 28 }}>
          <div className="lp-search-cell">
            <p className="lp-label">Response time</p>
            <p className="lp-value" style={{ fontWeight: 500, fontSize: 14 }}>
              Usually within one business day.
            </p>
          </div>
          <div className="lp-search-cell">
            <p className="lp-label">Coverage</p>
            <p className="lp-value" style={{ fontWeight: 500, fontSize: 14 }}>
              {totalCities ? `${totalCities} active cities with ${totalServices} listings in the database.` : 'Makkah, Madinah, Jeddah and nearby cities.'}
            </p>
          </div>
        </div>
      </Card>
      <Card surface="landing" className="landing-fade-up landing-fade-up-delay-1 !rounded-[28px] !p-8">
        <form onSubmit={handleContactSubmit} className="space-y-3">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder={t('landingBusiness.contactName')}
            className="lp-input"
          />
          <input
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder={t('landingBusiness.contactEmail')}
            className="lp-input"
          />
          <input
            value={company}
            onChange={(event) => setCompany(event.target.value)}
            placeholder={t('landingBusiness.contactCompany')}
            className="lp-input"
          />
          <textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            placeholder={t('landingBusiness.contactMessage')}
            rows={4}
            className="lp-input min-h-[7rem] resize-y"
          />
          <button type="submit" disabled={!isFormValid || submitting} className="lp-btn lp-btn-primary lp-btn-sm w-full sm:w-auto">
            {submitting ? t('landingBusiness.contactSending') : t('landingBusiness.contactSubmit')}
          </button>
          {submitError ? (
            <p className="text-sm text-red-700" role="alert">
              {submitError}
            </p>
          ) : null}
          {showSuccess ? <p className="text-sm text-emerald-800">{t('landingBusiness.contactSuccess')}</p> : null}
        </form>
      </Card>
    </section>
  )
}
