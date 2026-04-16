/**
 * API client for Ziyara backend (core).
 * Base URL: VITE_API_URL or http://localhost:8080/api/v1
 * Unwraps ApiResponse.data on success; 401 redirects to /login.
 */

import axios, { type AxiosInstance } from 'axios'
import type {
  ApiEnvelope,
  BookingDto,
  ContentPageDto,
  CreateMenuItemPayload,
  CreateMenuSectionPayload,
  CreatePortalServicePayload,
  CreateServiceImagePayload,
  PageDto,
  PortalDashboardDto,
  PortalEarningsDto,
  PortalStaffMemberDto,
  PortalSupportRequestDto,
  RestaurantMenuDto,
  RestaurantMenuItemDto,
  RestaurantMenuSectionDto,
  ServiceDto,
  ServiceImageDto,
  ServiceProviderDto,
  UpdatePortalServicePayload,
  UpdateProviderMePayload,
  PublicContactPayload,
  SystemSettingsDto,
  FeatureFlagDto,
  IntegrationApiKeyCreatedDto,
  IntegrationApiKeySummaryDto,
  UpdateSystemSettingsPayload,
  UpsertContentPagePayload,
  UpdateMenuItemPayload,
  UpdateMenuSectionPayload,
  UpdateServiceImagePayload,
  CreateServiceProviderPayload,
} from '../types/api'

/** Extract a user-friendly message from an API error (Axios or thrown object). */
export function getApiErrorMessage(err: unknown, fallback = 'Request failed'): string {
  if (err == null) return fallback
  const ax = err as { response?: { status?: number; data?: unknown }; message?: string }
  const data = ax.response?.data
  if (data != null && typeof data === 'object') {
    const obj = data as Record<string, unknown>
    const msg = obj.message ?? obj.error
    if (typeof msg === 'string' && msg) return msg
  }
  const status = ax.response?.status
  if (status === 403) return 'Access denied'
  if (status === 429) return 'Too many requests. Please wait a moment and try again.'
  if (status === 404) return 'Not found'
  if (status === 500) return 'Server error. Please try again later.'
  if (typeof ax.message === 'string' && ax.message) return ax.message
  return fallback
}

const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1'

const client: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Attach token and Accept-Language to every request
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  const lang = localStorage.getItem('ziyara-lang')
  if (lang === 'ar' || lang === 'en') config.headers['Accept-Language'] = lang
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

