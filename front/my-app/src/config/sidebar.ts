import type { Role } from '../types/auth'

export interface SidebarItem {
  id: string
  label: string
  href: string
  /** If set, only these roles see this item (e.g. Users only for super_admin). */
  allowedRoles?: Role[]
}

export interface SidebarSection {
  id: string
  label: string
  items: SidebarItem[]
}

/** All sidebar sections and items. Visibility is controlled by role. */
export const SIDEBAR_SECTIONS: SidebarSection[] = [
  {
    id: 'main',
    label: 'MAIN',
    items: [
      { id: 'dashboard', label: 'Dashboard', href: '/dashboard' },
      {
        id: 'main_find_customer',
        label: 'Search users',
        href: '/admin/find-customer',
        allowedRoles: ['super_admin'],
      },
      {
        id: 'main_deleted_items',
        label: 'Search deleted items',
        href: '/admin/deleted-items',
        allowedRoles: ['super_admin'],
      },
    ],
  },
  {
    id: 'services',
    label: 'SERVICES',
    items: [
      { id: 'hotels', label: 'Hotels', href: '/services/hotels' },
      { id: 'resorts', label: 'Resorts', href: '/services/resorts' },
      { id: 'restaurants', label: 'Restaurants', href: '/services/restaurants' },
      { id: 'taxis', label: 'Taxis', href: '/services/taxis' },
      { id: 'trips', label: 'Trips', href: '/services/trips' },
    ],
  },
  {
    id: 'management',
    label: 'MANAGEMENT',
    items: [
      { id: 'providers', label: 'Providers', href: '/management/providers' },
      { id: 'bookings', label: 'Bookings', href: '/management/bookings' },
      { id: 'payments', label: 'Payments', href: '/management/payments' },
      { id: 'discounts', label: 'Discounts', href: '/management/discounts' },
      { id: 'reports', label: 'Reports', href: '/management/reports' },
      { id: 'taxi_trips', label: 'Taxi trips', href: '/management/taxi-trips' },
      {
        id: 'currency_rates',
        label: 'Currency rates',
        href: '/management/currency-rates',
        allowedRoles: ['super_admin', 'executive', 'finance'],
      },
    ],
  },
  {
    id: 'support',
    label: 'SUPPORT',
    items: [
      { id: 'complaints', label: 'Complaints', href: '/support/complaints' },
      { id: 'reviews', label: 'Reviews', href: '/support/reviews' },
    ],
  },
  {
    id: 'admin',
    label: 'ADMIN',
    items: [
      { id: 'settings', label: 'Settings', href: '/admin/settings' },
      { id: 'users', label: 'Groups', href: '/management/users', allowedRoles: ['super_admin', 'hr'] },
      { id: 'roles', label: 'Roles', href: '/admin/roles', allowedRoles: ['super_admin'] },
      { id: 'logs', label: 'Logs', href: '/admin/logs' },
      { id: 'content', label: 'Content', href: '/admin/content', allowedRoles: ['super_admin', 'admin'] },
      { id: 'api', label: 'API', href: '/admin/api', allowedRoles: ['super_admin'] },
      {
        id: 'integrations',
        label: 'Integrations',
        href: '/admin/integrations',
        allowedRoles: ['super_admin', 'executive', 'admin'],
      },
    ],
  },
]

/** Canonical ordered list of company-dashboard nav item ids (keep aligned with backend CompanySidebarCatalog). */
export const ALL_SIDEBAR_ITEM_IDS: string[] = SIDEBAR_SECTIONS.flatMap((s) => s.items.map((i) => i.id))

/** Section IDs visible per role. Empty = no sidebar sections. */
export const ROLE_SIDEBAR_SECTIONS: Record<Role, string[]> = {
  super_admin: ['main', 'services', 'management', 'admin'],
  admin: ['main', 'services', 'management', 'support'],
  finance: ['main', 'management'],
  support: ['main', 'support'],
  executive: ['main', 'services', 'management', 'support'],
  hr: ['main', 'management', 'admin'],
  provider: ['main', 'services'],
  user: ['main', 'services'],
}

export function getSidebarSectionsForRole(role: Role): SidebarSection[] {
  const sectionIds = ROLE_SIDEBAR_SECTIONS[role] ?? []
  return SIDEBAR_SECTIONS.filter((s) => sectionIds.includes(s.id)).map((section) => ({
    ...section,
    items: section.items.filter(
      (item) => !item.allowedRoles || item.allowedRoles.includes(role)
    ),
  }))
}

/**
 * Filter static sections to only items in {@code visibleItemIds}, preserving global order
 * (section blocks appear when their first item appears in the id list).
 */
export function filterSectionsByVisibleIds(
  sections: SidebarSection[],
  visibleItemIds: string[]
): SidebarSection[] {
  if (!visibleItemIds.length) return []
  const itemById = new Map<string, { section: SidebarSection; item: SidebarItem }>()
  for (const section of sections) {
    for (const item of section.items) {
      itemById.set(item.id, { section, item })
    }
  }
  const sectionOrder: string[] = []
  const itemsBySection = new Map<string, SidebarItem[]>()
  for (const id of visibleItemIds) {
    const found = itemById.get(id)
    if (!found) continue
    const sid = found.section.id
    if (!itemsBySection.has(sid)) {
      sectionOrder.push(sid)
      itemsBySection.set(sid, [])
    }
    const list = itemsBySection.get(sid)!
    if (!list.some((i) => i.id === id)) list.push(found.item)
  }
  return sectionOrder.map((sid) => {
    const section = sections.find((s) => s.id === sid)!
    return { ...section, items: itemsBySection.get(sid)! }
  })
}

/** Client Portal (Provider Dashboard) sidebar – scoped to provider. */
export const PORTAL_SIDEBAR_SECTIONS: SidebarSection[] = [
  {
    id: 'portal',
    label: 'PORTAL',
    items: [
      { id: 'overview', label: 'Overview', href: '/portal' },
      { id: 'listings', label: 'Listings', href: '/portal/listings' },
      { id: 'bookings', label: 'Bookings', href: '/portal/bookings' },
      { id: 'staff', label: 'Staff', href: '/portal/staff' },
      { id: 'earnings', label: 'Earnings', href: '/portal/earnings' },
      { id: 'profile', label: 'Profile', href: '/portal/profile' },
      { id: 'support', label: 'Support', href: '/portal/support' },
    ],
  },
]
