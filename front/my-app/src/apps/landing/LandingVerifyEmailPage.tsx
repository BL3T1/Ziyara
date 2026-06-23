import { useState, useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authAPI, getApiErrorMessage } from '../../services/api'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

export function LandingVerifyEmailPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const email = searchParams.get('email') ?? ''
  useDocumentMeta({ title: 'Verify your email · Ziyara' })

  const [code, setCode] = useState('')
  const [sending, setSending] = useState(false)
  const [verifying, setVerifying] = useState(false)
  const [error, setError] = useState('')
  const [resendCooldown, setResendCooldown] = useState(0)
  const cooldownRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Auto-send OTP on mount when email is provided
  useEffect(() => {
    if (!email) return
    handleSendOtp()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    return () => {
      if (cooldownRef.current) clearInterval(cooldownRef.current)
    }
  }, [])

  async function handleSendOtp() {
    if (!email) return
    setSending(true)
    setError('')
    try {
      await authAPI.sendOtp({ email, type: 'EMAIL_VERIFICATION' })
      setResendCooldown(60)
      cooldownRef.current = setInterval(() => {
        setResendCooldown((n) => {
          if (n <= 1) {
            clearInterval(cooldownRef.current!)
            return 0
          }
          return n - 1
        })
      }, 1000)
    } catch (err) {
      setError(getApiErrorMessage(err, t('landingVerifyEmail.sendError')))
    } finally {
      setSending(false)
    }
  }

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault()
    if (!email || !code.trim()) return
    setVerifying(true)
    setError('')
    try {
      await authAPI.verifyOtp({ email, code: code.trim() })
      navigate('/login?verified=1')
    } catch (err) {
      setError(getApiErrorMessage(err, t('landingVerifyEmail.verifyError')))
    } finally {
      setVerifying(false)
    }
  }

  if (!email) {
    return (
      <div className="lp-sheet text-center">
        <p className="lp-text-muted">{t('landingVerifyEmail.noEmail')}</p>
        <button type="button" onClick={() => navigate('/signup')} className="lp-btn lp-btn-primary lp-btn-sm mt-4">
          {t('landingAuth.signUp')}
        </button>
      </div>
    )
  }

  return (
    <div className="lp-sheet mx-auto max-w-md">
      <div className="lp-glass-card !p-8 text-center">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full" style={{ background: 'rgba(61,112,128,0.12)' }}>
          <svg width="26" height="26" fill="none" stroke="var(--accent-teal)" strokeWidth="2" viewBox="0 0 24 24" aria-hidden>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <h1 className="text-xl font-bold lp-text-heading">{t('landingVerifyEmail.title')}</h1>
        <p className="mt-2 text-sm lp-text-muted">
          {t('landingVerifyEmail.sentTo')} <span className="font-semibold lp-text-heading">{email}</span>
        </p>

        <form onSubmit={handleVerify} className="mt-6 space-y-4 text-left">
          <label className="block">
            <span className="lp-label">{t('landingVerifyEmail.codeLabel')}</span>
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={8}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              placeholder="123456"
              className="mt-1 w-full rounded-xl border px-4 py-2.5 text-center text-2xl tracking-widest outline-none transition"
              style={{ borderColor: 'rgba(90,122,130,0.3)', background: 'rgba(255,255,255,0.7)', color: 'var(--ink-heading)' }}
              required
              autoFocus
            />
          </label>
          {error ? <p className="text-sm text-red-600">{error}</p> : null}
          <button
            type="submit"
            disabled={verifying || !code.trim()}
            className="lp-btn lp-btn-primary w-full disabled:cursor-not-allowed disabled:opacity-50"
          >
            {verifying ? t('landingVerifyEmail.verifying') : t('landingVerifyEmail.verify')}
          </button>
        </form>

        <div className="mt-5 text-sm lp-text-muted">
          {t('landingVerifyEmail.noCode')}{' '}
          <button
            type="button"
            disabled={sending || resendCooldown > 0}
            onClick={handleSendOtp}
            className="font-semibold underline disabled:cursor-not-allowed disabled:opacity-50"
            style={{ color: 'var(--accent-teal)' }}
          >
            {sending ? t('landingVerifyEmail.sending') : resendCooldown > 0 ? `${t('landingVerifyEmail.resend')} (${resendCooldown}s)` : t('landingVerifyEmail.resend')}
          </button>
        </div>
      </div>
    </div>
  )
}
