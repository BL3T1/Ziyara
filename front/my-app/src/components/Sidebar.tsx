import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { SIDEBAR_SECTIONS, filterSectionsByVisibleIds, getSidebarSectionsForRole } from '../config/sidebar'
import type { SidebarSection as SidebarSectionType, SidebarItem } from '../config/sidebar'
import { Logo } from './Logo'
import { SidebarIcons, type SidebarIconId } from './SidebarIcons'

const SIDEBAR_ITEM_ICON: Partial<Record<string, SidebarIconId>> = {
  main_find_customer: 'search_user',
  main_deleted_items: 'search_deleted',
  currency_rates: 'sales',
}

function getIconForItem(item: SidebarItem) {
  const iconId = (SIDEBAR_ITEM_ICON[item.id] ?? item.id) as SidebarIconId
  const Icon = SidebarIcons[iconId]
  return Icon ?? SidebarIcons.dashboard
}

/** Overlap chunk decode with hover/focus before navigating to /dashboard */
function prefetchDashboardRouteChunks() {
  void import('../pages/DashboardPage')
  void import('../pages/SalesDashboardPage')
}

function SidebarSection({
  section,
  collapsed,
  isDark,
  isRtl,
  t,
}: {
  section: SidebarSectionType
  collapsed: boolean
  isDark: boolean
  isRtl: boolean
  t: (key: string) => string
}) {
  const sectionLabel = t(`section.${section.id}`)
  return (
    <div className="mb-7 last:mb-2">
      {!collapsed && (
        <div className="mb-2.5 flex items-center gap-2 px-3">
          <span className="h-px w-5 shrink-0 rounded-full bg-gradient-to-r from-secondary/70 to-primary/40" aria-hidden />
          <span
            className={`text-[0.65rem] font-bold uppercase tracking-[0.14em] ${isDark ? 'text-slate-500' : 'text-slate-500'}`}
          >
            {sectionLabel}
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
              end={item.href === '/dashboard'}
              title={collapsed ? itemLabel : undefined}
              onMouseEnter={item.href === '/dashboard' ? prefetchDashboardRouteChunks : undefined}
              onFocus={item.href === '/dashboard' ? prefetchDashboardRouteChunks : undefined}
            >
              {({ isActive }) => {
                const base =
                  'group relative flex w-full items-center gap-3 rounded-xl py-2 text-sm font-medium transition-all duration-200 ease-out outline-none focus-visible:ring-2 focus-visible:ring-secondary/50 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950'
                const pad = collapsed ? 'justify-center px-2' : 'px-2.5'
                let rowClass = `${base} ${pad} `
                if (!isDark) {
                  rowClass += `hover:bg-slate-100 hover:text-slate-900 ${isActive ? 'bg-slate-200 text-slate-900 shadow-sm' : 'text-slate-600'}`
                } else {
                  rowClass += isActive
                    ? 'bg-gradient-to-r from-slate-800/95 to-slate-800/40 text-white shadow-md shadow-black/25 ring-1 ring-white/[0.06] '
                    : 'text-slate-400 hover:bg-slate-800/55 hover:text-slate-100 '
                  if (isActive) {
                    rowClass += isRtl
                      ? 'before:absolute before:inset-y-1.5 before:end-0 before:w-[3px] before:rounded-full before:bg-gradient-to-b before:from-secondary before:to-primary before:shadow-[0_0_12px_rgba(172,158,120,0.35)] '
                      : 'before:absolute before:inset-y-1.5 before:start-0 before:w-[3px] before:rounded-full before:bg-gradient-to-b before:from-secondary before:to-primary before:shadow-[0_0_12px_rgba(172,158,120,0.35)] '
                  }
                }
                const iconClass =
                  `flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-[#a48c71] transition-[color,filter,background-color] duration-200 ` +
                  (isDark
                    ? isActive
                      ? 'bg-primary/20 [filter:drop-shadow(0_0_7px_rgba(164,140,113,0.98))_drop-shadow(0_0_18px_rgba(164,140,113,0.42))]'
                      : 'bg-slate-800/40 opacity-80 group-hover:bg-slate-700/60 group-hover:opacity-100'
                    : isActive
                      ? 'bg-white shadow-sm [filter:drop-shadow(0_0_7px_rgba(164,140,113,0.85))_drop-shadow(0_0_16px_rgba(164,140,113,0.38))]'
                      : 'opacity-75 group-hover:opacity-100')
                return (
                  <span className={rowClass}>
                    <span className={iconClass}>{getIconForItem(item)}</span>
                    {!collapsed && <span className="truncate">{itemLabel}</span>}
                  </span>
                )
              }}
            </NavLink>
          )
        })}
      </div>
    </div>
  )
}

export function Sidebar() {
  const { user, sidebarNav } = useAuth()
  const { sidebarCollapsed } = useLayout()
  const { t, locale } = useLanguage()
  const isRtl = locale === 'ar'
  const role = user?.role ?? 'user'
  const sections =
    sidebarNav?.source === 'rbac_role' && sidebarNav.visibleItemIds.length > 0
      ? filterSectionsByVisibleIds(SIDEBAR_SECTIONS, sidebarNav.visibleItemIds)
      : getSidebarSectionsForRole(role)
  /** Sidebar chrome is always dark (slate-900) — classic dashboard rail; theme toggle affects main content only. */
  const sidebarNavDark = true

  if (sections.length === 0) {
    return null
  }

  const railShadow = isRtl
    ? 'shadow-[-6px_0_32px_-10px_rgba(0,0,0,0.55)]'
    : 'shadow-[6px_0_32px_-10px_rgba(0,0,0,0.55)]'

  return (
    <aside
      className={`fixed inset-y-0 z-40 flex h-full flex-col border-slate-800/90 bg-gradient-to-b from-slate-900/92 via-slate-900/88 to-slate-950/94 supports-[backdrop-filter]:backdrop-blur-2xl ${railShadow} transition-[width] duration-300 ease-out motion-reduce:transition-none ${
        isRtl ? 'right-0 border-l' : 'left-0 border-r'
      } ${sidebarCollapsed ? 'w-16' : 'w-60'}`}
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
        <Logo compact className="relative z-[1] drop-shadow-sm" />
      </div>

      <nav
        className="sidebar-nav relative z-[1] min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-2.5 py-6 sm:px-3.5"
        aria-label="Sidebar navigation"
        style={{ scrollBehavior: 'smooth' }}
      >
        {sections.map((section) => (
          <SidebarSection
            key={section.id}
            section={section}
            collapsed={sidebarCollapsed}
            isDark={sidebarNavDark}
            isRtl={isRtl}
            t={t}
          />
        ))}
      </nav>
    </aside>
  )
}
