import React from 'react';
import { Link } from 'react-router-dom';
import { FiHome, FiArrowLeft } from 'react-icons/fi';
import './NotFoundPage.css';

function NotFoundPage() {
  return (
    <div className="not-found-page">
      <div className="content">
        <h1>404</h1>
        <h2>Page Not Found</h2>
        <p>The page you're looking for doesn't exist or has been moved.</p>
        
        <div className="actions">
          <button onClick={() => window.history.back()} className="back-btn">
            <FiArrowLeft /> Go Back
          </button>
          <Link to="/" className="home-btn">
            <FiHome /> Back to Home
          </Link>
        </div>
      </div>
    </div>
  );
}

export default NotFoundPage;