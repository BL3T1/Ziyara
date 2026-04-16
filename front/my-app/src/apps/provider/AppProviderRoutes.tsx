import { lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth } from '../../components/RequireAuth'
import { RequireSurfaceRole } from '../../components/RequireSurfaceRole'
import { HomeRedirect } from '../../components/HomeRedirect'
import { ClientPortalLayout } from '../../layouts/ClientPortalLayout'

const LoginPage = lazy(() => import('../../pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const ClientPortalOverview = lazy(() =>
  import('../../pages/portal/ClientPortalOverview').then((m) => ({ default: m.ClientPortalOverview })),
)
const PortalListingsPage = lazy(() =>
  import('../../pages/portal/PortalListingsPage').then((m) => ({ default: m.PortalListingsPage })),
)
const PortalListingFormPage = lazy(() =>
  import('../../pages/portal/PortalListingFormPage').then((m) => ({ default: m.PortalListingFormPage })),
)
const PortalBookingsPage = lazy(() =>
  import('../../pages/portal/PortalBookingsPage').then((m) => ({ default: m.PortalBookingsPage })),
)
const PortalEarningsPage = lazy(() =>
  import('../../pages/portal/PortalEarningsPage').then((m) => ({ default: m.PortalEarningsPage })),
)
const PortalProfilePage = lazy(() =>
  import('../../pages/portal/PortalProfilePage').then((m) => ({ default: m.PortalProfilePage })),
)
const PortalStaffPage = lazy(() =>
  import('../../pages/portal/PortalStaffPage').then((m) => ({ default: m.PortalStaffPage })),
)
const PortalSupportPage = lazy(() =>
  import('../../pages/portal/PortalSupportPage').then((m) => ({ default: m.PortalSupportPage })),
)

export function AppProviderRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<RequireSurfaceRole surface="provider" />}>
          <Route element={<ClientPortalLayout />}>
            <Route path="/portal" element={<ClientPortalOverview />} />
            <Route path="/portal/listings/new" element={<PortalListingFormPage />} />
            <Route path="/portal/listings/:id" element={<PortalListingFormPage />} />
            <Route path="/portal/listings" element={<PortalListingsPage />} />
            <Route path="/portal/bookings" element={<PortalBookingsPage />} />
            <Route path="/portal/staff" element={<PortalStaffPage />} />
            <Route path="/portal/earnings" element={<PortalEarningsPage />} />
            <Route path="/portal/profile" element={<PortalProfilePage />} />
            <Route path="/portal/support" element={<PortalSupportPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
