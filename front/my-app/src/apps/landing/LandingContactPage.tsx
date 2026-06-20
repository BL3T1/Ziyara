import { useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Card } from '../../components/Card'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, publicAPI } from '../../services/api'
import { useLandingPageContent } from './useLandingPageContent'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

const MAX_MESSAGE = 2000

export function LandingContactPage() {
  useDocumentMeta({ title: 'Contact & Support · Ziyara', description: 'Questions about your booking or account? Get in touch with the Ziyara support team.' })
  const { t } = useLanguage()
  const { readString: pageText } = useLandingPageContent('contact')

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [showSuccess, setShowSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<{ name?: string; email?: string; message?: string }>({})

  const touched = useRef({ name: false, email: false, message: false })

  function validateField(field: 'name' | 'email' | 'message', value: string): string | undefined {
    if (field === 'name' && value.trim().length < 2) return t('landingContact.errName')
    if (field === 'email' && !value.includes('@')) return t('landingContact.errEmail')
    if (field === 'message' && value.trim().length < 10) return t('landingContact.errMessage')
    return undefined
  }

  function handleBlur(field: 'name' | 'email' | 'message', value: string) {
    touched.current[field] = true
    setFieldErrors((prev) => ({ ...prev, [field]: validateField(field, value) }))
  }

  const isFormValid = useMemo(
    () => name.trim().length > 1 && email.includes('@') && message.trim().length > 10,
    [name, email, message],
  )

  const handleContactSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    // Validate all fields on submit
    const errs = {
      name: validateField('name', name),
      email: validateField('email', email),
      message: validateField('message', message),
    }
    setFieldErrors(errs)
    if (errs.name || errs.email || errs.message) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await publicAPI.submitContact({
        name: name.trim(),
        email: email.trim(),
        company: subject.trim() || undefined,
        message: message.trim(),
      })
      setShowSuccess(true)
      setName('')
      setEmail('')
      setSubject('')
      setMessage('')
      setFieldErrors({})
    } catch (err) {
      setSubmitError(getApiErrorMessage(err, t('landingBusiness.contactError')))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="landing-scroll-parallax grid gap-6 lg:grid-cols-[1.1fr_1fr]">
      {/* Info column */}
      <Card surface="landing" className="landing-fade-up h-full !rounded-[28px] !p-8">
        <p className="lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.contactEyebrow'))}</p>
        <h1 className="lp-h1 mt-3">
          {pageText('title', t('landingBusiness.contactTitle'))}
        </h1>
        <p className="lp-body mt-3.5">
          {pageText('body', t('landingBusiness.contactBody'))}
        </p>
        <div className="space-y-3 mt-7">
          <div className="lp-search-cell">
            <p className="lp-label">{t('landingContact.responseTimeLabel')}</p>
            <p className="lp-value lp-font-medium-sm">
              {t('landingContact.responseTimeValue')}
            </p>
          </div>
          <div className="lp-search-cell">
            <p className="lp-label">{t('landingContact.coverageLabel')}</p>
            <p className="lp-value lp-font-medium-sm">
              {t('landingContact.coverageFallback')}
            </p>
          </div>
        </div>
      </Card>

      {/* Form column */}
      <Card surface="landing" className="landing-fade-up landing-fade-up-delay-1 !rounded-[28px] !p-8">
        {showSuccess ? (
          /* Success state replaces the form */
          <div className="flex h-full flex-col items-center justify-center py-8 text-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-full lp-skeleton-teal">
              <svg className="h-7 w-7" fill="none" stroke="#22a06b" strokeWidth="2.2" viewBox="0 0 24 24" aria-hidden>
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </div>
            <p className="lp-h1 !text-xl">{t('landingBusiness.contactSuccess')}</p>
            <button
              type="button"
              onClick={() => setShowSuccess(false)}
              className="lp-btn lp-btn-outline lp-btn-sm"
            >
              {t('landingContact.sendAnother')}
            </button>
          </div>
        ) : (
          <form onSubmit={handleContactSubmit} className="space-y-4" noValidate>
            {/* Name */}
            <div>
              <label className="lp-field-label">
                {t('landingBusiness.contactName')} <span className="lp-text-error">*</span>
              </label>
              <input
                value={name}
                onChange={(e) => { setName(e.target.value); if (touched.current.name) setFieldErrors((p) => ({ ...p, name: validateField('name', e.target.value) })) }}
                onBlur={(e) => handleBlur('name', e.target.value)}
                className="lp-input mt-1"
                style={fieldErrors.name ? { borderColor: '#e74c3c' } : undefined}
              />
              {fieldErrors.name && <p className="mt-1 text-xs lp-text-error">{fieldErrors.name}</p>}
            </div>

            {/* Email */}
            <div>
              <label className="lp-field-label">
                {t('landingBusiness.contactEmail')} <span className="lp-text-error">*</span>
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); if (touched.current.email) setFieldErrors((p) => ({ ...p, email: validateField('email', e.target.value) })) }}
                onBlur={(e) => handleBlur('email', e.target.value)}
                className="lp-input mt-1"
                style={fieldErrors.email ? { borderColor: '#e74c3c' } : undefined}
              />
              {fieldErrors.email && <p className="mt-1 text-xs lp-text-error">{fieldErrors.email}</p>}
            </div>

            {/* Subject — optional */}
            <div>
              <label className="lp-field-label">
                {t('landingBusiness.contactCompany')}
                <span className="ms-1 font-normal normal-case lp-text-faint">({t('ui.cancel') === 'Cancel' ? 'optional' : 'اختياري'})</span>
              </label>
              <input
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                className="lp-input mt-1"
              />
            </div>

            {/* Message */}
            <div>
              <div className="flex items-baseline justify-between">
                <label className="lp-field-label">
                  {t('landingBusiness.contactMessage')} <span className="lp-text-error">*</span>
                </label>
                <span className="text-xs" style={{ color: message.length > MAX_MESSAGE * 0.85 ? '#c0392b' : 'var(--ink-faint)' }}>
                  {message.length}/{MAX_MESSAGE}
                </span>
              </div>
              <textarea
                value={message}
                onChange={(e) => { setMessage(e.target.value); if (touched.current.message) setFieldErrors((p) => ({ ...p, message: validateField('message', e.target.value) })) }}
                onBlur={(e) => handleBlur('message', e.target.value)}
                rows={4}
                maxLength={MAX_MESSAGE}
                className="lp-input mt-1 min-h-[7rem] resize-y"
                style={fieldErrors.message ? { borderColor: '#e74c3c' } : undefined}
              />
              {fieldErrors.message && <p className="mt-1 text-xs lp-text-error">{fieldErrors.message}</p>}
            </div>

            <button
              type="submit"
              disabled={!isFormValid || submitting}
              className="lp-btn lp-btn-primary lp-btn-sm w-full sm:w-auto disabled:opacity-60"
            >
              {submitting ? t('landingBusiness.contactSending') : t('landingBusiness.contactSubmit')}
            </button>

            {submitError && (
              <p className="text-sm lp-text-error" role="alert">{submitError}</p>
            )}
          </form>
        )}
      </Card>
    </section>
  )
}
