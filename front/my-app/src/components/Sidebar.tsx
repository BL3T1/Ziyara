import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { usePendingCounts } from '../context/PendingCountsContext'
import { SIDEBAR_SECTIONS, filterSectionsByVisibleIds, getVisibleItemIdsFromPermissions } from '../config/sidebar'
import type { SidebarSection as SidebarSectionType, SidebarItem } from '../config/sidebar'
import { usePermissions } from '../context/PermissionsContext'
import { Logo } from './Logo'
import { SidebarIcons, type SidebarIconId } from './SidebarIcons'

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

const SIDEBAR_ITEM_ICON: Partial<Record<string, SidebarIconId>> = {
  main_find_customer: 'search_user',
  main_deleted_items: 'search_deleted',
  currency_rates: 'sales',
  provider_messages: 'chat',
  map: 'map',
}

function getIconForItem(item: SidebarItem) {
  const iconId = (SIDEBAR_ITEM_ICON[item.id] ?? item.id) as SidebarIconId
  return SidebarIcons[iconId] ?? SidebarIcons.dashboard
}

function prefetchDashboardRouteChunks() {
  void import('../pages/DashboardPage')
  void import('../pages/SalesDashboardPage')
}

function SidebarSection({
  section,
  collapsed,
  isRtl,
  t,
  badges,
  onNavClick,
}: {
  section: SidebarSectionType
  collapsed: boolean
  isRtl: boolean
  t: (key: string) => string
  badges: Record<string, number>
  onNavClick?: () => void
}) {
  return (
    <div className="mb-6 last:mb-2">
      {!collapsed && (
        <div className="mb-1.5 flex items-center gap-2 px-3">
          <span
            className="text-[0.6rem] font-bold uppercase tracking-[0.18em] text-slate-400"
          >
            {t(`section.${section.id}`)}
          </span>
        </div>
      )}
      <div className="space-y-0.5">
        {section.items.map((item) => {
          const itemLabel = t(`nav.${item.id}`)
          const badgeCount = badges[item.id] ?? 0
          return (
            <NavLink
              key={item.id}
              to={item.href}
              end={item.href === '/dashboard'}
              title={collapsed ? itemLabel : undefined}
              onMouseEnter={item.href === '/dashboard' ? prefetchDashboardRouteChunks : undefined}
              onFocus={item.href === '/dashboard' ? prefetchDashboardRouteChunks : undefined}
              onClick={onNavClick}
            >
              {({ isActive }) => {
                const pad = collapsed ? 'justify-center px-0' : 'px-2'
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
                    <span className={`${iconWrap} relative`}>
                      {getIconForItem(item)}
                      {badgeCount > 0 && collapsed && (
                        <span className="absolute -right-0.5 -top-0.5 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-red-500 text-[0.55rem] font-bold text-white">
                          {badgeCount > 9 ? '9+' : badgeCount}
                        </span>
                      )}
                    </span>
                    {!collapsed && (
                      <>
                        <span className="flex-1 truncate text-[0.8125rem]">{itemLabel}</span>
                        {badgeCount > 0 && (
                          <span className="flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-red-500 px-1 text-[0.6rem] font-bold text-white">
                            {badgeCount > 99 ? '99+' : badgeCount}
                          </span>
                        )}
                      </>
                    )}
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
  const { sidebarNav } = useAuth()
  const { sidebarCollapsed, setSidebarCollapsed, mobileSidebarOpen, setMobileSidebarOpen } = useLayout()
  const { t, locale } = useLanguage()
  const pendingCounts = usePendingCounts()
  const { has } = usePermissions()
  const isRtl = locale === 'ar'
  const sections =
    sidebarNav
      ? filterSectionsByVisibleIds(SIDEBAR_SECTIONS, sidebarNav.visibleItemIds)
      : filterSectionsByVisibleIds(SIDEBAR_SECTIONS, getVisibleItemIdsFromPermissions(has))

  const badges: Record<string, number> = {
    complaints: pendingCounts.complaints,
    reviews: pendingCounts.reviews,
    media_approvals: pendingCounts.media_approvals,
  }

  if (sections.length === 0) return null

  const railBorder = isRtl ? 'right-0 border-l' : 'left-0 border-r'
  const railShadow = isRtl
    ? 'shadow-[-1px_0_0_0_rgba(255,255,255,0.04),-8px_0_32px_-8px_rgba(0,0,0,0.6)]'
    : 'shadow-[1px_0_0_0_rgba(255,255,255,0.04),8px_0_32px_-8px_rgba(0,0,0,0.6)]'

  const sidebarContent = (
    <aside
      style={{ colorScheme: 'dark' }}
      className={`fixed inset-y-0 z-40 flex h-full flex-col border-white/[0.05] bg-[#0a0e14] ${railShadow} ${railBorder} transition-[width] duration-300 ease-out motion-reduce:transition-none ${
        sidebarCollapsed ? 'w-[3.75rem]' : 'w-60'
      }`}
    >
      {/* Subtle vertical accent line */}
      <div
        aria-hidden
        className={`pointer-events-none absolute inset-y-0 w-px bg-gradient-to-b from-transparent via-[#1e4d6b]/60 to-transparent opacity-60 ${
          isRtl ? 'left-0' : 'right-0'
        }`}
      />

      {/* Logo area */}
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

      {/* Nav */}
      <nav
        className="sidebar-nav relative z-[1] min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-2 py-5 sm:px-2.5"
        aria-label="Sidebar navigation"
      >
        {sections.map((section) => (
          <SidebarSection
            key={section.id}
            section={section}
            collapsed={sidebarCollapsed}
            isRtl={isRtl}
            t={t}
            badges={badges}
            onNavClick={() => setMobileSidebarOpen(false)}
          />
        ))}
      </nav>

      {/* Collapse toggle at bottom */}
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

  return (
    <>
      {/* Desktop sidebar — visible lg+ */}
      <div className="hidden lg:block">{sidebarContent}</div>

      {/* Mobile overlay + drawer */}
      {mobileSidebarOpen && (
        <>
          <div
            className="fixed inset-0 z-30 bg-black/60 lg:hidden"
            aria-hidden
            onClick={() => setMobileSidebarOpen(false)}
          />
          <div className="lg:hidden" onClick={() => setMobileSidebarOpen(false)}>
            {sidebarContent}
          </div>
        </>
      )}
    </>
  )
}
