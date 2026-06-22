/**
 * API response and DTO types matching backend.
 * Backend envelope: { success, message?, data, timestamp? }
 */

// Auth
export interface AuthResponseDto {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn?: number
  userId: string
  email: string
  role: string  // Backend UserRole enum: SUPER_ADMIN | CUSTOMER | STAFF
  fullName: string
  mustChangePassword?: boolean
  hasPortalAccess?: boolean
}

// Dashboard
export interface DashboardKpiDto {
  totalRevenue: number
  revenueCurrency: string
  activeBookings: number
  totalBookings: number
  totalProviders: number
  pendingComplaints: number
  openTickets: number
}

export interface WeeklyRevenueItem {
  /** ISO week start date string, e.g. "2025-05-12" */
  week: string
  amount: number
}

/** GET /portal/dashboard */
export interface PortalDashboardDto {
  serviceCount: number
  totalBookings: number
  activeBookings: number
  totalRevenue: number
  revenueCurrency: string
  weeklyRevenue?: WeeklyRevenueItem[]
  bookingsLast30Days?: number
  bookingsPrev30Days?: number
  revenueLast30Days?: number
  revenuePrev30Days?: number
}

/** GET /portal/earnings */
export interface PortalEarningsDto {
  start?: string
  end?: string
  /** Backward-compat alias for grossRevenue */
  totalEarnings: number
  currency: string
  grossRevenue?: number
  platformCommissionPct?: number
  platformFee?: number
  providerNet?: number
  /** providerNet minus pending payout requests — the actual amount available to request */
  availableForPayout?: number
  bookingCount?: number
  perServiceBreakdown?: PortalServiceEarningRow[]
}

export interface PortalServiceEarningRow {
  serviceId: string
  serviceName: string
  bookingCount: number
  grossRevenue: number
  platformFee: number
  providerNet: number
}

/** GET /portal/payout-requests, POST /portal/payout-request */
export interface PortalPayoutRequestDto {
  id: string
  amount: number
  currency: string
  notes?: string | null
  status: string
  requestedAt: string
}

export interface LinkableUserDto {
  id: string
  email: string
  name?: string
  phone?: string
}

export interface ActivityFeedItemDto {
  id?: string
  type?: string
  title?: string
  /** Backend audit action */
  action?: string
  entityType?: string
  description?: string
  timestamp?: string
  userId?: string
  userDisplay?: string
  changeSummary?: string
  metadata?: Record<string, unknown>
}

/** GET /dashboard/bootstrap */
export interface DashboardBootstrapDto {
  kpis: DashboardKpiDto
  activity: ActivityFeedItemDto[]
  serviceHealth: ServiceHealthDto
  commissionAnalysis: CommissionAnalysisDto
  payouts: PayoutsResponseDto
}

/** GET /dashboard/live (polling: KPIs + activity + health only) */
export interface DashboardLiveDto {
  kpis: DashboardKpiDto
  activity: ActivityFeedItemDto[]
  serviceHealth: ServiceHealthDto
}

export interface ServiceHealthDto {
  serviceCountByType: Record<string, number>
  activeBookingCountByType: Record<string, number>
}

export interface CommissionAnalysisDto {
  start?: string
  end?: string
  totalBaseAmount?: number
  totalCommissionAmount?: number
  currency?: string
}

export interface PayoutDto {
  providerId?: string
  providerName?: string
  amount?: number
  currency?: string
  periodStart?: string
  periodEnd?: string
}

export interface PayoutsResponseDto {
  start?: string
  end?: string
  payouts?: PayoutDto[]
}

// Users (paginated)
export interface UserDto {
  id: string
  email: string
  username?: string
  phone?: string
  role: 'SUPER_ADMIN' | 'CUSTOMER' | 'STAFF'
  status?: string
  fullName?: string
  firstName?: string
  lastName?: string
  createdAt?: string
  lastLoginAt?: string
  emailVerified?: boolean
  phoneVerified?: boolean
}

/** GET /users/staff-role-options — active sys_roles; submit rbacRoleId as primaryRbacRoleId on POST /users */
export interface StaffDirectoryRoleOptionDto {
  source?: 'SYSTEM' | 'CUSTOM'
  /** sys_roles.id — submit as primaryRbacRoleId on POST /users */
  rbacRoleId?: string | null
  securityUserRole?: string | null
  code: string
  displayName: string
  groupId?: string | null
  groupName?: string | null
  groupCode?: string | null
}

