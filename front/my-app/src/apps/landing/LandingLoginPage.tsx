import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth, setStoredToken } from '../../context/AuthContext'
import { VITE_COMPANY_APP_URL, VITE_PROVIDER_APP_URL } from '../../config/appSurface'
import { authAPI, getApiErrorMessage } from '../../services/api'
import type { AuthResponseDto } from '../../types/api'
import { backendRoleToFrontend, isCompanyStaffRole, isProviderPortalRole } from '../../types/auth'
import { safeRedirect } from '../../utils/safeRedirect'
import { useLanguage } from '../../context/LanguageContext'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import { loginSchema } from '../../lib/validation'
import './landing-public.css'

function staffDashboardHint(): string {
  const b = VITE_COMPANY_APP_URL.replace(/\/$/, '')
  return b ? `${b}/login` : '/login'
}

function partnerPortalHint(): string {
  const b = VITE_PROVIDER_APP_URL.replace(/\/$/, '')
  return b ? `${b}/login` : '/login'
}

export function LandingLoginPage() {
  const { t, locale } = useLanguage()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { setUser, user, clearAuth } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(true)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    document.documentElement.classList.remove('dark')
    document.title = `${t('landingAuth.titleLogin')} — Ziyara`
  }, [locale, t])

  useEffect(() => {
    if (!user) return
    if (user.role === 'user') {
      navigate(safeRedirect(searchParams.get('next'), '/services'), { replace: true })
      return
    }
    clearAuth()
  }, [user, navigate, searchParams, clearAuth])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    const parsed = loginSchema.safeParse({ email, password })
    if (!parsed.success) {
      const errs = parsed.error.flatten().fieldErrors
      setFieldErrors({ email: errs.email?.[0], password: errs.password?.[0] })
      return
    }
    setLoading(true)
    try {
      const res = await authAPI.login({ email, password, rememberMe })
      const data = res.data as AuthResponseDto
      const hasBearer = Boolean(data?.accessToken)
      const hasCookieSession = Boolean(data?.userId) && !hasBearer
      if (!hasBearer && !hasCookieSession) {
        setError('Invalid response from server')
        return
      }
      const role = backendRoleToFrontend(data.role)

      if (isProviderPortalRole(role)) {
        setError(`${t('landingAuth.onlyPartners')} ${partnerPortalHint()}`)
        return
      }
      if (isCompanyStaffRole(role)) {
        setError(`${t('landingAuth.onlyTravelers')} ${staffDashboardHint()}`)
        return
      }
      if (role !== 'user') {
        setError(t('landingAuth.onlyTravelers'))
        return
      }

      if (hasBearer) {
        setStoredToken(data.accessToken as string)
      } else {
        localStorage.setItem('ziyara_cookie_session', '1')
      }
      setUser({
        id: String(data.userId),
        email: data.email,
        name: data.fullName || data.email,
        role,
      })
      const next = safeRedirect(searchParams.get('next'), '/services')
      navigate(next, { replace: true })
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Login failed'))
    } finally {
      setLoading(false)
    }
  }

  const registered = searchParams.get('registered') === '1'

  return (
    <div className="landing-parallax-root lp-www-root flex min-h-screen flex-col">
      {/* Ambient orbs */}
      <div aria-hidden className="pointer-events-none fixed inset-0 overflow-hidden">
        <div style={{
          position: 'absolute', top: '-18%', right: '-12%',
          width: 'clamp(320px, 45vw, 600px)', height: 'clamp(320px, 45vw, 600px)',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(126,196,232,0.22) 0%, transparent 68%)',
          filter: 'blur(48px)',
        }} />
        <div style={{
          position: 'absolute', bottom: '-14%', left: '-10%',
          width: 'clamp(280px, 40vw, 540px)', height: 'clamp(280px, 40vw, 540px)',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(200,160,120,0.18) 0%, transparent 68%)',
          filter: 'blur(52px)',
        }} />
        <div style={{
          position: 'absolute', top: '40%', left: '30%',
          width: 'clamp(200px, 28vw, 380px)', height: 'clamp(200px, 28vw, 380px)',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(61,112,128,0.09) 0%, transparent 70%)',
          filter: 'blur(40px)',
        }} />
      </div>

      <div className="lp-www-inner flex flex-1 flex-col items-center justify-center py-12">
        {/* Top row: back + language */}
        <div className="mb-8 flex w-full max-w-md items-center justify-between gap-3">
          <Link to="/" className="lp-link-quiet inline-flex items-center gap-1.5 text-sm font-medium">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden>
              <path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            {t('landingAuth.backHome')}
          </Link>
          <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
        </div>

        {/* Auth card */}
        <div
          className="lp-auth-card w-full max-w-md"
          style={{ animation: 'lp-card-enter 0.6s cubic-bezier(0.22,1,0.36,1) both' }}
        >
          {/* Logo */}
          <div className="mb-6 flex justify-center">
            <div className="lp-auth-logo-wrap">
              <img src="/logo.png" alt="" className="lp-auth-logo" width={160} height={48} />
            </div>
          </div>

          <h1 className="lp-h1 text-center" style={{ fontSize: 'clamp(1.35rem, 3vw, 1.6rem)' }}>
            {t('landingAuth.titleLogin')}
          </h1>
          <p className="mt-2 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            {t('landingAuth.subLogin')}
          </p>

          {registered ? (
            <p className="mt-5 rounded-2xl border border-emerald-200/80 bg-emerald-50/90 px-4 py-3 text-center text-sm text-emerald-900">
              {t('landingAuth.registeredHint')}
            </p>
          ) : null}
          {error ? (
            <p className="mt-5 rounded-2xl border border-red-200/80 bg-red-50/90 px-4 py-3 text-center text-sm text-red-900">{error}</p>
          ) : null}

          <form onSubmit={handleSubmit} className="mt-7 space-y-4">
            <div>
              <label htmlFor="landing-login-email" className="lp-field-label">
                {t('landingAuth.email')}
              </label>
              <input
                id="landing-login-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="lp-input"
                style={fieldErrors.email ? { borderColor: '#f87171' } : undefined}
              />
              {fieldErrors.email ? <p className="mt-1.5 text-xs text-red-600">{fieldErrors.email}</p> : null}
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between gap-2">
                <label htmlFor="landing-login-password" className="lp-field-label" style={{ marginBottom: 0 }}>
                  {t('landingAuth.password')}
                </label>
                <Link
                  to="/forgot-password"
                  className="text-xs font-semibold"
                  style={{ color: 'var(--accent-teal)', textDecoration: 'underline', textDecorationColor: 'var(--accent-teal)', textUnderlineOffset: '2px' }}
                >
                  {t('landingAuth.forgotPassword')}
                </Link>
              </div>
              <input
                id="landing-login-password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="lp-input"
                style={fieldErrors.password ? { borderColor: '#f87171' } : undefined}
              />
              {fieldErrors.password ? <p className="mt-1.5 text-xs text-red-600">{fieldErrors.password}</p> : null}
            </div>

            <label className="flex cursor-pointer items-center gap-2.5 text-sm" style={{ color: 'var(--ink-muted)' }}>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300"
              />
              {t('landingAuth.rememberMe')}
            </label>

            <button
              type="submit"
              disabled={loading}
              className="lp-btn lp-btn-primary w-full"
              style={{ borderRadius: 14, marginTop: 4 }}
            >
              {loading ? t('landingAuth.signingIn') : t('landingAuth.signIn')}
            </button>
          </form>

          <p className="mt-7 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            {t('landingAuth.noAccount')}{' '}
            <Link
              to="/signup"
              className="font-semibold"
              style={{ color: 'var(--accent-teal)', textDecoration: 'underline', textDecorationColor: 'var(--accent-teal)', textUnderlineOffset: '2px' }}
            >
              {t('landingAuth.linkSignup')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
