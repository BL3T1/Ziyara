import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { authAPI, getApiErrorMessage } from '../../services/api'
import { useLanguage } from '../../context/LanguageContext'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import './landing-public.css'

export function LandingForgotPasswordPage() {
  const { t, locale } = useLanguage()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  useEffect(() => {
    document.documentElement.classList.remove('dark')
    document.title = `${t('landingAuth.titleForgotPassword')} — Ziyara`
  }, [locale, t])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authAPI.forgotPassword({ email: email.trim() })
      setSent(true)
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Request failed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="landing-parallax-root lp-www-root flex min-h-screen flex-col">
      {/* Ambient orbs */}
      <div aria-hidden className="pointer-events-none fixed inset-0 overflow-hidden">
        <div style={{ position: 'absolute', top: '-18%', right: '-12%', width: 'clamp(320px,45vw,600px)', height: 'clamp(320px,45vw,600px)', borderRadius: '50%', background: 'radial-gradient(circle, rgba(126,196,232,0.22) 0%, transparent 68%)', filter: 'blur(48px)' }} />
        <div style={{ position: 'absolute', bottom: '-14%', left: '-10%', width: 'clamp(280px,40vw,540px)', height: 'clamp(280px,40vw,540px)', borderRadius: '50%', background: 'radial-gradient(circle, rgba(200,160,120,0.18) 0%, transparent 68%)', filter: 'blur(52px)' }} />
      </div>

      <div className="lp-www-inner flex flex-1 flex-col items-center justify-center py-12">
        <div className="mb-8 flex w-full max-w-md items-center justify-between gap-3">
          <Link to="/login" className="lp-link-quiet inline-flex items-center gap-1.5 text-sm font-medium">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden><path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/></svg>
            {t('landingAuth.backToLogin')}
          </Link>
          <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
        </div>

        <div className="lp-auth-card w-full max-w-md" style={{ animation: 'lp-card-enter 0.6s cubic-bezier(0.22,1,0.36,1) both' }}>
          <div className="mb-6 flex justify-center">
            <div className="lp-auth-logo-wrap">
              <img src="/logo.png" alt="" className="lp-auth-logo" width={160} height={48} />
            </div>
          </div>

          <h1 className="lp-h1 text-center" style={{ fontSize: 'clamp(1.35rem,3vw,1.6rem)' }}>
            {t('landingAuth.titleForgotPassword')}
          </h1>
          <p className="mt-2 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            {t('landingAuth.subForgotPassword')}
          </p>

          {error ? (
            <p className="mt-5 rounded-2xl border border-red-200/80 bg-red-50/90 px-4 py-3 text-center text-sm text-red-900">{error}</p>
          ) : null}

          {sent ? (
            <p className="mt-5 rounded-2xl border border-emerald-200/80 bg-emerald-50/90 px-4 py-3 text-center text-sm text-emerald-900">
              {t('landingAuth.resetSentHint')}
            </p>
          ) : (
            <form onSubmit={handleSubmit} className="mt-7 space-y-4">
              <div>
                <label htmlFor="landing-forgot-email" className="lp-field-label">
                  {t('landingAuth.email')}
                </label>
                <input
                  id="landing-forgot-email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="you@example.com"
                  className="lp-input"
                />
              </div>
              <button type="submit" disabled={loading} className="lp-btn lp-btn-primary w-full" style={{ borderRadius: 14 }}>
                {loading ? t('landingAuth.sendingReset') : t('landingAuth.sendResetLink')}
              </button>
            </form>
          )}

          <p className="mt-7 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            <Link
              to="/login"
              className="font-semibold"
              style={{ color: 'var(--accent-teal)', textDecoration: 'underline', textDecorationColor: 'var(--accent-teal)', textUnderlineOffset: '2px' }}
            >
              {t('landingAuth.backToLogin')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
