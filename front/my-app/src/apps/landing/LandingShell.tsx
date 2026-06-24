import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import { CurrencySwitcher } from '../../components/CurrencySwitcher'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { authAPI } from '../../services/api'
import { LANDING_NAV_ITEMS } from './landingNav'
import { useLandingMotion } from './useLandingMotion'
import { useLandingGSAP } from './useLandingGSAP'
import { LandingPublicFooter } from './LandingPublicFooter'
import { LandingToastProvider } from './LandingToast'
import './landing-public.css'

export function LandingShell() {
  const { t } = useLanguage()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)
  useLandingMotion()
  useLandingGSAP(location.key)

  useEffect(() => {
    document.documentElement.classList.remove('dark')
  }, [])

  useEffect(() => {
    if (!userMenuOpen) return
    function handleClick(e: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [userMenuOpen])

  async function handleLogout() {
    try { await authAPI.logout() } catch { /* ignore */ }
    logout()
    navigate('/')
  }

  return (
    <div className="landing-parallax-root lp-www-root min-h-screen flex flex-col bg-background antialiased">
      {/* Mobile menu backdrop */}
      {menuOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm"
          aria-hidden
          onClick={() => setMenuOpen(false)}
        />
      )}

      <LandingToastProvider>
        {/* ── Floating Navigation Bar ───────────────────────────────────── */}
        <header
          role="banner"
          className="fixed top-3 left-1/2 -translate-x-1/2 w-[95%] max-w-container-max rounded-xl bg-white/70 backdrop-blur-2xl border border-outline-variant/20 shadow-md z-50 flex justify-between items-center h-16 px-6 flex-wrap"
        >
          {/* Brand */}
          <NavLink to="/" end onClick={() => setMenuOpen(false)} className="no-underline flex-shrink-0">
            <span className="font-headline-md text-headline-md font-bold text-on-primary bg-[#0E1626] px-4 py-1 rounded-full select-none cursor-pointer hover:scale-95 duration-150 ease-in-out transition-transform">
              Ziyara
            </span>
          </NavLink>

          {/* Desktop nav links — centered */}
          <nav className="hidden md:flex items-center gap-8" aria-label="Primary">
            {LANDING_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  isActive
                    ? 'font-label-md text-label-md text-stitch-primary font-bold border-b-2 border-stitch-primary pb-1 px-3 py-2 no-underline transition-colors'
                    : 'font-label-md text-label-md text-on-surface-variant font-medium hover:text-stitch-primary hover:bg-surface-variant/50 transition-colors px-3 py-2 rounded-lg no-underline'
                }
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
          </nav>

          {/* Actions */}
          <div className="flex items-center gap-3">
            {/* Language + currency utilities */}
            <div className="hidden lg:flex gap-1 items-center text-on-surface-variant">
              <CurrencySwitcher />
              <LanguageToggleButton ariaLabel={t('common.changeLanguage')} className="p-2 rounded-full hover:bg-surface-variant/50 transition-colors" />
            </div>

            {user?.role === 'user' ? (
              <>
                {/* User dropdown */}
                <div className="lp-user-menu" ref={userMenuRef}>
                  <button
                    type="button"
                    className="lp-user-menu__trigger"
                    onClick={() => setUserMenuOpen((v) => !v)}
                    aria-expanded={userMenuOpen}
                    aria-haspopup="menu"
                  >
                    <span className="lp-user-menu__avatar" aria-hidden>
                      {(user.name || '?').charAt(0).toUpperCase()}
                    </span>
                    <span className="lp-user-menu__name truncate">{user.name || t('common.accountMenu')}</span>
                    <svg className="lp-user-menu__chevron" width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden>
                      <path d="M2 4l4 4 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </button>
                  {userMenuOpen && (
                    <div className="lp-user-menu__dropdown" role="menu">
                      <Link to="/my-bookings" className="lp-user-menu__item" role="menuitem" onClick={() => setUserMenuOpen(false)}>
                        {t('landingMyBookings.navLabel')}
                      </Link>
                      <Link to="/account" className="lp-user-menu__item" role="menuitem" onClick={() => setUserMenuOpen(false)}>
                        {t('account.navLabel') || 'Account'}
                      </Link>
                      <div className="lp-user-menu__divider" />
                      <button type="button" className="lp-user-menu__item lp-user-menu__item--danger" role="menuitem" onClick={handleLogout}>
                        {t('common.logOut')}
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="hidden sm:block font-label-md text-label-md text-stitch-primary font-semibold px-4 py-2 hover:bg-stitch-primary/5 rounded-lg transition-colors no-underline"
                >
                  {t('landingTraveler.ctaSignIn')}
                </Link>
                <Link
                  to="/signup"
                  className="no-underline bg-stitch-primary text-on-primary font-label-md text-label-md px-5 py-2 rounded-lg hover:bg-surface-tint hover:shadow-md transition-all hidden md:block"
                >
                  {t('landingAuth.createAccount')}
                </Link>
              </>
            )}

            {/* Hamburger — mobile only */}
            <button
              type="button"
              className="md:hidden p-2 rounded-full hover:bg-surface-variant/50 text-on-surface"
              aria-label={menuOpen ? t('landingBusiness.navClose') : t('landingBusiness.navMenu')}
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((v) => !v)}
            >
              <svg width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" viewBox="0 0 24 24" aria-hidden>
                {menuOpen
                  ? <><path d="M6 6l12 12M6 18L18 6"/></>
                  : <><path d="M3 6h18M3 12h18M3 18h18"/></>
                }
              </svg>
            </button>
          </div>

          {/* Mobile drawer */}
          <nav
            className={`lp-mobile-drawer ${menuOpen ? 'lp-mobile-drawer--open' : ''}`}
            aria-label="Mobile navigation"
            aria-hidden={!menuOpen}
          >
            {LANDING_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => `lp-mobile-drawer__link${isActive ? ' lp-mobile-drawer__link--active' : ''}`}
                onClick={() => setMenuOpen(false)}
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
            <div className="lp-mobile-drawer__divider" />
            {user?.role === 'user' ? (
              <>
                <Link to="/my-bookings" className="lp-mobile-drawer__link" onClick={() => setMenuOpen(false)}>
                  {t('landingMyBookings.navLabel')}
                </Link>
                <Link to="/account" className="lp-mobile-drawer__link" onClick={() => setMenuOpen(false)}>
                  {t('account.navLabel') || 'Account'}
                </Link>
                <button type="button" className="lp-mobile-drawer__link lp-mobile-drawer__link--danger" onClick={handleLogout}>
                  {t('common.logOut')}
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="lp-mobile-drawer__link" onClick={() => setMenuOpen(false)}>
                  {t('landingTraveler.ctaSignIn')}
                </Link>
                <Link to="/signup" className="lp-mobile-drawer__link" onClick={() => setMenuOpen(false)}>
                  {t('landingAuth.createAccount')}
                </Link>
              </>
            )}
            <div className="lp-mobile-drawer__divider" />
            <div className="flex items-center gap-2 px-2 py-1">
              <CurrencySwitcher />
              <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
            </div>
          </nav>
        </header>

        {/* ── Page content (pt-20 to clear fixed nav) ───────────────────── */}
        <main className="flex-1 pt-20">
          <Outlet />
        </main>

        <LandingPublicFooter />
      </LandingToastProvider>
    </div>
  )
}