export interface PageDto<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// Roles & groups
export interface RoleDto {
  id: string
  name: string
  code?: string
  level?: string
  groupId?: string
  userCount?: number
  permissions?: string[]
  permissionIds?: string[]
  description?: string
  systemRole?: boolean
  /** Custom role dashboard sidebar (ordered); empty/absent = use assignee UserRole default */
  navigationItemIds?: string[] | null
  maxDiscountPct?: number
  /** Assignable to provider portal staff (custom roles only) */
  providerRole?: boolean
  /** Maximum single payout request amount for provider roles; null = unlimited */
  maxPayoutRequestAmount?: number | null
}

export interface UserNavigationDto {
  visibleItemIds: string[]
  source: string
  rbacRoleId?: string
  rbacRoleCode?: string
  userRole?: string
}

/** GET /users/rbac/custom-roles */
export interface RbacRoleOptionDto {
  id: string
  code?: string
  name?: string
}

/** GET /users/{id}/rbac-role or GET /users/by-email/{email}/rbac-role */
export interface UserRbacAssignmentDto {
  roleId?: string
  roleCode?: string
  roleName?: string
}

/** Admin review moderation list row (GET /reviews) */
export interface ReviewAdminRowDto {
  id: string
  bookingId?: string
  userId?: string
  userName?: string
  serviceId?: string
  serviceName?: string
  rating?: number
  comment?: string
  response?: string
  status?: string
  createdAt?: string
}

export interface ExchangeRateRowDto {
  id: string
  fromCurrency?: string
  toCurrency?: string
  rate?: number | string
  effectiveDate?: string
}

export interface GroupDto {
  id: string
  name: string
  code?: string
  description?: string
}

/** Organizational group with aggregates (GET /roles/groups/summary) */
export interface GroupSummaryDto {
  id: string
  name: string
  code?: string
  description?: string
  roleCount: number
  userCount: number
}

export interface PermissionCatalogueItemDto {
  id: string
  code: string
  name: string
  resource?: string
  action?: string
  locked?: boolean
}

// Services (bookable) — values match backend {@code ServiceType} enum names
export type ServiceTypeDto = 'HOTEL' | 'RESORT' | 'RESTAURANT' | 'TAXI' | 'TRIP'

/** Partner / listing types allowed when creating a provider (DB stores enum name). */
export const PARTNER_SERVICE_TYPE_VALUES: readonly ServiceTypeDto[] = [
  'HOTEL',
  'RESORT',
  'RESTAURANT',
  'TAXI',
  'TRIP',
] as const

export interface ServiceDto {
  id: string
  providerId?: string
  type: ServiceTypeDto
  name: string
  description?: string
  location?: string
  address?: string
  city?: string
  country?: string
  latitude?: number
  longitude?: number
  basePrice?: number
  currency?: string
  status?: string
  starRating?: number
  totalRooms?: number
  availableRooms?: number
  maxGuests?: number
  checkInTime?: string
  checkOutTime?: string
  policies?: string
  amenities?: Record<string, boolean>
  attributes?: Record<string, unknown>
  createdAt?: string
  updatedAt?: string
  rooms?: HotelRoomDto[]
  rejectionReason?: string
}

export type ServiceImageCategoryDto = 'PROPERTY' | 'ROOM' | 'TRIP' | 'OTHER'

export interface ServiceImageDto {
  id: string
  serviceId: string
  url: string
  altText?: string
  primary: boolean
  displayOrder: number
  category: ServiceImageCategoryDto
  contextKey?: string
}

export interface RestaurantMenuItemDto {
  id: string
  sectionId: string
  name: string
  description?: string
  price?: number
  currency?: string
  imageUrl?: string
  sortOrder: number
}

export interface RestaurantMenuSectionDto {
  id: string
  serviceId: string
  title: string
  sortOrder: number
  items: RestaurantMenuItemDto[]
}

export interface RestaurantMenuDto {
  serviceId: string
  sections: RestaurantMenuSectionDto[]
}

/** POST /services/{id}/images */
export interface CreateServiceImagePayload {
  url: string
  altText?: string
  category?: ServiceImageCategoryDto
  contextKey?: string
  primary?: boolean
  displayOrder?: number
}

/** PUT /services/{id}/images/{imageId} — null/omit fields unchanged (send only what you edit) */
export interface UpdateServiceImagePayload {
  url?: string
  altText?: string
  category?: ServiceImageCategoryDto
  contextKey?: string
  primary?: boolean
  displayOrder?: number
}

export interface CreateMenuSectionPayload {
  title: string
  sortOrder?: number
}

export interface UpdateMenuSectionPayload {
  title?: string
  sortOrder?: number
}

export interface CreateMenuItemPayload {
  name: string
  description?: string
  price?: number
  currency?: string
  imageUrl?: string
  sortOrder?: number
}

