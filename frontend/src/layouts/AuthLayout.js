import React from 'react';
import { Link } from 'react-router-dom';
import { FiArrowLeft } from 'react-icons/fi';
import './AuthLayout.css';

function AuthLayout({ children }) {
  return (
    <div className="auth-layout">
      <div className="auth-container">
        {/* Left Side - Branding */}
        <div className="auth-branding">
          <Link to="/" className="back-link">
            <FiArrowLeft /> Back to Home
          </Link>
          <div className="branding-content">
            <h1>Ziyarah</h1>
            <p>Your digital booking ecosystem for Hotels, Resorts, Restaurants, Taxis, and Trips.</p>
            <ul className="features-list">
              <li>Book hotels, restaurants, and trips</li>
              <li>Multi-currency support</li>
              <li>Real-time booking management</li>
              <li>24/7 customer support</li>
            </ul>
          </div>
        </div>

        {/* Right Side - Auth Form */}
        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            {children}
          </div>
        </div>
      </div>
    </div>
  );
}

export default AuthLayout;