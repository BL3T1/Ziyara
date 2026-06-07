import type { ReactNode } from 'react'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { useSessionExpiryWarning } from '../hooks/useSessionExpiryWarning'
import { DashboardFooter, DashboardHeader, Sidebar } from '../components'
import { CompanyNavBootstrap } from './CompanyNavBootstrap'

interface MainLayoutProps {
  children: ReactNode
  pageTitle?: string
}

function MainLayoutInner({ children, pageTitle }: MainLayoutProps) {
  const { sidebarCollapsed } = useLayout()
  const { locale, t } = useLanguage()
  const { logout } = useAuth()
  const toast = useToast()
  useSessionExpiryWarning(
    () => toast.warning(t('auth.sessionExpiringWarning')),
    () => { toast.error(t('auth.sessionExpired')); logout() },
  )
  const isRtl = locale === 'ar'
  const contentPadding = sidebarCollapsed ? 'lg:pl-[3.75rem]' : 'lg:pl-60'
  const contentPaddingRtl = sidebarCollapsed ? 'lg:pr-[3.75rem]' : 'lg:pr-60'
  return (
    <div className="flex min-h-screen flex-col bg-white text-black transition-colors duration-300 dark:bg-[#111827] dark:text-slate-100">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-[9999] focus:rounded focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-white"
      >
        Skip to main content
      </a>
      <CompanyNavBootstrap />
      <Sidebar />
      <div className={`flex flex-1 flex-col transition-[padding] duration-300 ease-out-expo ${isRtl ? contentPaddingRtl : contentPadding}`}>
        <DashboardHeader pageTitle={pageTitle} />
        <main id="main-content" className="layout-main-surface text-slate-900 dark:text-slate-100">
          <div className="layout-main-surface__glow" aria-hidden>
            <div className="absolute -start-24 top-0 h-[28rem] w-[28rem] rounded-full bg-primary/[0.07] blur-3xl dark:bg-primary/[0.14]" />
            <div className="absolute -end-20 top-24 h-72 w-72 rounded-full bg-secondary/[0.12] blur-3xl dark:bg-secondary/[0.08]" />
            <div className="absolute bottom-0 start-1/3 h-64 w-96 -translate-x-1/2 rounded-full bg-slate-200/60 blur-3xl dark:bg-slate-800/40 rtl:translate-x-1/2" />
          </div>
          <div className="layout-main-surface__content">{children}</div>
        </main>
        <DashboardFooter />
      </div>
    </div>
  )
}

export function MainLayout(props: MainLayoutProps) {
  return <MainLayoutInner {...props} />
}
