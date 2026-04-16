import { Suspense } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useLanguage } from '../context/LanguageContext'
import { RoutePageFallback } from '../components/RoutePageFallback'
import { MainLayout } from './MainLayout'
import { getPageTitleKeyForPath } from '../config/routes'
import { DisplayCurrencyProvider } from '../context/DisplayCurrencyContext'

export function PageLayout() {
  const { pathname } = useLocation()
  const { t } = useLanguage()
  const pageTitle = t(getPageTitleKeyForPath(pathname))

  return (
    <MainLayout pageTitle={pageTitle}>
      <div key={pathname} className="layout-page-enter app-page-stack">
        <DisplayCurrencyProvider>
          <Suspense fallback={<RoutePageFallback />}>
            <Outlet />
          </Suspense>
        </DisplayCurrencyProvider>
      </div>
    </MainLayout>
  )
}
