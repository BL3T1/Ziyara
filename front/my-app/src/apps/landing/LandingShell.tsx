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

  // Close user menu on outside click
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
    <div className="landing-parallax-root lp-www-root flex min-h-screen flex-col">
      {/* Mobile menu backdrop */}
      {menuOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm"
          aria-hidden
          onClick={() => setMenuOpen(false)}
        />
      )}

      <LandingToastProvider>
      <div className="lp-www-inner">
        <header className="lp-nav" role="banner">
          {/* Brand */}
          <NavLink to="/" className="lp-nav-brand no-underline" end>
            <img src="/logo.png" alt="Ziyara" className="lp-brand-logo shrink-0" width={220} height={64} />
          </NavLink>

          {/* Desktop nav links — hidden on mobile */}
          <nav className="lp-nav-links lp-nav-links--desktop" aria-label="Primary">
            {LANDING_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => (isActive ? 'lp-nav-active' : undefined)}
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
          </nav>

          {/* Actions */}
          <div className="lp-nav-actions">
            {user?.role === 'user' ? (
              <>
                {/* Browse CTA */}
                <Link to="/services" className="lp-btn lp-btn-outline lp-btn-sm lp-nav-cta-browse">
                  {t('landingTraveler.ctaBrowse')}
                </Link>

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
                <Link to="/signup" className="lp-btn lp-btn-outline lp-btn-sm">
                  {t('landingAuth.createAccount')}
                </Link>
                <Link to="/login" className="lp-btn lp-btn-primary lp-btn-sm">
                  {t('landingTraveler.ctaSignIn')}
                </Link>
              </>
            )}
            <CurrencySwitcher />
            <div className="lp-lang-shell">
              <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
            </div>

            {/* Hamburger — mobile only */}
            <button
              type="button"
              className="lp-hamburger"
              aria-label={menuOpen ? t('landingBusiness.navClose') : t('landingBusiness.navMenu')}
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((v) => !v)}
            >
              <span className={`lp-hamburger__bar ${menuOpen ? 'lp-hamburger__bar--top-open' : ''}`} />
              <span className={`lp-hamburger__bar ${menuOpen ? 'lp-hamburger__bar--mid-open' : ''}`} />
              <span className={`lp-hamburger__bar ${menuOpen ? 'lp-hamburger__bar--bot-open' : ''}`} />
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
          </nav>
        </header>

        <main className="lp-main">
          <Outlet />
        </main>

        <LandingPublicFooter />
      </div>
      </LandingToastProvider>
    </div>
  )
}
