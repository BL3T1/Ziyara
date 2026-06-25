/**
 * API client for Ziyara backend (core).
 * Base URL: VITE_API_URL or http://localhost:8080/api/v1
 * Unwraps ApiResponse.data on success; 401 redirects to /login.
 */

import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

// Extend InternalAxiosRequestConfig to carry our retry flag
interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}
import type {
  ApiEnvelope,
  BookingDto,
  ContentPageDto,
  CreateMenuItemPayload,
  CreateMenuSectionPayload,
  CreateHotelRoomPayload,
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
  HotelRoomDto,
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
  UpdateHotelRoomPayload,
  UpdateServiceImagePayload,
  CreateServiceProviderPayload,
  UpdateServiceProviderPayload,
  CreatePortalDiscountPayload,
  DiscountBalanceDto,
  DiscountDto,
  CreateWebhookSubscriptionPayload,
  WebhookDeliveryDto,
  WebhookSubscriptionDto,
  AddPaymentPayload,
  PaymentDto,
  ProviderMapPinDto,
  DeliveryLocationDto,
  LinkableUserDto,
  ReviewDto,
  ProfileEditRequestDto,
  ProviderRestaurantDto,
  IdentityStatusDto,
  IdentityVerificationEntryDto,
  ProviderFeatureSetDto,
  MarkRoomOccupiedPayload,
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
  if (status === 502 || status === 503)
    return 'The API server is unreachable (bad gateway). Check that the backend is running and that the API URL / proxy matches your setup.'
  if (status === 504) return 'The API server took too long to respond. Try again, or check backend and database health.'
  if (status === 500) return 'Server error. Please try again later.'
  if (typeof ax.message === 'string' && ax.message) return ax.message
  return fallback
}

const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1'

const client: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const m = document.cookie.match(new RegExp('(?:^|; )' + name.replace(/[.$?*|{}()[\]\\/+^]/g, '\\$&') + '=([^;]*)'))
  return m ? decodeURIComponent(m[1]) : null
}

// Attach token, CSRF (Spring CookieCsrfTokenRepository), and Accept-Language to every request
client.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  const lang = localStorage.getItem('ziyara-lang')
  if (lang === 'ar' || lang === 'en') config.headers['Accept-Language'] = lang
  const method = (config.method ?? 'get').toUpperCase()
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    const xsrf = readCookie('XSRF-TOKEN')
    if (xsrf) config.headers['X-XSRF-TOKEN'] = xsrf
  }
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

// Flag to prevent infinite refresh loops
let _refreshing = false
let _refreshQueue: Array<(token: string | null) => void> = []

function clearSession() {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
  sessionStorage.removeItem('ziyara_cookie_session')
}

// Unwrap { success, data }, attempt silent refresh on 401, let MFA errors propagate
client.interceptors.response.use(
  (response) => {
    const body = response.data as ApiEnvelope<unknown>
    if (body && typeof body.success === 'boolean' && body.data !== undefined) {
      return { ...response, data: body.data }
    }
    return response
  },
  async (error) => {
    const status = error.response?.status as number | undefined
    const errorCode = (error.response?.data as Record<string, unknown> | undefined)?.code as string | undefined
    const requestUrl = (error.config?.url ?? '') as string

    // MFA challenge — let the login page handle this, do NOT redirect
    if (status === 401 && errorCode === 'MFA_CODE_REQUIRED') {
      return Promise.reject(error)
    }

    // Never attempt refresh for auth endpoints themselves
    const isAuthEndpoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/refresh')
    const retryableConfig = error.config as RetryableConfig | undefined
    if (status === 401 && !isAuthEndpoint && !retryableConfig?._retried) {
      if (_refreshing) {
        // Queue the retry until refresh completes
        return new Promise((resolve, reject) => {
          _refreshQueue.push((newToken) => {
            if (!newToken) return reject(error)
            error.config.headers.Authorization = `Bearer ${newToken}`
            resolve(client(error.config))
          })
        })
      }

      _refreshing = true
      if (retryableConfig) retryableConfig._retried = true

      try {
        const res = await client.post<unknown>('/auth/refresh', null)
        const data = res.data as { accessToken?: string } | null
        const newToken = data?.accessToken ?? null
        if (newToken) {
          sessionStorage.setItem('token', newToken)
          _refreshQueue.forEach((cb) => cb(newToken))
          _refreshQueue = []
          error.config.headers.Authorization = `Bearer ${newToken}`
          return client(error.config)
        }
      } catch {
        _refreshQueue.forEach((cb) => cb(null))
        _refreshQueue = []
      } finally {
        _refreshing = false
      }

      // Refresh failed — clear session and redirect (guard against reload loop when already on login)
      clearSession()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }

    if (status === 401 && isAuthEndpoint) {
      // Wrong credentials during login — propagate so the form can show the error
      return Promise.reject(error)
    }

    // Reject the full error so callers can read response.status and response.data
    return Promise.reject(error)
  }
)

