import { useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { isCompanyStaffRole } from '../types/auth'
import { usersAPI } from '../services/api'
import type { UserNavigationDto } from '../types/api'

/** Loads GET /users/me/navigation for company staff so Sidebar can use RBAC-visible items. */
export function CompanyNavBootstrap() {
  const { user, isAuthenticated, setSidebarNav } = useAuth()

  useEffect(() => {
    if (!isAuthenticated || !user) {
      setSidebarNav(null)
      return
    }
    if (!isCompanyStaffRole(user.role)) {
      setSidebarNav(null)
      return
    }
    let cancelled = false
    usersAPI
      .getMyNavigation()
      .then((res) => {
        if (cancelled) return
        const d = res.data as UserNavigationDto
        if (
          d?.source === 'rbac_role' &&
          d.visibleItemIds &&
          Array.isArray(d.visibleItemIds) &&
          d.visibleItemIds.length > 0
        ) {
          setSidebarNav({ visibleItemIds: d.visibleItemIds, source: 'rbac_role' })
        } else {
          setSidebarNav(null)
        }
      })
      .catch(() => {
        if (!cancelled) setSidebarNav(null)
      })
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, user?.id, user?.role, setSidebarNav])

  return null
}
