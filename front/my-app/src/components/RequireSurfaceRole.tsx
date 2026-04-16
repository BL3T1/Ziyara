import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { isCompanyStaffRole, isProviderPortalRole, type Role } from '../types/auth'

type SurfaceGate = 'company' | 'provider'

function roleAllowed(surface: SurfaceGate, role: Role): boolean {
  if (surface === 'company') return isCompanyStaffRole(role)
  return isProviderPortalRole(role)
}

/**
 * Ensures authenticated user is allowed on this app surface (company vs provider bundle).
 * Clears session and sends to login if wrong role (tampered storage / old token).
 */
export function RequireSurfaceRole({ surface }: { surface: SurfaceGate }) {
  const { user, clearAuth } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!roleAllowed(surface, user.role)) {
    clearAuth()
    return (
      <Navigate
        to="/login"
        replace
        state={{ portalError: 'wrong_portal' as const }}
      />
    )
  }

  return <Outlet />
}
