/**
 * Build-time app surface: which product this bundle serves (multi-domain Docker builds).
 * Set via VITE_APP_SURFACE at build time. Defaults to `company` for local dev / single dashboard.
 */
export type AppSurface = 'company' | 'provider' | 'landing'

const RAW = (import.meta.env.VITE_APP_SURFACE ?? 'company').toLowerCase()

function parseSurface(value: string): AppSurface {
  if (value === 'provider' || value === 'landing' || value === 'company') return value
  return 'company'
}

export const APP_SURFACE: AppSurface = parseSurface(RAW)

export const isCompanySurface = APP_SURFACE === 'company'
export const isProviderSurface = APP_SURFACE === 'provider'
export const isLandingSurface = APP_SURFACE === 'landing'

/** Public marketing site login URLs (absolute); used on landing CTAs. */
export const VITE_COMPANY_APP_URL = import.meta.env.VITE_COMPANY_APP_URL ?? ''
export const VITE_PROVIDER_APP_URL = import.meta.env.VITE_PROVIDER_APP_URL ?? ''

/** Company dashboard URL for cross-links from the provider bundle (relative when env unset). */
export function resolveCompanyDashboardUrl(): string {
  const b = VITE_COMPANY_APP_URL.replace(/\/$/, '')
  return b ? `${b}/dashboard` : '/dashboard'
}

/**
 * Reserved for a future ticket system. The legacy `/support/tickets` UI is not routed in the app bundle.
 */
export function resolveCompanyTicketsUrl(): string {
  const b = VITE_COMPANY_APP_URL.replace(/\/$/, '')
  return b ? `${b}/support/tickets` : '/support/tickets'
}
