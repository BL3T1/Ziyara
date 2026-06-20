import type { ReactNode } from 'react'

interface CardProps {
  children: ReactNode
  className?: string
  /** Marketing landing (www.local): glass surface using `landing-public.css` tokens. */
  surface?: 'default' | 'landing'
}

export function Card({ children, className = '', surface = 'default' }: CardProps) {
  if (surface === 'landing') {
    return (
      <article className={`lp-glass-card group p-6 text-[#1a2838] ${className}`}>
        <div
          className="pointer-events-none absolute inset-x-0 top-0 z-[2] h-px bg-gradient-to-r from-transparent via-[rgb(90_122_130/0.4)] to-[rgb(182_152_122/0.3)] opacity-90"
          aria-hidden
        />
        <div className="absolute inset-0 z-0 bg-gradient-to-br from-[rgb(90_122_130/0.06)] to-[rgb(182_152_122/0.08)] opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
        <div className="relative z-[1]">{children}</div>
      </article>
    )
  }

  return (
    <article
      className={`group relative w-full min-w-0 overflow-hidden rounded-2xl border border-slate-200/90 bg-white p-6 shadow-[0_1px_3px_rgba(0,0,0,0.04),0_4px_20px_-4px_rgba(0,0,0,0.07)] ring-1 ring-slate-900/[0.03] transition-all duration-300 hover:-translate-y-px hover:border-slate-300/80 hover:shadow-[0_4px_24px_-4px_rgba(0,0,0,0.12)] dark:border-white/[0.06] dark:bg-[#0d1117] dark:shadow-[0_1px_2px_rgba(0,0,0,0.4),0_8px_32px_-8px_rgba(0,0,0,0.55)] dark:ring-white/[0.04] dark:hover:border-white/[0.1] dark:hover:shadow-[0_8px_40px_-8px_rgba(0,0,0,0.7)] ${className}`}
    >
      {/* Top shimmer line */}
      <div
        className="pointer-events-none absolute inset-x-0 top-0 z-[2] h-px bg-gradient-to-r from-transparent via-primary-700/25 to-secondary-500/15 opacity-80 dark:via-primary-600/40 dark:to-secondary-400/25"
        aria-hidden
      />
      {/* Hover glow */}
      <div className="absolute inset-0 z-0 bg-gradient-to-br from-primary-800/[0.025] to-secondary-500/[0.04] opacity-0 transition-opacity duration-300 group-hover:opacity-100 dark:from-primary-800/[0.07] dark:to-secondary-600/[0.05]" />
      <div className="relative z-[1]">{children}</div>
    </article>
  )
}