// Unwrap { success, data } and redirect on 401
client.interceptors.response.use(
  (response) => {
    const body = response.data as ApiEnvelope<unknown>
    if (body && typeof body.success === 'boolean' && body.data !== undefined) {
      return { ...response, data: body.data }
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    // Reject the full error so callers can read response.status and response.data
    return Promise.reject(error)
  }
)

// --- Auth ---
export const authAPI = {
  login: (body: { email: string; password: string; rememberMe?: boolean }) =>
    client.post<unknown>('/auth/login', body),
  logout: () => client.post('/auth/logout'),
  refresh: (refreshToken: string) =>
    client.post<unknown>('/auth/refresh', null, {
      headers: { 'Refresh-Token': refreshToken },
    }),
}

// --- Services (bookable) ---
export const servicesAPI = {
  list: (params?: {
    page?: number
    size?: number
    type?: string
    status?: string
    city?: string
    country?: string
    providerId?: string
  }) =>
    client.get<unknown>('/services', {
      params: { page: 0, size: 50, ...params },
    }),
  search: (params?: {
    page?: number
    size?: number
    q?: string
    type?: string
    city?: string
    minPrice?: number
    maxPrice?: number
  }) =>
    client.get<unknown>('/services/search', {
      params: { page: 0, size: 50, ...params },
    }),
  get: (id: string) => client.get<unknown>(`/services/${id}`),
  getImages: (id: string) => client.get<ServiceImageDto[]>(`/services/${id}/images`),
  getMenu: (id: string) => client.get<RestaurantMenuDto>(`/services/${id}/menu`),
  addImage: (id: string, body: CreateServiceImagePayload) =>
    client.post<ServiceImageDto>(`/services/${id}/images`, body),
  updateImage: (id: string, imageId: string, body: UpdateServiceImagePayload) =>
    client.put<ServiceImageDto>(`/services/${id}/images/${imageId}`, body),
  deleteImage: (id: string, imageId: string) =>
    client.delete<void>(`/services/${id}/images/${imageId}`),
  uploadImage: (id: string, form: FormData) =>
    client.post<ServiceImageDto>(`/services/${id}/images/upload`, form),
  createMenuSection: (id: string, body: CreateMenuSectionPayload) =>
    client.post<RestaurantMenuSectionDto>(`/services/${id}/menu/sections`, body),
  updateMenuSection: (id: string, sectionId: string, body: UpdateMenuSectionPayload) =>
    client.put<RestaurantMenuSectionDto>(`/services/${id}/menu/sections/${sectionId}`, body),
  deleteMenuSection: (id: string, sectionId: string) =>
    client.delete<void>(`/services/${id}/menu/sections/${sectionId}`),
  createMenuItem: (id: string, sectionId: string, body: CreateMenuItemPayload) =>
    client.post<RestaurantMenuItemDto>(`/services/${id}/menu/sections/${sectionId}/items`, body),
  updateMenuItem: (id: string, itemId: string, body: UpdateMenuItemPayload) =>
    client.put<RestaurantMenuItemDto>(`/services/${id}/menu/items/${itemId}`, body),
  deleteMenuItem: (id: string, itemId: string) =>
    client.delete<void>(`/services/${id}/menu/items/${itemId}`),
}

/** Provider portal: dashboard, listings CRUD, bookings, earnings */
export const portalAPI = {
  getDashboard: () => client.get<PortalDashboardDto>('/portal/dashboard'),
  listServices: (params?: { page?: number; size?: number }) =>
    client.get<PageDto<ServiceDto>>('/portal/services', {
      params: { page: params?.page ?? 0, size: params?.size ?? 20 },
    }),
  createService: (body: CreatePortalServicePayload) =>
    client.post<ServiceDto>('/portal/services', body),
  updateService: (id: string, body: UpdatePortalServicePayload) =>
    client.put<ServiceDto>(`/portal/services/${id}`, body),
  deleteService: (id: string) => client.delete<void>(`/portal/services/${id}`),
  listBookings: () => client.get<BookingDto[]>('/portal/bookings'),
  getEarnings: (params?: { start?: string; end?: string }) =>
    client.get<PortalEarningsDto>('/portal/earnings', { params }),
}

/** Provider portal team (migration 023 + /portal/staff) */
export const portalStaffAPI = {
  list: () => client.get<PortalStaffMemberDto[]>('/portal/staff'),
  add: (body: { userId: string; title?: string }) =>
    client.post<PortalStaffMemberDto>('/portal/staff', body),
  createUser: (body: { email: string; password: string; role: string; phone?: string; title?: string }) =>
    client.post<PortalStaffMemberDto>('/portal/staff/users', body),
  update: (userId: string, body: { title?: string }) =>
    client.put<PortalStaffMemberDto>(`/portal/staff/${userId}`, body),
  remove: (userId: string) => client.delete<void>(`/portal/staff/${userId}`),
}

/** Phase 5: provider portal support requests (migration 025) */
export const portalSupportAPI = {
  list: () => client.get<PortalSupportRequestDto[]>('/portal/support-requests'),
  create: (body: { subject: string; body: string }) =>
    client.post<PortalSupportRequestDto>('/portal/support-requests', body),
}

/** Provider portal: same media/menu operations scoped to own listings */
export const portalServicesAPI = {
  getImages: (id: string) => client.get<ServiceImageDto[]>(`/portal/services/${id}/images`),
  getMenu: (id: string) => client.get<RestaurantMenuDto>(`/portal/services/${id}/menu`),
  addImage: (id: string, body: CreateServiceImagePayload) =>
    client.post<ServiceImageDto>(`/portal/services/${id}/images`, body),
  updateImage: (id: string, imageId: string, body: UpdateServiceImagePayload) =>
    client.put<ServiceImageDto>(`/portal/services/${id}/images/${imageId}`, body),
  deleteImage: (id: string, imageId: string) =>
    client.delete<void>(`/portal/services/${id}/images/${imageId}`),
  uploadImage: (id: string, form: FormData) =>
    client.post<ServiceImageDto>(`/portal/services/${id}/images/upload`, form),
  createMenuSection: (id: string, body: CreateMenuSectionPayload) =>
    client.post<RestaurantMenuSectionDto>(`/portal/services/${id}/menu/sections`, body),
  updateMenuSection: (id: string, sectionId: string, body: UpdateMenuSectionPayload) =>
    client.put<RestaurantMenuSectionDto>(`/portal/services/${id}/menu/sections/${sectionId}`, body),
  deleteMenuSection: (id: string, sectionId: string) =>
    client.delete<void>(`/portal/services/${id}/menu/sections/${sectionId}`),
  createMenuItem: (id: string, sectionId: string, body: CreateMenuItemPayload) =>
    client.post<RestaurantMenuItemDto>(`/portal/services/${id}/menu/sections/${sectionId}/items`, body),
  updateMenuItem: (id: string, itemId: string, body: UpdateMenuItemPayload) =>
    client.put<RestaurantMenuItemDto>(`/portal/services/${id}/menu/items/${itemId}`, body),
  deleteMenuItem: (id: string, itemId: string) =>
    client.delete<void>(`/portal/services/${id}/menu/items/${itemId}`),
}

// --- Dashboard ---
export const dashboardAPI = {
  /** Single round-trip: KPIs, activity, health, commission, payouts (server parallelizes). */
  getBootstrap: (params?: { start?: string; end?: string; activityLimit?: number }) =>
    client.get<unknown>('/dashboard/bootstrap', {
      params: {
        activityLimit: params?.activityLimit ?? 15,
        start: params?.start,
        end: params?.end,
      },
    }),
  getLive: (params?: { start?: string; end?: string; activityLimit?: number }) =>
    client.get<unknown>('/dashboard/live', {
      params: {
        activityLimit: params?.activityLimit ?? 15,
        start: params?.start,
        end: params?.end,
      },
    }),
  getKpis: (params?: { start?: string; end?: string }) =>
    client.get<unknown>('/dashboard/kpis', { params }),
  getActivity: (limit = 20) =>
    client.get<unknown>('/dashboard/activity', { params: { limit } }),
  getServiceHealth: () => client.get<unknown>('/dashboard/service-health'),
  getCommissionAnalysis: (params?: { start?: string; end?: string }) =>
    client.get<unknown>('/dashboard/commission-analysis', { params }),
  getPayouts: (params?: { start?: string; end?: string }) =>
    client.get<unknown>('/dashboard/payouts', { params }),
}

// --- Users ---
export const usersAPI = {
  list: (params?: { page?: number; size?: number; status?: string; role?: string }) =>
    client.get<unknown>('/users', { params: { page: 0, size: 100, ...params } }),
  get: (id: string) => client.get<unknown>(`/users/${id}`),
  getMe: () => client.get<unknown>('/users/me'),
  getMyNavigation: () => client.get<unknown>('/users/me/navigation'),
  getStaffRoleOptions: () => client.get<unknown>('/users/staff-role-options'),
  getRbacCustomRoles: () => client.get<unknown>('/users/rbac/custom-roles'),
  getUserRbacRole: (userId: string) => client.get<unknown>(`/users/${userId}/rbac-role`),
  getUserRbacRoleByEmail: (email: string) =>
    client.get<unknown>(`/users/by-email/${encodeURIComponent(email.trim())}/rbac-role`),
  updateMe: (body: { email?: string; phone?: string; status?: string }) =>
    client.put<unknown>('/users/me', body),
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    client.post<unknown>('/users/me/change-password', body),
  getLoginHistory: (id: string) => client.get<unknown>(`/users/${id}/login-history`),
  create: (body: Record<string, unknown>) => client.post<unknown>('/users', body),
  update: (id: string, body: Record<string, unknown>) =>
    client.put<unknown>(`/users/${id}`, body),
  delete: (id: string) => client.delete<unknown>(`/users/${id}`),
  freeze: (id: string) => client.post<unknown>(`/users/${id}/freeze`),
  unfreeze: (id: string) => client.post<unknown>(`/users/${id}/unfreeze`),
  resetPassword: (id: string, body: { newPassword: string }) =>
    client.post<unknown>(`/users/${id}/reset-password`, body),
  assignRbacRole: (id: string, body: { roleId?: string | null }) =>
    client.put<unknown>(`/users/${id}/rbac-role`, body),
  assignRbacRoleByEmail: (email: string, body: { roleId?: string | null }) =>
    client.put<unknown>(`/users/by-email/${encodeURIComponent(email.trim())}/rbac-role`, body),
}

// --- Roles & groups ---
export const rolesAPI = {
  list: () => client.get<unknown>('/roles'),
  get: (id: string) => client.get<unknown>(`/roles/${id}`),
  getGroups: () => client.get<unknown>('/roles/groups'),
  createGroup: (body: {
    name: string
    code?: string
    description?: string
    nameAr?: string
    descriptionAr?: string
  }) => client.post<unknown>('/roles/groups', body),
  /** Groups with roleCount and userCount (super_admin) */
  getGroupSummaries: () => client.get<unknown>('/roles/groups/summary'),
  getGroupMembers: (groupId: string, params?: { page?: number; size?: number }) =>
    client.get<unknown>(`/roles/groups/${groupId}/users`, { params: { page: 0, size: 20, ...params } }),
  getPermissionCatalogue: () => client.get<unknown>('/roles/permissions/catalogue'),
  create: (body: { name: string; description?: string; groupId?: string; permissionIds?: string[] }) =>
    client.post<unknown>('/roles', body),
  updatePermissions: (id: string, body: { permissionIds: string[] }) =>
    client.put<unknown>(`/roles/${id}/permissions`, body),
  /** Super admin: name/description (en + ar) */
  updateDetails: (
    id: string,
    body: { name?: string; description?: string; nameAr?: string; descriptionAr?: string },
  ) => client.put<unknown>(`/roles/${id}`, body),
  delete: (id: string, body?: { targetRoleId?: string }) =>
    client.delete<unknown>(`/roles/${id}`, { data: body }),
  updateNavigation: (id: string, body: { visibleItemIds: string[] }) =>
    client.put<unknown>(`/roles/${id}/navigation`, body),
}

// --- Providers ---
export const providersAPI = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>('/providers', { params: { page: 0, size: 20, ...params } }),
  /** Portal: current provider profile */
  getMe: () => client.get<ServiceProviderDto>('/providers/me'),
  updateMe: (body: UpdateProviderMePayload) => client.put<ServiceProviderDto>('/providers/me', body),
  get: (id: string) => client.get<unknown>(`/providers/${id}`),
  create: (body: CreateServiceProviderPayload) => client.post<ServiceProviderDto>('/providers', body),
  updateCommission: (id: string, body: { commissionRate: number }) =>
    client.patch<unknown>(`/providers/${id}/commission`, body),
  update: (id: string, body: Record<string, unknown>) =>
    client.put<unknown>(`/providers/${id}`, body),
  approve: (id: string) => client.post<unknown>(`/providers/${id}/approve`),
  reject: (id: string, body?: { reason?: string }) =>
    client.post<unknown>(`/providers/${id}/reject`, body ?? {}),
  suspend: (id: string) => client.post<unknown>(`/providers/${id}/suspend`),
}

