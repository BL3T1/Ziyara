import { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { setStoredToken } from '../context/AuthContext'
import { isCompanySurface, isProviderSurface } from '../config/appSurface'
import { authAPI, usersAPI, providersAPI, getApiErrorMessage } from '../services/api'
import { backendRoleToFrontend, isCompanyStaffRole, isProviderPortalRole } from '../types/auth'
import { getDashboardRouteForRole } from '../utils/routes'
import { safeRedirect } from '../utils/safeRedirect'
import type { AuthResponseDto } from '../types/api'
import type { PortalLoginError } from '../components/HomeRedirect'
import { ThemeToggleButton } from '../components/ThemeToggleButton'
import { LanguageToggleButton } from '../components/LanguageToggleButton'
import { PasswordInput } from '../components/PasswordInput'
import { useLanguage } from '../context/LanguageContext'
import { MfaChallengePage } from './MfaChallengePage'
import { loginSchema, companyLoginSchema } from '../lib/validation'

const UserIcon = () => (
  <svg className="h-5 w-5 shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
)
const LockIcon = () => (
  <svg className="h-5 w-5 shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
)

const PORTAL_ERROR_MESSAGES: Record<PortalLoginError, string> = {
  wrong_portal: 'This account cannot sign in here. Use the correct portal for your role.',
  company_staff_only: 'Company staff must use the company dashboard to sign in, not the partner portal.',
  partner_only: 'Partner accounts must use the partner portal to sign in.',
  customer_not_company: 'Customer accounts cannot use the company dashboard.',
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { setUser } = useAuth()
  const { t } = useLanguage()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
  const [loading, setLoading] = useState(false)
  const [mfaCredentials, setMfaCredentials] = useState<{ email: string; password: string } | null>(null)

  const fromPath = (location.state as { from?: { pathname: string } } | null)?.from?.pathname

  useEffect(() => {
    const portalError = (location.state as { portalError?: PortalLoginError } | null)?.portalError
    if (portalError && PORTAL_ERROR_MESSAGES[portalError]) {
      setError(PORTAL_ERROR_MESSAGES[portalError])
      navigate(safeRedirect(location.pathname, '/dashboard'), { replace: true, state: {} })
    }
  }, [location, navigate])

  // Admin handoff: provider portal opened by admin with a pre-issued token in the URL hash
  useEffect(() => {
    if (!isProviderSurface) return
    const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ''))
    const handoffToken = hashParams.get('admin_token')
    if (!handoffToken) return
    window.history.replaceState(null, '', window.location.pathname + window.location.search)
    setLoading(true)
    setStoredToken(handoffToken)
    Promise.allSettled([usersAPI.getMe(), providersAPI.getMe()])
      .then(([meResult, providerResult]) => {
        if (meResult.status !== 'fulfilled') throw new Error('invalid')
        const me = meResult.value.data as { id?: string; email?: string; role?: string; firstName?: string; lastName?: string }
        if (!me?.role) throw new Error('invalid')
        const hasPortalAccess = providerResult.status === 'fulfilled'
        const role = backendRoleToFrontend(me.role, hasPortalAccess)
        setUser({
          id: String(me.id ?? ''),
          email: me.email ?? '',
          name: [me.firstName, me.lastName].filter(Boolean).join(' ') || '',
          role,
          mustChangePassword: false,
        })
        navigate(safeRedirect(getDashboardRouteForRole(role), '/dashboard'), { replace: true })
      })
      .catch(() => {
        sessionStorage.removeItem('token')
        setLoading(false)
      })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    const schema = isCompanySurface ? companyLoginSchema : loginSchema
    const parsed = schema.safeParse({ email, password })
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
      const role = backendRoleToFrontend(data.role, data.hasPortalAccess)

      if (isCompanySurface) {
        if (role === 'provider') {
          setError(PORTAL_ERROR_MESSAGES.partner_only)
          return
        }
        if (role === 'user') {
          setError(PORTAL_ERROR_MESSAGES.customer_not_company)
          return
        }
        if (!isCompanyStaffRole(role)) {
          setError(PORTAL_ERROR_MESSAGES.wrong_portal)
          return
        }
      }

      if (isProviderSurface && !isProviderPortalRole(role)) {
        setError(PORTAL_ERROR_MESSAGES.company_staff_only)
        return
      }

      if (hasBearer) {
        setStoredToken(data.accessToken as string)
      } else {
        sessionStorage.setItem('ziyara_cookie_session', '1')
      }
      const mustChangePassword = Boolean(data.mustChangePassword)
      setUser({
        id: String(data.userId),
        email: data.email,
        name: data.fullName || '',
        role,
        mustChangePassword,
      })
      if (mustChangePassword) {
        navigate('/account/change-password', { replace: true })
        return
      }
      if (isCompanySurface && isCompanyStaffRole(role)) {
        void import('./DashboardPage')
        void import('./SalesDashboardPage')
      }
      navigate(safeRedirect(getDashboardRouteForRole(role), '/dashboard'), { replace: true })
    } catch (err: unknown) {
      // Detect MFA challenge — show TOTP input instead of generic error
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      if (code === 'MFA_CODE_REQUIRED') {
        setMfaCredentials({ email, password })
        return
      }
      setError(getApiErrorMessage(err, 'Login failed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden px-4 py-12">
      <div className="pointer-events-none fixed inset-0 bg-slate-100 dark:bg-slate-950" aria-hidden />
      <div className="pointer-events-none fixed inset-0" aria-hidden>
        <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-primary/[0.07] to-secondary/[0.08] dark:from-slate-950 dark:via-primary/[0.14] dark:to-slate-900/90" />
        <div className="absolute -left-40 -top-20 h-[28rem] w-[28rem] rounded-full bg-primary/[0.14] blur-[80px] dark:bg-primary/[0.26]" />
        <div className="absolute -right-32 -bottom-16 h-[24rem] w-[24rem] rounded-full bg-secondary/[0.18] blur-[72px] dark:bg-secondary/[0.14]" />
        <div className="absolute bottom-1/3 left-1/2 h-72 w-[32rem] -translate-x-1/2 rounded-full bg-primary/[0.06] blur-[56px] dark:bg-slate-700/30" />
        <div className="absolute right-1/4 top-1/4 h-48 w-48 rounded-full bg-secondary/[0.1] blur-[48px] dark:bg-secondary/[0.08]" />
      </div>

      <div className="fixed end-4 top-4 z-20 flex items-center gap-2">
        <LanguageToggleButton className="flex h-11 min-w-[2.75rem] items-center justify-center rounded-2xl border border-slate-200/90 bg-white/90 px-2 text-slate-600 shadow-md shadow-slate-900/[0.06] ring-1 ring-slate-900/[0.04] backdrop-blur-md outline-none transition-all hover:border-primary/25 hover:text-primary focus-visible:ring-2 focus-visible:ring-[rgb(30_77_107/0.35)] focus-visible:ring-offset-2 focus-visible:ring-offset-white active:scale-95 dark:border-slate-600/80 dark:bg-slate-800/90 dark:text-slate-300 dark:shadow-black/30 dark:ring-white/[0.06] dark:hover:border-primary/40 dark:hover:text-secondary dark:focus-visible:ring-offset-slate-900" />
        <ThemeToggleButton className="flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200/90 bg-white/90 text-slate-600 shadow-md shadow-slate-900/[0.06] ring-1 ring-slate-900/[0.04] backdrop-blur-md outline-none transition-all hover:border-primary/25 hover:text-primary focus-visible:ring-2 focus-visible:ring-[rgb(30_77_107/0.35)] focus-visible:ring-offset-2 focus-visible:ring-offset-white active:scale-95 dark:border-slate-600/80 dark:bg-slate-800/90 dark:text-slate-300 dark:shadow-black/30 dark:ring-white/[0.06] dark:hover:border-primary/40 dark:hover:text-secondary dark:focus-visible:ring-offset-slate-900" />
      </div>

      <div className="relative z-10 mb-8 flex flex-col items-center">
        <img
          src="/logo.png"
          alt="Ziyara"
          className="h-44 w-auto drop-shadow-lg transition-transform duration-300 hover:scale-[1.02] sm:h-52"
        />
      </div>

      {mfaCredentials ? (
        <MfaChallengePage
          email={mfaCredentials.email}
          password={mfaCredentials.password}
          onBack={() => setMfaCredentials(null)}
          onSuccess={(route) => navigate(safeRedirect(route, '/dashboard'), { replace: true })}
        />
      ) : null}

      {!mfaCredentials ? <div className="relative z-10 w-full max-w-md rounded-3xl border border-slate-200/90 bg-white/88 px-8 py-10 shadow-2xl shadow-slate-900/[0.1] ring-1 ring-slate-900/[0.04] backdrop-blur-2xl dark:border-slate-600/50 dark:bg-slate-900/80 dark:shadow-[0_24px_64px_-12px_rgba(0,0,0,0.7)] dark:ring-white/[0.06]" style={{ animation: 'layout-page-enter 0.45s cubic-bezier(0.2, 0.8, 0.2, 1) both' }}>
        <div
          className="pointer-events-none absolute inset-x-0 top-0 h-px rounded-t-3xl bg-gradient-to-r from-transparent via-primary/35 to-secondary/25"
          aria-hidden
        />
        <h2 className="mb-8 text-center text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
          {t('login.welcomeBack')}
        </h2>
        {error && (
          <div className="mb-4 rounded-xl border border-red-200/80 bg-red-50 px-3 py-2.5 text-sm text-red-800 dark:border-red-800/60 dark:bg-red-950/40 dark:text-red-200">
            {error}
          </div>
        )}
        {!error && fromPath && (
          <div className="mb-4 rounded-xl border border-primary/20 bg-primary/5 px-3 py-2.5 text-sm text-primary dark:border-primary/30 dark:bg-primary/10 dark:text-secondary">
            {t('login.sessionRequired')}
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
              {isCompanySurface ? t('login.username') : t('login.email')}
            </label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                <UserIcon />
              </span>
              <input
                id="email"
                type={isCompanySurface ? 'text' : 'email'}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder={isCompanySurface ? t('login.usernamePlaceholder') : t('login.emailPlaceholder')}
                autoComplete={isCompanySurface ? 'username' : 'email'}
                className="w-full rounded-xl border bg-slate-50/80 py-3 pl-11 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 dark:bg-slate-800/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800/90 dark:focus:ring-primary/25 dark:focus:border-primary/40"
                style={{ borderColor: fieldErrors.email ? '#f87171' : undefined }}
              />
            </div>
            {fieldErrors.email ? <p className="mt-1 text-xs text-red-600 dark:text-red-400">{fieldErrors.email}</p> : null}
          </div>

          <div>
            <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
              {t('login.password')}
            </label>
            <PasswordInput
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              leftSlot={
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                  <LockIcon />
                </span>
              }
              className="w-full rounded-xl border bg-slate-50/80 py-3 pl-11 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 dark:bg-slate-800/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800/90 dark:focus:ring-primary/25 dark:focus:border-primary/40"
              style={{ borderColor: fieldErrors.password ? '#f87171' : undefined }}
            />
            {fieldErrors.password ? <p className="mt-1 text-xs text-red-600 dark:text-red-400">{fieldErrors.password}</p> : null}
          </div>

          <div className="flex items-center justify-between gap-3">
            <label className="flex cursor-pointer items-center gap-2">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-500 dark:bg-slate-800"
              />
              <span className="text-sm text-slate-600 dark:text-slate-400">{t('login.rememberMe')}</span>
            </label>
            <Link
              to="/forgot-password"
              className="text-sm font-medium text-primary transition-colors hover:text-primary/80 dark:text-secondary dark:hover:text-secondary/90"
            >
              {t('login.forgotPassword')}
            </Link>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="relative w-full overflow-hidden rounded-xl py-3.5 text-sm font-bold tracking-wide text-slate-900 transition-all hover:-translate-y-0.5 hover:shadow-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-secondary focus-visible:ring-offset-2 focus-visible:ring-offset-white active:scale-[0.985] active:translate-y-0 disabled:opacity-60 dark:text-slate-900 dark:focus-visible:ring-offset-slate-900"
            style={{
              background: 'linear-gradient(145deg, #d4b88a 0%, #c09a6a 45%, #a87e50 100%)',
              boxShadow: '0 1px 0 rgba(255,255,255,0.2) inset, 0 4px 16px rgba(180,140,80,0.35), 0 12px 32px rgba(160,120,64,0.2)',
            }}
          >
            <span className="relative z-10">{loading ? t('login.signingIn') : t('login.signIn')}</span>
          </button>
        </form>
      </div> : null}
    </div>
  )
}
