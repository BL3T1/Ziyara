import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FiMenu, FiX, FiUser, FiLogOut, FiCalendar, FiHelpCircle, FiBell } from 'react-icons/fi';
import './MainLayout.css';

function MainLayout({ children }) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="main-layout">
      {/* Header */}
      <header className="main-header">
        <div className="header-container">
          {/* Logo */}
          <Link to="/" className="logo">
            <span className="logo-text">Ziyarah</span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="desktop-nav">
            <Link to="/" className="nav-link">Home</Link>
            <Link to="/hotels" className="nav-link">Hotels</Link>
            <Link to="/restaurants" className="nav-link">Restaurants</Link>
            <Link to="/trips" className="nav-link">Trips</Link>
          </nav>

          {/* User Actions */}
          <div className="user-actions">
            {isAuthenticated ? (
              <>
                <Link to="/my-bookings" className="nav-link" title="My Bookings">
                  <FiCalendar />
                </Link>
                <Link to="/support" className="nav-link" title="Support">
                  <FiHelpCircle />
                </Link>
                <Link to="/profile" className="nav-link" title="Profile">
                  <FiUser />
                </Link>
                <button onClick={handleLogout} className="logout-btn" title="Logout">
                  <FiLogOut />
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="nav-link">Login</Link>
                <Link to="/register" className="register-btn">Sign Up</Link>
              </>
            )}
          </div>

          {/* Mobile Menu Button */}
          <button 
            className="mobile-menu-btn"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <FiX /> : <FiMenu />}
          </button>
        </div>

        {/* Mobile Navigation */}
        {mobileMenuOpen && (
          <nav className="mobile-nav">
            <Link to="/" onClick={() => setMobileMenuOpen(false)}>Home</Link>
            <Link to="/hotels" onClick={() => setMobileMenuOpen(false)}>Hotels</Link>
            <Link to="/restaurants" onClick={() => setMobileMenuOpen(false)}>Restaurants</Link>
            <Link to="/trips" onClick={() => setMobileMenuOpen(false)}>Trips</Link>
            {isAuthenticated ? (
              <>
                <Link to="/my-bookings" onClick={() => setMobileMenuOpen(false)}>My Bookings</Link>
                <Link to="/profile" onClick={() => setMobileMenuOpen(false)}>Profile</Link>
                <Link to="/support" onClick={() => setMobileMenuOpen(false)}>Support</Link>
                <button onClick={handleLogout}>Logout</button>
              </>
            ) : (
              <>
                <Link to="/login" onClick={() => setMobileMenuOpen(false)}>Login</Link>
                <Link to="/register" onClick={() => setMobileMenuOpen(false)}>Sign Up</Link>
              </>
            )}
          </nav>
        )}
      </header>

      {/* Main Content */}
      <main className="main-content">
        {children}
      </main>

      {/* Footer */}
      <footer className="main-footer">
        <div className="footer-container">
          <div className="footer-section">
            <h3>Ziyarah</h3>
            <p>Your digital booking ecosystem for Hotels, Resorts, Restaurants, Taxis, and Trips.</p>
          </div>
          
          <div className="footer-section">
            <h4>Services</h4>
            <Link to="/hotels">Hotels</Link>
            <Link to="/restaurants">Restaurants</Link>
            <Link to="/trips">Trips</Link>
          </div>
          
          <div className="footer-section">
            <h4>Support</h4>
            <Link to="/support">Help Center</Link>
            <Link to="/support/tickets/new">Report Issue</Link>
          </div>
          
          <div className="footer-section">
            <h4>Legal</h4>
            <Link to="/terms">Terms of Service</Link>
            <Link to="/privacy">Privacy Policy</Link>
          </div>
        </div>
        
        <div className="footer-bottom">
          <p>&copy; {new Date().getFullYear()} Ziyarah. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
}

export default MainLayout;