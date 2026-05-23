import { useRef, useState, type ClipboardEvent, type KeyboardEvent } from 'react'
import { authAPI, getApiErrorMessage } from '../services/api'
import { setStoredToken } from '../context/AuthContext'
import { useAuth } from '../context/AuthContext'
import { backendRoleToFrontend } from '../types/auth'
import { getDashboardRouteForRole } from '../utils/routes'
import type { AuthResponseDto } from '../types/api'

interface MfaChallengePageProps {
  email: string
  password: string
  onBack: () => void
  onSuccess: (route: string) => void
}

export function MfaChallengePage({ email, password, onBack, onSuccess }: MfaChallengePageProps) {
  const { setUser } = useAuth()
  const [digits, setDigits] = useState<string[]>(Array(6).fill(''))
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const inputs = useRef<Array<HTMLInputElement | null>>(Array(6).fill(null))

  const fullCode = digits.join('')
  const codeComplete = fullCode.length === 6 && digits.every((d) => d.length === 1)

  function focusAt(i: number) {
    inputs.current[i]?.focus()
  }

  function updateDigit(i: number, val: string) {
    const next = [...digits]
    next[i] = val.slice(-1)
    setDigits(next)
    if (val && i < 5) focusAt(i + 1)
  }

  function onPaste(e: ClipboardEvent<HTMLInputElement>) {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (!pasted) return
    const next = [...digits]
    for (let i = 0; i < 6; i++) next[i] = pasted[i] ?? ''
    setDigits(next)
    focusAt(Math.min(pasted.length, 5))
  }

  function onKeyDown(i: number, e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && !digits[i] && i > 0) {
      const next = [...digits]
      next[i - 1] = ''
      setDigits(next)
      focusAt(i - 1)
    }
  }

  async function submit() {
    if (!codeComplete || loading) return
    setError('')
    setLoading(true)
    try {
      const res = await authAPI.login({ email, password, mfaCode: fullCode })
      const data = res.data as AuthResponseDto
      if (data?.accessToken) setStoredToken(data.accessToken as string)
      const role = backendRoleToFrontend(data.role)
      setUser({ id: String(data.userId), email: data.email, name: data.fullName || data.email, role })
      onSuccess(getDashboardRouteForRole(role))
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Invalid verification code'))
      setDigits(Array(6).fill(''))
      focusAt(0)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative z-10 w-full max-w-md rounded-3xl border border-slate-200/90 bg-white/85 px-8 py-10 shadow-2xl shadow-slate-900/[0.08] ring-1 ring-slate-900/[0.04] backdrop-blur-xl dark:border-slate-600/50 dark:bg-slate-900/75 dark:shadow-[0_24px_64px_-12px_rgba(0,0,0,0.65)] dark:ring-white/[0.06]">
      <div
        className="pointer-events-none absolute inset-x-0 top-0 h-px rounded-t-3xl bg-gradient-to-r from-transparent via-primary/35 to-secondary/25"
        aria-hidden
      />

      {/* Lock icon */}
      <div className="mb-6 flex justify-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 dark:bg-primary/20">
          <svg className="h-8 w-8 text-primary" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
      </div>

      <h2 className="mb-2 text-center text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
        Two-factor authentication
      </h2>
      <p className="mb-8 text-center text-sm text-slate-500 dark:text-slate-400">
        Enter the 6-digit code from your authenticator app
      </p>

      {error && (
        <div className="mb-4 rounded-xl border border-red-200/80 bg-red-50 px-3 py-2.5 text-sm text-red-800 dark:border-red-800/60 dark:bg-red-950/40 dark:text-red-200">
          {error}
        </div>
      )}

      {/* Digit fields */}
      <div className="mb-8 flex justify-between gap-2">
        {digits.map((digit, i) => (
          <input
            key={i}
            ref={(el) => { inputs.current[i] = el }}
            type="text"
            inputMode="numeric"
            maxLength={1}
            value={digit}
            onChange={(e) => updateDigit(i, e.target.value.replace(/\D/g, ''))}
            onKeyDown={(e) => onKeyDown(i, e)}
            onPaste={i === 0 ? onPaste : undefined}
            className="h-14 w-11 rounded-xl border-2 text-center text-xl font-bold text-slate-900 outline-none transition-colors dark:text-slate-50
              border-slate-200 bg-slate-50 dark:border-slate-600 dark:bg-slate-800/60
              focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20
              dark:focus:border-primary dark:focus:bg-slate-800/90 dark:focus:ring-primary/30"
          />
        ))}
      </div>

      <button
        type="button"
        disabled={!codeComplete || loading}
        onClick={submit}
        className="mb-4 w-full rounded-xl bg-secondary py-3.5 text-sm font-bold tracking-wide text-slate-900 shadow-lg shadow-secondary/25 ring-1 ring-slate-900/10 transition-all hover:brightness-105 hover:shadow-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-secondary focus-visible:ring-offset-2 focus-visible:ring-offset-white active:scale-[0.99] disabled:opacity-50 dark:ring-white/10 dark:focus-visible:ring-offset-slate-900"
      >
        {loading ? 'Verifying…' : 'Verify'}
      </button>

      <button
        type="button"
        onClick={onBack}
        className="w-full text-sm font-medium text-primary transition-colors hover:text-primary/80 dark:text-secondary dark:hover:text-secondary/90"
      >
        ← Back to sign in
      </button>
    </div>
  )
}
