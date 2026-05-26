package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.LoginHistoryEntryResponse;
import com.ziyara.backend.application.dto.request.AssignUserRbacRoleRequest;
import com.ziyara.backend.application.dto.request.ChangePasswordRequest;
import com.ziyara.backend.application.dto.request.CreateUserRequest;
import com.ziyara.backend.application.dto.request.ResetPasswordAdminRequest;
import com.ziyara.backend.application.dto.request.UpdateUserRequest;
import com.ziyara.backend.application.dto.response.RbacRoleOptionResponse;
import com.ziyara.backend.application.dto.response.StaffDirectoryRoleOptionResponse;
import com.ziyara.backend.application.dto.response.UserNavigationResponse;
import com.ziyara.backend.application.dto.response.UserRbacAssignmentResponse;
import com.ziyara.backend.application.query.UserQueryHandler;
import com.ziyara.backend.application.service.CompanyStaffRoleCatalogService;
import com.ziyara.backend.application.service.NavigationService;
import com.ziyara.backend.application.service.RbacAssignmentQueryService;
import com.ziyara.backend.application.service.UserRbacAssignmentService;
import com.ziyara.backend.application.query.dto.UserListQuery;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * User management API (CQRS: GET = jOOQ query handler, writes = JPA command handler).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserQueryHandler userQueryHandler;
    private final UserCommandHandler userCommandHandler;
    private final NavigationService navigationService;
    private final UserRbacAssignmentService userRbacAssignmentService;
    private final RbacAssignmentQueryService rbacAssignmentQueryService;
    private final CompanyStaffRoleCatalogService companyStaffRoleCatalogService;

    private static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalizeEmailPath(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UUID resolveUserIdByEmail(String emailRaw) {
        String email = normalizeEmailPath(emailRaw);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        return userQueryHandler.findUserIdByEmail(email);
    }

    @GetMapping("/staff-role-options")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(
            summary = "Company dashboard creatable roles",
            description = "Single picklist for POST /users: SYSTEM rows (enum-backed sys_roles) plus active custom RBAC roles "
                    + "(source=CUSTOM). Send staff-role-options.rbacRoleId as primaryRbacRoleId, or legacy role enum name. "
                    + "Excludes CUSTOMER, provider portal roles, SUPER_ADMIN, and inactive or ineligible custom roles.")
    public ResponseEntity<ApiResponse<List<StaffDirectoryRoleOptionResponse>>> listStaffRoleOptions() {
        return ResponseEntity.ok(ApiResponse.success(companyStaffRoleCatalogService.listDashboardCreatableRoles()));
    }

    @GetMapping("/rbac/custom-roles")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "List custom roles for RBAC assignment", description = "Minimal rows for dropdowns (HR cannot call /roles)")
    public ResponseEntity<ApiResponse<List<RbacRoleOptionResponse>>> listCustomRbacRoles() {
        return ResponseEntity.ok(ApiResponse.success(rbacAssignmentQueryService.listCustomRolesForAssignment()));
    }

    @GetMapping("/by-email/{email}/rbac-role")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Get user's RBAC role assignment by email", description = "Same as GET /{id}/rbac-role; email should be URL-encoded (e.g. %40 for @)")
    public ResponseEntity<ApiResponse<UserRbacAssignmentResponse>> getUserRbacRoleByEmail(@PathVariable String email) {
        UUID id = resolveUserIdByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(rbacAssignmentQueryService.getUserRbacAssignment(id)));
    }

    @PatchMapping("/by-email/{email}/rbac-role")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Assign RBAC role by email", description = "Same as PATCH /{id}/rbac-role; email should be URL-encoded (e.g. %40 for @)")
    public ResponseEntity<ApiResponse<Void>> assignRbacRoleByEmail(
            @PathVariable String email,
            @RequestBody(required = false) AssignUserRbacRoleRequest request
    ) {
        try {
            UUID id = resolveUserIdByEmail(email);
            UUID roleId = request != null ? request.getRoleId() : null;
            userRbacAssignmentService.assignOrClearPrimaryRbacRole(id, roleId);
            return ResponseEntity.ok(ApiResponse.success("RBAC role assignment updated", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/rbac-role")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Get user's RBAC role assignment", description = "sys_user_roles primary role, if any")
    public ResponseEntity<ApiResponse<UserRbacAssignmentResponse>> getUserRbacRole(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(rbacAssignmentQueryService.getUserRbacAssignment(id)));
    }

    @GetMapping("/me/navigation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Current user sidebar navigation", description = "Effective dashboard nav item IDs (RBAC role config or default by UserRole)")
    public ResponseEntity<ApiResponse<UserNavigationResponse>> getMyNavigation() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        UserNavigationResponse nav = navigationService.resolveNavigationForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(nav));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        return userQueryHandler.findById(userId)
                .map(u -> ResponseEntity.ok(ApiResponse.success(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found")));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update current user", description = "Update the authenticated user's profile (email, phone)")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        UpdateUserRequest safe = new UpdateUserRequest();
        safe.setEmail(request.getEmail());
        safe.setPhone(request.getPhone());
        safe.setStatus(request.getStatus());
        userCommandHandler.update(userId, safe);
        return userQueryHandler.findById(userId)
                .map(u -> ResponseEntity.ok(ApiResponse.success(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found")));
    }

    @PostMapping("/me/fcm-token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register FCM push token", description = "Saves the device FCM token so the backend can send push notifications to this device")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(@RequestBody Map<String, String> body) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("token is required"));
        }
        userCommandHandler.updateFcmToken(userId, token);
        return ResponseEntity.ok(ApiResponse.success("FCM token registered", null));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password", description = "Change the authenticated user's password (requires current password)")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        try {
            userCommandHandler.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password changed", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "List users", description = "Paginated company staff directory (excludes customers and provider accounts). Optional status/role filter. Admin/HR only.")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role
    ) {
        UserListQuery query = UserListQuery.builder()
                .page(page)
                .size(size)
                .status(status)
                .role(role)
                .build();
        Page<UserResponse> result = userQueryHandler.findPage(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER') or @userSecurity.isSelf(#id)")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        return userQueryHandler.findById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.success(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found")));
    }

    @GetMapping("/{id}/login-history")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER') or @userSecurity.isSelf(#id)")
    @Operation(summary = "Get user login history", description = "Returns last login(s) from users table")
    public ResponseEntity<ApiResponse<List<LoginHistoryEntryResponse>>> getLoginHistory(@PathVariable UUID id) {
        if (userQueryHandler.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        try {
            List<LoginHistoryEntryResponse> history = userQueryHandler.getLoginHistory(id);
            return ResponseEntity.ok(ApiResponse.success(history != null ? history : List.of()));
        } catch (Throwable e) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Create user", description = "Admin/HR only")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UUID userId = userCommandHandler.create(request);
        UserResponse response = userQueryHandler.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found after creation"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created", response));
    }

    @PatchMapping("/{id}/rbac-role")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Assign RBAC role for sidebar", description = "Company staff only; one primary role; omit or null roleId to clear")
    public ResponseEntity<ApiResponse<Void>> assignRbacRole(
            @PathVariable UUID id,
            @RequestBody(required = false) AssignUserRbacRoleRequest request
    ) {
        try {
            UUID roleId = request != null ? request.getRoleId() : null;
            userRbacAssignmentService.assignOrClearPrimaryRbacRole(id, roleId);
            return ResponseEntity.ok(ApiResponse.success("RBAC role assignment updated", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER') or @userSecurity.isSelf(#id)")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        userCommandHandler.update(id, request);
        return userQueryHandler.findById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.success(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Soft-delete user", description = "Admin/HR only")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userCommandHandler.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Freeze user account")
    public ResponseEntity<ApiResponse<Void>> freeze(@PathVariable UUID id) {
        userCommandHandler.freeze(id);
        return ResponseEntity.ok(ApiResponse.success("User frozen", null));
    }

    @PostMapping("/{id}/unfreeze")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Unfreeze user account")
    public ResponseEntity<ApiResponse<Void>> unfreeze(@PathVariable UUID id) {
        userCommandHandler.unfreeze(id);
        return ResponseEntity.ok(ApiResponse.success("User unfrozen", null));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_MANAGER')")
    @Operation(summary = "Reset user password", description = "Admin/HR only")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordAdminRequest request
    ) {
        userCommandHandler.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset", null));
    }

}