// --- Discounts ---
export const discountsAPI = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>('/discounts', { params: { page: 0, size: 100, ...params } }),
  get: (id: string) => client.get<unknown>(`/discounts/${id}`),
  create: (body: Record<string, unknown>) => client.post<unknown>('/discounts', body),
  update: (id: string, body: Record<string, unknown>) =>
    client.put<unknown>(`/discounts/${id}`, body),
  delete: (id: string) => client.delete<unknown>(`/discounts/${id}`),
  approve: (id: string) => client.post<unknown>(`/discounts/${id}/approve`),
  deactivate: (id: string) => client.post<unknown>(`/discounts/${id}/deactivate`),
  validate: (code: string, amount: number) =>
    client.post<unknown>('/discounts/validate', { code }, { params: { amount } }),
}

// --- Reports ---
export const reportsAPI = {
  getRevenueReport: (
    start: string,
    end: string,
    opts?: { scope?: string; providerId?: string; customerId?: string },
  ) =>
    client.get<unknown>('/reports/revenue', {
      params: {
        start,
        end,
        scope: opts?.scope ?? 'ALL',
        ...(opts?.providerId ? { providerId: opts.providerId } : {}),
        ...(opts?.customerId ? { customerId: opts.customerId } : {}),
      },
    }),
  getBookingReport: (
    start: string,
    end: string,
    opts?: { scope?: string; providerId?: string; customerId?: string },
  ) =>
    client.get<unknown>('/reports/bookings', {
      params: {
        start,
        end,
        scope: opts?.scope ?? 'ALL',
        ...(opts?.providerId ? { providerId: opts.providerId } : {}),
        ...(opts?.customerId ? { customerId: opts.customerId } : {}),
      },
    }),
  searchReportCustomers: (q: string, limit = 20) =>
    client.get<unknown>('/reports/customer-search', { params: { q, limit } }),
}