// --- Auth ---
export const authAPI = {
  login: (body: { email: string; password: string; rememberMe?: boolean; mfaCode?: string }) =>
    client.post<unknown>('/auth/login', body),
  register: (body: { firstName: string; lastName: string; email: string; password: string; phone?: string; role: 'CUSTOMER' }) =>
    client.post<unknown>('/auth/register', body),
  sendOtp: (body: { email: string; type?: string }) =>
    client.post<void>('/auth/otp/send', { emailOrPhone: body.email }),
  verifyOtp: (body: { email: string; code: string }) =>
    client.post<void>('/auth/otp/verify', { emailOrPhone: body.email, otp: body.code }),
  forgotPassword: (body: { email: string }) => client.post<unknown>('/auth/password/forgot', body),
  resetPasswordWithToken: (body: { token: string; newPassword: string }) =>
    client.post<unknown>('/auth/password/reset', body),
  logout: () => client.post('/auth/logout'),
  refresh: (refreshToken?: string) =>
    client.post<unknown>(
      '/auth/refresh',
      null,
      refreshToken
        ? {
            headers: { 'Refresh-Token': refreshToken },
          }
        : undefined,
    ),
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
  checkAvailability: (id: string, date: string, nights = 1) =>
    client.get<{ available: boolean; message?: string }>(`/services/${id}/availability`, { params: { date, nights } }),
  getImages: (id: string) => client.get<ServiceImageDto[]>(`/services/${id}/images`),
  getMenu: (id: string) => client.get<RestaurantMenuDto>(`/services/${id}/menu`),
  getServiceReviews: (id: string) => client.get<ReviewDto[]>(`/services/${id}/reviews`),
  addImage: (id: string, body: CreateServiceImagePayload) =>
    client.post<ServiceImageDto>(`/services/${id}/images`, body),
  updateImage: (id: string, imageId: string, body: UpdateServiceImagePayload) =>
    client.patch<ServiceImageDto>(`/services/${id}/images/${imageId}`, body),
  deleteImage: (id: string, imageId: string) =>
    client.delete<void>(`/services/${id}/images/${imageId}`),
  uploadImage: (id: string, form: FormData) =>
    client.post<ServiceImageDto>(`/services/${id}/images/upload`, form),
  listRooms: (id: string) => client.get<HotelRoomDto[]>(`/services/${id}/rooms`),
  createRoom: (id: string, body: CreateHotelRoomPayload) =>
    client.post<HotelRoomDto>(`/services/${id}/rooms`, body),
  updateRoom: (id: string, roomId: string, body: UpdateHotelRoomPayload) =>
    client.patch<HotelRoomDto>(`/services/${id}/rooms/${roomId}`, body),
  deleteRoom: (id: string, roomId: string) => client.delete<void>(`/services/${id}/rooms/${roomId}`),
  uploadRoomImage: (id: string, roomId: string, form: FormData) =>
    client.post(`/services/${id}/rooms/${roomId}/images/upload`, form),
  createMenuSection: (id: string, body: CreateMenuSectionPayload) =>
    client.post<RestaurantMenuSectionDto>(`/services/${id}/menu/sections`, body),
  updateMenuSection: (id: string, sectionId: string, body: UpdateMenuSectionPayload) =>
    client.patch<RestaurantMenuSectionDto>(`/services/${id}/menu/sections/${sectionId}`, body),
  deleteMenuSection: (id: string, sectionId: string) =>
    client.delete<void>(`/services/${id}/menu/sections/${sectionId}`),
  createMenuItem: (id: string, sectionId: string, body: CreateMenuItemPayload) =>
    client.post<RestaurantMenuItemDto>(`/services/${id}/menu/sections/${sectionId}/items`, body),
  updateMenuItem: (id: string, itemId: string, body: UpdateMenuItemPayload) =>
    client.patch<RestaurantMenuItemDto>(`/services/${id}/menu/items/${itemId}`, body),
  uploadMenuItemImage: (id: string, itemId: string, form: FormData) =>
    client.post<RestaurantMenuItemDto>(`/services/${id}/menu/items/${itemId}/image/upload`, form),
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
    client.patch<ServiceDto>(`/portal/services/${id}`, body),
  deleteService: (id: string) => client.delete<void>(`/portal/services/${id}`),
  listBookings: () => client.get<BookingDto[]>('/portal/bookings'),
  listBookingPayments: (bookingId: string) =>
    client.get<PaymentDto[]>(`/portal/bookings/${bookingId}/payments`),
  approveCashPayment: (bookingId: string, body: { amount: number; currency: string; notes?: string }) =>
    client.post<PaymentDto>(`/portal/bookings/${bookingId}/payments/cash-approve`, body),
  addPayment: (bookingId: string, body: AddPaymentPayload) =>
    client.post<PaymentDto>(`/portal/bookings/${bookingId}/payments`, body),
  getEarnings: (params?: { start?: string; end?: string }) =>
    client.get<PortalEarningsDto>('/portal/earnings', { params }),
  requestPayout: (body: { amount: number; notes?: string }) =>
    client.post<unknown>('/portal/payout-request', body),
  listPayouts: (params?: { page?: number; size?: number }) =>
    client.get<{ content: unknown[]; totalElements: number; totalPages: number }>('/portal/payout-requests', { params }),
  exportEarnings: (params?: { start?: string; end?: string }) =>
    client.get('/portal/earnings/export', { params, responseType: 'blob' }),
  getDiscountBalance: () => client.get<DiscountBalanceDto>('/portal/discount-balance'),
  listDiscounts: (params?: { page?: number; size?: number }) =>
    client.get<{ content: DiscountDto[]; totalElements: number; totalPages: number }>('/portal/discounts', { params }),
  createDiscount: (body: CreatePortalDiscountPayload) => client.post<DiscountDto>('/portal/discounts', body),
  deactivateDiscount: (id: string) => client.delete<void>(`/portal/discounts/${id}`),
}

