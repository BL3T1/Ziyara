package com.ziyara.backend.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Shared {@link org.springframework.security.access.prepost.PreAuthorize} SpEL snippets.
 *
 * All constants use pure {@code hasAuthority('permission:code')} checks — no role
 * checks. Any user who holds the required permission via their assigned role (system
 * or custom) can access the endpoint. Super Admin always passes because V17 seeds
 * every permission into the SUPER_ADMIN sys_role_permissions rows.
 */
public final class ApiAuthorizationExpressions {

    private ApiAuthorizationExpressions() {
    }

    // -------------------------------------------------------------------------
    // Company staff — broad gate: any user holding at least one company-scoped
    // permission qualifies. Used as class-level @PreAuthorize on controllers
    // whose every endpoint is equally accessible to all internal staff.
    // -------------------------------------------------------------------------

    public static final String COMPANY_STAFF =
            "hasAuthority('bookings:read') or hasAuthority('bookings:write')"
            + " or hasAuthority('users:read') or hasAuthority('users:write')"
            + " or hasAuthority('providers:read') or hasAuthority('providers:write')"
            + " or hasAuthority('payments:read') or hasAuthority('payments:write')"
            + " or hasAuthority('discounts:read') or hasAuthority('discounts:write') or hasAuthority('discounts:approve')"
            + " or hasAuthority('reports:read')"
            + " or hasAuthority('analytics:read')"
            + " or hasAuthority('roles:read') or hasAuthority('roles:write')"
            + " or hasAuthority('settings:read') or hasAuthority('settings:write')"
            + " or hasAuthority('audit:read')"
            + " or hasAuthority('content:read') or hasAuthority('content:write')"
            + " or hasAuthority('services:read') or hasAuthority('services:write')"
            + " or hasAuthority('taxi:read') or hasAuthority('taxi:write')"
            + " or hasAuthority('currency:read') or hasAuthority('currency:write')"
            + " or hasAuthority('providers_messages:read') or hasAuthority('providers_messages:write')"
            + " or hasAuthority('complaints:read') or hasAuthority('complaints:write')"
            + " or hasAuthority('reviews:read') or hasAuthority('reviews:write')"
            + " or hasAuthority('customers:read') or hasAuthority('customers:write')"
            + " or hasAuthority('internal_tickets:read') or hasAuthority('internal_tickets:write')"
            + " or hasAuthority('leads:read') or hasAuthority('leads:write')"
            + " or hasAuthority('deleted_items:read') or hasAuthority('deleted_items:restore')"
            + " or hasAuthority('deleted_items:company:read') or hasAuthority('deleted_items:company:restore')"
            + " or hasAuthority('deleted_items:providers:read') or hasAuthority('deleted_items:providers:restore')"
            + " or hasAuthority('deleted_items:users:read') or hasAuthority('deleted_items:users:restore')"
            + " or hasAuthority('payouts:read') or hasAuthority('payouts:write') or hasAuthority('payouts:approve')"
            + " or hasAuthority('webhooks:read') or hasAuthority('webhooks:write')"
            + " or hasAuthority('system:super_ops') or hasAuthority('system:bulk_export')";

    // -------------------------------------------------------------------------
    // Customers
    // -------------------------------------------------------------------------

    public static final String CUSTOMERS_READ = "hasAuthority('customers:read')";

    public static final String CUSTOMERS_WRITE = "hasAuthority('customers:write')";

    // -------------------------------------------------------------------------
    // Bookings
    // -------------------------------------------------------------------------

    public static final String BOOKINGS_READ = "hasAuthority('bookings:read')";

    public static final String BOOKINGS_WRITE = "hasAuthority('bookings:write')";

    // -------------------------------------------------------------------------
    // Users / HR
    // -------------------------------------------------------------------------

    public static final String USERS_READ = "hasAuthority('users:read')";

    public static final String USERS_WRITE = "hasAuthority('users:write')";

    public static final String USERS_RESET_PASSWORD = "hasAuthority('users:reset_password')";

    public static final String USERS_SELF_OR_READ =
            "hasAuthority('users:read') or @userSecurity.isSelf(#id)";

    public static final String USERS_SELF_OR_WRITE =
            "hasAuthority('users:write') or @userSecurity.isSelf(#id)";

    // -------------------------------------------------------------------------
    // Roles / RBAC management
    // -------------------------------------------------------------------------

    public static final String ROLES_READ = "hasAuthority('roles:read')";

    public static final String ROLES_WRITE = "hasAuthority('roles:write')";

    // -------------------------------------------------------------------------
    // Content / CMS
    // -------------------------------------------------------------------------

    public static final String CONTENT_PAGE_EDITOR = "hasAuthority('content:write')";

