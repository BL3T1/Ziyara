import { useEffect, useRef, useState, useCallback, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { getPageIconForPath } from '../config/routes'
import { isCompanySurface } from '../config/appSurface'
import { isCompanyStaffRole, isSuperAdminRole, type Role } from '../types/auth'
import { SidebarIcons } from './SidebarIcons'
import { Avatar, DEFAULT_AVATAR } from './Avatar'
import { NotificationPanel } from './NotificationPanel'
import { notificationsAPI } from '../services/api'
import type { NotificationInboxDto } from '../types/api'
import { ThemeToggleButton } from './ThemeToggleButton'
import { LanguageToggleButton } from './LanguageToggleButton'
import { DeletedArchiveSearchModal, GlobalSearchModal } from './GlobalSearchModals'

const BellIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
    <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
  </svg>
)

const MenuIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="4" x2="20" y1="12" y2="12" />
    <line x1="4" x2="20" y1="6" y2="6" />
    <line x1="4" x2="20" y1="18" y2="18" />
  </svg>
)

const SearchToolbarIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8" />
    <path d="m21 21-4.3-4.3" />
  </svg>
)

const DeletedSearchIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 6h18" />
    <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
    <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
    <circle cx="12" cy="13" r="3" />
    <path d="M14.5 15.5 18 19" />
  </svg>
)

interface DashboardHeaderProps {
  roleLabel?: string
  pageTitle?: string
  userAvatarUrl?: string | null
  /** When false, notifications bell is hidden (e.g. provider portal). Default true. */
  showNotifications?: boolean
  /** Rendered in the toolbar pill after the theme toggle when notifications are off. */
  toolbarAddon?: ReactNode
}