export const portalCashAPI = {
  recordCollection: (bookingId: string, body: { amount: number; notes?: string; collectedAt?: string }) =>
    client.post<unknown>(`/portal/cash/bookings/${bookingId}/record`, body),
  listCollections: (params?: { page?: number; size?: number }) =>
    client.get<unknown>('/portal/cash/collections', { params }),
  getDailySheet: (params?: { date?: string }) =>
    client.get<unknown>('/portal/cash/daily-sheet', { params }),
}

export const adminCashAPI = {
  listPending: (params?: { page?: number; size?: number; providerId?: string }) =>
    client.get<unknown>('/admin/cash/pending-reconciliation', { params }),
  reconcile: (collectionId: string, body: { notes?: string }) =>
    client.post<unknown>(`/admin/cash/collections/${collectionId}/reconcile`, body),
  dispute: (collectionId: string, body: { reason: string }) =>
    client.post<unknown>(`/admin/cash/collections/${collectionId}/dispute`, body),
}

/** Provider portal team (migration 023 + /portal/staff) */
export const portalStaffAPI = {
  list: () => client.get<PortalStaffMemberDto[]>('/portal/staff'),
  add: (body: { userId: string; title?: string }) =>
    client.post<PortalStaffMemberDto>('/portal/staff', body),
  createUser: (body: { email: string; password: string; roleId: string; phone?: string; title?: string }) =>
    client.post<PortalStaffMemberDto>('/portal/staff/users', body),
  listAssignableRoles: () =>
    client.get<{ id: string; code: string; name: string }[]>('/portal/staff/roles'),
  update: (userId: string, body: { title?: string; email?: string }) =>
    client.patch<PortalStaffMemberDto>(`/portal/staff/${userId}`, body),
  remove: (userId: string) => client.delete<void>(`/portal/staff/${userId}`),
  resetPassword: (userId: string, body: { newPassword: string }) =>
    client.post<void>(`/portal/staff/${userId}/reset-password`, body),
  listLinkable: () =>
    client.get<LinkableUserDto[]>('/portal/staff/linkable'),
}

