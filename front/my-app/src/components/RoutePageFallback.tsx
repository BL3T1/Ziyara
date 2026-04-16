import { useLanguage } from '../context/LanguageContext'

/** Shown while lazy route chunks load (inside Suspense). */
export function RoutePageFallback() {
  const { t } = useLanguage()
  return (
    <div
      className="flex min-h-[min(42vh,22rem)] flex-col items-center justify-center py-10 sm:py-14"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <div className="w-full max-w-md rounded-2xl border border-slate-200/90 bg-white/80 p-8 shadow-lg shadow-slate-900/[0.04] ring-1 ring-slate-900/[0.03] backdrop-blur-md dark:border-slate-700/70 dark:bg-slate-900/50 dark:shadow-black/30 dark:ring-white/[0.05]">
        <div
          className="pointer-events-none mb-6 h-px w-full rounded-full bg-gradient-to-r from-transparent via-primary/30 to-secondary/20 dark:via-primary/35"
          aria-hidden
        />
        <div className="flex flex-col items-center gap-5">
          <div
            className="h-10 w-10 animate-spin rounded-full border-2 border-primary/25 border-t-primary dark:border-primary/35"
            aria-hidden
          />
          <div className="w-full space-y-2.5" aria-hidden>
            <div className="h-2.5 w-3/5 max-w-[12rem] animate-pulse rounded-full bg-slate-200/90 dark:bg-slate-700/70" />
            <div className="h-2.5 w-full animate-pulse rounded-full bg-slate-100/95 dark:bg-slate-800/80" />
            <div className="h-2.5 w-4/5 animate-pulse rounded-full bg-slate-100/95 dark:bg-slate-800/80" />
          </div>
          <p className="text-center text-sm font-medium text-slate-600 dark:text-slate-400">{t('ui.loading')}</p>
        </div>
      </div>
    </div>
  )
}