    // -------------------------------------------------------------------------
    // Discounts
    // -------------------------------------------------------------------------

    public static final String DISCOUNT_CREATE = "hasAuthority('discounts:write')";

    public static final String DISCOUNT_APPROVE = "hasAuthority('discounts:approve')";

    // -------------------------------------------------------------------------
    // Providers
    // -------------------------------------------------------------------------

    public static final String PROVIDER_SUBMIT = "hasAuthority('providers:write')";

    /** @deprecated Use {@link #PROVIDERS_APPROVE} for approve/reject actions. */
    public static final String PROVIDER_APPROVE_OR_REJECT = "hasAuthority('providers:write')";

    public static final String PROVIDERS_APPROVE = "hasAuthority('providers:approve')";

    public static final String MEDIA_SUBMISSIONS_APPROVE = "hasAuthority('media_submissions:approve')";

    public static final String SERVICES_PUBLISH = "hasAuthority('services:publish')";

    public static final String REVIEWS_MODERATE = "hasAuthority('reviews:moderate')";

    public static final String PROVIDER_COMMISSION = "hasAuthority('providers:write')";

    // -------------------------------------------------------------------------
    // Payments
    // -------------------------------------------------------------------------

    public static final String PAYMENTS_READ = "hasAuthority('payments:read')";

    public static final String PAYMENTS_WRITE = "hasAuthority('payments:write')";

    public static final String PAYMENTS_REFUND = "hasAuthority('payments:write')";

    // -------------------------------------------------------------------------
    // Payouts (Finance > Provider Payouts page)
    // -------------------------------------------------------------------------

    public static final String PAYOUTS_READ = "hasAuthority('payouts:read')";

    public static final String PAYOUTS_WRITE = "hasAuthority('payouts:write')";

    public static final String PAYOUTS_APPROVE = "hasAuthority('payouts:approve')";

    // -------------------------------------------------------------------------
    // Currency / FX rates
    // -------------------------------------------------------------------------

    public static final String CURRENCY_READ = "hasAuthority('currency:read')";

    public static final String CURRENCY_WRITE = "hasAuthority('currency:write')";

    // -------------------------------------------------------------------------
    // Reports / analytics
    // -------------------------------------------------------------------------

    public static final String REPORTS_READ = "hasAuthority('reports:read')";

    // -------------------------------------------------------------------------
    // Settings / feature flags / integrations
    // -------------------------------------------------------------------------

    public static final String SETTINGS_READ = "hasAuthority('settings:read')";

    public static final String SETTINGS_WRITE = "hasAuthority('settings:write')";

    // -------------------------------------------------------------------------
    // Audit logs
    // -------------------------------------------------------------------------

    public static final String AUDIT_READ = "hasAuthority('audit:read')";

    // -------------------------------------------------------------------------
    // Taxi
    // -------------------------------------------------------------------------

    public static final String TAXI_READ = "hasAuthority('taxi:read')";

    public static final String TAXI_WRITE = "hasAuthority('taxi:write')";

    // -------------------------------------------------------------------------
    // Subscriptions
    // -------------------------------------------------------------------------

    public static final String SUBSCRIPTIONS_READ = "hasAuthority('providers:read')";

    // -------------------------------------------------------------------------
    // Deleted items / recycle bin
    // -------------------------------------------------------------------------

    public static final String DELETED_ITEMS_READ =
            "hasAuthority('deleted_items:read')"
            + " or hasAuthority('deleted_items:company:read')"
            + " or hasAuthority('deleted_items:providers:read')"
            + " or hasAuthority('deleted_items:users:read')";

    public static final String DELETED_ITEMS_RESTORE =
            "hasAuthority('deleted_items:restore')"
            + " or hasAuthority('deleted_items:company:restore')"
            + " or hasAuthority('deleted_items:providers:restore')"
            + " or hasAuthority('deleted_items:users:restore')";

    // -------------------------------------------------------------------------
    // Provider partner portal — permission-based (no static role enum)
    // -------------------------------------------------------------------------

    public static final String PROVIDER_PORTAL  = "hasAuthority('portal:access')";

    public static final String PORTAL_MANAGER   = "hasAuthority('portal:manage')";

    public static final String PORTAL_FINANCE   = "hasAuthority('portal:finance')";

    // -------------------------------------------------------------------------
    // Runtime helper — used by BookingController and ReviewController to decide
    // whether to scope results to the authenticated user's own records.
    // -------------------------------------------------------------------------

    public static boolean isCompanyStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            String a = ga.getAuthority();
            if ("ROLE_STAFF".equals(a) || "ROLE_SUPER_ADMIN".equals(a)) {
                return true;
            }
        }
        return false;
    }
}