/** Phase 5: provider portal support requests (migration 025) */
export const portalSupportAPI = {
  list: () => client.get<PortalSupportRequestDto[]>('/portal/support-requests'),
  create: (body: { subject: string; body: string }) =>
    client.post<PortalSupportRequestDto>('/portal/support-requests', body),
}

/** Map: provider location pins and live delivery tracking */
export const mapAPI = {
  getPins: (params?: { types?: string }) =>
    client.get<ProviderMapPinDto[]>('/map/providers', { params }),
  getDeliveryLocation: (bookingId: string) =>
    client.get<DeliveryLocationDto>(`/map/delivery/${bookingId}`),
  getPortalPins: () =>
    client.get<ProviderMapPinDto[]>('/portal/map/pins'),
}

/** Staff-only: view and respond to provider portal support messages */
export const staffSupportRequestsAPI = {
  listAll: () => client.get<PortalSupportRequestDto[]>('/portal/support-requests/all'),
  respond: (id: string, response: string) =>
    client.post<PortalSupportRequestDto>(`/portal/support-requests/${id}/respond`, { response }),
}

/** Provider portal: same media/menu operations scoped to own listings */
export const portalServicesAPI = {
  getImages: (id: string) => client.get<ServiceImageDto[]>(`/portal/services/${id}/images`),
  getMenu: (id: string) => client.get<RestaurantMenuDto>(`/portal/services/${id}/menu`),
  addImage: (id: string, body: CreateServiceImagePayload) =>
    client.post<ServiceImageDto>(`/portal/services/${id}/images`, body),
  updateImage: (id: string, imageId: string, body: UpdateServiceImagePayload) =>
    client.patch<ServiceImageDto>(`/portal/services/${id}/images/${imageId}`, body),
  deleteImage: (id: string, imageId: string) =>
    client.delete<void>(`/portal/services/${id}/images/${imageId}`),
  uploadImage: (id: string, form: FormData) =>
    client.post<ServiceImageDto>(`/portal/services/${id}/images/upload`, form),
  listRooms: (id: string) => client.get<HotelRoomDto[]>(`/portal/services/${id}/rooms`),
  createRoom: (id: string, body: CreateHotelRoomPayload) =>
    client.post<HotelRoomDto>(`/portal/services/${id}/rooms`, body),
  updateRoom: (id: string, roomId: string, body: UpdateHotelRoomPayload) =>
    client.patch<HotelRoomDto>(`/portal/services/${id}/rooms/${roomId}`, body),
  deleteRoom: (id: string, roomId: string) => client.delete<void>(`/portal/services/${id}/rooms/${roomId}`),
  uploadRoomImage: (id: string, roomId: string, form: FormData) =>
    client.post(`/portal/services/${id}/rooms/${roomId}/images/upload`, form),
  createMenuSection: (id: string, body: CreateMenuSectionPayload) =>
    client.post<RestaurantMenuSectionDto>(`/portal/services/${id}/menu/sections`, body),
  updateMenuSection: (id: string, sectionId: string, body: UpdateMenuSectionPayload) =>
    client.patch<RestaurantMenuSectionDto>(`/portal/services/${id}/menu/sections/${sectionId}`, body),
  deleteMenuSection: (id: string, sectionId: string) =>
    client.delete<void>(`/portal/services/${id}/menu/sections/${sectionId}`),
  createMenuItem: (id: string, sectionId: string, body: CreateMenuItemPayload) =>
    client.post<RestaurantMenuItemDto>(`/portal/services/${id}/menu/sections/${sectionId}/items`, body),
  updateMenuItem: (id: string, itemId: string, body: UpdateMenuItemPayload) =>
    client.patch<RestaurantMenuItemDto>(`/portal/services/${id}/menu/items/${itemId}`, body),
  uploadMenuItemImage: (id: string, itemId: string, form: FormData) =>
    client.post<RestaurantMenuItemDto>(`/portal/services/${id}/menu/items/${itemId}/image/upload`, form),
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
  updateMe: (body: { email?: string; phone?: string; status?: string; firstName?: string; lastName?: string }) =>
    client.patch<unknown>('/users/me', body),
  getMyPermissions: () => client.get<string[]>('/users/me/permissions'),
  changePassword: (body: { currentPassword?: string; newPassword: string }) =>
    client.post<unknown>('/users/me/change-password', body),
  getLoginHistory: (id: string) => client.get<unknown>(`/users/${id}/login-history`),
  create: (body: Record<string, unknown>) => client.post<unknown>('/users', body),
  update: (id: string, body: Record<string, unknown>) =>
    client.patch<unknown>(`/users/${id}`, body),
  delete: (id: string) => client.delete<unknown>(`/users/${id}`),
  freeze: (id: string) => client.post<unknown>(`/users/${id}/freeze`),
  unfreeze: (id: string) => client.post<unknown>(`/users/${id}/unfreeze`),
  resetPassword: (id: string, body: { newPassword: string }) =>
    client.post<unknown>(`/users/${id}/reset-password`, body),
  assignRbacRole: (id: string, body: { roleId?: string | null }) =>
    client.patch<unknown>(`/users/${id}/rbac-role`, body),
  assignRbacRoleByEmail: (email: string, body: { roleId?: string | null }) =>
    client.patch<unknown>(`/users/by-email/${encodeURIComponent(email.trim())}/rbac-role`, body),
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
  updateGroup: (
    groupId: string,
    body: {
      name?: string
      code?: string
      description?: string
      nameAr?: string
      descriptionAr?: string
    },
  ) => client.patch<unknown>(`/roles/groups/${groupId}`, body),
  deleteGroup: (groupId: string) => client.delete<unknown>(`/roles/groups/${groupId}`),
  /** Groups with roleCount and userCount (super_admin) */
  getGroupSummaries: () => client.get<unknown>('/roles/groups/summary'),
  getGroupMembers: (groupId: string, params?: { page?: number; size?: number }) =>
    client.get<unknown>(`/roles/groups/${groupId}/users`, { params: { page: 0, size: 20, ...params } }),
  getPermissionCatalogue: () => client.get<unknown>('/roles/permissions/catalogue'),
  create: (body: { name: string; description?: string; groupId?: string; permissionIds?: string[]; providerRole?: boolean; maxDiscountPct?: number; maxPayoutRequestAmount?: number }) =>
    client.post<unknown>('/roles', body),
  updatePermissions: (id: string, body: { permissionIds: string[] }) =>
    client.put<unknown>(`/roles/${id}/permissions`, body),
  /** Super admin: name/description (en + ar) and optional group assignment */
  updateDetails: (
    id: string,
    body: {
      name?: string; description?: string; nameAr?: string; descriptionAr?: string
      groupId?: string | null; removeFromGroup?: boolean
      maxDiscountPct?: number; maxPayoutRequestAmount?: number
      clearPayoutLimit?: boolean
    },
  ) => client.patch<unknown>(`/roles/${id}`, body),
  delete: (id: string, body?: { targetRoleId?: string }) =>
    client.delete<unknown>(`/roles/${id}`, { data: body }),
  updateNavigation: (id: string, body: { visibleItemIds: string[] }) =>
    client.put<unknown>(`/roles/${id}/navigation`, body),
  getRoleMembers: (roleId: string, params?: { page?: number; size?: number }) =>
    client.get<unknown>(`/roles/${roleId}/users`, { params: { page: 0, size: 20, ...params } }),
}

