import { lazy, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'

const DashboardPage = lazy(() => import('./DashboardPage').then((m) => ({ default: m.DashboardPage })))
const SalesDashboardPage = lazy(() => import('./SalesDashboardPage').then((m) => ({ default: m.SalesDashboardPage })))

/**
 * Company app home: sales roles (frontend `admin`) see the sales dashboard; everyone else sees the executive/super-admin dashboard.
 */
export function CompanyDashboardPage() {
  const { user } = useAuth()

  useEffect(() => {
    if (user?.role === 'admin') {
      void import('./SalesDashboardPage')
    } else {
      void import('./DashboardPage')
    }
  }, [user?.role])

  if (user?.role === 'admin') {
    return <SalesDashboardPage />
  }
  return <DashboardPage />
}
