import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authAPI, userAPI } from '../services/api';

// Create Auth Context
const AuthContext = createContext(null);

// Auth Provider Component
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Initialize auth state
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');
      
      if (storedToken && storedUser) {
        try {
          setToken(storedToken);
          setUser(JSON.parse(storedUser));
          
          // Optionally verify token with backend
          // const profile = await userAPI.getProfile();
          // setUser(profile);
        } catch (err) {
          console.error('Auth initialization failed:', err);
          logout();
        }
      }
      setLoading(false);
    };
    
    initAuth();
  }, []);

  // Login function
  const login = useCallback(async (credentials) => {
    try {
      setError(null);
      setLoading(true);
      
      const response = await authAPI.login(credentials);
      
      if (response.success && response.data) {
        const { token: newToken, user: userData } = response.data;
        
        localStorage.setItem('token', newToken);
        localStorage.setItem('user', JSON.stringify(userData));
        
        setToken(newToken);
        setUser(userData);
        
        return { success: true };
      }
      
      return { success: false, message: response.message || 'Login failed' };
    } catch (err) {
      const message = err.message || 'Login failed';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  }, []);

  // Register function
  const register = useCallback(async (userData) => {
    try {
      setError(null);
      setLoading(true);
      
      const response = await authAPI.register(userData);
      
      if (response.success) {
        return { success: true, message: response.message };
      }
      
      return { success: false, message: response.message || 'Registration failed' };
    } catch (err) {
      const message = err.message || 'Registration failed';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  }, []);

  // Logout function
  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    setError(null);
    
    // Optionally call logout API
    authAPI.logout().catch(console.error);
  }, []);

  // Send OTP
  const sendOTP = useCallback(async (phone) => {
    try {
      setError(null);
      const response = await authAPI.sendOTP(phone);
      return { success: true, message: response.message };
    } catch (err) {
      const message = err.message || 'Failed to send OTP';
      setError(message);
      return { success: false, message };
    }
  }, []);

  // Verify OTP
  const verifyOTP = useCallback(async (data) => {
    try {
      setError(null);
      const response = await authAPI.verifyOTP(data);
      
      if (response.success && response.data) {
        const { token: newToken, user: userData } = response.data;
        
        localStorage.setItem('token', newToken);
        localStorage.setItem('user', JSON.stringify(userData));
        
        setToken(newToken);
        setUser(userData);
        
        return { success: true };
      }
      
      return { success: false, message: response.message || 'OTP verification failed' };
    } catch (err) {
      const message = err.message || 'OTP verification failed';
      setError(message);
      return { success: false, message };
    }
  }, []);

  // Forgot password
  const forgotPassword = useCallback(async (email) => {
    try {
      setError(null);
      const response = await authAPI.forgotPassword(email);
      return { success: true, message: response.message };
    } catch (err) {
      const message = err.message || 'Failed to send reset email';
      setError(message);
      return { success: false, message };
    }
  }, []);

  // Reset password
  const resetPassword = useCallback(async (data) => {
    try {
      setError(null);
      const response = await authAPI.resetPassword(data);
      return { success: true, message: response.message };
    } catch (err) {
      const message = err.message || 'Password reset failed';
      setError(message);
      return { success: false, message };
    }
  }, []);

  // Update profile
  const updateProfile = useCallback(async (data) => {
    try {
      setError(null);
      const response = await userAPI.updateProfile(data);
      
      if (response.success && response.data) {
        const updatedUser = { ...user, ...response.data };
        localStorage.setItem('user', JSON.stringify(updatedUser));
        setUser(updatedUser);
        return { success: true };
      }
      
      return { success: false, message: response.message };
    } catch (err) {
      const message = err.message || 'Profile update failed';
      setError(message);
      return { success: false, message };
    }
  }, [user]);

  // Check if user is authenticated
  const isAuthenticated = !!token && !!user;

  // Context value
  const value = {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    login,
    register,
    logout,
    sendOTP,
    verifyOTP,
    forgotPassword,
    resetPassword,
    updateProfile,
    setError,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

// Custom hook to use auth context
export function useAuth() {
  const context = useContext(AuthContext);
  
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  
  return context;
}

// Higher-order component for protected routes
export function withAuth(Component) {
  return function AuthenticatedComponent(props) {
    const { isAuthenticated, loading } = useAuth();
    
    if (loading) {
      return <div>Loading...</div>;
    }
    
    if (!isAuthenticated) {
      window.location.href = '/login';
      return null;
    }
    
    return <Component {...props} />;
  };
}

export default AuthContext;