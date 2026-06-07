/**
 * Fetches and caches the authenticated user's permission codes.
 * Uses GET /users/me/permissions — returns empty set when not authenticated.
 * Cached in sessionStorage so it survives page refreshes within a session.
 */
import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { usersAPI } from '../services/api'

const STORAGE_KEY = 'ziyara_perms'

interface PermissionsContextValue {
  /** True if the current user has the given permission code. Super admin always returns true. */
  has: (code: string) => boolean
  /** Raw permission code array (empty if not loaded or not authenticated). */
  permissions: string[]
  /** True while the initial permissions fetch is in flight. */
  loading: boolean
  /** Manually re-fetch permissions from the server (e.g. after an admin changes the current user's role). */
  refreshPermissions: () => void
}

const PermissionsContext = createContext<PermissionsContextValue | null>(null)

function loadStored(): Set<string> {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return new Set()
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? new Set<string>(arr) : new Set()
  } catch {
    return new Set()
  }
}

export function PermissionsProvider({ children }: { children: ReactNode }) {
  const { user, isAuthenticated } = useAuth()
  const [perms, setPerms] = useState<Set<string>>(() => loadStored())
  const [loading, setLoading] = useState(false)

  const fetchPermissions = useCallback(() => {
    if (!isAuthenticated) {
      sessionStorage.removeItem(STORAGE_KEY)
      setPerms(new Set())
      return
    }
    setLoading(true)
    usersAPI.getMyPermissions()
      .then((res) => {
        const codes = Array.isArray(res.data) ? (res.data as string[]) : []
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(codes))
        setPerms(new Set(codes))
      })
      .catch(() => {
        // Keep stale cached perms rather than breaking the UI
      })
      .finally(() => setLoading(false))
  }, [isAuthenticated])

  // Refetch when the authenticated user changes (login / logout / user switch)
  useEffect(() => {
    if (!isAuthenticated) {
      sessionStorage.removeItem(STORAGE_KEY)
      setPerms(new Set())
    } else {
      fetchPermissions()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, isAuthenticated])

  // Re-fetch when the tab regains focus so that mid-session role changes propagate
  // without requiring a full logout/login cycle.
  useEffect(() => {
    window.addEventListener('focus', fetchPermissions)
    return () => window.removeEventListener('focus', fetchPermissions)
  }, [fetchPermissions])

  const has = useCallback(
    (code: string): boolean => {
      // super_admin is the ROLE_SUPER_ADMIN bearer — they have all permissions
      if (user?.role === 'super_admin') return true
      return perms.has(code)
    },
    [user?.role, perms],
  )

  const value: PermissionsContextValue = {
    has,
    permissions: Array.from(perms),
    loading,
    refreshPermissions: fetchPermissions,
  }

  return <PermissionsContext.Provider value={value}>{children}</PermissionsContext.Provider>
}

export function usePermissions(): PermissionsContextValue {
  const ctx = useContext(PermissionsContext)
  if (!ctx) {
    return { has: () => false, permissions: [], loading: false, refreshPermissions: () => {} }
  }
  return ctx
}
