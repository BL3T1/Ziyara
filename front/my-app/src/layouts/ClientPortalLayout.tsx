/**
 * Client Portal (Provider Dashboard) layout.
 * Mirrors company MainLayout: RTL-aware sidebar rail, DashboardHeader, main glow, footer.
 */

import { Suspense } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { NavLink } from 'react-router-dom'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { PORTAL_SIDEBAR_SECTIONS } from '../config/sidebar'
import type { SidebarItem } from '../config/sidebar'
import { getPageTitleKeyForPath } from '../config/routes'
import { resolveCompanyDashboardUrl } from '../config/appSurface'
import { Logo } from '../components/Logo'
import { DashboardFooter, DashboardHeader, RoutePageFallback } from '../components'
import { SidebarIcons, type SidebarIconId } from '../components/SidebarIcons'

const PORTAL_ITEM_ICON: Partial<Record<string, SidebarIconId>> = {
  overview: 'dashboard',
  listings: 'hotels',
  bookings: 'bookings',
  staff: 'users',
  earnings: 'sales',
  profile: 'settings',
  support: 'chat',
}

function getIconForPortalItem(item: SidebarItem) {
  const id = PORTAL_ITEM_ICON[item.id] ?? 'dashboard'
  return SidebarIcons[id]
}

function PortalSidebar() {
  const { sidebarCollapsed, setSidebarCollapsed } = useLayout()
  const { t, locale } = useLanguage()
  const isRtl = locale === 'ar'

  const railShadow = isRtl
    ? 'shadow-[-6px_0_32px_-10px_rgba(0,0,0,0.55)]'
    : 'shadow-[6px_0_32px_-10px_rgba(0,0,0,0.55)]'

  return (
    <aside
      className={`fixed inset-y-0 z-40 flex h-full flex-col border-white/[0.05] bg-[#0a0e14] ${railShadow} transition-[width] duration-300 ease-out motion-reduce:transition-none ${
        isRtl ? 'right-0 border-l' : 'left-0 border-r'
      } ${sidebarCollapsed ? 'w-[3.75rem]' : 'w-60'}`}
    >
      <div
        aria-hidden
        className={`pointer-events-none absolute inset-y-0 w-px bg-gradient-to-b from-primary/50 via-secondary/30 to-transparent opacity-80 ${
          isRtl ? 'left-0' : 'right-0'
        }`}
      />
      <div
        className={`relative flex h-[4.25rem] shrink-0 items-center border-b border-slate-800/60 bg-slate-900/50 ${sidebarCollapsed ? 'justify-center px-0' : 'px-4'}`}
      >
        <div
          className="pointer-events-none absolute inset-x-3 bottom-0 h-px bg-gradient-to-r from-transparent via-primary/35 to-transparent"
          aria-hidden
        />
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
        className="sidebar-nav relative z-[1] min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-2.5 py-6 sm:px-3.5"
        aria-label="Portal navigation"
      >
        {PORTAL_SIDEBAR_SECTIONS.map((section) => (
          <div key={section.id} className="mb-7 last:mb-2">
            {!sidebarCollapsed && (
              <div className="mb-2.5 flex items-center gap-2 px-3">
                <span className="h-px w-5 shrink-0 rounded-full bg-gradient-to-r from-secondary/70 to-primary/40" aria-hidden />
                <span className="text-[0.65rem] font-bold uppercase tracking-[0.14em] text-slate-500">
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
                      const base =
                        'group relative flex w-full items-center gap-3 rounded-xl py-2 text-sm font-medium text-slate-400 transition-all duration-200 ease-out outline-none focus-visible:ring-2 focus-visible:ring-secondary/50 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950'
                      const pad = sidebarCollapsed ? 'justify-center px-2' : 'px-2.5'
                      const activeRow =
                        'bg-gradient-to-r from-slate-800/95 to-slate-800/40 text-white shadow-md shadow-black/25 ring-1 ring-white/[0.06] '
                      const activeRail = isRtl
                        ? 'before:absolute before:inset-y-1.5 before:end-0 before:w-[3px] before:rounded-full before:bg-gradient-to-b before:from-secondary before:to-primary before:shadow-[0_0_12px_rgba(172,158,120,0.35)] '
                        : 'before:absolute before:inset-y-1.5 before:start-0 before:w-[3px] before:rounded-full before:bg-gradient-to-b before:from-secondary before:to-primary before:shadow-[0_0_12px_rgba(172,158,120,0.35)] '
                      const rowClass =
                        `${base} ${pad} ` +
                        (isActive
                          ? `${activeRow}${activeRail}`
                          : 'hover:bg-slate-800/55 hover:text-slate-100')
                      const iconClass =
                        `flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-[#a48c71] transition-[color,filter,background-color] duration-200 ` +
                        (isActive
                          ? 'bg-primary/20 [filter:drop-shadow(0_0_7px_rgba(164,140,113,0.98))_drop-shadow(0_0_18px_rgba(164,140,113,0.42))]'
                          : 'bg-slate-800/40 opacity-80 group-hover:bg-slate-700/60 group-hover:opacity-100')
                      return (
                        <span className={rowClass}>
                          <span className={iconClass}>{getIconForPortalItem(item)}</span>
                          {!sidebarCollapsed && <span className="truncate">{itemLabel}</span>}
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
    </aside>
  )
}

function ClientPortalLayoutInner() {
  const { pathname } = useLocation()
  const { sidebarCollapsed } = useLayout()
  const { t, locale } = useLanguage()
  const isRtl = locale === 'ar'
  const contentPadding = sidebarCollapsed ? 'pl-[3.75rem]' : 'pl-60'
  const contentPaddingRtl = sidebarCollapsed ? 'pr-[3.75rem]' : 'pr-60'
  const pageTitle = t(getPageTitleKeyForPath(pathname))

  const companyToolbarLink = (
    <a
      href={resolveCompanyDashboardUrl()}
      className="hidden items-center rounded-xl px-3 py-2 text-xs font-semibold text-slate-300 outline-none transition-colors hover:bg-slate-700/70 hover:text-white focus-visible:ring-2 focus-visible:ring-[rgb(172_158_120/0.45)] focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900 sm:inline-flex dark:focus-visible:ring-offset-slate-950"
    >
      {t('portal.companyDashboard')}
    </a>
  )

  return (
    <div className="flex min-h-screen flex-col bg-white text-black transition-colors duration-300 dark:bg-[#020409] dark:text-slate-100">
      <PortalSidebar />
      <div
        className={`flex flex-1 flex-col transition-[padding] duration-200 ${isRtl ? contentPaddingRtl : contentPadding}`}
      >
        <DashboardHeader
          roleLabel={t('role.provider')}
          pageTitle={pageTitle}
          showNotifications={false}
          toolbarAddon={companyToolbarLink}
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
        <DashboardFooter />
      </div>
    </div>
  )
}

export function ClientPortalLayout() {
  return <ClientPortalLayoutInner />
}