// --- Bookings ---
export const bookingsAPI = {
  list: (params?: { scope?: string; status?: string; page?: number; size?: number }) =>
    client.get<unknown>('/bookings', { params: { page: 0, size: 20, ...params } }),
  listAdmin: (params?: { status?: string; page?: number; size?: number }) =>
    client.get<unknown>('/bookings/admin', { params: { page: 0, size: 20, ...params } }),
  get: (id: string) => client.get<unknown>(`/bookings/${id}`),
  getByReference: (ref: string) => client.get<unknown>(`/bookings/reference/${ref}`),
  confirm: (id: string) => client.post<unknown>(`/bookings/${id}/confirm`),
  cancel: (id: string, body?: { reason?: string }) =>
    client.post<unknown>(`/bookings/${id}/cancel`, null, { params: body?.reason ? { reason: body.reason } : {} }),
  getVoucher: (id: string) => client.get<unknown>(`/bookings/${id}/voucher`),
}

// --- Taxi bookings (ops) ---
export const taxiBookingsAPI = {
  listActive: () => client.get<unknown[]>('/taxi-bookings/active'),
  get: (id: string) => client.get<unknown>(`/taxi-bookings/${id}`),
  updateStatus: (id: string, status: string) =>
    client.patch<unknown>(`/taxi-bookings/${id}/status`, null, { params: { status } }),
  assignDriver: (
    id: string,
    params: { driverId: string; driverName: string; plate: string; model: string },
  ) => client.post<unknown>(`/taxi-bookings/${id}/assign`, null, { params }),
}

