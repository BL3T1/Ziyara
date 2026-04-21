/**
 * Aggregated dashboard “surface” for navigation — not the same as backend {@code UserRole}.
 * - {@code user}: B2C consumer (backend CUSTOMER); not company staff.
 * - {@code provider}: partner portal (PROVIDER_*, TAXI_OPERATOR).
 * - Other values: company internal staff (JWT UserRole SALES_*, FINANCE_*, etc.).
 */
export type Role =
  | 'super_admin'
  | 'admin'
  | 'finance'
  | 'support'
  | 'executive'
  | 'hr'
  | 'provider'
  | 'user'

export interface User {
  id: string
  email: string
  name: string
  role: Role
}

export const ROLE_LABELS: Record<Role, string> = {
  super_admin: 'Super Admin',
  admin: 'Admin',
  finance: 'Finance',
  support: 'Support',
  executive: 'Executive',
  hr: 'HR',
  provider: 'Provider',
  user: 'User',
}

/** Map backend UserRole enum to frontend Role (org groups Z1–Z6 internal staff, Z7 customer) */
export function backendRoleToFrontend(backendRole: string): Role {
  const r = (backendRole || '').toUpperCase()
  if (r === 'SUPER_ADMIN') return 'super_admin'
  if (r === 'HR_MANAGER') return 'hr'
  if (r === 'CEO') return 'executive'
  /** General Manager: same dashboard scope as sales managers, not discount approver (CEO / Super Admin only). */
  if (r === 'GENERAL_MANAGER') return 'admin'
  if (r.startsWith('SALES_')) return 'admin'
  if (r.startsWith('FINANCE_') || r === 'ACCOUNTANT') return 'finance'
  if (r.startsWith('SUPPORT_')) return 'support'
  if (r.startsWith('PROVIDER_') || r === 'TAXI_OPERATOR') return 'provider'
  if (r === 'CUSTOMER') return 'user'
  return 'user'
}

/** Company internal dashboard: staff only (excludes B2C customers and provider portal accounts). */
export function isCompanyStaffRole(role: Role): boolean {
  return role !== 'provider' && role !== 'user'
}

/** Provider partner portal (hotels, taxis, etc.). */
export function isProviderPortalRole(role: Role): boolean {
  return role === 'provider'
}

/** True if the raw role string is super admin (handles legacy / mismatched casing in storage). */
export function isSuperAdminRole(role: string | null | undefined): boolean {
  if (!role) return false
  const n = String(role).trim()
  if (n === 'super_admin') return true
  return n.toUpperCase().replace(/-/g, '_') === 'SUPER_ADMIN'
}

/** May submit a new discount (often pending until Super Admin or CEO approves). */
export function canCreateDiscount(role: Role): boolean {
  return (
    role === 'super_admin' ||
    role === 'executive' ||
    role === 'admin' ||
    role === 'finance' ||
    role === 'hr'
  )
}

/** May approve pending discounts (backend: Super Admin and CEO JWT roles only). */
export function canApproveDiscount(role: Role): boolean {
  return role === 'super_admin' || role === 'executive'
}

/** Create partner (provider) accounts: Super Admin / CEO (active) or Sales (pending). */
export function canCreateProvider(role: Role): boolean {
  return role === 'super_admin' || role === 'executive' || role === 'admin'
}

/** Approve or reject pending provider submissions (backend: SUPER_ADMIN, CEO). */
export function canApproveRejectProvider(role: Role): boolean {
  return role === 'super_admin' || role === 'executive'
}

/** Commission rates (Phase 6): finance and executive visibility; aligns with sensitive payout data. */
export function canViewProviderCommission(role: Role): boolean {
  return role === 'super_admin' || role === 'executive' || role === 'finance'
}