// --- Subscriptions ---
export const subscriptionsAPI = {
  list: () => client.get<unknown>('/admin/subscriptions'),
  get: (providerId: string) => client.get<unknown>(`/admin/subscriptions/${providerId}`),
  upsert: (providerId: string, body: { plan: string; staffLimit: number }) =>
    client.put<unknown>(`/admin/subscriptions/${providerId}`, body),
}

// --- Providers ---
export const providersAPI = {
  list: (params?: { page?: number; size?: number; status?: string; type?: string }) =>
    client.get<unknown>('/providers', { params: { page: 0, size: 20, ...params } }),
  /** Portal: current provider profile */
  getMe: () => client.get<ServiceProviderDto>('/providers/me'),
  updateMe: (body: UpdateProviderMePayload) => client.patch<ServiceProviderDto>('/providers/me', body),
  get: (id: string) => client.get<unknown>(`/providers/${id}`),
  create: (body: CreateServiceProviderPayload) => client.post<ServiceProviderDto>('/providers', body),
  updateCommission: (id: string, body: { profitMargin: number }) =>
    client.patch<unknown>(`/providers/${id}/commission`, body),
  update: (id: string, body: UpdateServiceProviderPayload) =>
    client.patch<ServiceProviderDto>(`/providers/${id}`, body),
  approve: (id: string) => client.post<unknown>(`/providers/${id}/approve`),
  reject: (id: string, body?: { reason?: string }) =>
    client.post<unknown>(`/providers/${id}/reject`, body ?? {}),
  suspend: (id: string) => client.post<unknown>(`/providers/${id}/suspend`),
  delete: (id: string) => client.delete<unknown>(`/providers/${id}`),
  resetPassword: (id: string, body: { newPassword: string }) => client.post<unknown>(`/providers/${id}/reset-password`, body),
  adminToken: (id: string) => client.post<unknown>(`/providers/${id}/admin-token`),
}