export interface UpdateMenuItemPayload {
  name?: string
  description?: string
  price?: number
  currency?: string
  imageUrl?: string
  sortOrder?: number
}

export type HotelRoomStatusDto = 'ACTIVE' | 'INACTIVE'

export interface HotelRoomImageDto {
  id: string
  roomId: string
  url: string
  altText?: string
  primary: boolean
  displayOrder: number
}

export interface HotelRoomDto {
  id: string
  serviceId: string
  roomType: string
  roomName: string
  description?: string
  capacity: number
  basePrice?: number
  currency?: string
  quantityTotal: number
  quantityAvailable: number
  amenities?: Record<string, unknown>
  status: HotelRoomStatusDto
  sortOrder: number
  images: HotelRoomImageDto[]
}

export interface CreateHotelRoomPayload {
  roomType: string
  roomName: string
  description?: string
  capacity: number
  basePrice?: number
  currency?: string
  quantityTotal: number
  quantityAvailable: number
  amenities?: Record<string, unknown>
  status?: HotelRoomStatusDto
  sortOrder?: number
}

export interface UpdateHotelRoomPayload {
  roomType?: string
  roomName?: string
  description?: string
  capacity?: number
  basePrice?: number
  currency?: string
  quantityTotal?: number
  quantityAvailable?: number
  amenities?: Record<string, unknown>
  status?: HotelRoomStatusDto
  sortOrder?: number
}

// Providers
export interface ServiceProviderDto {
  id: string
  userId?: string
  name: string
  phone?: string
  email?: string
  address?: string
  description?: string
  logoUrl?: string
  rating?: number
  status: string
  verified?: boolean
  profitMargin?: number
  type?: string
  registrationNumber?: string
  reviewCount?: number
  createdAt?: string
  approvedBy?: string
  approvedAt?: string
  subscriptionPlan?: string
  staffLimit?: number
  globalRate?: number
  expiryDate?: string
  expired?: boolean
}

export interface ProviderSubscriptionDto {
  id: string
  providerId: string
  providerName?: string
  plan: string
  staffLimit: number
}

/** POST /providers */
export interface CreateServiceProviderPayload {
  name: string
  phone: string
  address: string
  email?: string
  type?: string
  registrationNumber?: string
  description?: string
  logoUrl?: string
  managerEmail?: string
  managerPassword?: string
  managerPhone?: string
  managerRole?: string
  subscriptionPlan?: 'FREE' | 'PRO'
  globalRate?: number
  expiryDate?: string
}

/** PUT /providers/{id} (company staff); partial updates — include fields to change */
export type ProviderStatusDto =
  | 'PENDING_APPROVAL'
  | 'PENDING_VERIFICATION'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'INACTIVE'
  | 'REJECTED'
  | 'BLOCKED'

export interface UpdateServiceProviderPayload {
  name?: string
  phone?: string
  email?: string
  address?: string
  description?: string
  logoUrl?: string
  status?: ProviderStatusDto
  verified?: boolean
  profitMargin?: number
  globalRate?: number
  expiryDate?: string
}

export interface ProviderMediaSubmissionDto {
  id: string
  providerId: string
  serviceId?: string
  imageType: string
  contextKey?: string
  fileUrl: string
  altText?: string
  primary: boolean
  status: string
  submittedBy?: string
  submittedAt?: string
  reviewedBy?: string
  reviewedAt?: string
  reviewNote?: string
  createdAt?: string
}

/** PUT /providers/me (partial update; null/omit = unchanged on server) */
export interface UpdateProviderMePayload {
  name?: string
  phone?: string
  email?: string
  address?: string
  description?: string
  logoUrl?: string
}

/** POST /portal/services */
export interface CreatePortalServicePayload {
  providerId: string
  type: ServiceTypeDto
  name: string
  description?: string
  city?: string
  country?: string
  address?: string
  basePrice: number
  currency?: string
  maxGuests?: number
  totalRooms?: number
  availableRooms?: number
  starRating?: number
  checkInTime?: string
  checkOutTime?: string
  latitude?: number
  longitude?: number
  policies?: string
  amenities?: Record<string, boolean>
  attributes?: Record<string, unknown>
}

export type ServiceStatusDto =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'SUSPENDED'
  | 'PENDING_APPROVAL'
  | 'AVAILABLE'
  | 'UNAVAILABLE'
  | 'MAINTENANCE'
  | 'DISCONTINUED'
  | 'HIDDEN'

