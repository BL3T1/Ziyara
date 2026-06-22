import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authAPI, getApiErrorMessage } from '../services/api'
import { useLanguage } from '../context/LanguageContext'
import { ThemeToggleButton } from '../components/ThemeToggleButton'
import { LanguageToggleButton } from '../components/LanguageToggleButton'

const MailIcon = () => (
  <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="20" height="16" x="2" y="4" rx="2" />
    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
  </svg>
)

export function ForgotPasswordPage() {
  const { t } = useLanguage()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim()) return
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
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden px-4 py-12">
      <div className="pointer-events-none fixed inset-0 bg-slate-100 dark:bg-slate-950" aria-hidden />
      <div className="pointer-events-none fixed inset-0" aria-hidden>
        <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-primary/[0.07] to-secondary/[0.08] dark:from-slate-950 dark:via-primary/[0.14] dark:to-slate-900/90" />
        <div className="absolute -left-40 -top-20 h-[28rem] w-[28rem] rounded-full bg-primary/[0.14] blur-[80px] dark:bg-primary/[0.26]" />
        <div className="absolute -right-32 -bottom-16 h-[24rem] w-[24rem] rounded-full bg-secondary/[0.18] blur-[72px] dark:bg-secondary/[0.14]" />
        <div className="absolute right-1/4 top-1/4 h-48 w-48 rounded-full bg-secondary/[0.1] blur-[48px] dark:bg-secondary/[0.08]" />
      </div>

      <div className="fixed end-4 top-4 z-20 flex items-center gap-2">
        <LanguageToggleButton className="flex h-11 min-w-[2.75rem] items-center justify-center rounded-2xl border border-slate-200/90 bg-white/90 px-2 text-slate-600 shadow-md shadow-slate-900/[0.06] ring-1 ring-slate-900/[0.04] backdrop-blur-md outline-none transition-all hover:border-primary/25 hover:text-primary focus-visible:ring-2 focus-visible:ring-[rgb(30_77_107/0.35)] focus-visible:ring-offset-2 dark:border-slate-600/80 dark:bg-slate-800/90 dark:text-slate-300 dark:shadow-black/30 dark:ring-white/[0.06] dark:hover:text-secondary" />
        <ThemeToggleButton className="flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200/90 bg-white/90 text-slate-600 shadow-md shadow-slate-900/[0.06] ring-1 ring-slate-900/[0.04] backdrop-blur-md outline-none transition-all hover:border-primary/25 hover:text-primary focus-visible:ring-2 focus-visible:ring-[rgb(30_77_107/0.35)] focus-visible:ring-offset-2 dark:border-slate-600/80 dark:bg-slate-800/90 dark:text-slate-300 dark:shadow-black/30 dark:ring-white/[0.06]" />
      </div>

      <div className="relative z-10 mb-8 flex flex-col items-center">
        <img src="/logo.png" alt="Ziyara" className="h-44 w-auto drop-shadow-lg sm:h-52" />
      </div>

      <div
        className="relative z-10 w-full max-w-md rounded-3xl border border-slate-200/90 bg-white/88 px-8 py-10 shadow-2xl shadow-slate-900/[0.1] ring-1 ring-slate-900/[0.04] backdrop-blur-2xl dark:border-slate-600/50 dark:bg-slate-900/80 dark:shadow-[0_24px_64px_-12px_rgba(0,0,0,0.7)] dark:ring-white/[0.06]"
        style={{ animation: 'layout-page-enter 0.45s cubic-bezier(0.2, 0.8, 0.2, 1) both' }}
      >
        <div className="pointer-events-none absolute inset-x-0 top-0 h-px rounded-t-3xl bg-gradient-to-r from-transparent via-primary/35 to-secondary/25" aria-hidden />

        <Link
          to="/login"
          className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-200"
        >
          <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden>
            <path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          {t('landingAuth.backToLogin')}
        </Link>

        <h2 className="mb-2 text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
          {t('landingAuth.titleForgotPassword')}
        </h2>
        <p className="mb-6 text-sm text-slate-500 dark:text-slate-400">
          {t('landingAuth.subForgotPassword')}
        </p>

        {error && (
          <div className="mb-4 rounded-xl border border-red-200/80 bg-red-50 px-3 py-2.5 text-sm text-red-800 dark:border-red-800/60 dark:bg-red-950/40 dark:text-red-200">
            {error}
          </div>
        )}

        {sent ? (
          <div className="rounded-xl border border-emerald-200/80 bg-emerald-50 px-4 py-4 text-sm text-emerald-800 dark:border-emerald-800/60 dark:bg-emerald-950/30 dark:text-emerald-300">
            {t('landingAuth.resetSentHint')}
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="forgot-email" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                {t('landingAuth.email')}
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500">
                  <MailIcon />
                </span>
                <input
                  id="forgot-email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="you@example.com"
                  className="w-full rounded-xl border bg-slate-50/80 py-3 pl-11 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 dark:bg-slate-800/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800/90 dark:focus:ring-primary/25"
                />
              </div>
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
              <span className="relative z-10">{loading ? t('landingAuth.sendingReset') : t('landingAuth.sendResetLink')}</span>
            </button>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-slate-500 dark:text-slate-400">
          <Link to="/login" className="font-semibold text-primary transition-colors hover:text-primary/80 dark:text-secondary dark:hover:text-secondary/90">
            {t('landingAuth.backToLogin')}
          </Link>
        </p>
      </div>
    </div>
  )
}
