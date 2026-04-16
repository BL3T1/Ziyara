import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { VITE_COMPANY_APP_URL } from '../../config/appSurface'
import { DashboardFooter } from '../../components/DashboardFooter'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import { Logo } from '../../components/Logo'
import { ThemeToggleButton } from '../../components/ThemeToggleButton'
import { useLanguage } from '../../context/LanguageContext'
import { LANDING_NAV_ITEMS } from './landingNav'
import { useLandingMotion } from './useLandingMotion'

function joinBase(base: string, path: string): string {
  const b = base.replace(/\/$/, '')
  const p = path.startsWith('/') ? path : `/${path}`
  return b ? `${b}${p}` : p
}

export function LandingShell() {
  const { t } = useLanguage()
  const customerLogin = joinBase(VITE_COMPANY_APP_URL, '/login')
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  useLandingMotion()

  useEffect(() => {
    setMobileNavOpen(false)
  }, [location.pathname])

  return (
    <div className="landing-parallax-root flex min-h-screen flex-col bg-slate-100 text-slate-900 transition-colors duration-300 dark:bg-[#020617] dark:text-slate-100">
      <header className="sticky top-0 z-50 h-[4.25rem] border-b border-slate-800/60 bg-slate-950/92 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.45)] backdrop-blur-xl supports-[backdrop-filter]:bg-slate-950/88">
        <div
          className="pointer-events-none absolute inset-x-0 bottom-0 h-px bg-gradient-to-r from-transparent via-primary/35 to-secondary/25"
          aria-hidden
        />
        <div className="relative mx-auto flex h-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <div className="flex min-w-0 items-center gap-3">
            <Logo compact className="relative z-[1] shrink-0 drop-shadow-sm [&_img]:brightness-[1.05]" />
            <span className="hidden text-lg font-semibold tracking-tight text-slate-100 sm:inline">Ziyara</span>
          </div>
          <nav className="hidden items-center gap-1 text-sm md:flex">
            {LANDING_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `rounded-xl px-3 py-2 outline-none transition focus-visible:ring-2 focus-visible:ring-secondary/50 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 ${
                    isActive
                      ? 'bg-white/10 text-white shadow-sm ring-1 ring-white/10'
                      : 'text-slate-300 hover:bg-white/5 hover:text-white'
                  }`
                }
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
          </nav>
          <button
            type="button"
            className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-600/60 bg-slate-800/50 text-slate-200 outline-none transition hover:bg-slate-700/80 hover:text-white focus-visible:ring-2 focus-visible:ring-secondary/50 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 md:hidden"
            aria-expanded={mobileNavOpen}
            aria-controls="landing-mobile-nav"
            aria-label={mobileNavOpen ? t('landingBusiness.navClose') : t('landingBusiness.navMenu')}
            onClick={() => setMobileNavOpen((o) => !o)}
          >
            {mobileNavOpen ? (
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" aria-hidden>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" aria-hidden>
                <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              </svg>
            )}
          </button>
          <div className="flex shrink-0 flex-wrap items-center justify-end gap-2 sm:gap-3">
            <div className="flex items-center gap-1.5 rounded-2xl border border-slate-700/50 bg-slate-800/35 p-1 shadow-inner shadow-black/20">
              <ThemeToggleButton className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-slate-300 transition-all hover:bg-slate-700/80 hover:text-white active:scale-95" />
              <span className="hidden h-5 w-px shrink-0 bg-slate-600/55 sm:block" aria-hidden />
              <LanguageToggleButton className="flex h-9 min-w-[2.5rem] shrink-0 items-center justify-center rounded-xl px-1.5 text-slate-300 transition-all hover:bg-slate-700/80 hover:text-white active:scale-95" />
            </div>
            <a
              href={customerLogin}
              className="inline-flex items-center rounded-2xl bg-gradient-to-br from-primary to-primary/90 px-4 py-2 text-xs font-bold text-white shadow-md shadow-black/25 ring-1 ring-white/10 outline-none transition hover:opacity-95 focus-visible:ring-2 focus-visible:ring-secondary/55 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 active:scale-[0.98]"
            >
              {t('landingBusiness.partnerCustomerCta')}
            </a>
          </div>
        </div>
      </header>

      {mobileNavOpen ? (
        <div
          id="landing-mobile-nav"
          className="fixed inset-x-0 top-[4.25rem] z-40 border-b border-slate-800/80 bg-slate-950/98 px-4 py-4 shadow-2xl shadow-black/40 backdrop-blur-xl md:hidden"
        >
          <nav className="flex flex-col gap-1">
            {LANDING_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `rounded-xl px-3 py-3 text-base font-medium outline-none transition focus-visible:ring-2 focus-visible:ring-secondary/50 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 ${
                    isActive ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/5 hover:text-white'
                  }`
                }
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
          </nav>
        </div>
      ) : null}

      <main className="relative flex-1 overflow-x-hidden bg-gradient-to-b from-slate-50 via-slate-100/80 to-slate-100 px-4 py-10 sm:px-6 sm:py-12 lg:px-10 lg:py-14 dark:bg-[#020617]">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.34] dark:opacity-[0.18]"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg width='72' height='72' viewBox='0 0 72 72' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 0h36v36H0V0zm36 36h36v36H36V36z' fill='%2394a3b8' fill-opacity='0.06' fill-rule='evenodd'/%3E%3C/svg%3E")`,
          }}
          aria-hidden
        />
        <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
          <div className="absolute -left-24 -top-10 h-[30rem] w-[30rem] rounded-full bg-primary/[0.1] blur-3xl dark:bg-primary/[0.2]" />
          <div className="absolute -right-24 top-24 h-[24rem] w-[24rem] rounded-full bg-secondary/[0.16] blur-3xl dark:bg-secondary/[0.13]" />
          <div className="absolute bottom-0 left-1/3 h-64 w-96 -translate-x-1/2 rounded-full bg-slate-200/70 blur-3xl dark:bg-slate-800/45" />
          <div className="absolute inset-x-0 top-0 h-64 bg-gradient-to-b from-white/45 to-transparent dark:from-white/[0.03]" />
        </div>
        <div className="relative z-[1] mx-auto w-full max-w-7xl">
          <Outlet />
        </div>
      </main>

      <DashboardFooter />
    </div>
  )
}