/** PUT /portal/services/{id} */
export interface UpdatePortalServicePayload {
  name?: string
  description?: string
  city?: string
  country?: string
  address?: string
  basePrice?: number
  status?: ServiceStatusDto
  maxGuests?: number
  totalRooms?: number
  availableRooms?: number
  starRating?: number
  checkInTime?: string
  checkOutTime?: string
  latitude?: number
  longitude?: number
  policies?: string
  amenities?: Record<string, boolean>
  attributes?: Record<string, unknown>
}

export type DiscountTypeDto = 'PERCENTAGE' | 'FIXED_AMOUNT'
export type DiscountStatusDto = 'PENDING_APPROVAL' | 'ACTIVE' | 'INACTIVE' | 'EXPIRED' | 'FULLY_REDEEMED' | 'SCHEDULED'

export interface DiscountBalanceDto {
  providerId: string
  currency: string
  allocatedAmount: number
  spentAmount: number
  availableAmount: number
}

export interface CreatePortalDiscountPayload {
  code: string
  type: DiscountTypeDto
  value: number
  description?: string
  endDate: string
  usageLimit?: number
  minBookingAmount?: number
  maxDiscountAmount?: number
  applicableServiceIds?: string[]
}

// Bookings
export interface BookingDto {
  id: string
  bookingReference: string
  customerId: string
  customerEmail?: string
  customerName?: string
  serviceId: string
  serviceName?: string
  serviceType?: string
  checkInDate?: string
  checkOutDate?: string
  guests?: number
  rooms?: number
  baseAmount?: number
  totalAmount?: number
  currency?: string
  status: string
  paymentMethod?: string
  paymentStatus?: string
  createdAt?: string
}

export interface AddPaymentPayload {
  amount: number
  currency: string
  method: 'CASH' | 'BANK_TRANSFER' | 'CHEQUE' | 'OTHER'
  transactionReference?: string
  notes?: string
}

// Payments
export interface PaymentDto {
  id: string
  bookingId?: string
  /** Human booking reference when provided by API */
  bookingReference?: string
  amount: number
  currency: string
  method: string
  status: string
  transactionReference?: string
  processedAt?: string
  gatewayReference?: string
  threeDsStatus?: string
  entityType?: string
  entityId?: string
  category?: string
}

// Map
export interface ProviderMapPinDto {
  id: string
  name: string
  type: string
  latitude: number
  longitude: number
  status?: string
  thumbnailUrl?: string
}

export interface DeliveryLocationDto {
  latitude: number
  longitude: number
  status?: string
  updatedAt?: string
}

// Tickets
export interface TicketDto {
  id: string
  ticketNumber: string
  type?: string
  subject: string
  priority: string
  status: string
  createdAt?: string
  reporterId?: string
  assignedToId?: string
}

// Complaints
/** GET /portal/staff — team row */
export interface PortalStaffMemberDto {
  staffLinkId?: string | null
  userId: string
  email?: string
  phone?: string
  role?: string
  title?: string | null
  owner?: boolean
  createdAt?: string
}

/** GET/POST /portal/support-requests */
export interface PortalSupportRequestDto {
  id: string
  providerId?: string | null
  providerName?: string | null
  subject: string
  body: string
  userId?: string | null
  createdAt?: string
  staffResponse?: string | null
  respondedAt?: string | null
  respondedByUserId?: string | null
}

export interface ComplaintDto {
  id: string
  ticketNumber?: string
  customerId?: string
  bookingId?: string
  bookingReference?: string
  subject: string
  description?: string
  category?: string
  priority: string
  status: string
  assignedAgentId?: string
  createdAt?: string
  updatedAt?: string
  resolutionNotes?: string
}

// Discounts
export interface DiscountDto {
  id: string
  code: string
  description?: string
  type: string
  value: number
  minBookingAmount?: number
  maxDiscountAmount?: number
  startDate?: string
  endDate?: string
  usageLimit?: number
  usageCount?: number
  status: string
  /** COMPANY | PROVIDER | BOTH */
  sponsor?: string
  /** Explicit company-side amount when sponsor = BOTH */
  companyValue?: number
  /** Explicit provider-side amount when sponsor = BOTH */
  providerValue?: number
  createdAt?: string
  updatedAt?: string
  /** Scope: null = any provider */
  providerId?: string
  applicableServiceIds?: string[]
  applicableMenuSectionIds?: string[]
  applicableMenuItemIds?: string[]
  applicableRoomTypeIds?: string[]
}

// Notifications
export interface NotificationDto {
  id: string
  userId?: string
  type?: string
  channel?: string
  status?: string
  title?: string
  message?: string
  sentAt?: string
  readAt?: string | null
  createdAt?: string
  referenceId?: string
}

