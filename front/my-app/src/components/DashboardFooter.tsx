import { useLanguage } from '../context/LanguageContext'
import { Link } from 'react-router-dom'

const FOOTER_LINK_KEYS = ['footer.privacy', 'footer.terms', 'footer.contact'] as const
const FOOTER_HREFS = ['/privacy', '/terms', '/contact'] as const

export function DashboardFooter() {
  const { t } = useLanguage()
  return (
    <footer className="relative border-t border-slate-200/60 bg-white text-black dark:border-white/[0.05] dark:bg-[#0a0e14] dark:text-slate-400">
      <div
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent dark:via-primary/25"
        aria-hidden
      />
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-9 lg:px-8">
        <div className="flex flex-col items-center justify-between gap-5 sm:flex-row sm:gap-4">
          <p className="text-center text-sm text-slate-600 sm:text-start dark:text-slate-400">
            © {new Date().getFullYear()} Ziyarah. {t('footer.allRightsReserved')}.
          </p>
          <div className="flex flex-wrap justify-center gap-x-8 gap-y-2">
            {FOOTER_LINK_KEYS.map((key, i) => (
              <Link
                key={key}
                to={FOOTER_HREFS[i]}
                className="rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-50 dark:text-slate-400 dark:hover:text-secondary dark:focus-visible:ring-secondary/40 dark:focus-visible:ring-offset-slate-950"
              >
                {t(key)}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  )
}
