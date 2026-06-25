export interface SidebarItem {
  id: string
  label: string
  href: string
}

export interface SidebarSection {
  id: string
  label: string
  items: SidebarItem[]
}

/** All sidebar sections and items. Visibility is controlled by NAV_READ_PERMISSIONS (ABAC) or the backend nav list. */
export const SIDEBAR_SECTIONS: SidebarSection[] = [
  {
    id: 'main',
    label: 'MAIN',
    items: [
      { id: 'dashboard', label: 'Dashboard', href: '/dashboard' },
      { id: 'main_find_customer', label: 'Search users', href: '/admin/find-customer' },
      { id: 'main_deleted_items', label: 'Search deleted items', href: '/admin/deleted-items' },
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
      { id: 'admin_cash', label: 'Cash Reconciliation', href: '/admin/cash' },
      { id: 'payouts', label: 'Provider Payouts', href: '/management/payouts' },
      { id: 'discounts', label: 'Discounts', href: '/management/discounts' },
      { id: 'media_approvals', label: 'Media Approvals', href: '/media-approvals' },
      { id: 'profile_edit_requests', label: 'Profile Edits', href: '/admin/profile-edit-requests' },
      { id: 'identity_verifications', label: 'ID Verifications', href: '/admin/identity-verifications' },
      { id: 'reports', label: 'Reports', href: '/management/reports' },
      { id: 'taxi_trips', label: 'Taxi trips', href: '/management/taxi-trips' },
      { id: 'currency_rates', label: 'Currency rates', href: '/management/currency-rates' },
      { id: 'map', label: 'Map', href: '/map' },
    ],
  },
  {
    id: 'support',
    label: 'SUPPORT',
    items: [
      { id: 'complaints', label: 'Complaints', href: '/support/complaints' },
      { id: 'reviews', label: 'Reviews', href: '/support/reviews' },
      { id: 'provider_messages', label: 'Provider Messages', href: '/support/tickets' },
    ],
  },
  {
    id: 'admin',
    label: 'ADMIN',
    items: [
      { id: 'settings', label: 'Settings', href: '/admin/settings' },
      { id: 'users', label: 'Groups', href: '/management/users' },
      { id: 'roles', label: 'Roles', href: '/admin/roles' },
      { id: 'subscriptions', label: 'Subscriptions', href: '/admin/subscriptions' },
      { id: 'logs', label: 'Logs', href: '/admin/logs' },
      { id: 'content', label: 'Content', href: '/admin/content' },
      { id: 'integrations', label: 'Integrations', href: '/admin/integrations' },
      { id: 'webhooks', label: 'Webhooks', href: '/admin/webhooks' },
    ],
  },
]

/** Canonical ordered list of company-dashboard nav item ids (keep aligned with backend CompanySidebarCatalog). */
export const ALL_SIDEBAR_ITEM_IDS: string[] = SIDEBAR_SECTIONS.flatMap((s) => s.items.map((i) => i.id))

/**
 * AUTHORITATIVE access gate for ABAC sidebar visibility.
 * Each entry maps a nav item ID to the permission code a user must hold to see it.
 * Undefined = always visible (no permission gate, e.g. dashboard).
 *
 * Rule: every new sidebar item MUST have a corresponding entry here AND in NAV_PERMISSION_MAP (RolesPage.tsx).
 * Do NOT gate visibility using role checks elsewhere — this map is the single source of truth.
 */
export const NAV_READ_PERMISSIONS: Record<string, string | undefined> = {
  dashboard:          undefined,
  main_find_customer: 'customers:read',
  main_deleted_items: 'deleted_items:company:read',
  hotels:             'services:read',
  resorts:            'services:read',
  restaurants:        'services:read',
  taxis:              'taxi:read',
  trips:              'services:read',
  providers:          'providers:read',
  bookings:           'bookings:read',
  payments:           'payments:read',
  admin_cash:         'payments:cash-reconcile',
  payouts:            'payouts:read',
  discounts:          'discounts:read',
  media_approvals:    'media_submissions:approve',
  profile_edit_requests: 'profile_edits:approve',
  identity_verifications: 'customers:read',
  reports:            'reports:read',
  taxi_trips:         'taxi:read',
  currency_rates:     'currency:read',
  complaints:         'complaints:read',
  reviews:            'reviews:read',
  provider_messages:  'providers_messages:read',
  settings:           'settings:read',
  users:              'users:read',
  roles:              'roles:read',
  logs:               'audit:read',
  content:            'content:read',
  integrations:       'settings:read',
  webhooks:           'webhooks:read',
  subscriptions:      'providers:read',
  map:                'providers:read',
}

