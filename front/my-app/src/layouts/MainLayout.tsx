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
  const contentPadding = sidebarCollapsed ? 'pl-[3.75rem]' : 'pl-60'
  const contentPaddingRtl = sidebarCollapsed ? 'pr-[3.75rem]' : 'pr-60'
  return (
    <div className="flex min-h-screen flex-col bg-white text-black transition-colors duration-300 dark:bg-[#020409] dark:text-slate-100">
      <CompanyNavBootstrap />
      <Sidebar />
      <div className={`flex flex-1 flex-col transition-[padding] duration-300 ease-out-expo ${isRtl ? contentPaddingRtl : contentPadding}`}>
        <DashboardHeader pageTitle={pageTitle} />
        <main className="layout-main-surface text-slate-900 dark:text-slate-100">
          <div className="layout-main-surface__glow" aria-hidden>
            <div className="absolute -start-32 top-0 h-96 w-96 rounded-full bg-[#1e4d6b]/[0.06] blur-3xl dark:bg-[#1e4d6b]/[0.12]" />
            <div className="absolute -end-24 top-32 h-64 w-64 rounded-full bg-[#ac9e78]/[0.08] blur-3xl dark:bg-[#ac9e78]/[0.06]" />
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
