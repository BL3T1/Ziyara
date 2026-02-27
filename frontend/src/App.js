import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import AuthLayout from './layouts/AuthLayout';

// Pages
import HomePage from './pages/HomePage';
import HotelsPage from './pages/services/HotelsPage';
import RestaurantsPage from './pages/services/RestaurantsPage';
import TripsPage from './pages/services/TripsPage';
import ServiceDetailPage from './pages/services/ServiceDetailPage';
import BookingPage from './pages/booking/BookingPage';
import BookingConfirmationPage from './pages/booking/BookingConfirmationPage';
import MyBookingsPage from './pages/booking/MyBookingsPage';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ProfilePage from './pages/user/ProfilePage';
import SupportPage from './pages/support/SupportPage';
import TicketDetailPage from './pages/support/TicketDetailPage';
import CreateTicketPage from './pages/support/CreateTicketPage';
import NotFoundPage from './pages/NotFoundPage';

// Dashboards & Roles
import RoleSelectionPage from './pages/dashboard/RoleSelectionPage';
import {
  SuperAdminDashboard,
  SalesDashboard,
  FinanceDashboard,
  SupportDashboard,
  ExecutiveDashboard,
  HRDashboard
} from './pages/roles/RoleDashboards';

// Protected Route Component
function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <div className="loading-screen">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

// Public Route (redirect if authenticated)
function PublicRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <div className="loading-screen">Loading...</div>;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return children;
}

function AppRoutes() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<MainLayout><HomePage /></MainLayout>} />
      <Route path="/hotels" element={<MainLayout><HotelsPage /></MainLayout>} />
      <Route path="/restaurants" element={<MainLayout><RestaurantsPage /></MainLayout>} />
      <Route path="/trips" element={<MainLayout><TripsPage /></MainLayout>} />
      <Route path="/services/:id" element={<MainLayout><ServiceDetailPage /></MainLayout>} />

      {/* Auth Routes */}
      <Route path="/login" element={<PublicRoute><AuthLayout><LoginPage /></AuthLayout></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><AuthLayout><RegisterPage /></AuthLayout></PublicRoute>} />
      <Route path="/forgot-password" element={<PublicRoute><AuthLayout><ForgotPasswordPage /></AuthLayout></PublicRoute>} />

      {/* Role Dashboards */}
      <Route path="/role-selection" element={<RoleSelectionPage />} />
      <Route path="/admin/dashboard" element={<ProtectedRoute><SuperAdminDashboard /></ProtectedRoute>} />
      <Route path="/sales/dashboard" element={<ProtectedRoute><SalesDashboard /></ProtectedRoute>} />
      <Route path="/finance/dashboard" element={<ProtectedRoute><FinanceDashboard /></ProtectedRoute>} />
      <Route path="/support/dashboard" element={<ProtectedRoute><SupportDashboard /></ProtectedRoute>} />
      <Route path="/executive/dashboard" element={<ProtectedRoute><ExecutiveDashboard /></ProtectedRoute>} />
      <Route path="/hr/dashboard" element={<ProtectedRoute><HRDashboard /></ProtectedRoute>} />

      {/* Protected Routes */}
      <Route path="/booking/:serviceId" element={<ProtectedRoute><MainLayout><BookingPage /></MainLayout></ProtectedRoute>} />
      <Route path="/booking/confirmation/:bookingId" element={<ProtectedRoute><MainLayout><BookingConfirmationPage /></MainLayout></ProtectedRoute>} />
      <Route path="/my-bookings" element={<ProtectedRoute><MainLayout><MyBookingsPage /></MainLayout></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><MainLayout><ProfilePage /></MainLayout></ProtectedRoute>} />
      <Route path="/support" element={<ProtectedRoute><MainLayout><SupportPage /></MainLayout></ProtectedRoute>} />
      <Route path="/support/tickets/:id" element={<ProtectedRoute><MainLayout><TicketDetailPage /></MainLayout></ProtectedRoute>} />
      <Route path="/support/tickets/new" element={<ProtectedRoute><MainLayout><CreateTicketPage /></MainLayout></ProtectedRoute>} />

      {/* 404 */}
      <Route path="*" element={<MainLayout><NotFoundPage /></MainLayout>} />
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              style: {
                background: '#22c55e',
              },
            },
            error: {
              style: {
                background: '#ef4444',
              },
            },
          }}
        />
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}

export default App;