// --- Provider media submissions ---
export const portalMediaAPI = {
  getMySubmissions: () => client.get<unknown[]>('/portal/media-submissions'),
  submitServiceImage: (serviceId: string, form: FormData) =>
    client.post<unknown>(`/portal/services/${serviceId}/images/submit`, form),
  submitProviderLogo: (form: FormData) =>
    client.post<unknown>('/portal/logo/submit', form),
}

export const adminMediaAPI = {
  list: () => client.get<unknown[]>('/admin/media-submissions'),
  approve: (id: string) => client.post<unknown>(`/admin/media-submissions/${id}/approve`),
  reject: (id: string, note?: string) =>
    client.post<unknown>(`/admin/media-submissions/${id}/reject`, { note }),
}

// --- Discounts ---
export const discountsAPI = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>('/discounts', { params: { page: 0, size: 100, ...params } }),
  get: (id: string) => client.get<unknown>(`/discounts/${id}`),
  create: (body: Record<string, unknown>) => client.post<unknown>('/discounts', body),
  update: (id: string, body: Record<string, unknown>) =>
    client.patch<unknown>(`/discounts/${id}`, body),
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
  getAnalytics: (start: string, end: string) =>
    client.get<unknown>('/reports/analytics', { params: { start, end } }),
  exportReport: (start: string, end: string, type: 'revenue' | 'bookings', format: 'excel' | 'pdf' | 'csv') =>
    client.get<Blob>('/reports/export', {
      params: { start, end, type, format },
      responseType: 'blob',
    }),
}

// --- Bookings ---
export const bookingsAPI = {
  create: (body: {
    serviceId: string
    checkInDate: string
    checkOutDate?: string
    guests?: number
    rooms?: number
    specialRequests?: string
    discountCode?: string
    currency?: string
    paymentMethod?: string
  }) => client.post<unknown>('/bookings', body),
  list: (params?: { scope?: string; status?: string; page?: number; size?: number }) =>
    client.get<unknown>('/bookings', { params: { page: 0, size: 20, ...params } }),
  listMy: () => client.get<BookingDto[]>('/bookings/my'),
  listAdmin: (params?: {
    status?: string
    providerId?: string
    serviceType?: string
    dateFrom?: string
    dateTo?: string
    page?: number
    size?: number
  }) =>
    client.get<unknown>('/bookings/admin', { params: { page: 0, size: 20, ...params } }),
  get: (id: string) => client.get<unknown>(`/bookings/${id}`),
  getByReference: (ref: string) => client.get<unknown>(`/bookings/reference/${ref}`),
  confirm: (id: string) => client.post<unknown>(`/bookings/${id}/confirm`),
  cancel: (id: string, body?: { reason?: string }) =>
    client.post<unknown>(`/bookings/${id}/cancel`, null, { params: body?.reason ? { reason: body.reason } : {} }),
  getVoucher: (id: string) => client.get<import('../types/api').VoucherDto>(`/bookings/${id}/voucher`),
  reject: (id: string, reason?: string) =>
    client.post<unknown>(`/bookings/${id}/reject`, null, reason ? { params: { reason } } : {}),
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

// --- Permission matrix ---
export const permissionsAPI = {
  getMatrix: () => client.get<unknown>('/admin/permissions/matrix'),
  upsert: (body: { roleId: string; module: string; action: string; granted: boolean }) =>
    client.post<unknown>('/admin/permissions/matrix', body),
  updateRole: (roleId: string, permissions: Array<{ module: string; action: string; granted: boolean }>) =>
    client.put<unknown>(`/admin/permissions/roles/${roleId}`, permissions),
}

// --- Payments ---
export const paymentsAPI = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>('/payments', { params: { page: 0, size: 20, ...params } }),
  summary: () => client.get<{ totalCollected: number; totalPending: number; totalRefunded: number; currency: string }>('/payments/summary'),
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
    client.patch<unknown>(`/currency/rates/${id}`, body),
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
  getServiceReviews: (serviceId: string) => client.get<import('../types/api').ReviewDto[]>(`/reviews/service/${serviceId}`),
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
  getUnreadCount: () => client.get<number>('/notifications/unread-count'),
}

