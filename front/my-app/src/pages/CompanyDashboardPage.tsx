import { lazy, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { usePermissions } from '../context/PermissionsContext'

const DashboardPage = lazy(() => import('./DashboardPage').then((m) => ({ default: m.DashboardPage })))
const SalesDashboardPage = lazy(() => import('./SalesDashboardPage').then((m) => ({ default: m.SalesDashboardPage })))

/**
 * Company app home: routes to SalesDashboardPage for staff whose permissions are
 * scoped to sales (bookings:read without reports:read); everyone else sees the full dashboard.
 * Uses ABAC rather than a role string so that permission changes take effect without re-login.
 */
export function CompanyDashboardPage() {
  const { user } = useAuth()
  const { has } = usePermissions()

  // Show sales dashboard only for admin-surface users who have bookings access but
  // not full reporting access — i.e., narrowly scoped sales reps, not broad managers.
  const showSalesDashboard = user?.role === 'admin' && has('bookings:read') && !has('reports:read')

  useEffect(() => {
    if (showSalesDashboard) {
      void import('./SalesDashboardPage')
    } else {
      void import('./DashboardPage')
    }
  }, [showSalesDashboard])

  if (showSalesDashboard) {
    return <SalesDashboardPage />
  }
  return <DashboardPage />
}
