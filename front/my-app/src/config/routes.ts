import type { SidebarIconId } from '../components/SidebarIcons'

/**
 * Path-to-page-title mapping for header display.
 * Used by PageLayout to set DashboardHeader pageTitle.
 */
export const ROUTE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/sales': 'Sales',
  '/analytics': 'Analytics',
  '/services/hotels': 'Hotels',
  '/services/resorts': 'Resorts',
  '/services/restaurants': 'Restaurants',
  '/services/taxis': 'Taxis',
  '/services/trips': 'Trips',
  '/management/users': 'Groups',
  '/management/providers': 'Providers',
  '/management/providers/new': 'New provider',
  '/management/bookings': 'Bookings',
  '/management/payments': 'Payments',
  '/management/discounts': 'Discounts',
  '/management/reports': 'Reports',
  '/support/complaints': 'Complaints',
  '/support/reviews': 'Reviews',
  '/support/tickets': 'Provider messages',
  '/management/taxi-trips': 'Taxi trips',
  '/management/currency-rates': 'Currency rates',
  '/admin/settings': 'Settings',
  '/admin/roles': 'Roles',
  '/admin/logs': 'Logs',
  '/admin/content': 'Content',
  '/admin/api': 'API',
  '/admin/integrations': 'Integrations',
}

/** Path to i18n title key (e.g. 'title.dashboard') for translated page titles. */
export const ROUTE_TITLE_KEYS: Record<string, string> = {
  '/dashboard': 'title.dashboard',
  '/sales': 'title.salesDashboard',
  '/analytics': 'title.analytics',
  '/services/hotels': 'title.hotels',
  '/services/resorts': 'title.resorts',
  '/services/restaurants': 'title.restaurants',
  '/services/taxis': 'title.taxis',
  '/services/trips': 'title.trips',
  '/management/users': 'title.groups',
  '/management/providers': 'title.providers',
  '/management/providers/new': 'title.providerNew',
  '/management/bookings': 'title.bookings',
  '/management/payments': 'title.payments',
  '/management/discounts': 'title.discounts',
  '/management/reports': 'title.reports',
  '/support/complaints': 'title.complaints',
  '/support/reviews': 'title.reviews',
  '/support/tickets': 'title.tickets',
  '/management/taxi-trips': 'title.taxiTrips',
  '/management/currency-rates': 'title.currencyRates',
  '/admin/settings': 'title.settings',
  '/admin/roles': 'title.roles',
  '/admin/logs': 'title.logs',
  '/admin/content': 'title.content',
  '/admin/api': 'title.api',
  '/admin/integrations': 'title.integrations',
  '/portal': 'title.portalOverview',
  '/portal/listings/new': 'title.listingNew',
  '/portal/listings': 'title.listings',
  '/portal/bookings': 'title.bookings',
  '/portal/staff': 'title.staff',
  '/portal/earnings': 'title.earnings',
  '/portal/profile': 'title.profile',
  '/portal/support': 'title.portalSupport',
}

/** Path to header icon (sidebar item id). Header shows this icon for the current page. */
export const ROUTE_ICONS: Record<string, SidebarIconId> = {
  '/dashboard': 'dashboard',
  '/sales': 'analytics',
  '/analytics': 'analytics',
  '/services/hotels': 'hotels',
  '/services/resorts': 'resorts',
  '/services/restaurants': 'restaurants',
  '/services/taxis': 'taxis',
  '/services/trips': 'trips',
  '/management/users': 'users',
  '/management/providers': 'providers',
  '/management/providers/new': 'providers',
  '/management/bookings': 'bookings',
  '/management/payments': 'payments',
  '/management/discounts': 'discounts',
  '/management/reports': 'reports',
  '/support/complaints': 'complaints',
  '/support/reviews': 'reviews',
  '/support/tickets': 'chat',
  '/management/taxi-trips': 'taxi_trips',
  '/management/currency-rates': 'sales',
  '/admin/settings': 'settings',
  '/admin/roles': 'roles',
  '/admin/logs': 'logs',
  '/admin/content': 'content',
  '/admin/api': 'api',
  '/admin/integrations': 'integrations',
  '/portal': 'dashboard',
  '/portal/listings/new': 'hotels',
  '/portal/listings': 'hotels',
  '/portal/bookings': 'bookings',
  '/portal/staff': 'users',
  '/portal/earnings': 'sales',
  '/portal/profile': 'settings',
  '/portal/support': 'chat',
}

const SERVICE_TYPE_TITLES: Record<string, string> = {
  hotels: 'Hotels',
  resorts: 'Resorts',
  restaurants: 'Restaurants',
  taxis: 'Taxis',
  trips: 'Trips',
}

export function getPageTitleForPath(pathname: string): string {
  const exact = ROUTE_TITLES[pathname]
  if (exact) return exact
  const providerEdit = pathname.match(/^\/management\/providers\/([^/]+)$/)
  if (
    providerEdit &&
    providerEdit[1] !== 'new' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(providerEdit[1])
  ) {
    return 'Edit provider'
  }
  const servicesMatch = pathname.match(/^\/services\/([^/]+)(?:\/([^/]+))?/)
  if (servicesMatch) {
    const [, type] = servicesMatch
    return servicesMatch[2] ? 'Service detail' : (SERVICE_TYPE_TITLES[type] ?? type)
  }
  return 'System Overview'
}

/** Returns i18n key for page title (e.g. 'title.dashboard'). Use with t(key). */
export function getPageTitleKeyForPath(pathname: string): string {
  const exact = ROUTE_TITLE_KEYS[pathname]
  if (exact) return exact
  const providerEdit = pathname.match(/^\/management\/providers\/([^/]+)$/)
  if (
    providerEdit &&
    providerEdit[1] !== 'new' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(providerEdit[1])
  ) {
    return 'title.providerEdit'
  }
  if (pathname.startsWith('/portal/listings/') && pathname !== '/portal/listings/new') {
    return 'title.listingEdit'
  }
  const servicesMatch = pathname.match(/^\/services\/([^/]+)(?:\/([^/]+))?/)
  if (servicesMatch) {
    const [, type] = servicesMatch
    const typeKey = type as keyof typeof SERVICE_TYPE_TITLES
    return servicesMatch[2] ? 'title.serviceDetail' : `title.${typeKey in SERVICE_TYPE_TITLES ? typeKey : 'dashboard'}`
  }
  return 'title.dashboard'
}

export function getPageIconForPath(pathname: string): SidebarIconId {
  const exact = ROUTE_ICONS[pathname]
  if (exact) return exact
  const providerEdit = pathname.match(/^\/management\/providers\/([^/]+)$/)
  if (
    providerEdit &&
    providerEdit[1] !== 'new' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(providerEdit[1])
  ) {
    return 'providers'
  }
  if (pathname.startsWith('/portal/listings')) return 'hotels'
  if (pathname.startsWith('/support/reviews')) return 'reviews'
  const servicesMatch = pathname.match(/^\/services\/([^/]+)/)
  if (servicesMatch) {
    const icon = ROUTE_ICONS[`/services/${servicesMatch[1]}`]
    if (icon) return icon
  }
  return 'dashboard'
}