// --- Audit logs ---
export const auditLogsAPI = {
  getRecent: (params?: { limit?: number; search?: string }) =>
    client.get<unknown>('/audit-logs', { params }),
  getFiltered: (params?: { page?: number; size?: number; entityType?: string; action?: string; dateFrom?: string; dateTo?: string }) =>
    client.get<unknown>('/audit-logs/filter', { params }),
  getForUser: (userId: string) => client.get<unknown>(`/audit-logs/user/${userId}`),
}

// --- Content pages (landing CMS) ---
export const contentPagesAPI = {
  get: (slug: string, lang?: 'en' | 'ar') =>
    client.get<ContentPageDto>(`/content-pages/${slug}`, { params: lang ? { lang } : undefined }),
  upsert: (slug: string, body: UpsertContentPagePayload) =>
    client.put<ContentPageDto>(`/content-pages/${slug}`, body),
}

/** Super admin: customer lookup & soft-delete recycle (users + services) */
export const settingsAPI = {
  get: () => client.get<SystemSettingsDto>('/admin/settings'),
  update: (body: UpdateSystemSettingsPayload) =>
    client.patch<SystemSettingsDto>('/admin/settings', body),
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
  permanentDelete: (body: { entityType: string; id: string }) =>
    client.delete<unknown>('/admin/super/deleted/permanent', { data: body }),
  customerBookings: (userId: string, params?: { page?: number; size?: number; status?: string }) =>
    client.get<unknown>(`/admin/super/customers/${userId}/bookings`, { params }),
  customerPayments: (userId: string, params?: { page?: number; size?: number }) =>
    client.get<unknown>(`/admin/super/customers/${userId}/payments`, { params }),
}

/** Finance ops: admin payout management — /admin/payouts */
export const adminPayoutsAPI = {
  getSummary: (params?: { start?: string; end?: string }) =>
    client.get<import('../types/api').AdminPayoutSummaryDto>('/admin/payouts/summary', { params }),
  list: (params?: {
    page?: number
    size?: number
    status?: string
    providerId?: string
    start?: string
    end?: string
    q?: string
  }) =>
    client.get<import('../types/api').PageDto<import('../types/api').AdminPayoutDto>>(
      '/admin/payouts',
      { params: { page: 0, size: 50, ...params } },
    ),
  get: (id: string) =>
    client.get<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}`),
  approve: (id: string, body?: import('../types/api').AdminPayoutActionPayload) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/approve`, body ?? {}),
  hold: (id: string) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/hold`, {}),
  releaseHold: (id: string) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/release-hold`, {}),
  cancel: (id: string) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/cancel`, {}),
  retry: (id: string) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/retry`, {}),
  markPaid: (id: string, body?: import('../types/api').AdminPayoutActionPayload) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/mark-paid`, body ?? {}),
  schedule: (id: string, scheduledAt: string) =>
    client.post<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/schedule`, { scheduledAt }),
  updateNotes: (id: string, notes: string) =>
    client.patch<import('../types/api').AdminPayoutDto>(`/admin/payouts/${id}/notes`, { notes }),
  bulkApprove: (body: import('../types/api').BulkPayoutActionPayload) =>
    client.post<import('../types/api').BulkPayoutResultDto>('/admin/payouts/bulk/approve', body),
  bulkHold: (body: import('../types/api').BulkPayoutActionPayload) =>
    client.post<import('../types/api').BulkPayoutResultDto>('/admin/payouts/bulk/hold', body),
  bulkReleaseHold: (body: import('../types/api').BulkPayoutActionPayload) =>
    client.post<import('../types/api').BulkPayoutResultDto>('/admin/payouts/bulk/release-hold', body),
  createManual: (body: import('../types/api').CreateManualPayoutPayload) =>
    client.post<import('../types/api').AdminPayoutDto>('/admin/payouts/manual', body),
  export: (params?: { status?: string; start?: string; end?: string }) =>
    client.get('/admin/payouts/export', { params, responseType: 'blob' }),
}

