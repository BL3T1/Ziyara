package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Shared {@link org.springframework.security.access.prepost.PreAuthorize} SpEL snippets.
 * Aligns with {@link UserRole#isCompanyDirectoryUser()} (company internal staff)
 * vs provider-facing roles.
 */
public final class ApiAuthorizationExpressions {

    private ApiAuthorizationExpressions() {
    }

    /**
     * Internal company dashboard users (excludes {@code CUSTOMER} and provider portal roles).
     */
    public static final String COMPANY_STAFF = "hasAnyRole("
            + "'SUPER_ADMIN',"
            + "'SALES_MANAGER','SALES_REPRESENTATIVE',"
            + "'FINANCE_MANAGER','ACCOUNTANT',"
            + "'SUPPORT_MANAGER','SUPPORT_AGENT',"
            + "'CEO','GENERAL_MANAGER')";

    /**
     * Website content editors for landing/CMS pages.
     * Explicitly includes sales roles so sales teams can edit public content:
     * SUPER_ADMIN, SALES_MANAGER, SALES_REPRESENTATIVE, plus exec roles.
     */
    public static final String CONTENT_PAGE_EDITOR = "hasAnyRole("
            + "'SUPER_ADMIN',"
            + "'SALES_MANAGER','SALES_REPRESENTATIVE',"
            + "'CEO','GENERAL_MANAGER')";

    /**
     * Provider partner portal accounts.
     */
    public static final String PROVIDER_PORTAL = "hasAnyRole("
            + "'PROVIDER_MANAGER','PROVIDER_FINANCE','PROVIDER_STAFF','TAXI_OPERATOR')";

    /**
     * Submit a new discount (may stay pending until Super Admin or CEO approves).
     * Managers, sales, finance, HR; excludes support agents.
     */
    public static final String DISCOUNT_CREATE = "hasAnyRole("
            + "'SUPER_ADMIN','CEO','GENERAL_MANAGER',"
            + "'SALES_MANAGER','SALES_REPRESENTATIVE',"
            + "'FINANCE_MANAGER','ACCOUNTANT',"
            + "'SUPPORT_MANAGER',"
            + "'HR_MANAGER')";

    /** Only Super Admin or CEO may approve (activate) a pending discount. */
    public static final String DISCOUNT_APPROVE = "hasAnyRole('SUPER_ADMIN','CEO')";

    /**
     * Submit a new provider account: Super Admin / CEO (immediate activation path) or Sales (pending approval).
     */
    public static final String PROVIDER_SUBMIT = "hasAnyRole('SUPER_ADMIN','CEO','SALES_MANAGER','SALES_REPRESENTATIVE')";

    /** Approve or reject pending providers (same gate as discount approval). */
    public static final String PROVIDER_APPROVE_OR_REJECT = "hasAnyRole('SUPER_ADMIN','CEO')";

    /** View payments, transactions, and financial reports. */
    public static final String PAYMENTS_READ =
            "hasAnyRole('SUPER_ADMIN','FINANCE_MANAGER','ACCOUNTANT','CEO','GENERAL_MANAGER')"
                    + " or hasAuthority('payments:read')";

    /** Initiate or update a payment record (complete/fail state transitions). */
    public static final String PAYMENTS_WRITE =
            "hasAnyRole('SUPER_ADMIN','FINANCE_MANAGER','ACCOUNTANT','CEO','GENERAL_MANAGER')"
                    + " or hasAuthority('payments:write')";

    /** Issue a refund — stricter gate than PAYMENTS_WRITE. */
    public static final String PAYMENTS_REFUND =
            "hasAnyRole('SUPER_ADMIN','FINANCE_MANAGER')"
                    + " or hasAuthority('payments:write')";

    /**
     * Partner portal: full management access.
     * Matches the account owner (PROVIDER_MANAGER) or any staff assigned ProviderStaffRole.MANAGER.
     */
    public static final String PORTAL_MANAGER =
            "hasRole('PROVIDER_MANAGER') or hasAuthority('PROVIDER_ROLE_MANAGER')";

    /**
     * Partner portal: financial operations (invoices, payouts, billing).
     * Matches PROVIDER_FINANCE account or staff assigned ProviderStaffRole.FINANCIAL_ADMIN.
     */
    public static final String PORTAL_FINANCE =
            "hasRole('PROVIDER_FINANCE') or hasAuthority('PROVIDER_ROLE_FINANCIAL_ADMIN')";

    /**
     * Runtime check (e.g. query-parameter guards) — stays aligned with {@link UserRole#isCompanyDirectoryUser()}.
     */
    public static boolean isCompanyStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            String a = ga.getAuthority();
            if (a == null || !a.startsWith("ROLE_")) {
                continue;
            }
            try {
                UserRole role = UserRole.valueOf(a.substring("ROLE_".length()));
                if (role.isCompanyDirectoryUser()) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // unknown authority
            }
        }
        return false;
    }
}