// --- Payments ---
export const paymentsAPI = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>('/payments', { params: { page: 0, size: 20, ...params } }),
  get: (id: string) => client.get<unknown>(`/payments/${id}`),
  getByRef: (ref: string) => client.get<unknown>(`/payments/transaction/${ref}`),
  refund: (id: string, body: { amount?: number; reason: string }) =>
    client.post<unknown>(`/payments/${id}/refund`, body),
}

// --- Currency / exchange rates (finance) ---
export const currencyAPI = {
  listRates: () => client.get<unknown[]>('/currency/rates'),
  getRate: (id: string) => client.get<unknown>(`/currency/rates/${id}`),
  createRate: (body: Record<string, unknown>) => client.post<unknown>('/currency/rates', body),
  updateRate: (id: string, body: Record<string, unknown>) =>
    client.put<unknown>(`/currency/rates/${id}`, body),
  deleteRate: (id: string) => client.delete<unknown>(`/currency/rates/${id}`),
}

// --- Tickets ---
export const ticketsAPI = {
  list: (params?: { status?: string; priority?: string }) =>
    client.get<unknown>('/tickets', { params }),
  get: (id: string) => client.get<unknown>(`/tickets/${id}`),
  getComments: (id: string) => client.get<unknown>(`/tickets/${id}/comments`),
  addComment: (id: string, body: { comment: string; isInternal?: boolean; isResolution?: boolean }) =>
    client.post<unknown>(`/tickets/${id}/comments`, {
      comment: body.comment,
      isInternal: body.isInternal ?? true,
      isResolution: body.isResolution ?? false,
    }),
  getStats: () => client.get<unknown>('/tickets/stats'),
  resolve: (id: string, params?: { notes?: string; summary?: string }) =>
    client.post<unknown>(`/tickets/${id}/resolve`, null, {
      params: { notes: params?.notes ?? 'Resolved', summary: params?.summary ?? '' },
    }),
  close: (id: string) => client.post<unknown>(`/tickets/${id}/close`),
  reopen: (id: string) => client.post<unknown>(`/tickets/${id}/reopen`),
}

// --- Complaints ---
export const complaintsAPI = {
  list: (params?: { page?: number; size?: number; status?: string; priority?: string }) =>
    client.get<unknown>('/complaints', { params: { page: 0, size: 100, ...params } }),
  get: (id: string) => client.get<unknown>(`/complaints/${id}`),
  getComments: (id: string, includeInternal?: boolean) =>
    client.get<unknown>(`/complaints/${id}/comments`, {
      params: { includeInternal: includeInternal ?? false },
    }),
  addComment: (id: string, body: { comment: string; isInternal?: boolean }) =>
    client.post<unknown>(`/complaints/${id}/comments`, {
      comment: body.comment,
      isInternal: body.isInternal ?? false,
    }),
  assign: (id: string, body: { agentId: string }) =>
    client.post<unknown>(`/complaints/${id}/assign`, body),
  resolve: (id: string, body?: { notes?: string }) =>
    client.post<unknown>(`/complaints/${id}/resolve`, body ?? {}),
  escalate: (id: string, body: { escalateToId: string }) =>
    client.post<unknown>(`/complaints/${id}/escalate`, body),
  close: (id: string) => client.post<unknown>(`/complaints/${id}/close`),
}

