import { Suspense } from 'react'
import { Outlet, useLocation, Link, NavLink } from 'react-router-dom'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { PORTAL_SIDEBAR_SECTIONS } from '../config/sidebar'
import type { SidebarItem } from '../config/sidebar'
import { getPageTitleKeyForPath } from '../config/routes'
import { Logo } from '../components/Logo'
import { DashboardHeader, RoutePageFallback } from '../components'
import { SidebarIcons, type SidebarIconId } from '../components/SidebarIcons'
import { VITE_COMPANY_APP_URL } from '../config/appSurface'

function AdminModeBanner() {
  const companyBase = (VITE_COMPANY_APP_URL || '').replace(/\/$/, '')
  return (
    <div className="flex items-center justify-between gap-4 bg-amber-500/10 border-b border-amber-400/30 px-4 py-2 text-xs font-medium text-amber-700 dark:bg-amber-500/[0.08] dark:text-amber-300">
      <span>
        <span className="mr-1.5 inline-block rounded bg-amber-500/20 px-1.5 py-0.5 text-[0.65rem] font-bold uppercase tracking-wider text-amber-700 dark:text-amber-300">
          Admin View
        </span>
        You are browsing this provider portal as Super Admin. Data is scoped to this provider.
      </span>
      {companyBase ? (
        <a
          href={`${companyBase}/dashboard`}
          className="shrink-0 rounded-md bg-amber-600 px-3 py-1 text-white hover:bg-amber-700 dark:bg-amber-500 dark:hover:bg-amber-400"
        >
          Back to Dashboard
        </a>
      ) : (
        <Link
          to="/dashboard"
          className="shrink-0 rounded-md bg-amber-600 px-3 py-1 text-white hover:bg-amber-700 dark:bg-amber-500 dark:hover:bg-amber-400"
        >
          Back to Dashboard
        </Link>
      )}
    </div>
  )
}

const PORTAL_ITEM_ICON: Partial<Record<string, SidebarIconId>> = {
  overview: 'dashboard',
  listings: 'hotels',
  bookings: 'bookings',
  portal_cash: 'payments',
  staff: 'users',
  earnings: 'sales',
  discounts: 'discounts',
  media: 'content',
  portal_map: 'map',
  profile: 'settings',
  support: 'chat',
}

function getIconForPortalItem(item: SidebarItem) {
  const id = PORTAL_ITEM_ICON[item.id] ?? 'dashboard'
  return SidebarIcons[id]
}

const CollapseIcon = ({ collapsed, rtl }: { collapsed: boolean; rtl: boolean }) => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    style={{
      transform: collapsed
        ? rtl ? 'rotate(180deg)' : 'rotate(0deg)'
        : rtl ? 'rotate(0deg)' : 'rotate(180deg)',
      transition: 'transform 300ms ease',
    }}
  >
    <path d="M11 19l-7-7 7-7" />
    <path d="M18 19l-7-7 7-7" />
  </svg>
)

