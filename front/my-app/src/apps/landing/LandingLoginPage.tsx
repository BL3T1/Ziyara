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
          <h1 className="lp-h1 text-center !text-2xl">{t('landingAuth.titleLogin')}</h1>
          <p className="lp-body mt-2 text-center text-sm">{t('landingAuth.subLogin')}</p>

          {registered ? (
            <p className="mt-4 rounded-xl border border-emerald-200/80 bg-emerald-50/90 px-3 py-2.5 text-center text-sm text-emerald-900">
              {t('landingAuth.registeredHint')}
            </p>
          ) : null}

          {error ? (
            <p className="mt-4 rounded-xl border border-red-200/80 bg-red-50/90 px-3 py-2.5 text-center text-sm text-red-900">{error}</p>
          ) : null}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label htmlFor="landing-login-email" className="mb-1 block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                {t('landingAuth.email')}
              </label>
              <input
                id="landing-login-email"
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
              <div className="mb-1 flex items-center justify-between gap-2">
                <label htmlFor="landing-login-password" className="block text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--ink-muted)' }}>
                  {t('landingAuth.password')}
                </label>
                <Link to="/forgot-password" className="text-xs font-semibold underline decoration-[var(--accent-teal)] underline-offset-2" style={{ color: 'var(--accent-teal)' }}>
                  {t('landingAuth.forgotPassword')}
                </Link>
              </div>
              <input
                id="landing-login-password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full rounded-xl border px-3 py-2.5 text-sm outline-none focus:ring-2"
                style={{ borderColor: 'rgba(90, 122, 130, 0.25)', color: 'var(--ink-heading)' }}
              />
            </div>
            <label className="flex cursor-pointer items-center gap-2 text-sm" style={{ color: 'var(--ink-muted)' }}>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300"
              />
              {t('landingAuth.rememberMe')}
            </label>
            <button type="submit" disabled={loading} className="lp-btn lp-btn-primary w-full py-3 text-center">
              {loading ? t('landingAuth.signingIn') : t('landingAuth.signIn')}
            </button>
          </form>

          <p className="mt-6 text-center text-sm" style={{ color: 'var(--ink-muted)' }}>
            {t('landingAuth.noAccount')}{' '}
            <Link to="/signup" className="font-semibold underline decoration-[var(--accent-teal)] underline-offset-2" style={{ color: 'var(--accent-teal)' }}>
              {t('landingAuth.linkSignup')}
            </Link>
          </p>
        </div>

      </div>
    </div>
  )
}
