import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { isCompanySurface, isProviderSurface } from '../config/appSurface'
import { isCompanyStaffRole, isProviderPortalRole } from '../types/auth'
import { getDashboardRouteForRole } from '../utils/routes'

export type PortalLoginError =
  | 'wrong_portal'
  | 'company_staff_only'
  | 'partner_only'
  | 'customer_not_company'

/**
 * Root path "/" redirect: unauthenticated → /login, authenticated → role dashboard.
 * Clears session if stored user is not allowed on this app surface (wrong bundle / stale data).
 */
export function HomeRedirect() {
  const { user, clearAuth } = useAuth()
  const location = useLocation()
  if (!user) {
    // Preserve the deep-link destination so RequireAuth's `from` state reaches LoginPage
    const from = (location.state as { from?: { pathname: string } } | null)?.from
    return <Navigate to="/login" replace state={from ? { from } : undefined} />
  }

  if (isCompanySurface) {
    if (user.role === 'provider') {
      clearAuth()
      return <Navigate to="/login" replace state={{ portalError: 'partner_only' satisfies PortalLoginError }} />
    }
    if (user.role === 'user') {
      clearAuth()
      return (
        <Navigate to="/login" replace state={{ portalError: 'customer_not_company' satisfies PortalLoginError }} />
      )
    }
    if (!isCompanyStaffRole(user.role)) {
      clearAuth()
      return <Navigate to="/login" replace state={{ portalError: 'wrong_portal' satisfies PortalLoginError }} />
    }
  }

  if (isProviderSurface && !isProviderPortalRole(user.role)) {
    clearAuth()
    return (
      <Navigate to="/login" replace state={{ portalError: 'company_staff_only' satisfies PortalLoginError }} />
    )
  }

  return <Navigate to={getDashboardRouteForRole(user.role)} replace />
}
