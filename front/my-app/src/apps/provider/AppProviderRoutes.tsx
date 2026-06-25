import { lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth } from '../../components/RequireAuth'
import { RequirePermission } from '../../components/RequirePermission'
import { RequireSurfaceRole } from '../../components/RequireSurfaceRole'
import { HomeRedirect } from '../../components/HomeRedirect'
import { ClientPortalLayout } from '../../layouts/ClientPortalLayout'

const LoginPage = lazy(() => import('../../pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const ChangePasswordPage = lazy(() =>
  import('../../pages/ChangePasswordPage').then((m) => ({ default: m.ChangePasswordPage })),
)
const ForgotPasswordPage = lazy(() => import('../../pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })))
const ResetPasswordPage = lazy(() => import('../../pages/ResetPasswordPage').then((m) => ({ default: m.ResetPasswordPage })))
const ClientPortalOverview = lazy(() =>
  import('../../pages/portal/ClientPortalOverview').then((m) => ({ default: m.ClientPortalOverview })),
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
const PortalDiscountsPage = lazy(() =>
  import('../../pages/portal/PortalDiscountsPage').then((m) => ({ default: m.PortalDiscountsPage })),
)
const PortalMediaPage = lazy(() =>
  import('../../pages/portal/PortalMediaPage').then((m) => ({ default: m.PortalMediaPage })),
)
const PortalMapPage = lazy(() =>
  import('../../pages/portal/PortalMapPage').then((m) => ({ default: m.PortalMapPage })),
)
const PortalCashSheetPage = lazy(() =>
  import('../../pages/portal/PortalCashSheetPage').then((m) => ({ default: m.PortalCashSheetPage })),
)
const PortalRoomsPage = lazy(() =>
  import('../../pages/portal/PortalRoomsPage').then((m) => ({ default: m.PortalRoomsPage })),
)
const PortalMenuPage = lazy(() =>
  import('../../pages/portal/PortalMenuPage').then((m) => ({ default: m.PortalMenuPage })),
)
export function AppProviderRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/account/change-password" element={<ChangePasswordPage />} />
        <Route element={<RequireSurfaceRole surface="provider" />}>
          <Route element={<ClientPortalLayout />}>
            <Route path="/portal" element={<ClientPortalOverview />} />
            <Route path="/portal/rooms" element={<RequirePermission code="portal:manage"><PortalRoomsPage /></RequirePermission>} />
            <Route path="/portal/menu" element={<RequirePermission code="portal:manage"><PortalMenuPage /></RequirePermission>} />
            <Route path="/portal/bookings" element={<PortalBookingsPage />} />
            <Route path="/portal/staff" element={<PortalStaffPage />} />
            <Route path="/portal/earnings" element={<RequirePermission code="portal:finance"><PortalEarningsPage /></RequirePermission>} />
            <Route path="/portal/discounts" element={<RequirePermission code="portal:finance"><PortalDiscountsPage /></RequirePermission>} />
            <Route path="/portal/profile" element={<PortalProfilePage />} />
            <Route path="/portal/support" element={<PortalSupportPage />} />
            <Route path="/portal/media" element={<PortalMediaPage />} />
            <Route path="/portal/map" element={<PortalMapPage />} />
            <Route path="/portal/cash" element={<RequirePermission code="portal:finance"><PortalCashSheetPage /></RequirePermission>} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
