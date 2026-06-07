import { useLanguage } from '../context/LanguageContext'
import { VITE_LANDING_APP_URL } from '../config/appSurface'

const FOOTER_LINKS = [
  { key: 'footer.privacy',  path: '/privacy'  },
  { key: 'footer.terms',    path: '/terms'    },
  { key: 'footer.contact',  path: '/contact'  },
] as const

/**
 * Resolve a footer link to an absolute URL on the landing site (when configured)
 * or hide it entirely so dead-end dashboard redirects never happen.
 */
function landingHref(path: string): string | null {
  const base = VITE_LANDING_APP_URL.replace(/\/$/, '')
  return base ? `${base}${path}` : null
}

export function DashboardFooter() {
  const { t } = useLanguage()
  const visibleLinks = FOOTER_LINKS.map((l) => ({ ...l, href: landingHref(l.path) })).filter((l) => l.href !== null)

  return (
    <footer className="relative border-t border-slate-200/60 bg-white text-black dark:border-white/[0.05] dark:bg-[#111827] dark:text-slate-400">
      <div
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent dark:via-primary/25"
        aria-hidden
      />
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-9 lg:px-8">
        <div className="flex flex-col items-center justify-between gap-5 sm:flex-row sm:gap-4">
          <p className="text-center text-sm text-slate-600 sm:text-start dark:text-slate-400">
            © {new Date().getFullYear()} Ziyarah. {t('footer.allRightsReserved')}.
          </p>
          {visibleLinks.length > 0 && (
            <div className="flex flex-wrap justify-center gap-x-8 gap-y-2">
              {visibleLinks.map((l) => (
                <a
                  key={l.key}
                  href={l.href!}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-50 dark:text-slate-400 dark:hover:text-secondary dark:focus-visible:ring-secondary/40 dark:focus-visible:ring-offset-slate-950"
                >
                  {t(l.key)}
                </a>
              ))}
            </div>
          )}
        </div>
      </div>
    </footer>
  )
}
