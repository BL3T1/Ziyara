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
      <div className="lp-www-inner flex flex-1 flex-col items-center justify-center py-12">
        <div className="mb-6 flex w-full max-w-md items-center justify-between gap-3">
          <Link to="/login" className="lp-link-quiet text-sm font-medium">
            ← {t('landingAuth.backToLogin')}
          </Link>
          <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
        </div>

        <div className="lp-sheet w-full max-w-md !rounded-[28px] !p-8">
          <div className="mb-2 flex justify-center">
            <div className="inline-flex rounded-2xl bg-white/95 px-5 py-2 shadow-sm ring-1 ring-slate-900/[0.06]">
              <img src="/logo.png" alt="" className="h-14 w-auto" width={160} height={48} />
            </div>
          </div>
          <h1 className="lp-h1 text-center !text-2xl">{t('landingAuth.titleForgotPassword')}</h1>
          <p className="lp-body mt-2 text-center text-sm">{t('landingAuth.subForgotPassword')}</p>

          {error ? (
            <p className="mt-4 rounded-xl border border-red-200/80 bg-red-50/90 px-3 py-2.5 text-center text-sm text-red-900">{error}</p>
          ) : null}

          {sent ? (
            <p className="mt-4 rounded-xl border border-emerald-200/80 bg-emerald-50/90 px-3 py-2.5 text-center text-sm text-emerald-900">
              {t('landingAuth.resetSentHint')}
            </p>
          ) : (
            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <div>
                <label htmlFor="landing-forgot-email" className="mb-1 block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                  {t('landingAuth.email')}
                </label>
                <input
                  id="landing-forgot-email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                  style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
                />
              </div>
              <button type="submit" disabled={loading} className="lp-btn lp-btn-primary w-full py-3 text-center">
                {loading ? t('landingAuth.sendingReset') : t('landingAuth.sendResetLink')}
              </button>
            </form>
          )}

          <p className="mt-6 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            <Link to="/login" className="font-semibold underline decoration-[var(--accent-teal)] underline-offset-2" style={{ color: 'var(--accent-teal)' }}>
              {t('landingAuth.backToLogin')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
