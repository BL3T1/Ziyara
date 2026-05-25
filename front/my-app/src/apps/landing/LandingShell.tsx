import { useEffect } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { LanguageToggleButton } from '../../components/LanguageToggleButton'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { authAPI } from '../../services/api'
import { LANDING_NAV_ITEMS } from './landingNav'
import { useLandingMotion } from './useLandingMotion'
import { useLandingGSAP } from './useLandingGSAP'
import { LandingPublicFooter } from './LandingPublicFooter'
import './landing-public.css'

export function LandingShell() {
  const { t } = useLanguage()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  useLandingMotion()
  useLandingGSAP(location.key)

  useEffect(() => {
    document.documentElement.classList.remove('dark')
  }, [])

  useEffect(() => {
    document.title = 'Ziyara — Book stays, dining, rides & experiences'
  }, [location.pathname])

  return (
    <div className="landing-parallax-root lp-www-root flex min-h-screen flex-col">
      <div className="lp-www-inner">
        <header className="lp-nav">
          <NavLink to="/" className="lp-nav-brand no-underline" end>
            <img src="/logo.png" alt="" className="lp-brand-logo shrink-0" width={140} height={40} />
            <span className="lp-logo-text truncate">Ziyara</span>
          </NavLink>
          <nav className="lp-nav-links" aria-label="Primary">
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
          <div className="lp-nav-actions">
            {user?.role === 'user' ? (
              <>
                <span className="max-w-[160px] truncate text-sm font-medium" style={{ color: 'var(--ink-muted)' }} title={user.email}>
                  {user.name || user.email}
                </span>
                <Link to="/services" className="lp-btn lp-btn-outline lp-btn-sm">
                  {t('landingTraveler.ctaBrowse')}
                </Link>
                <button
                  type="button"
                  className="lp-link-quiet text-sm font-medium"
                  onClick={async () => {
                    try {
                      await authAPI.logout()
                    } catch {
                      /* ignore */
                    }
                    logout()
                    navigate('/')
                  }}
                >
                  {t('common.logOut')}
                </button>
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
            <div className="lp-lang-shell">
              <LanguageToggleButton ariaLabel={t('common.changeLanguage')} />
            </div>
          </div>
        </header>

        <main className="lp-main">
          <Outlet />
        </main>

        <LandingPublicFooter />
      </div>
    </div>
  )
}
