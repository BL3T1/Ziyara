import type { Role } from '../types/auth'

/**
 * Dashboard (or portal) route for a given role after login.
 */
export function getDashboardRouteForRole(role: Role): string {
  if (role === 'provider') return '/portal'
  if (role === 'admin') return '/dashboard'
  return '/dashboard'
}
