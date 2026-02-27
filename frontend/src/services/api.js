import axios from 'axios';

// API base URL - can be configured via environment variable
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle errors
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response) {
      // Handle specific error codes
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
      return Promise.reject(error.response.data);
    }
    return Promise.reject(error);
  }
);

// =============================================================================
// AUTH API
// =============================================================================
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/auth/register', userData),
  logout: () => api.post('/auth/logout'),
  sendOTP: (phone) => api.post('/auth/otp/send', { phone }),
  verifyOTP: (data) => api.post('/auth/otp/verify', data),
  forgotPassword: (email) => api.post('/auth/password/forgot', { email }),
  resetPassword: (data) => api.post('/auth/password/reset', data),
  refreshToken: () => api.post('/auth/refresh'),
};

// =============================================================================
// USER API
// =============================================================================
export const userAPI = {
  getProfile: () => api.get('/users/me'),
  updateProfile: (data) => api.put('/users/me', data),
  changePassword: (data) => api.post('/users/me/change-password', data),
  getLoginHistory: () => api.get('/users/me/login-history'),
  freezeAccount: () => api.post('/users/me/freeze'),
};

// =============================================================================
// SERVICE API (Hotels, Restaurants, Trips, Taxis)
// =============================================================================
export const serviceAPI = {
  // List services with filters
  list: (params) => api.get('/services', { params }),
  
  // Get single service details
  get: (id) => api.get(`/services/${id}`),
  
  // Search services
  search: (params) => api.get('/services/search', { params }),
  
  // Check availability
  checkAvailability: (id, params) => api.get(`/services/${id}/availability`, { params }),
  
  // Get service images
  getImages: (id) => api.get(`/services/${id}/images`),
  
  // Get services by type
  getHotels: (params) => api.get('/services', { params: { type: 'HOTEL', ...params } }),
  getResorts: (params) => api.get('/services', { params: { type: 'RESORT', ...params } }),
  getRestaurants: (params) => api.get('/services', { params: { type: 'RESTAURANT', ...params } }),
  getTrips: (params) => api.get('/services', { params: { type: 'TRIP', ...params } }),
  getTaxis: (params) => api.get('/services', { params: { type: 'TAXI', ...params } }),
};

// =============================================================================
// BOOKING API
// =============================================================================
export const bookingAPI = {
  // Create booking
  create: (data) => api.post('/bookings', data),
  
  // Get user bookings
  list: (params) => api.get('/bookings', { params }),
  
  // Get booking details
  get: (id) => api.get(`/bookings/${id}`),
  
  // Update booking
  update: (id, data) => api.put(`/bookings/${id}`, data),
  
  // Cancel booking
  cancel: (id, reason) => api.post(`/bookings/${id}/cancel`, { reason }),
  
  // Confirm booking
  confirm: (id) => api.post(`/bookings/${id}/confirm`),
  
  // Get booking voucher
  getVoucher: (id) => api.get(`/bookings/${id}/voucher`),
  
  // Add taxi to booking
  addTaxi: (id, taxiData) => api.post(`/bookings/${id}/taxi`, taxiData),
};

// =============================================================================
// PAYMENT API
// =============================================================================
export const paymentAPI = {
  // Process payment
  process: (data) => api.post('/payments', data),
  
  // Get payment details
  get: (id) => api.get(`/payments/${id}`),
  
  // Request refund
  refund: (id, data) => api.post(`/payments/${id}/refund`, data),
  
  // Get payment by transaction ref
  getByRef: (ref) => api.get(`/payments/transaction/${ref}`),
};

// =============================================================================
// COMPLAINT API
// =============================================================================
export const complaintAPI = {
  // List complaints
  list: (params) => api.get('/complaints', { params }),
  
  // Create complaint
  create: (data) => api.post('/complaints', data),
  
  // Get complaint details
  get: (id) => api.get(`/complaints/${id}`),
  
  // Update complaint
  update: (id, data) => api.put(`/complaints/${id}`, data),
  
  // Add comment to complaint
  addComment: (id, comment) => api.post(`/complaints/${id}/comments`, { comment }),
};

// =============================================================================
// INTERNAL TICKET API (Bug Reports, Feature Requests)
// =============================================================================
export const ticketAPI = {
  // List tickets
  list: (params) => api.get('/tickets', { params }),
  
  // Create ticket
  create: (data) => api.post('/tickets', data),
  
  // Get ticket details
  get: (id) => api.get(`/tickets/${id}`),
  
  // Update ticket
  update: (id, data) => api.put(`/tickets/${id}`, data),
  
  // Delete ticket
  delete: (id) => api.delete(`/tickets/${id}`),
  
  // Ticket workflow actions
  acknowledge: (id) => api.post(`/tickets/${id}/acknowledge`),
  assign: (id, assigneeId) => api.post(`/tickets/${id}/assign`, null, { params: { assigneeId } }),
  startProgress: (id) => api.post(`/tickets/${id}/start-progress`),
  requestInfo: (id) => api.post(`/tickets/${id}/request-info`),
  moveToTesting: (id) => api.post(`/tickets/${id}/testing`),
  resolve: (id, notes, summary) => api.post(`/tickets/${id}/resolve`, null, { params: { notes, summary } }),
  verify: (id) => api.post(`/tickets/${id}/verify`),
  close: (id) => api.post(`/tickets/${id}/close`),
  reopen: (id) => api.post(`/tickets/${id}/reopen`),
  cancel: (id, reason) => api.post(`/tickets/${id}/cancel`, null, { params: { reason } }),
  
  // Comments
  getComments: (id) => api.get(`/tickets/${id}/comments`),
  addComment: (id, data) => api.post(`/tickets/${id}/comments`, data),
  
  // Statistics
  getStats: () => api.get('/tickets/stats'),
  getOverdue: () => api.get('/tickets/overdue'),
};

// =============================================================================
// REVIEW API
// =============================================================================
export const reviewAPI = {
  list: (params) => api.get('/reviews', { params }),
  create: (data) => api.post('/reviews', data),
  get: (id) => api.get(`/reviews/${id}`),
  update: (id, data) => api.put(`/reviews/${id}`, data),
  delete: (id) => api.delete(`/reviews/${id}`),
};

// =============================================================================
// DISCOUNT API
// =============================================================================
export const discountAPI = {
  validate: (code) => api.post('/discounts/validate', { code }),
  apply: (code, bookingId) => api.post('/discounts/apply', { code, bookingId }),
};

// =============================================================================
// NOTIFICATION API
// =============================================================================
export const notificationAPI = {
  list: (params) => api.get('/notifications', { params }),
  get: (id) => api.get(`/notifications/${id}`),
  markAsRead: (id) => api.post(`/notifications/${id}/read`),
  markAllAsRead: () => api.post('/notifications/read-all'),
};

// =============================================================================
// EXCHANGE RATE API
// =============================================================================
export const exchangeRateAPI = {
  list: () => api.get('/exchange-rates'),
  convert: (from, to, amount) => api.get('/exchange-rates/convert', { params: { from, to, amount } }),
};

// =============================================================================
// DASHBOARD API
// =============================================================================
export const dashboardAPI = {
  getRevenue: (params) => api.get('/dashboard/revenue', { params }),
  getBookings: (params) => api.get('/dashboard/bookings', { params }),
  getCustomers: (params) => api.get('/dashboard/customers', { params }),
  getProviders: (params) => api.get('/dashboard/providers', { params }),
};

export default api;