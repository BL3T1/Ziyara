/**
 * Aggregated dashboard “surface” for navigation — not the same as backend {@code UserRole}.
 * Backend only sends SUPER_ADMIN, CUSTOMER, or STAFF. Use {@code backendRoleToFrontend}
 * (which also accepts {@code hasPortalAccess}) to map to a frontend Role.
 * - {@code user}: B2C consumer (backend CUSTOMER).
 * - {@code provider}: partner portal (backend STAFF with portal:access permission).
 * - Other values: company internal staff (backend STAFF without portal access).
 */
export type Role =
  | 'super_admin'
  | 'admin'
  | 'finance'
  | 'support'
  | 'executive'
  | 'provider'
  | 'user'
  | 'staff'

export interface User {
  id: string
  email: string
  name: string
  role: Role
  mustChangePassword?: boolean
}

export const ROLE_LABELS: Record<Role, string> = {
  super_admin: 'Super Admin',
  admin: 'Admin',
  finance: 'Finance',
  support: 'Support',
  executive: 'Executive',
  provider: 'Provider',
  user: 'User',
  staff: 'Staff',
}

/** Map backend UserRole enum + portal flag to frontend Role */
export function backendRoleToFrontend(backendRole: string, hasPortalAccess?: boolean): Role {
  const r = (backendRole || '').toUpperCase()
  if (r === 'SUPER_ADMIN') return 'super_admin'
  if (r === 'CUSTOMER') return 'user'
  if (hasPortalAccess) return 'provider'
  // STAFF without portal access = internal company staff
  return 'staff'
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