function PortalSidebar() {
  const { sidebarCollapsed, setSidebarCollapsed } = useLayout()
  const { t, locale } = useLanguage()
  const isRtl = locale === 'ar'

  const railBorder = isRtl ? 'right-0 border-l' : 'left-0 border-r'
  const railShadow = isRtl
    ? 'shadow-[-1px_0_0_0_rgba(255,255,255,0.04),-8px_0_32px_-8px_rgba(0,0,0,0.6)]'
    : 'shadow-[1px_0_0_0_rgba(255,255,255,0.04),8px_0_32px_-8px_rgba(0,0,0,0.6)]'

  return (
    <aside
      style={{ colorScheme: 'dark' }}
      className={`fixed inset-y-0 z-40 flex h-full flex-col border-white/[0.05] bg-[#0a0e14] ${railShadow} ${railBorder} transition-[width] duration-300 ease-out motion-reduce:transition-none ${
        sidebarCollapsed ? 'w-[3.75rem]' : 'w-60'
      }`}
    >
      <div
        aria-hidden
        className={`pointer-events-none absolute inset-y-0 w-px bg-gradient-to-b from-transparent via-[#1e4d6b]/60 to-transparent opacity-60 ${
          isRtl ? 'left-0' : 'right-0'
        }`}
      />

      <div
        className={`relative flex h-[4.25rem] shrink-0 items-center border-b border-white/[0.05] ${
          sidebarCollapsed ? 'justify-center px-0' : 'px-4'
        }`}
      >
        <Logo
          compact
          className="relative z-[1] drop-shadow-sm"
          expandAction={
            sidebarCollapsed
              ? { onClick: () => setSidebarCollapsed(false), ariaLabel: t('common.expandSidebar') }
              : undefined
          }
        />
      </div>

      <nav
        className="sidebar-nav relative z-[1] min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-2 py-5 sm:px-2.5"
        aria-label="Portal navigation"
      >
        {PORTAL_SIDEBAR_SECTIONS.map((section) => (
          <div key={section.id} className="mb-6 last:mb-2">
            {!sidebarCollapsed && (
              <div className="mb-1.5 flex items-center gap-2 px-3">
                <span className="text-[0.6rem] font-bold uppercase tracking-[0.18em] text-slate-400">
                  {t(`section.${section.id}`)}
                </span>
              </div>
            )}
            <div className="space-y-0.5">
              {section.items.map((item) => {
                const itemLabel = t(`nav.${item.id}`)
                return (
                  <NavLink
                    key={item.id}
                    to={item.href}
                    end={item.href === '/portal'}
                    title={sidebarCollapsed ? itemLabel : undefined}
                  >
                    {({ isActive }) => {
                      const pad = sidebarCollapsed ? 'justify-center px-0' : 'px-2'
                      const baseRow = `group relative flex w-full items-center gap-3 rounded-xl py-2 text-sm font-medium transition-all duration-150 outline-none focus-visible:ring-2 focus-visible:ring-secondary/50 focus-visible:ring-offset-1 focus-visible:ring-offset-slate-950 ${pad}`

                      const rowActive = isActive
                        ? 'bg-white/[0.07] text-white shadow-sm shadow-black/20 ring-1 ring-white/[0.07] '
                        : 'text-slate-300 hover:bg-white/[0.04] hover:text-white '

                      const activeRail = isActive
                        ? isRtl
                          ? 'before:absolute before:inset-y-2 before:end-0 before:w-0.5 before:rounded-full before:bg-gradient-to-b before:from-secondary/90 before:to-primary/70 before:shadow-[0_0_10px_rgba(172,158,120,0.4)] '
                          : 'before:absolute before:inset-y-2 before:start-0 before:w-0.5 before:rounded-full before:bg-gradient-to-b before:from-secondary/90 before:to-primary/70 before:shadow-[0_0_10px_rgba(172,158,120,0.4)] '
                        : ''

                      const iconWrap =
                        'flex h-8 w-8 shrink-0 items-center justify-center rounded-lg transition-all duration-150 ' +
                        (isActive
                          ? 'text-secondary/90 [filter:drop-shadow(0_0_6px_rgba(172,158,120,0.55))]'
                          : 'text-slate-400 group-hover:text-slate-200')

                      return (
                        <span className={`${baseRow}${rowActive}${activeRail}`}>
                          <span className={iconWrap}>{getIconForPortalItem(item)}</span>
                          {!sidebarCollapsed && (
                            <span className="truncate text-[0.8125rem]">{itemLabel}</span>
                          )}
                        </span>
                      )
                    }}
                  </NavLink>
                )
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="shrink-0 border-t border-white/[0.05] p-2">
        <button
          type="button"
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          className={`flex w-full items-center rounded-xl px-2 py-2.5 text-slate-400 transition-all duration-150 hover:bg-white/[0.04] hover:text-slate-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary/40 ${
            sidebarCollapsed ? 'justify-center' : 'gap-3'
          }`}
          aria-label={sidebarCollapsed ? t('common.expandSidebar') : t('common.collapseSidebar')}
          title={sidebarCollapsed ? t('common.expandSidebar') : t('common.collapseSidebar')}
        >
          <CollapseIcon collapsed={sidebarCollapsed} rtl={isRtl} />
          {!sidebarCollapsed && (
            <span className="text-[0.75rem] font-medium">{t('common.collapseSidebar')}</span>
          )}
        </button>
      </div>
    </aside>
  )
}

function ClientPortalLayoutInner() {
  const { pathname } = useLocation()
  const { sidebarCollapsed } = useLayout()
  const { t, locale } = useLanguage()
  const { user } = useAuth()
  const isRtl = locale === 'ar'
  const isAdminView = user?.role === 'super_admin'
  const contentPadding = sidebarCollapsed ? 'pl-[3.75rem]' : 'pl-60'
  const contentPaddingRtl = sidebarCollapsed ? 'pr-[3.75rem]' : 'pr-60'
  const pageTitle = t(getPageTitleKeyForPath(pathname))

  return (
    <div className="flex min-h-screen flex-col bg-white text-black transition-colors duration-300 dark:bg-[#111827] dark:text-slate-100">
      <PortalSidebar />
      <div
        className={`flex flex-1 flex-col transition-[padding] duration-200 ${isRtl ? contentPaddingRtl : contentPadding}`}
      >
        {isAdminView && <AdminModeBanner />}
        <DashboardHeader
          roleLabel={t('role.provider')}
          pageTitle={pageTitle}
          showNotifications={true}
        />
        <main className="layout-main-surface text-slate-900 dark:text-slate-100">
          <div className="layout-main-surface__glow" aria-hidden>
            <div className="absolute -start-24 top-0 h-[28rem] w-[28rem] rounded-full bg-primary/[0.07] blur-3xl dark:bg-primary/[0.14]" />
            <div className="absolute -end-20 top-24 h-72 w-72 rounded-full bg-secondary/[0.12] blur-3xl dark:bg-secondary/[0.08]" />
            <div className="absolute bottom-0 start-1/3 h-64 w-96 -translate-x-1/2 rounded-full bg-slate-200/60 blur-3xl dark:bg-slate-800/40 rtl:translate-x-1/2" />
          </div>
          <div key={pathname} className="layout-main-surface__content layout-page-enter app-page-stack">
            <Suspense fallback={<RoutePageFallback />}>
              <Outlet />
            </Suspense>
          </div>
        </main>
      </div>
    </div>
  )
}

export function ClientPortalLayout() {
  return <ClientPortalLayoutInner />
}
