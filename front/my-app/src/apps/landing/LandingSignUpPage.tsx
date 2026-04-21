import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authAPI, getApiErrorMessage } from '../../services/api'
import { useLanguage } from '../../context/LanguageContext'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import './landing-public.css'

export function LandingSignUpPage() {
  const { t, locale } = useLanguage()
  const navigate = useNavigate()
  const { user, clearAuth } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    document.documentElement.classList.remove('dark')
    document.title = `${t('landingAuth.titleSignup')} — Ziyara`
  }, [locale, t])

  useEffect(() => {
    if (user?.role === 'user') {
      navigate('/services', { replace: true })
    } else if (user) {
      clearAuth()
    }
  }, [user, navigate, clearAuth])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (password.length < 6) {
      setError(t('landingAuth.passwordTooShort'))
      return
    }
    if (password !== confirm) {
      setError(t('landingAuth.passwordsMismatch'))
      return
    }
    setLoading(true)
    try {
      await authAPI.register({
        email: email.trim(),
        password,
        phone: phone.trim() || undefined,
        role: 'CUSTOMER',
      })
      navigate('/login?registered=1', { replace: true })
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Registration failed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="landing-parallax-root lp-www-root flex min-h-screen flex-col">
      <div className="lp-www-inner flex flex-1 flex-col items-center justify-center py-12">
        <div className="mb-6 flex w-full max-w-md items-center justify-between gap-3">
          <Link to="/" className="lp-link-quiet text-sm font-medium">
            ← {t('landingAuth.backHome')}
          </Link>
          <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
        </div>

        <div className="lp-sheet w-full max-w-md !rounded-[28px] !p-8">
          <div className="mb-2 flex justify-center">
            <div className="lp-auth-logo-wrap">
              <img src="/logo.png" alt="" className="lp-auth-logo" width={160} height={48} />
            </div>
          </div>
          <h1 className="lp-h1 text-center !text-2xl">{t('landingAuth.titleSignup')}</h1>
          <p className="lp-body mt-2 text-center text-sm">{t('landingAuth.subSignup')}</p>

          {error ? (
            <p className="mt-4 rounded-xl border border-red-200/80 bg-red-50/90 px-3 py-2.5 text-center text-sm text-red-900">{error}</p>
          ) : null}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label htmlFor="landing-signup-email" className="mb-1 block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingAuth.email')}
              </label>
              <input
                id="landing-signup-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              />
            </div>
            <div>
              <label htmlFor="landing-signup-phone" className="mb-1 block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingAuth.phoneOptional')}
              </label>
              <input
                id="landing-signup-phone"
                type="tel"
                autoComplete="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              />
            </div>
            <div>
              <label htmlFor="landing-signup-password" className="mb-1 block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingAuth.password')}
              </label>
              <input
                id="landing-signup-password"
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
                className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              />
            </div>
            <div>
              <label htmlFor="landing-signup-confirm" className="mb-1 block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingAuth.confirmPassword')}
              </label>
              <input
                id="landing-signup-confirm"
                type="password"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
                className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              />
            </div>
            <button type="submit" disabled={loading} className="lp-btn lp-btn-primary w-full py-3 text-center">
              {loading ? t('landingAuth.registering') : t('landingAuth.createAccount')}
            </button>
          </form>

          <p className="mt-6 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            {t('landingAuth.hasAccount')}{' '}
            <Link to="/login" className="font-semibold underline decoration-[var(--accent-teal)] underline-offset-2" style={{ color: 'var(--accent-teal)' }}>
              {t('landingAuth.linkLogin')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
