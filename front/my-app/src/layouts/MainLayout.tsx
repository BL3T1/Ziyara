import type { ReactNode } from 'react'
import { useLayout } from '../context/LayoutContext'
import { useLanguage } from '../context/LanguageContext'
import { DashboardFooter, DashboardHeader, Sidebar } from '../components'
import { CompanyNavBootstrap } from './CompanyNavBootstrap'

interface MainLayoutProps {
  children: ReactNode
  pageTitle?: string
}

function MainLayoutInner({ children, pageTitle }: MainLayoutProps) {
  const { sidebarCollapsed } = useLayout()
  const { locale } = useLanguage()
  const isRtl = locale === 'ar'
  const contentPadding = sidebarCollapsed ? 'pl-16' : 'pl-60'
  const contentPaddingRtl = sidebarCollapsed ? 'pr-16' : 'pr-60'
  return (
    <div className="flex min-h-screen flex-col bg-slate-100 text-black transition-colors duration-300 dark:bg-slate-950 dark:text-slate-100">
      <CompanyNavBootstrap />
      <Sidebar />
      <div className={`flex flex-1 flex-col transition-[padding] duration-200 ${isRtl ? contentPaddingRtl : contentPadding}`}>
        <DashboardHeader pageTitle={pageTitle} />
        <main className="layout-main-surface text-slate-900 dark:text-slate-100">
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
