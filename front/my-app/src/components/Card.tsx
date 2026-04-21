import type { ReactNode } from 'react'

interface CardProps {
  children: ReactNode
  className?: string
  /** Marketing landing (www.local): glass surface using `landing-public.css` tokens. */
  surface?: 'default' | 'landing'
}

/**
 * Reusable card with hover effects (lift, shadow, border, gradient).
 * Use for stats, content blocks, or any card UI across the app.
 */
export function Card({ children, className = '', surface = 'default' }: CardProps) {
  if (surface === 'landing') {
    return (
      <article className={`lp-glass-card group p-6 text-[#1a2838] ${className}`}>
        <div
          className="pointer-events-none absolute inset-x-0 top-0 z-[2] h-px bg-gradient-to-r from-transparent via-[rgb(90_122_130/0.35)] to-[rgb(182_152_122/0.25)] opacity-90"
          aria-hidden
        />
        <div className="absolute inset-0 z-0 bg-gradient-to-br from-[rgb(90_122_130/0.06)] to-[rgb(182_152_122/0.08)] opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
        <div className="relative z-[1]">{children}</div>
      </article>
    )
  }

  return (
    <article
      className={`group relative w-full min-w-0 overflow-hidden rounded-2xl border border-slate-200/80 bg-white/78 p-6 text-black shadow-md shadow-slate-900/[0.05] ring-1 ring-slate-900/[0.04] backdrop-blur-md transition-all duration-300 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-xl hover:shadow-primary/[0.07] dark:border-slate-700/65 dark:bg-slate-900/42 dark:text-slate-100 dark:shadow-[0_8px_28px_-14px_rgba(0,0,0,0.55)] dark:ring-white/[0.08] dark:hover:border-primary/40 ${className}`}
    >
      <div
        className="pointer-events-none absolute inset-x-0 top-0 z-[2] h-px bg-gradient-to-r from-transparent via-primary/30 to-secondary/20 opacity-90 dark:via-primary/35 dark:to-secondary/25"
        aria-hidden
      />
      <div className="absolute inset-0 z-0 bg-gradient-to-br from-primary/[0.035] to-secondary/[0.055] opacity-0 transition-opacity duration-300 group-hover:opacity-100 dark:from-primary/[0.08] dark:to-secondary/[0.06]" />
      <div className="relative z-[1]">{children}</div>
    </article>
  )
}
