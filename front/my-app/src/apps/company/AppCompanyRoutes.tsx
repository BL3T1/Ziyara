import { lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth } from '../../components/RequireAuth'
import { RequireSurfaceRole } from '../../components/RequireSurfaceRole'
import { HomeRedirect } from '../../components/HomeRedirect'
import { PageLayout } from '../../layouts/PageLayout'

const LoginPage = lazy(() => import('../../pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const ForgotPasswordPage = lazy(() => import('../../pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })))
const ResetPasswordPage = lazy(() => import('../../pages/ResetPasswordPage').then((m) => ({ default: m.ResetPasswordPage })))
const AnalyticsPage = lazy(() => import('../../pages/AnalyticsPage').then((m) => ({ default: m.AnalyticsPage })))
const ServiceTypePage = lazy(() =>
  import('../../pages/services/ServiceTypePage').then((m) => ({ default: m.ServiceTypePage })),
)
const ServiceDetailPage = lazy(() =>
  import('../../pages/services/ServiceDetailPage').then((m) => ({ default: m.ServiceDetailPage })),
)
const UsersPage = lazy(() => import('../../pages/management/UsersPage').then((m) => ({ default: m.UsersPage })))
const GroupMembersPage = lazy(() =>
  import('../../pages/management/GroupMembersPage').then((m) => ({ default: m.GroupMembersPage })),
)
const StaffUserDetailPage = lazy(() =>
  import('../../pages/management/StaffUserDetailPage').then((m) => ({ default: m.StaffUserDetailPage })),
)
const ProvidersPage = lazy(() =>
  import('../../pages/management/ProvidersPage').then((m) => ({ default: m.ProvidersPage })),
)
const CreateProviderPage = lazy(() =>
  import('../../pages/management/CreateProviderPage').then((m) => ({ default: m.CreateProviderPage })),
)
const EditProviderPage = lazy(() =>
  import('../../pages/management/EditProviderPage').then((m) => ({ default: m.EditProviderPage })),
)
const BookingsPage = lazy(() =>
  import('../../pages/management/BookingsPage').then((m) => ({ default: m.BookingsPage })),
)
const DiscountsPage = lazy(() =>
  import('../../pages/management/DiscountsPage').then((m) => ({ default: m.DiscountsPage })),
)
const PaymentsPage = lazy(() =>
  import('../../pages/management/PaymentsPage').then((m) => ({ default: m.PaymentsPage })),
)
const ReportsPage = lazy(() =>
  import('../../pages/management/ReportsPage').then((m) => ({ default: m.ReportsPage })),
)
const RolesPage = lazy(() => import('../../pages/admin/RolesPage').then((m) => ({ default: m.RolesPage })))
const AuditLogsPage = lazy(() =>
  import('../../pages/admin/AuditLogsPage').then((m) => ({ default: m.AuditLogsPage })),
)
const SettingsPage = lazy(() =>
  import('../../pages/admin/SettingsPage').then((m) => ({ default: m.SettingsPage })),
)
const ApiPage = lazy(() => import('../../pages/admin/ApiPage').then((m) => ({ default: m.ApiPage })))
const IntegrationsPage = lazy(() =>
  import('../../pages/admin/IntegrationsPage').then((m) => ({ default: m.IntegrationsPage })),
)
const ContentPagesPage = lazy(() =>
  import('../../pages/admin/ContentPagesPage').then((m) => ({ default: m.ContentPagesPage })),
)
const TicketsPage = lazy(() =>
  import('../../pages/support/TicketsPage').then((m) => ({ default: m.TicketsPage })),
)
const TicketDetailPage = lazy(() =>
  import('../../pages/support/TicketDetailPage').then((m) => ({ default: m.TicketDetailPage })),
)
const CustomerSearchPage = lazy(() =>
  import('../../pages/admin/CustomerSearchPage').then((m) => ({ default: m.CustomerSearchPage })),
)
const CustomerProfilePage = lazy(() =>
  import('../../pages/admin/CustomerProfilePage').then((m) => ({ default: m.CustomerProfilePage })),
)
const DeletedItemsPage = lazy(() =>
  import('../../pages/admin/DeletedItemsPage').then((m) => ({ default: m.DeletedItemsPage })),
)
const PermissionMatrixPage = lazy(() =>
  import('../../pages/admin/PermissionMatrixPage').then((m) => ({ default: m.PermissionMatrixPage })),
)
const ComplaintsPage = lazy(() =>
  import('../../pages/support/ComplaintsPage').then((m) => ({ default: m.ComplaintsPage })),
)
const ReviewsPage = lazy(() =>
  import('../../pages/support/ReviewsPage').then((m) => ({ default: m.ReviewsPage })),
)
const TaxiTripsPage = lazy(() =>
  import('../../pages/management/TaxiTripsPage').then((m) => ({ default: m.TaxiTripsPage })),
)
const CurrencyRatesPage = lazy(() =>
  import('../../pages/management/CurrencyRatesPage').then((m) => ({ default: m.CurrencyRatesPage })),
)
const CompanyDashboardPage = lazy(() =>
  import('../../pages/CompanyDashboardPage').then((m) => ({ default: m.CompanyDashboardPage })),
)
export function AppCompanyRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<RequireSurfaceRole surface="company" />}>
          <Route element={<PageLayout />}>
            <Route path="/dashboard" element={<CompanyDashboardPage />} />
            <Route path="/sales" element={<Navigate to="/dashboard" replace />} />
            <Route path="/sales/providers" element={<Navigate to="/management/providers" replace />} />
            <Route path="/analytics" element={<AnalyticsPage />} />
            <Route path="/services/:type" element={<ServiceTypePage />} />
            <Route path="/services/:type/:id" element={<ServiceDetailPage />} />
            <Route path="/hotels" element={<Navigate to="/services/hotels" replace />} />
            <Route path="/resorts" element={<Navigate to="/services/resorts" replace />} />
            <Route path="/restaurants" element={<Navigate to="/services/restaurants" replace />} />
            <Route path="/trips" element={<Navigate to="/services/trips" replace />} />
            <Route path="/hotels/:id" element={<ServiceDetailPage />} />
            <Route path="/resorts/:id" element={<ServiceDetailPage />} />
            <Route path="/restaurants/:id" element={<ServiceDetailPage />} />
            <Route path="/trips/:id" element={<ServiceDetailPage />} />
            <Route path="/management/users" element={<UsersPage />} />
            <Route path="/management/groups/:groupId/members" element={<GroupMembersPage />} />
            <Route path="/management/staff/:userId" element={<StaffUserDetailPage />} />
            <Route path="/management/providers/new" element={<CreateProviderPage />} />
            <Route path="/management/providers/:providerId" element={<EditProviderPage />} />
            <Route path="/management/providers" element={<ProvidersPage />} />
            <Route path="/management/bookings" element={<BookingsPage />} />
            <Route path="/management/payments" element={<PaymentsPage />} />
            <Route path="/management/discounts" element={<DiscountsPage />} />
            <Route path="/management/reports" element={<ReportsPage />} />
            <Route path="/support/complaints" element={<ComplaintsPage />} />
            <Route path="/support/reviews" element={<ReviewsPage />} />
            <Route path="/support/tickets" element={<TicketsPage />} />
            <Route path="/support/tickets/:ticketId" element={<TicketDetailPage />} />
            <Route path="/management/taxi-trips" element={<TaxiTripsPage />} />
            <Route path="/management/currency-rates" element={<CurrencyRatesPage />} />
            <Route path="/admin/settings" element={<SettingsPage />} />
            <Route path="/admin/roles" element={<RolesPage />} />
            <Route path="/admin/permissions" element={<PermissionMatrixPage />} />
            <Route path="/admin/logs" element={<AuditLogsPage />} />
            <Route path="/admin/find-customer" element={<CustomerSearchPage />} />
            <Route path="/admin/customers/:userId" element={<CustomerProfilePage />} />
            <Route path="/admin/deleted-items" element={<DeletedItemsPage />} />
            <Route path="/admin/api" element={<ApiPage />} />
            <Route path="/admin/integrations" element={<IntegrationsPage />} />
            <Route path="/admin/content" element={<ContentPagesPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
