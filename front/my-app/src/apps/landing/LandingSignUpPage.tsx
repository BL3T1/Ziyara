import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authAPI, getApiErrorMessage } from '../../services/api'
import { useLanguage } from '../../context/LanguageContext'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import { signUpSchema } from '../../lib/validation'
import './landing-public.css'

export function LandingSignUpPage() {
  const { t, locale } = useLanguage()
  const navigate = useNavigate()
  const { user, clearAuth } = useAuth()
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [confirm, setConfirm] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{ firstName?: string; lastName?: string; email?: string; password?: string; confirmPassword?: string }>({})
  const [loading, setLoading] = useState(false)

  function passwordStrength(pw: string): { score: number; label: string; color: string } {
    if (!pw) return { score: 0, label: '', color: 'transparent' }
    let score = 0
    if (pw.length >= 8) score++
    if (pw.length >= 12) score++
    if (/[A-Z]/.test(pw)) score++
    if (/[0-9]/.test(pw)) score++
    if (/[^A-Za-z0-9]/.test(pw)) score++
    if (score <= 1) return { score, label: t('landingAuth.strengthWeak'), color: 'var(--status-weak)' }
    if (score <= 2) return { score, label: t('landingAuth.strengthFair'), color: 'var(--status-fair)' }
    if (score <= 3) return { score, label: t('landingAuth.strengthGood'), color: 'var(--status-good)' }
    return { score, label: t('landingAuth.strengthStrong'), color: 'var(--status-strong)' }
  }
  const strength = passwordStrength(password)

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
    setFieldErrors({})
    const parsed = signUpSchema.safeParse({ firstName, lastName, email, password, confirmPassword: confirm })
    if (!parsed.success) {
      const errs = parsed.error.flatten().fieldErrors
      const formErrors = parsed.error.flatten().formErrors
      setFieldErrors({
        firstName: errs.firstName?.[0],
        lastName: errs.lastName?.[0],
        email: errs.email?.[0],
        password: errs.password?.[0],
        confirmPassword: errs.confirmPassword?.[0] ?? formErrors[0],
      })
      return
    }
    setLoading(true)
    try {
      await authAPI.register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
        phone: phone.trim() || undefined,
        role: 'CUSTOMER',
      })
      navigate(`/verify-email?email=${encodeURIComponent(email.trim())}`, { replace: true })
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
              <img src="/logo.png" alt="" className="lp-auth-logo" width={360} height={120} />
            </div>
          </div>
          <h1 className="lp-h1 text-center !text-2xl">{t('landingAuth.titleSignup')}</h1>
          <p className="lp-body mt-2 text-center text-sm">{t('landingAuth.subSignup')}</p>

          {error ? (
            <p className="mt-4 rounded-xl border border-red-200/80 bg-red-50/90 px-3 py-2.5 text-center text-sm text-red-900">{error}</p>
          ) : null}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="landing-signup-firstname" className="mb-1 block text-xs font-semibold uppercase tracking-wide lp-text-muted">
                  {t('landingAuth.firstName')}
                </label>
                <input
                  id="landing-signup-firstname"
                  type="text"
                  autoComplete="given-name"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                  style={{ borderColor: fieldErrors.firstName ? '#f87171' : 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
                />
                {fieldErrors.firstName ? <p className="mt-1 text-xs text-red-600">{fieldErrors.firstName}</p> : null}
              </div>
              <div>
                <label htmlFor="landing-signup-lastname" className="mb-1 block text-xs font-semibold uppercase tracking-wide lp-text-muted">
                  {t('landingAuth.lastName')}
                </label>
                <input
                  id="landing-signup-lastname"
                  type="text"
                  autoComplete="family-name"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                  style={{ borderColor: fieldErrors.lastName ? '#f87171' : 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
                />
                {fieldErrors.lastName ? <p className="mt-1 text-xs text-red-600">{fieldErrors.lastName}</p> : null}
              </div>
            </div>
            <div>
              <label htmlFor="landing-signup-email" className="mb-1 block text-xs font-semibold uppercase tracking-wide lp-text-muted">
                {t('landingAuth.email')}
              </label>
              <input
                id="landing-signup-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: fieldErrors.email ? '#f87171' : 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              />
              {fieldErrors.email ? <p className="mt-1 text-xs text-red-600">{fieldErrors.email}</p> : null}
            </div>
            <div>
              <label htmlFor="landing-signup-phone" className="mb-1 block text-xs font-semibold uppercase tracking-wide lp-text-muted">
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
              <label htmlFor="landing-signup-password" className="mb-1 block text-xs font-semibold uppercase tracking-wide lp-text-muted">
                {t('landingAuth.password')}
              </label>
              <div className="relative">
                <input
                  id="landing-signup-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full rounded-xl border px-3 py-2.5 pe-10 text-sm outline-none focus:ring-2"
                  style={{ borderColor: fieldErrors.password ? '#f87171' : 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
                />
                <button
                  type="button"
                  tabIndex={-1}
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute inset-y-0 end-0 flex items-center px-3 lp-text-faint bg-transparent border-none cursor-pointer"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
              {/* Strength meter */}
              {password && (
                <div className="mt-2 space-y-1">
                  <div className="flex gap-1">
                    {[1, 2, 3, 4].map((i) => (
                      <div key={i} className="h-1 flex-1 rounded-full transition-colors duration-200"
                        style={{ background: i <= strength.score ? strength.color : 'rgba(90,100,110,0.15)' }} />
                    ))}
                  </div>
                  <p className="text-xs font-medium" style={{ color: strength.color }}>{strength.label}</p>
                </div>
              )}
              {fieldErrors.password ? <p className="mt-1 text-xs text-red-600">{fieldErrors.password}</p> : null}
            </div>
            <div>
              <label htmlFor="landing-signup-confirm" className="mb-1 block text-xs font-semibold uppercase tracking-wide lp-text-muted">
                {t('landingAuth.confirmPassword')}
              </label>
              <div className="relative">
                <input
                  id="landing-signup-confirm"
                  type={showConfirm ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  className="w-full rounded-xl border px-3 py-2.5 pe-10 text-sm outline-none focus:ring-2"
                  style={{ borderColor: fieldErrors.confirmPassword ? '#f87171' : 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
                />
                <button
                  type="button"
                  tabIndex={-1}
                  onClick={() => setShowConfirm((v) => !v)}
                  className="absolute inset-y-0 end-0 flex items-center px-3 lp-text-faint bg-transparent border-none cursor-pointer"
                  aria-label={showConfirm ? 'Hide password' : 'Show password'}
                >
                  {showConfirm ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
              {fieldErrors.confirmPassword ? <p className="mt-1 text-xs text-red-600">{fieldErrors.confirmPassword}</p> : null}
            </div>
            <button type="submit" disabled={loading} className="lp-btn lp-btn-primary w-full py-3 text-center">
              {loading ? t('landingAuth.registering') : t('landingAuth.createAccount')}
            </button>
          </form>

          <p className="mt-6 text-center text-sm lp-text-muted">
            {t('landingAuth.hasAccount')}{' '}
            <Link to="/login" className="font-semibold underline decoration-[var(--accent-teal)] underline-offset-2 lp-text-accent">
              {t('landingAuth.linkLogin')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