export function DashboardHeader({
  roleLabel: roleLabelProp,
  pageTitle,
  userAvatarUrl: userAvatarUrlProp,
  showNotifications = true,
  toolbarAddon,
}: DashboardHeaderProps) {
  const { pathname } = useLocation()
  const { user, logout } = useAuth()
  const { theme, toggleSidebar, sidebarCollapsed, setMobileSidebarOpen } = useLayout()
  const { t } = useLanguage()
  const navigate = useNavigate()

  const displayPageTitle = pageTitle ?? t('common.systemOverview')
  const avatarUrl = userAvatarUrlProp ?? (user as { profileImageUrl?: string } | null)?.profileImageUrl ?? DEFAULT_AVATAR
  const pageIconId = getPageIconForPath(pathname)
  const PageIcon = SidebarIcons[pageIconId as keyof typeof SidebarIcons] ?? SidebarIcons.dashboard
  const roleKey = (user as { role?: string } | null)?.role
  const roleLabel = roleLabelProp ?? (roleKey ? t(`role.${roleKey}`) : t('role.user'))

  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0)
  const bellButtonRef = useRef<HTMLButtonElement>(null)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)

  const closeUserMenu = useCallback(() => setUserMenuOpen(false), [])

  useEffect(() => {
    if (!userMenuOpen) return
    const onOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        closeUserMenu()
      }
    }
    document.addEventListener('mousedown', onOutside)
    return () => document.removeEventListener('mousedown', onOutside)
  }, [userMenuOpen, closeUserMenu])

  // Pre-fetch unread count on mount so the badge shows immediately
  useEffect(() => {
    if (!showNotifications) return
    notificationsAPI
      .list({ page: 0, size: 1 })
      .then((res) => {
        const inbox = res.data as NotificationInboxDto | null
        setUnreadNotificationCount(Number(inbox?.unreadCount ?? 0))
      })
      .catch(() => { /* silently ignore — badge stays at 0 */ })
  }, [showNotifications])
  const [globalSearchOpen, setGlobalSearchOpen] = useState(false)
  const [deletedSearchOpen, setDeletedSearchOpen] = useState(false)

  const staffRole = roleKey as Role | undefined
  const showSearchTools = Boolean(isCompanySurface && staffRole && isCompanyStaffRole(staffRole))

  useEffect(() => {
    if (!showSearchTools) return
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setGlobalSearchOpen(true)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [showSearchTools])

  const btnCls = 'flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-all hover:bg-slate-100 hover:text-slate-700 active:scale-95 dark:text-slate-400 dark:hover:bg-white/[0.06] dark:hover:text-slate-200'

  return (
    <header className="sticky top-0 z-50 h-[4.25rem] border-b border-slate-200 bg-white/95 shadow-sm backdrop-blur-xl dark:border-white/[0.05] dark:bg-[#0a0e14]/95 dark:shadow-[0_1px_0_rgba(255,255,255,0.04),0_4px_24px_-4px_rgba(0,0,0,0.5)]">
      <div
        className="pointer-events-none absolute inset-x-0 bottom-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-secondary/20 dark:via-[#1e4d6b]/50 dark:to-[#ac9e78]/30"
        aria-hidden
      />
      <div className="relative mx-auto flex h-full max-w-[100rem] items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
        <div className="flex min-w-0 flex-1 items-center gap-3">
          {/* Mobile hamburger — only visible below lg */}
          <button
            type="button"
            onClick={() => setMobileSidebarOpen(true)}
            className={`${btnCls} lg:hidden`}
            aria-label={t('common.toggleSidebar')}
          >
            <MenuIcon />
          </button>
          {/* Desktop collapse toggle */}
          {!sidebarCollapsed && (
            <button
              type="button"
              onClick={toggleSidebar}
              className={`${btnCls} hidden lg:inline-flex`}
              aria-label={t('common.toggleSidebar')}
            >
              <MenuIcon />
            </button>
          )}
          <div className="flex min-w-0 items-center gap-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500 ring-1 ring-slate-200 dark:bg-white/[0.05] dark:text-slate-400 dark:ring-white/[0.06]">
              {PageIcon}
            </span>
            <div className="min-w-0 flex flex-col gap-0.5">
              <span className="truncate text-[0.6rem] font-bold uppercase tracking-[0.16em] text-slate-400 dark:text-slate-600">
                {roleLabel}
              </span>
              <span className="truncate text-[0.9375rem] font-semibold leading-tight tracking-tight text-slate-800 dark:text-slate-100">
                {displayPageTitle}
              </span>
            </div>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-1.5 sm:gap-2">
          {/* Tool pill */}
          <div className="flex items-center gap-0.5 rounded-xl border border-white/[0.06] bg-white/[0.03] p-0.5">
            {showSearchTools && (
              <>
                <button
                  type="button"
                  onClick={() => setGlobalSearchOpen(true)}
                  className={btnCls}
                  aria-label={t('globalSearch.openAria')}
                  title={`${t('globalSearch.openAria')} (Ctrl+K)`}
                >
                  <SearchToolbarIcon />
                </button>
                {isSuperAdminRole(user?.role) && (
                  <button
                    type="button"
                    onClick={() => setDeletedSearchOpen(true)}
                    className={btnCls}
                    aria-label={t('globalSearch.deletedAria')}
                    title={t('globalSearch.deletedAria')}
                  >
                    <DeletedSearchIcon />
                  </button>
                )}
                <span className="hidden h-4 w-px shrink-0 bg-white/[0.08] sm:block mx-0.5" aria-hidden />
              </>
            )}
            <ThemeToggleButton
              className={btnCls}
              ariaLabel={theme === 'dark' ? t('common.switchToLightMode') : t('common.switchToDarkMode')}
            />
            <span className="hidden h-4 w-px shrink-0 bg-white/[0.08] sm:block mx-0.5" aria-hidden />
            <LanguageToggleButton
              className={`${btnCls} min-w-[2rem] px-1`}
              ariaLabel={t('common.changeLanguage')}
            />
            {showNotifications ? (
              <div className="relative">
                <button
                  ref={bellButtonRef}
                  type="button"
                  onClick={() => setNotificationsOpen((v) => !v)}
                  className={`${btnCls} relative`}
                  aria-label={t('common.notifications')}
                >
                  <BellIcon />
                  {unreadNotificationCount > 0 && (
                    <span className="absolute right-0.5 top-0.5 flex h-[1.1rem] min-w-[1.1rem] items-center justify-center rounded-full bg-rose-500 px-0.5 text-[9px] font-bold text-white" aria-hidden>
                      {unreadNotificationCount > 9 ? '9+' : unreadNotificationCount}
                    </span>
                  )}
                </button>
                <NotificationPanel
                  isOpen={notificationsOpen}
                  onClose={() => setNotificationsOpen(false)}
                  anchorRef={bellButtonRef}
                  onMarkAllRead={() => setUnreadNotificationCount(0)}
                  onUnreadCount={setUnreadNotificationCount}
                />
              </div>
            ) : (
              toolbarAddon
            )}
          </div>

          {/* User pill + dropdown */}
          <div className="relative" ref={userMenuRef}>
            <button
              type="button"
              aria-label={t('common.accountMenu')}
              onClick={() => setUserMenuOpen((v) => !v)}
              className="flex items-center gap-1.5 rounded-xl border border-white/[0.06] bg-white/[0.03] py-1 pl-1 pr-2 transition-colors hover:bg-white/[0.06]"
            >
              <Avatar src={avatarUrl} size="md" className="ring-1 ring-[#ac9e78]/25 ring-offset-1 ring-offset-[#0a0e14]" />
              <span className="text-xs font-semibold text-slate-400 transition-colors group-hover:text-slate-200 hidden sm:block">
                {user?.name?.split(' ')[0] ?? t('common.accountMenu')}
              </span>
              <svg className="hidden sm:block h-3 w-3 text-slate-500 transition-transform" style={{ transform: userMenuOpen ? 'rotate(180deg)' : 'rotate(0deg)' }} viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                <path d="M2 4l4 4 4-4" />
              </svg>
            </button>

            {userMenuOpen && (
              <div className="absolute end-0 top-full z-50 mt-1.5 min-w-[10rem] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl shadow-slate-200/60 dark:border-white/[0.08] dark:bg-[#0d1219] dark:shadow-black/40">
                <div className="py-1">
                  <button
                    type="button"
                    onClick={() => { closeUserMenu(); navigate('/account/change-password') }}
                    className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-sm text-slate-700 transition-colors hover:bg-slate-50 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-white/[0.06] dark:hover:text-slate-100"
                  >
                    <svg className="h-4 w-4 shrink-0 text-slate-400 dark:text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <rect width="18" height="11" x="3" y="11" rx="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    {t('common.changePassword')}
                  </button>
                  <div className="my-1 h-px bg-slate-100 dark:bg-white/[0.06]" aria-hidden />
                  <button
                    type="button"
                    onClick={() => { closeUserMenu(); logout(); navigate('/') }}
                    className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-sm text-rose-600 transition-colors hover:bg-rose-50 hover:text-rose-700 dark:text-rose-400 dark:hover:bg-white/[0.06] dark:hover:text-rose-300"
                  >
                    <svg className="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                      <polyline points="16 17 21 12 16 7" />
                      <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                    {t('common.logOut')}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
      <GlobalSearchModal open={globalSearchOpen} onClose={() => setGlobalSearchOpen(false)} />
      <DeletedArchiveSearchModal open={deletedSearchOpen} onClose={() => setDeletedSearchOpen(false)} />
    </header>
  )
}
