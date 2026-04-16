import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'

const THEME_KEY = 'ziyara-theme'
const SIDEBAR_KEY = 'ziyara-sidebar-collapsed'
type Theme = 'light' | 'dark'

function loadTheme(): Theme {
  if (typeof window === 'undefined') return 'light'
  const t = localStorage.getItem(THEME_KEY) as Theme | null
  if (t === 'dark' || t === 'light') return t
  if (window.matchMedia('(prefers-color-scheme: dark)').matches) return 'dark'
  return 'light'
}

function loadSidebarCollapsed(): boolean {
  if (typeof window === 'undefined') return false
  const v = localStorage.getItem(SIDEBAR_KEY)
  return v === 'true'
}

function applyTheme(theme: Theme) {
  const el = document.documentElement
  if (theme === 'dark') el.classList.add('dark')
  else el.classList.remove('dark')
}

interface LayoutContextValue {
  theme: Theme
  setTheme: (t: Theme) => void
  toggleTheme: () => void
  sidebarCollapsed: boolean
  setSidebarCollapsed: (v: boolean) => void
  toggleSidebar: () => void
}

const LayoutContext = createContext<LayoutContextValue | null>(null)

export function LayoutProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(loadTheme)
  const [sidebarCollapsed, setSidebarCollapsedState] = useState(loadSidebarCollapsed)

  useEffect(() => {
    applyTheme(theme)
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    localStorage.setItem(SIDEBAR_KEY, String(sidebarCollapsed))
  }, [sidebarCollapsed])

  const setTheme = useCallback((t: Theme) => setThemeState(t), [])
  const toggleTheme = useCallback(() => setThemeState((t) => (t === 'dark' ? 'light' : 'dark')), [])
  const setSidebarCollapsed = useCallback((v: boolean) => setSidebarCollapsedState(v), [])
  const toggleSidebar = useCallback(() => setSidebarCollapsedState((v) => !v), [])

  const value: LayoutContextValue = {
    theme,
    setTheme,
    toggleTheme,
    sidebarCollapsed,
    setSidebarCollapsed,
    toggleSidebar,
  }

  return <LayoutContext.Provider value={value}>{children}</LayoutContext.Provider>
}

export function useLayout() {
  const ctx = useContext(LayoutContext)
  if (!ctx) throw new Error('useLayout must be used within LayoutProvider')
  return ctx
}