/** Derive visible sidebar item IDs purely from the user's permission set (ABAC). */
export function getVisibleItemIdsFromPermissions(has: (code: string) => boolean): string[] {
  return ALL_SIDEBAR_ITEM_IDS.filter((id) => {
    const perm = NAV_READ_PERMISSIONS[id]
    return perm === undefined || has(perm)
  })
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

/** Permission gates for portal sidebar items. Undefined = always visible. */
export const PORTAL_NAV_PERMISSIONS: Record<string, string | undefined> = {
  portal_cash: 'portal:finance',
  earnings:    'portal:finance',
  discounts:   'portal:finance',
  rooms:       'portal:manage',
  menu:        'portal:manage',
}

/** Filter portal sidebar sections to only items the user has permission to see. */
export function filterPortalSectionsByPermissions(
  has: (code: string) => boolean,
  sections: SidebarSection[] = PORTAL_SIDEBAR_SECTIONS,
): SidebarSection[] {
  return sections
    .map((s) => ({
      ...s,
      items: s.items.filter((i) => {
        const perm = PORTAL_NAV_PERMISSIONS[i.id]
        return perm === undefined || has(perm)
      }),
    }))
    .filter((s) => s.items.length > 0)
}

/** Shared portal items present for every provider type. */
const PORTAL_COMMON_ITEMS: SidebarItem[] = [
  { id: 'overview',    label: 'Overview',   href: '/portal' },
  { id: 'bookings',   label: 'Bookings',   href: '/portal/bookings' },
  { id: 'portal_cash', label: 'Cash Sheet', href: '/portal/cash' },
  { id: 'staff',      label: 'Staff',      href: '/portal/staff' },
  { id: 'earnings',   label: 'Earnings',   href: '/portal/earnings' },
  { id: 'discounts',  label: 'Discounts',  href: '/portal/discounts' },
  { id: 'media',      label: 'Media',      href: '/portal/media' },
  { id: 'portal_map', label: 'Map',        href: '/portal/map' },
  { id: 'profile',    label: 'Profile',    href: '/portal/profile' },
  { id: 'support',    label: 'Support',    href: '/portal/support' },
]

/** Client Portal (Provider Dashboard) sidebar – hotel / resort / apartment / default. */
export const PORTAL_SIDEBAR_SECTIONS: SidebarSection[] = [
  {
    id: 'portal',
    label: 'PORTAL',
    items: [
      PORTAL_COMMON_ITEMS[0],
      { id: 'rooms', label: 'Rooms', href: '/portal/rooms' },
      ...PORTAL_COMMON_ITEMS.slice(1),
    ],
  },
]

/** Restaurant-specific portal sidebar. */
export const PORTAL_SIDEBAR_SECTIONS_RESTAURANT: SidebarSection[] = [
  {
    id: 'portal',
    label: 'PORTAL',
    items: [
      PORTAL_COMMON_ITEMS[0],
      { id: 'menu', label: 'Menu', href: '/portal/menu' },
      ...PORTAL_COMMON_ITEMS.slice(1),
    ],
  },
]

/** Generic portal sidebar for provider types without room/menu management (taxi, trip, etc.). */
export const PORTAL_SIDEBAR_SECTIONS_DEFAULT: SidebarSection[] = [
  {
    id: 'portal',
    label: 'PORTAL',
    items: [...PORTAL_COMMON_ITEMS],
  },
]

/** Return the correct portal sidebar sections based on provider type string (e.g. "HOTEL", "RESTAURANT"). */
export function getPortalSidebarSections(providerType: string | null | undefined): SidebarSection[] {
  const t = (providerType ?? '').toUpperCase()
  if (t === 'RESTAURANT') return PORTAL_SIDEBAR_SECTIONS_RESTAURANT
  if (t === 'HOTEL' || t === 'RESORT' || t === 'APARTMENT' || t === 'EVENT_SPACE') return PORTAL_SIDEBAR_SECTIONS
  return PORTAL_SIDEBAR_SECTIONS_DEFAULT
}
