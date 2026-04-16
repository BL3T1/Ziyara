import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import type { User } from '../types/auth'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export interface SidebarNavigationState {
  visibleItemIds: string[]
  source: string
}

function loadStoredUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    const u = JSON.parse(raw) as User
    return u && u.id && u.email ? u : null
  } catch {
    return null
  }
}

interface AuthContextValue {
  user: User | null
  setUser: (user: User | null) => void
  isAuthenticated: boolean
  logout: () => void
  /** Clear token + user (e.g. wrong portal after login). */
  clearAuth: () => void
  /** Company dashboard: from GET /users/me/navigation; null = use role-based static sidebar. */
  sidebarNav: SidebarNavigationState | null
  setSidebarNav: (nav: SidebarNavigationState | null) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

interface AuthProviderProps {
  children: ReactNode
  defaultUser?: User | null
}

export function AuthProvider({ children, defaultUser = null }: AuthProviderProps) {
  const [user, setUserState] = useState<User | null>(() => loadStoredUser() ?? defaultUser ?? null)
  const [sidebarNav, setSidebarNav] = useState<SidebarNavigationState | null>(null)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token && user) {
      setUserState(null)
      localStorage.removeItem(USER_KEY)
    }
  }, [user])

  const setUser = useCallback((u: User | null) => {
    setUserState(u)
    if (u) localStorage.setItem(USER_KEY, JSON.stringify(u))
    else localStorage.removeItem(USER_KEY)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUserState(null)
    setSidebarNav(null)
  }, [])

  const clearAuth = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUserState(null)
    setSidebarNav(null)
  }, [])

  const value: AuthContextValue = {
    user: user ?? null,
    setUser,
    isAuthenticated: user !== null,
    logout,
    clearAuth,
    sidebarNav,
    setSidebarNav,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}
