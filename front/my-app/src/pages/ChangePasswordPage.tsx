import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { usersAPI, getApiErrorMessage } from '../services/api'
import { useLanguage } from '../context/LanguageContext'
import { getDashboardRouteForRole } from '../utils/routes'
import { PasswordInput } from '../components/PasswordInput'

const LockIcon = () => (
  <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="18" height="11" x="3" y="11" rx="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
)

export function ChangePasswordPage() {
  const { user, setUser, logout } = useAuth()
  const { t } = useLanguage()
  const navigate = useNavigate()
  const isForced = Boolean(user?.mustChangePassword)

  const [current, setCurrent] = useState('')   // not used when isForced
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (next !== confirm) {
      setError(t('landingAuth.passwordsMismatch'))
      return
    }
    setLoading(true)
    try {
      await usersAPI.changePassword({ currentPassword: isForced ? undefined : current, newPassword: next })
      setSuccess(true)
      if (user) {
        setUser({ ...user, mustChangePassword: false })
      }
      setTimeout(() => {
        const route = user ? getDashboardRouteForRole(user.role) : '/dashboard'
        navigate(route, { replace: true })
      }, 1500)
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, t('landingAuth.failedToReset')))
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
      </div>

      <div className="relative z-10 w-full max-w-md rounded-3xl border border-slate-200/90 bg-white/88 px-8 py-10 shadow-2xl shadow-slate-900/[0.1] ring-1 ring-slate-900/[0.04] backdrop-blur-2xl dark:border-slate-600/50 dark:bg-slate-900/80 dark:shadow-[0_24px_64px_-12px_rgba(0,0,0,0.7)] dark:ring-white/[0.06]">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-px rounded-t-3xl bg-gradient-to-r from-transparent via-primary/35 to-secondary/25" aria-hidden />

        <div className="mb-6 flex flex-col items-center gap-3">
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary dark:bg-primary/20">
            <LockIcon />
          </span>
          <h2 className="text-center text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
            {t('common.changePassword')}
          </h2>
          {isForced && (
            <p className="text-center text-sm text-amber-600 dark:text-amber-400">
              {t('common.changePasswordForced')}
            </p>
          )}
        </div>

        {success ? (
          <div className="rounded-xl border border-green-200/80 bg-green-50 px-3 py-2.5 text-center text-sm text-green-800 dark:border-green-800/60 dark:bg-green-950/40 dark:text-green-200">
            {t('landingAuth.passwordResetSuccess')}
          </div>
        ) : (
          <>
            {error && (
              <div className="mb-4 rounded-xl border border-red-200/80 bg-red-50 px-3 py-2.5 text-sm text-red-800 dark:border-red-800/60 dark:bg-red-950/40 dark:text-red-200">
                {error}
              </div>
            )}
            <form onSubmit={handleSubmit} className="space-y-5">
              {!isForced && (
                <div>
                  <label htmlFor="current" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                    {t('landingAuth.currentPassword')}
                  </label>
                  <PasswordInput
                    id="current"
                    value={current}
                    onChange={(e) => setCurrent(e.target.value)}
                    required
                    placeholder="••••••••"
                    leftSlot={
                      <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                        <LockIcon />
                      </span>
                    }
                    className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-3 pl-11 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus:bg-white focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/20 dark:border-slate-600 dark:bg-slate-800/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800/90 dark:focus:border-primary/40 dark:focus:ring-primary/25"
                  />
                </div>
              )}

              <div>
                <label htmlFor="next" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                  {t('landingAuth.newPassword')}
                </label>
                <PasswordInput
                  id="next"
                  value={next}
                  onChange={(e) => setNext(e.target.value)}
                  required
                  placeholder="••••••••"
                  leftSlot={
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                      <LockIcon />
                    </span>
                  }
                  className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-3 pl-11 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus:bg-white focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/20 dark:border-slate-600 dark:bg-slate-800/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800/90 dark:focus:border-primary/40 dark:focus:ring-primary/25"
                />
              </div>

              <div>
                <label htmlFor="confirm" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                  {t('landingAuth.confirmPassword')}
                </label>
                <PasswordInput
                  id="confirm"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  required
                  placeholder="••••••••"
                  leftSlot={
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                      <LockIcon />
                    </span>
                  }
                  className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-3 pl-11 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus:bg-white focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/20 dark:border-slate-600 dark:bg-slate-800/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800/90 dark:focus:border-primary/40 dark:focus:ring-primary/25"
                />
              </div>

              <div className="flex flex-col gap-2 pt-1">
                <button
                  type="submit"
                  disabled={loading}
                  className="relative w-full overflow-hidden rounded-xl py-3.5 text-sm font-bold tracking-wide text-slate-900 transition-all hover:-translate-y-0.5 hover:shadow-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-secondary focus-visible:ring-offset-2 focus-visible:ring-offset-white active:scale-[0.985] active:translate-y-0 disabled:opacity-60 dark:text-slate-900 dark:focus-visible:ring-offset-slate-900"
                  style={{
                    background: 'linear-gradient(145deg, #d4b88a 0%, #c09a6a 45%, #a87e50 100%)',
                    boxShadow: '0 1px 0 rgba(255,255,255,0.2) inset, 0 4px 16px rgba(180,140,80,0.35)',
                  }}
                >
                  {loading ? t('landingAuth.resetting') : t('landingAuth.resetPasswordSubmit')}
                </button>
                {!isForced && (
                  <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="w-full rounded-xl py-2.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
                  >
                    {t('landingAuth.cancel')}
                  </button>
                )}
                {isForced && (
                  <button
                    type="button"
                    onClick={() => { logout(); navigate('/login', { replace: true }) }}
                    className="w-full rounded-xl py-2.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
                  >
                    {t('common.logOut')}
                  </button>
                )}
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  )
}
