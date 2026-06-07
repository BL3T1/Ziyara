package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.application.query.*;
import com.ziyara.backend.application.service.*;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import com.ziyara.backend.modules.pricing.api.PricingEngineApi;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.modules.sys.api.RoleServiceApi;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security posture test for every controller.
 *
 * Three tiers of assertion per endpoint:
 *   1. Anonymous → 401 Unauthorized
 *   2. Authenticated with wrong role/permission → 403 Forbidden
 *   3. Authenticated with correct permission → not 401 / not 403
 *      (service mocks return null by default; controllers may return 4xx/5xx for bad
 *       input — that is acceptable here, the goal is to verify security passed)
 *
 * Service mocks are declared at class level. Spring Security short-circuits before
 * reaching service code for 401/403 cases so mock behaviour is irrelevant there.
 */
@WebMvcTest(controllers = {
        AuthController.class,
        UserController.class,
        SuperAdminController.class,
        BookingController.class,
        PaymentController.class,
        AdminPayoutController.class,
        RoleManagementController.class,
        ComplaintController.class,
        ServiceController.class,
        ServiceProviderController.class,
        DiscountController.class,
        PortalController.class,
        NotificationController.class,
        MfaController.class,
        AuditLogController.class,
        DashboardController.class,
        ContentPageController.class,
        ReviewController.class,
        ReportController.class,
        TaxiBookingController.class,
        TaxiTrackingController.class,
        WebhookSubscriptionController.class,
        AdminFeatureFlagsController.class,
        AdminSystemSettingsController.class,
        AdminIntegrationApiKeysController.class,
        AdminPiiRegistryController.class,
        AdminPermissionsController.class,
        ProviderMediaSubmissionController.class,
        UserConsentController.class,
        UserDataExportController.class,
        PublicContactController.class,
        InternalTicketController.class,
        DepartmentController.class,
        EmployeeController.class,
        PricingController.class
})
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AllEndpointsSecurityWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AllEndpointsSecurityWebMvcTest {

    @Autowired MockMvc mvc;

    // ── Security infrastructure ────────────────────────────────────────────────
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtTokenBlocklistService jwtTokenBlocklistService;
    @MockBean JwtIdleTimeoutService jwtIdleTimeoutService;

    // ── Auth ───────────────────────────────────────────────────────────────────
    @MockBean AuthService authService;
    @MockBean LoginRateLimitService loginRateLimitService;
    @MockBean SecurityEventService securityEventService;
    @MockBean SecurityAlertService securityAlertService;

    // ── Users ──────────────────────────────────────────────────────────────────
    @MockBean UserQueryHandler userQueryHandler;
    @MockBean UserCommandHandler userCommandHandler;
    @MockBean NavigationService navigationService;
    @MockBean UserRbacAssignmentService userRbacAssignmentService;
    @MockBean RbacAssignmentQueryService rbacAssignmentQueryService;
    @MockBean CompanyStaffRoleCatalogService companyStaffRoleCatalogService;

    // ── Roles ──────────────────────────────────────────────────────────────────
    @MockBean RoleServiceApi roleManagementService;

    // ── Bookings / Payments ────────────────────────────────────────────────────
    @MockBean BookingServiceApi bookingService;
    @MockBean PaymentServiceApi paymentService;

    // ── Payouts ────────────────────────────────────────────────────────────────
    @MockBean AdminPayoutService adminPayoutService;

    // ── Complaints ─────────────────────────────────────────────────────────────
    @MockBean ComplaintService complaintService;
    @MockBean ComplaintCommentService commentService;
    @MockBean ComplaintQueryHandler complaintQueryHandler;

    // ── Services / Providers ───────────────────────────────────────────────────
    @MockBean ServiceService serviceService;
    @MockBean ServiceQueryHandler serviceQueryHandler;
    @MockBean ServiceImageService serviceImageService;
    @MockBean RestaurantMenuService restaurantMenuService;
    @MockBean HotelRoomService hotelRoomService;
    @MockBean ServiceProviderService providerService;

    // ── Discounts ──────────────────────────────────────────────────────────────
    @MockBean DiscountCodeService discountService;
    @MockBean DiscountQueryHandler discountQueryHandler;

    // ── Portal ─────────────────────────────────────────────────────────────────
    @MockBean PortalService portalService;
    @MockBean ProviderMediaSubmissionService providerMediaSubmissionService;
    @MockBean PortalStaffService portalStaffService;

    // ── Notifications ──────────────────────────────────────────────────────────
    @MockBean NotificationService notificationService;

    // ── MFA ────────────────────────────────────────────────────────────────────
    @MockBean UserMfaService userMfaService;

    // ── Audit / Reports ────────────────────────────────────────────────────────
    @MockBean AuditServiceApi auditLogService;
    @MockBean ReportService reportService;
    @MockBean ReportExportService reportExportService;
    @MockBean SuperAdminRecoveryService superAdminRecoveryService;

    // ── Dashboard ──────────────────────────────────────────────────────────────
    @MockBean DashboardService dashboardService;
    @MockBean DashboardQueryHandler dashboardQueryHandler;
    @MockBean DashboardBootstrapService dashboardBootstrapService;

    // ── Content / Reviews ──────────────────────────────────────────────────────
    @MockBean ContentPageService contentPageService;
    @MockBean ReviewService reviewService;

    // ── Taxi ───────────────────────────────────────────────────────────────────
    @MockBean TaxiBookingService taxiBookingService;
    @MockBean SimpMessagingTemplate broker;

    // ── Webhooks ───────────────────────────────────────────────────────────────
    @MockBean WebhookService webhookService;

    // ── Admin ──────────────────────────────────────────────────────────────────
    @MockBean FeatureFlagService featureFlagService;
    @MockBean SystemSettingsService systemSettingsService;
    @MockBean IntegrationApiKeyService integrationApiKeyService;
    @MockBean PiiRegistryService piiRegistryService;
    @MockBean DSLContext dsl;
    @MockBean AdminActivityLogService activityLogService;

    // ── Misc ───────────────────────────────────────────────────────────────────
    @MockBean ContactLeadService contactLeadService;
    @MockBean InternalTicketService ticketService;
    @MockBean DepartmentService departmentService;
    @MockBean EmployeeService employeeService;
    @MockBean PricingEngineApi pricingService;
    @MockBean UserConsentService userConsentService;
    @MockBean DataExportService dataExportService;

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {
        @Bean
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService,
                UserDetailsService userDetailsService,
                SecurityContextRepository securityContextRepository,
                JwtCookieProperties jwtCookieProperties,
                JwtTokenBlocklistService jwtTokenBlocklistService,
                JwtIdleTimeoutService jwtIdleTimeoutService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService,
                    securityContextRepository, jwtCookieProperties, jwtTokenBlocklistService,
                    jwtIdleTimeoutService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — reachable without credentials
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class PublicEndpoints {

        @Test
        void login_isPublic() throws Exception {
            mvc.perform(post("/auth/login")
                            .contentType("application/json")
                            .content("{\"identifier\":\"a\",\"password\":\"b\"}"))
                    .andExpect(status().is(not(401)));
        }

        @Test
        void register_isPublic() throws Exception {
            mvc.perform(post("/auth/register")
                            .contentType("application/json")
                            .content("{\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                    .andExpect(status().is(not(401)));
        }

        @Test
        void forgotPassword_isPublic() throws Exception {
            mvc.perform(post("/auth/password/forgot")
                            .contentType("application/json")
                            .content("{\"email\":\"a@b.com\"}"))
                    .andExpect(status().is(not(401)));
        }

        @Test
        void resetPassword_isPublic() throws Exception {
            mvc.perform(post("/auth/password/reset")
                            .contentType("application/json")
                            .content("{\"token\":\"t\",\"newPassword\":\"p\"}"))
                    .andExpect(status().is(not(401)));
        }

        @Test
        void servicesList_isPublic() throws Exception {
            mvc.perform(get("/services")).andExpect(status().is(not(401)));
        }

        @Test
        void servicesGetById_isPublic() throws Exception {
            mvc.perform(get("/services/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().is(not(401)));
        }

        @Test
        void contentPagesList_isPublic() throws Exception {
            mvc.perform(get("/content-pages")).andExpect(status().is(not(401)));
        }

        @Test
        void publicContact_isPublic() throws Exception {
            mvc.perform(post("/public/contact")
                            .contentType("application/json")
                            .content("{\"name\":\"A\",\"email\":\"a@b.com\",\"message\":\"hi\"}"))
                    .andExpect(status().is(not(401)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUTH CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Auth {

        @Test
        void logout_anonymous_401() throws Exception {
            mvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
        }

        @Test
        void refresh_anonymous_401() throws Exception {
            mvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void logout_authenticated_notBlocked() throws Exception {
            mvc.perform(post("/auth/logout").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Users {

        @Test void getMe_anonymous_401() throws Exception {
            mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        }

        @Test void getMyPermissions_anonymous_401() throws Exception {
            mvc.perform(get("/users/me/permissions")).andExpect(status().isUnauthorized());
        }

        @Test void getMyNavigation_anonymous_401() throws Exception {
            mvc.perform(get("/users/me/navigation")).andExpect(status().isUnauthorized());
        }

        @Test void changePassword_anonymous_401() throws Exception {
            mvc.perform(post("/users/me/change-password")
                            .contentType("application/json")
                            .content("{\"newPassword\":\"abc123\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void listUsers_anonymous_401() throws Exception {
            mvc.perform(get("/users")).andExpect(status().isUnauthorized());
        }

        @Test void createUser_anonymous_401() throws Exception {
            mvc.perform(post("/users").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void deleteUser_anonymous_401() throws Exception {
            mvc.perform(delete("/users/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void resetUserPassword_anonymous_401() throws Exception {
            mvc.perform(post("/users/{id}/reset-password", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void freezeUser_anonymous_401() throws Exception {
            mvc.perform(post("/users/{id}/freeze", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        void listUsers_insufficientRole_403() throws Exception {
            mvc.perform(get("/users").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "users:write")
        void listUsers_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/users").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser
        void getMe_authenticated_notBlocked() throws Exception {
            mvc.perform(get("/users/me").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser
        void changePassword_authenticated_notBlocked() throws Exception {
            mvc.perform(post("/users/me/change-password")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json")
                            .content("{\"newPassword\":\"abc123\"}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "users:write")
        void createUser_withPermission_notBlocked() throws Exception {
            mvc.perform(post("/users")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json")
                            .content("{\"email\":\"t@t.com\",\"role\":\"staff\"}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUPER ADMIN CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class SuperAdmin {

        @Test void searchCustomers_anonymous_401() throws Exception {
            mvc.perform(get("/admin/super/customers/search").param("q", "x"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void listRecentDeleted_anonymous_401() throws Exception {
            mvc.perform(get("/admin/super/deleted/recent")).andExpect(status().isUnauthorized());
        }

        @Test void searchDeleted_anonymous_401() throws Exception {
            mvc.perform(get("/admin/super/deleted/search").param("q", "x"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void restore_anonymous_401() throws Exception {
            mvc.perform(post("/admin/super/deleted/restore")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void permanentDelete_anonymous_401() throws Exception {
            mvc.perform(delete("/admin/super/deleted/permanent"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void searchCustomers_insufficientRole_403() throws Exception {
            mvc.perform(get("/admin/super/customers/search")
                            .param("q", "x").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "customers:read")
        void searchCustomers_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/super/customers/search")
                            .param("q", "x").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "deleted_items:company:read")
        void listRecentDeleted_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/super/deleted/recent").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Bookings {

        @Test void adminList_anonymous_401() throws Exception {
            mvc.perform(get("/bookings/admin")).andExpect(status().isUnauthorized());
        }

        @Test void reject_anonymous_401() throws Exception {
            mvc.perform(post("/bookings/{id}/reject", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "bookings:read")
        void adminList_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/bookings/admin").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "bookings:write")
        void reject_withPermission_notBlocked() throws Exception {
            mvc.perform(post("/bookings/{id}/reject", "00000000-0000-0000-0000-000000000001")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Payments {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/payments")).andExpect(status().isUnauthorized());
        }

        @Test void getById_anonymous_401() throws Exception {
            mvc.perform(get("/payments/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void summary_anonymous_401() throws Exception {
            mvc.perform(get("/payments/summary")).andExpect(status().isUnauthorized());
        }

        @Test void refund_anonymous_401() throws Exception {
            mvc.perform(post("/payments/{id}/refund", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void complete_anonymous_401() throws Exception {
            mvc.perform(post("/payments/{id}/complete", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/payments").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "payments:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/payments").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "payments:read")
        void refund_readOnly_403() throws Exception {
            mvc.perform(post("/payments/{id}/refund", "00000000-0000-0000-0000-000000000001")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "payments:refund")
        void refund_withPermission_notBlocked() throws Exception {
            mvc.perform(post("/payments/{id}/refund", "00000000-0000-0000-0000-000000000001")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN PAYOUT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class AdminPayouts {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/admin/payouts")).andExpect(status().isUnauthorized());
        }

        @Test void summary_anonymous_401() throws Exception {
            mvc.perform(get("/admin/payouts/summary")).andExpect(status().isUnauthorized());
        }

        @Test void createManual_anonymous_401() throws Exception {
            mvc.perform(post("/admin/payouts/manual")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void bulkApprove_anonymous_401() throws Exception {
            mvc.perform(post("/admin/payouts/bulk/approve")
                            .contentType("application/json").content("{\"ids\":[]}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void export_anonymous_401() throws Exception {
            mvc.perform(get("/admin/payouts/export")).andExpect(status().isUnauthorized());
        }

        @Test void approve_anonymous_401() throws Exception {
            mvc.perform(post("/admin/payouts/{id}/approve", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void hold_anonymous_401() throws Exception {
            mvc.perform(post("/admin/payouts/{id}/hold", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void schedule_anonymous_401() throws Exception {
            mvc.perform(post("/admin/payouts/{id}/schedule", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/admin/payouts").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "payouts:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/payouts").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "payouts:read")
        void createManual_readOnly_403() throws Exception {
            mvc.perform(post("/admin/payouts/manual")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "payouts:write")
        void createManual_withPermission_notBlocked() throws Exception {
            mvc.perform(post("/admin/payouts/manual")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json")
                            .content("{\"providerId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":100}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "payouts:write")
        void bulkApprove_writeNotSufficient_403() throws Exception {
            // bulk approve requires payouts:approve, not just payouts:write
            mvc.perform(post("/admin/payouts/bulk/approve")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{\"ids\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "payouts:approve")
        void bulkApprove_withApprovePermission_notBlocked() throws Exception {
            mvc.perform(post("/admin/payouts/bulk/approve")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{\"ids\":[]}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ROLE MANAGEMENT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Roles {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/roles")).andExpect(status().isUnauthorized());
        }

        @Test void getById_anonymous_401() throws Exception {
            mvc.perform(get("/roles/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/roles").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void delete_anonymous_401() throws Exception {
            mvc.perform(delete("/roles/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void updatePermissions_anonymous_401() throws Exception {
            mvc.perform(put("/roles/{id}/permissions", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void catalogue_anonymous_401() throws Exception {
            mvc.perform(get("/roles/permissions/catalogue")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/roles").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "roles:read")
        void list_withReadPermission_notBlocked() throws Exception {
            mvc.perform(get("/roles").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "roles:read")
        void create_readOnly_403() throws Exception {
            mvc.perform(post("/roles")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "roles:write")
        void create_withWritePermission_notBlocked() throws Exception {
            mvc.perform(post("/roles")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{\"name\":\"Test\"}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPLAINT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Complaints {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/complaints")).andExpect(status().isUnauthorized());
        }

        @Test void getById_anonymous_401() throws Exception {
            mvc.perform(get("/complaints/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/complaints").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void resolve_anonymous_401() throws Exception {
            mvc.perform(post("/complaints/{id}/resolve", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_customerRole_403() throws Exception {
            mvc.perform(get("/complaints").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVICE CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Services {

        @Test void list_isPublic() throws Exception {
            mvc.perform(get("/services")).andExpect(status().is(not(401)));
        }

        @Test void getById_isPublic() throws Exception {
            mvc.perform(get("/services/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().is(not(401)));
        }

        @Test void getImages_isPublic() throws Exception {
            mvc.perform(get("/services/{id}/images", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().is(not(401)));
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/services").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void approve_anonymous_401() throws Exception {
            mvc.perform(post("/services/{id}/approve", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void delete_anonymous_401() throws Exception {
            mvc.perform(delete("/services/{id}", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void addImage_anonymous_401() throws Exception {
            mvc.perform(post("/services/{id}/images", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void create_insufficientRole_403() throws Exception {
            mvc.perform(post("/services")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "services:publish")
        void approve_withPermission_notBlocked() throws Exception {
            mvc.perform(post("/services/{id}/approve", "00000000-0000-0000-0000-000000000001")
                            .header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVICE PROVIDER CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ServiceProviders {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/providers")).andExpect(status().isUnauthorized());
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/providers").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void getAdminToken_anonymous_401() throws Exception {
            mvc.perform(post("/providers/{id}/admin-token", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void update_anonymous_401() throws Exception {
            mvc.perform(patch("/providers/{id}", "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "providers:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/providers").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "providers:read")
        void create_readOnly_403() throws Exception {
            mvc.perform(post("/providers")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DISCOUNT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Discounts {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/discounts")).andExpect(status().isUnauthorized());
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/discounts").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void approve_anonymous_401() throws Exception {
            mvc.perform(post("/discounts/{id}/approve", "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void validate_anonymous_401() throws Exception {
            mvc.perform(post("/discounts/validate").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "discounts:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/discounts").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PORTAL CONTROLLER (requires providers:portal authority)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Portal {

        @Test void dashboard_anonymous_401() throws Exception {
            mvc.perform(get("/portal/dashboard")).andExpect(status().isUnauthorized());
        }

        @Test void listServices_anonymous_401() throws Exception {
            mvc.perform(get("/portal/services")).andExpect(status().isUnauthorized());
        }

        @Test void bookings_anonymous_401() throws Exception {
            mvc.perform(get("/portal/bookings")).andExpect(status().isUnauthorized());
        }

        @Test void requestPayout_anonymous_401() throws Exception {
            mvc.perform(post("/portal/payout-request")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void earnings_anonymous_401() throws Exception {
            mvc.perform(get("/portal/earnings")).andExpect(status().isUnauthorized());
        }

        @Test void mediaSubmissions_anonymous_401() throws Exception {
            mvc.perform(get("/portal/media-submissions")).andExpect(status().isUnauthorized());
        }

        // Company staff should not access provider portal
        @Test
        @WithMockUser(authorities = "providers:read")
        void dashboard_companyStaff_403() throws Exception {
            mvc.perform(get("/portal/dashboard").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "providers:portal")
        void dashboard_providerRole_notBlocked() throws Exception {
            mvc.perform(get("/portal/dashboard").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Notifications {

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/notifications").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void create_insufficientRole_403() throws Exception {
            mvc.perform(post("/notifications")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "settings:write")
        void create_withPermission_notBlocked() throws Exception {
            mvc.perform(post("/notifications")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MFA CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Mfa {

        @Test void enrollStart_anonymous_401() throws Exception {
            mvc.perform(post("/users/me/mfa/enroll/start")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void enrollConfirm_anonymous_401() throws Exception {
            mvc.perform(post("/users/me/mfa/enroll/confirm")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void disable_anonymous_401() throws Exception {
            mvc.perform(post("/users/me/mfa/disable")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void enrollStart_authenticated_notBlocked() throws Exception {
            mvc.perform(post("/users/me/mfa/enroll/start")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT LOG CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class AuditLogs {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/audit-logs")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/audit-logs").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "audit:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/audit-logs").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Dashboard {

        @Test void get_anonymous_401() throws Exception {
            mvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "reports:read")
        void get_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/dashboard").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN CONTROLLERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class AdminFeatureFlags {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/admin/feature-flags")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/admin/feature-flags").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "settings:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/feature-flags").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    @Nested
    class AdminSystemSettings {

        @Test void get_anonymous_401() throws Exception {
            mvc.perform(get("/admin/settings")).andExpect(status().isUnauthorized());
        }

        @Test void patch_anonymous_401() throws Exception {
            mvc.perform(patch("/admin/settings").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void get_insufficientRole_403() throws Exception {
            mvc.perform(get("/admin/settings").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "settings:write")
        void get_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/settings").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    @Nested
    class AdminIntegrationApiKeys {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/admin/integration-api-keys")).andExpect(status().isUnauthorized());
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/admin/integration-api-keys")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "settings:read")
        void list_withReadPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/integration-api-keys").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(authorities = "settings:read")
        void create_readOnly_403() throws Exception {
            mvc.perform(post("/admin/integration-api-keys")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "settings:write")
        void create_withWritePermission_notBlocked() throws Exception {
            mvc.perform(post("/admin/integration-api-keys")
                            .header("Authorization", "Bearer t")
                            .contentType("application/json").content("{\"name\":\"k\"}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    @Nested
    class AdminPiiRegistry {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/admin/pii-registry")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/admin/pii-registry").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "audit:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/pii-registry").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    @Nested
    class AdminPermissions {

        @Test void catalogue_anonymous_401() throws Exception {
            mvc.perform(get("/admin/permissions/catalogue")).andExpect(status().isUnauthorized());
        }

        @Test void unlocked_anonymous_401() throws Exception {
            mvc.perform(get("/admin/permissions/unlocked")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "roles:read")
        void catalogue_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/permissions/catalogue").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REPORT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Reports {

        @Test void get_anonymous_401() throws Exception {
            mvc.perform(get("/reports")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void get_insufficientRole_403() throws Exception {
            mvc.perform(get("/reports").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "reports:read")
        void get_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/reports").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAXI BOOKING CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class TaxiBookings {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/taxi-bookings")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "taxi:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/taxi-bookings").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROVIDER MEDIA SUBMISSION CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ProviderMediaSubmissions {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/admin/media-submissions")).andExpect(status().isUnauthorized());
        }

        @Test void approve_anonymous_401() throws Exception {
            mvc.perform(post("/admin/media-submissions/{id}/approve",
                            "00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test void reject_anonymous_401() throws Exception {
            mvc.perform(post("/admin/media-submissions/{id}/reject",
                            "00000000-0000-0000-0000-000000000001")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void list_insufficientRole_403() throws Exception {
            mvc.perform(get("/admin/media-submissions").header("Authorization", "Bearer t"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "media_submissions:approve")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/admin/media-submissions").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REVIEW CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class Reviews {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/reviews")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "reviews:read")
        void list_withPermission_notBlocked() throws Exception {
            mvc.perform(get("/reviews").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WEBHOOK / USER DATA / MISC
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class WebhookSubscriptions {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/webhook-subscriptions")).andExpect(status().isUnauthorized());
        }

        @Test void create_anonymous_401() throws Exception {
            mvc.perform(post("/webhook-subscriptions")
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UserConsent {

        @Test void get_anonymous_401() throws Exception {
            mvc.perform(get("/user-consent")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void get_authenticated_notBlocked() throws Exception {
            mvc.perform(get("/user-consent").header("Authorization", "Bearer t"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    @Nested
    class UserDataExport {

        @Test void request_anonymous_401() throws Exception {
            mvc.perform(post("/user-data-export").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class InternalTickets {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/internal-tickets")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Departments {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/departments")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Employees {

        @Test void list_anonymous_401() throws Exception {
            mvc.perform(get("/employees")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Pricing {

        @Test void get_anonymous_401() throws Exception {
            mvc.perform(get("/pricing")).andExpect(status().isUnauthorized());
        }
    }
}