// --- Reviews (admin list + public service reviews) ---
export const reviewsAPI = {
  /** Company staff: paginated moderation list */
  listAdmin: (params?: {
    page?: number
    size?: number
    status?: string
    serviceId?: string
    start?: string
    end?: string
  }) =>
    client.get<PageDto<unknown>>('/reviews', { params: { page: 0, size: 20, ...params } }),
  getServiceReviews: (serviceId: string) => client.get<unknown[]>(`/reviews/service/${serviceId}`),
  get: (id: string) => client.get<unknown>(`/reviews/${id}`),
  moderate: (id: string, body: { status: string }) =>
    client.post<unknown>(`/reviews/${id}/moderate`, body),
  respond: (id: string, response: string) =>
    client.post<unknown>(`/reviews/${id}/respond`, null, { params: { response } }),
}

// --- Notifications ---
export const notificationsAPI = {
  list: (params?: { page?: number; size?: number }) =>
    client.get<unknown>('/notifications', { params: { page: 0, size: 20, ...params } }),
  get: (id: string) => client.get<unknown>(`/notifications/${id}`),
  markAsRead: (id: string) => client.patch<unknown>(`/notifications/${id}/read`),
  markAllAsRead: () => client.post<unknown>('/notifications/read-all'),
}

// --- Audit logs ---
export const auditLogsAPI = {
  getRecent: (params?: { limit?: number; search?: string }) =>
    client.get<unknown>('/audit-logs', { params }),
}

// --- Content pages (landing CMS) ---
export const contentPagesAPI = {
  get: (slug: string, lang?: 'en' | 'ar') =>
    client.get<ContentPageDto>(`/content-pages/${slug}`, { params: lang ? { lang } : undefined }),
  upsert: (slug: string, body: UpsertContentPagePayload) =>
    client.put<ContentPageDto>(`/content-pages/${slug}`, body),
}

/** Super admin: customer lookup & soft-delete recycle (users + services) */
/** Executive roles: SUPER_ADMIN, CEO, GENERAL_MANAGER */
export const settingsAPI = {
  get: () => client.get<SystemSettingsDto>('/admin/settings'),
  update: (body: UpdateSystemSettingsPayload) =>
    client.put<SystemSettingsDto>('/admin/settings', body),
}

/** Feature flags (SUPER_ADMIN, CEO, GENERAL_MANAGER) + integration API keys (Super Admin only) */
export const integrationsAPI = {
  listFeatureFlags: () => client.get<FeatureFlagDto[]>('/admin/feature-flags'),
  upsertFeatureFlag: (body: { flagKey: string; enabled?: boolean; description?: string }) =>
    client.put<FeatureFlagDto>('/admin/feature-flags', body),
  listApiKeys: () => client.get<IntegrationApiKeySummaryDto[]>('/admin/integration-api-keys'),
  createApiKey: (body: { name: string }) =>
    client.post<IntegrationApiKeyCreatedDto>('/admin/integration-api-keys', body),
  revokeApiKey: (id: string) => client.delete<void>(`/admin/integration-api-keys/${id}`),
}

/** Unauthenticated public marketing endpoints */
export const publicAPI = {
  submitContact: (body: PublicContactPayload) => client.post<unknown>('/public/contact', body),
}

export const adminSuperAPI = {
  searchCustomers: (q: string, limit = 25) =>
    client.get<unknown>('/admin/super/customers/search', { params: { q, limit } }),
  listRecentDeleted: (limit = 50) =>
    client.get<unknown>('/admin/super/deleted/recent', { params: { limit } }),
  searchDeleted: (q: string, limit = 50) =>
    client.get<unknown>('/admin/super/deleted/search', { params: { q, limit } }),
  restoreDeleted: (body: { entityType: string; id: string }) =>
    client.post<unknown>('/admin/super/deleted/restore', body),
  customerBookings: (userId: string, params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>(`/admin/super/customers/${userId}/bookings`, { params }),
  customerPayments: (userId: string, params?: { page?: number; size?: number }) =>
    client.get<unknown>(`/admin/super/customers/${userId}/payments`, { params }),
}

export default client