/** Outbound webhook subscription management — /admin/webhooks */
export const webhooksAPI = {
  list: (params?: { page?: number; size?: number }) =>
    client.get<WebhookSubscriptionDto[]>('/admin/webhooks', { params }),
  create: (body: CreateWebhookSubscriptionPayload) =>
    client.post<WebhookSubscriptionDto>('/admin/webhooks', body),
  delete: (id: string) => client.delete<void>(`/admin/webhooks/${id}`),
  setActive: (id: string, active: boolean) =>
    client.patch<void>(`/admin/webhooks/${id}/active`, null, { params: { active } }),
  ping: (id: string) => client.post<void>(`/admin/webhooks/${id}/ping`),
  listDeliveries: (id: string, params?: { page?: number; size?: number }) =>
    client.get<WebhookDeliveryDto[]>(`/admin/webhooks/${id}/deliveries`, { params }),
  listEvents: () => client.get<string[]>('/admin/webhooks/events'),
}

// --- Portal profile edit approval ---
export const portalProfileAPI = {
  submitEdit: (body: Record<string, unknown>) =>
    client.put<ProfileEditRequestDto>('/portal/profile', body),
  getEditStatus: () =>
    client.get<ProfileEditRequestDto | null>('/portal/profile/edit-status'),
  uploadLogo: (form: FormData) =>
    client.post<string>('/portal/profile/logo', form),
}

// --- Admin profile edit requests ---
export const adminProfileEditAPI = {
  listPending: () =>
    client.get<ProfileEditRequestDto[]>('/admin/profile-edit-requests'),
  approve: (id: string) =>
    client.post<ProfileEditRequestDto>(`/admin/profile-edit-requests/${id}/approve`),
  reject: (id: string, reason?: string) =>
    client.post<ProfileEditRequestDto>(`/admin/profile-edit-requests/${id}/reject`, { reason }),
}

// --- Portal restaurant ---
export const portalRestaurantAPI = {
  get: () => client.get<ProviderRestaurantDto>('/portal/restaurant'),
  create: (body: { name: string; nameAr?: string; description?: string; logoUrl?: string; openingHours?: Record<string, string> }) =>
    client.post<ProviderRestaurantDto>('/portal/restaurant', body),
  update: (body: { name?: string; nameAr?: string; description?: string; logoUrl?: string; openingHours?: Record<string, string> }) =>
    client.put<ProviderRestaurantDto>('/portal/restaurant', body),
}

// --- Walk-in conflict ---
export const portalWalkInAPI = {
  markOccupied: (serviceId: string, roomId: string, body: MarkRoomOccupiedPayload) =>
    client.post<unknown>(`/portal/services/${serviceId}/rooms/${roomId}/walk-in`, body),
  getFloors: (serviceId: string) =>
    client.get<number[]>(`/portal/services/${serviceId}/rooms/floors`),
  getFilteredRooms: (serviceId: string, params?: { floor?: number; category?: string; status?: string }) =>
    client.get<HotelRoomDto[]>(`/portal/services/${serviceId}/rooms/filtered`, { params }),
}

// --- Identity verification ---
export const identityAPI = {
  getStatus: () => client.get<IdentityStatusDto>('/profile/identity-status'),
  upload: (form: FormData) =>
    client.post<IdentityStatusDto>('/profile/identity-document', form),
}

// --- Admin identity verification ---
export const adminIdentityAPI = {
  list: (status?: string) =>
    client.get<IdentityVerificationEntryDto[]>('/admin/customers/identity-verifications', { params: { status } }),
  verify: (userId: string, body: { approved: boolean; reason?: string }) =>
    client.post<IdentityStatusDto>(`/admin/customers/${userId}/verify-identity`, body),
}

// --- Admin provider type ---
export const adminProviderTypeAPI = {
  updateType: (providerId: string, providerType: string) =>
    client.put<ProviderFeatureSetDto>(`/admin/providers/${providerId}/type`, { providerType }),
  getFeatures: (providerId: string) =>
    client.get<ProviderFeatureSetDto>(`/admin/providers/${providerId}/features`),
}

export default client