export interface NotificationInboxDto {
  notifications: PageDto<NotificationDto>
  unreadCount: number
}

export interface ContentPageDto {
  slug: string
  content: Record<string, unknown>
  published: boolean
  updatedAt?: string
}

export interface UpsertContentPagePayload {
  contentEn: Record<string, unknown>
  contentAr: Record<string, unknown>
  published?: boolean
}

/** GET /admin/settings */
export interface SystemSettingsDto {
  companyDisplayName: string
  defaultCurrency: string
  maintenanceMode: boolean
  providerMaintenanceMode?: boolean
}

/** PUT /admin/settings */
export interface UpdateSystemSettingsPayload {
  companyDisplayName?: string
  defaultCurrency?: string
  maintenanceMode?: boolean
  providerMaintenanceMode?: boolean
}

/** GET /admin/feature-flags */
export interface FeatureFlagDto {
  id?: string
  flagKey: string
  enabled: boolean
  description?: string | null
  updatedAt?: string
  updatedBy?: string | null
}

/** GET /admin/integration-api-keys */
export interface IntegrationApiKeySummaryDto {
  id: string
  name: string
  keyPrefix: string
  createdAt?: string
  revokedAt?: string | null
  lastUsedAt?: string | null
}

/** POST /admin/integration-api-keys */
export interface IntegrationApiKeyCreatedDto extends IntegrationApiKeySummaryDto {
  plainSecret: string
}

/** POST /public/contact */
export interface PublicContactPayload {
  name: string
  email: string
  company?: string
  message: string
}

/** Super-admin deleted search row */
export interface DeletedItemDto {
  entityType: string
  id: string
  label: string
  detail?: string
  deletedAt?: string
}

// Webhooks
export interface WebhookSubscriptionDto {
  id: string
  providerId?: string
  name: string
  url: string
  events: string[]
  active: boolean
  createdAt: string
  /** Only returned on creation */
  secret?: string
}

export interface WebhookDeliveryDto {
  id: string
  subscriptionId: string
  event: string
  status: 'PENDING' | 'DELIVERED' | 'FAILED'
  httpStatus?: number
  attemptCount: number
  lastAttemptAt?: string
  createdAt: string
}

export interface CreateWebhookSubscriptionPayload {
  name: string
  url: string
  events: string[]
  providerId?: string
}

// Audit
export interface AuditLogDto {
  id: string
  userId?: string
  userDisplay?: string
  action: string
  entityType?: string
  entityId?: string
  oldValue?: string
  newValue?: string
  ipAddress?: string
  createdAt: string
}

// Reviews
export interface ReviewDto {
  id: string
  bookingId?: string
  userId?: string
  userName?: string
  serviceId: string
  rating: number
  comment?: string
  response?: string
  status?: string
  createdAt?: string
}

// Voucher
export interface VoucherDto {
  bookingId: string
  bookingReference: string
  checkInDate: string
  checkOutDate: string
  serviceName: string
  serviceId: string
  customerEmail: string
  customerId: string
  totalAmount: number
  currency: string
}

// Admin Payouts (Finance > Provider Payouts page)
export type PayoutStatus =
  | 'PENDING'
  | 'ON_HOLD'
  | 'SCHEDULED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'REJECTED'

export interface AdminPayoutDto {
  id: string
  providerId: string
  providerName?: string
  providerEmail?: string
  amount: number
  currency: string
  notes?: string
  status: PayoutStatus
  requestedAt?: string
  processedAt?: string
  processedBy?: string
  rejectionReason?: string
  transactionId?: string
  scheduledAt?: string
  manual?: boolean
  statusHistory?: AdminPayoutStatusHistoryEntry[]
}

export interface AdminPayoutStatusHistoryEntry {
  fromStatus?: string
  toStatus: string
  timestamp: string
  userId?: string
  userDisplay?: string
  notes?: string
}

export interface AdminPayoutSummaryDto {
  totalPayable: number
  pendingCount: number
  processingCount: number
  totalCompletedInPeriod: number
  failedOnHoldCount: number
  currency: string
}

export interface AdminPayoutActionPayload {
  notes?: string
  reason?: string
  transactionId?: string
  scheduledAt?: string
}

export interface CreateManualPayoutPayload {
  providerId: string
  amount: number
  currency?: string
  memo?: string
  executeImmediately?: boolean
}

export interface BulkPayoutActionPayload {
  ids: string[]
  notes?: string
}

export interface BulkPayoutResultDto {
  processed: number
  failed: string[]
}

// Backend envelope
export interface ApiEnvelope<T> {
  success: boolean
  message?: string
  data: T
  timestamp?: string
}
