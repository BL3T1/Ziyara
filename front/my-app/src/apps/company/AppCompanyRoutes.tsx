import { lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth } from '../../components/RequireAuth'
import { RequireSurfaceRole } from '../../components/RequireSurfaceRole'
import { RequirePermission } from '../../components/RequirePermission'
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
const PayoutsPage = lazy(() =>
  import('../../pages/management/PayoutsPage').then((m) => ({ default: m.PayoutsPage })),
)
const ReportsPage = lazy(() =>
  import('../../pages/management/ReportsPage').then((m) => ({ default: m.ReportsPage })),
)
const MapPage = lazy(() => import('../../pages/admin/MapPage').then((m) => ({ default: m.MapPage })))
const RolesPage = lazy(() => import('../../pages/admin/RolesPage').then((m) => ({ default: m.RolesPage })))
const RoleMembersPage = lazy(() => import('../../pages/admin/RoleMembersPage').then((m) => ({ default: m.RoleMembersPage })))
const SubscriptionsPage = lazy(() => import('../../pages/admin/SubscriptionsPage').then((m) => ({ default: m.SubscriptionsPage })))
const AuditLogsPage = lazy(() =>
  import('../../pages/admin/AuditLogsPage').then((m) => ({ default: m.AuditLogsPage })),
)
const SettingsPage = lazy(() =>
  import('../../pages/admin/SettingsPage').then((m) => ({ default: m.SettingsPage })),
)
const IntegrationsPage = lazy(() =>
  import('../../pages/admin/IntegrationsPage').then((m) => ({ default: m.IntegrationsPage })),
)
const WebhookSubscriptionsPage = lazy(() =>
  import('../../pages/admin/WebhookSubscriptionsPage').then((m) => ({ default: m.WebhookSubscriptionsPage })),
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
const ChangePasswordPage = lazy(() =>
  import('../../pages/ChangePasswordPage').then((m) => ({ default: m.ChangePasswordPage })),
)
const MediaSubmissionsPage = lazy(() =>
  import('../../pages/management/MediaSubmissionsPage').then((m) => ({ default: m.MediaSubmissionsPage })),
)
const AdminCashReconciliationPage = lazy(() =>
  import('../../pages/management/AdminCashReconciliationPage').then((m) => ({ default: m.AdminCashReconciliationPage })),
)
const ProfileEditRequestsPage = lazy(() =>
  import('../../pages/admin/ProfileEditRequestsPage').then((m) => ({ default: m.ProfileEditRequestsPage })),
)
const IdentityVerificationsPage = lazy(() =>
  import('../../pages/admin/IdentityVerificationsPage').then((m) => ({ default: m.IdentityVerificationsPage })),
)
export function AppCompanyRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/account/change-password" element={<ChangePasswordPage />} />
        <Route element={<RequireSurfaceRole surface="company" />}>
          <Route element={<PageLayout />}>
            <Route path="/dashboard" element={<CompanyDashboardPage />} />
            <Route path="/sales" element={<Navigate to="/dashboard" replace />} />
            <Route path="/sales/providers" element={<Navigate to="/management/providers" replace />} />
            <Route path="/analytics" element={<RequirePermission code="analytics:read"><AnalyticsPage /></RequirePermission>} />
            <Route path="/services/:type" element={<RequirePermission code="services:read"><ServiceTypePage /></RequirePermission>} />
            <Route path="/services/:type/:id" element={<RequirePermission code="services:read"><ServiceDetailPage /></RequirePermission>} />
            <Route path="/hotels" element={<Navigate to="/services/hotels" replace />} />
            <Route path="/resorts" element={<Navigate to="/services/resorts" replace />} />
            <Route path="/restaurants" element={<Navigate to="/services/restaurants" replace />} />
            <Route path="/trips" element={<Navigate to="/services/trips" replace />} />
            <Route path="/hotels/:id" element={<RequirePermission code="services:read"><ServiceDetailPage /></RequirePermission>} />
            <Route path="/resorts/:id" element={<RequirePermission code="services:read"><ServiceDetailPage /></RequirePermission>} />
            <Route path="/restaurants/:id" element={<RequirePermission code="services:read"><ServiceDetailPage /></RequirePermission>} />
            <Route path="/trips/:id" element={<RequirePermission code="services:read"><ServiceDetailPage /></RequirePermission>} />
            <Route path="/management/users" element={<RequirePermission code="users:read"><UsersPage /></RequirePermission>} />
            <Route path="/management/groups/:groupId/members" element={<RequirePermission code="users:read"><GroupMembersPage /></RequirePermission>} />
            <Route path="/management/staff/:userId" element={<RequirePermission code="users:read"><StaffUserDetailPage /></RequirePermission>} />
            <Route path="/management/providers/new" element={<RequirePermission code="providers:write"><CreateProviderPage /></RequirePermission>} />
            <Route path="/management/providers/:providerId" element={<RequirePermission code="providers:read"><EditProviderPage /></RequirePermission>} />
            <Route path="/management/providers" element={<RequirePermission code="providers:read"><ProvidersPage /></RequirePermission>} />
            <Route path="/management/bookings" element={<RequirePermission code="bookings:read"><BookingsPage /></RequirePermission>} />
            <Route path="/management/payments" element={<RequirePermission code="payments:read"><PaymentsPage /></RequirePermission>} />
            <Route path="/admin/cash" element={<RequirePermission code="payments:cash-reconcile"><AdminCashReconciliationPage /></RequirePermission>} />
            <Route path="/management/payouts" element={<RequirePermission code="payouts:read"><PayoutsPage /></RequirePermission>} />
            <Route path="/management/discounts" element={<RequirePermission code="discounts:read"><DiscountsPage /></RequirePermission>} />
            <Route path="/management/reports" element={<RequirePermission code="reports:read"><ReportsPage /></RequirePermission>} />
            <Route path="/support/complaints" element={<RequirePermission code="complaints:read"><ComplaintsPage /></RequirePermission>} />
            <Route path="/support/reviews" element={<RequirePermission code="reviews:read"><ReviewsPage /></RequirePermission>} />
            <Route path="/support/tickets" element={<RequirePermission code="support:read"><TicketsPage /></RequirePermission>} />
            <Route path="/support/tickets/:ticketId" element={<RequirePermission code="support:read"><TicketDetailPage /></RequirePermission>} />
            <Route path="/management/taxi-trips" element={<RequirePermission code="taxi:read"><TaxiTripsPage /></RequirePermission>} />
            <Route path="/management/currency-rates" element={<RequirePermission code="currency:read"><CurrencyRatesPage /></RequirePermission>} />
            <Route path="/map" element={<RequirePermission code="providers:read"><MapPage /></RequirePermission>} />
            <Route path="/admin/settings" element={<RequirePermission code="settings:read"><SettingsPage /></RequirePermission>} />
            <Route path="/admin/roles" element={<RequirePermission code="roles:read"><RolesPage /></RequirePermission>} />
            <Route path="/admin/roles/:roleId/members" element={<RequirePermission code="roles:read"><RoleMembersPage /></RequirePermission>} />
            <Route path="/admin/subscriptions" element={<RequirePermission code="providers:read"><SubscriptionsPage /></RequirePermission>} />
            <Route path="/admin/logs" element={<RequirePermission code="audit:read"><AuditLogsPage /></RequirePermission>} />
            <Route path="/admin/find-customer" element={<RequirePermission code="customers:read"><CustomerSearchPage /></RequirePermission>} />
            <Route path="/admin/customers/:userId" element={<RequirePermission code="customers:read"><CustomerProfilePage /></RequirePermission>} />
            <Route path="/admin/deleted-items" element={<RequirePermission code="deleted_items:company:read"><DeletedItemsPage /></RequirePermission>} />
<Route path="/admin/integrations" element={<RequirePermission code="settings:read"><IntegrationsPage /></RequirePermission>} />
            <Route path="/admin/webhooks" element={<RequirePermission code="webhooks:read"><WebhookSubscriptionsPage /></RequirePermission>} />
            <Route path="/admin/content" element={<RequirePermission code="content:read"><ContentPagesPage /></RequirePermission>} />
            <Route path="/admin/media-submissions" element={<RequirePermission code="media_submissions:approve"><MediaSubmissionsPage /></RequirePermission>} />
            <Route path="/media-approvals" element={<RequirePermission code="media_submissions:approve"><MediaSubmissionsPage /></RequirePermission>} />
            <Route path="/admin/profile-edit-requests" element={<RequirePermission code="profile_edits:approve"><ProfileEditRequestsPage /></RequirePermission>} />
            <Route path="/admin/identity-verifications" element={<RequirePermission code="customers:read"><IdentityVerificationsPage /></RequirePermission>} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
