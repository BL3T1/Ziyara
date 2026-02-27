import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FiSearch, FiMapPin, FiStar, FiUsers, FiShield, FiClock } from 'react-icons/fi';
import { serviceApi } from '../services/api';
import './HomePage.css';

function HomePage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [featuredServices, setFeaturedServices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadFeaturedServices();
  }, []);

  const loadFeaturedServices = async () => {
    try {
      const response = await serviceApi.getServices({ limit: 6 });
      setFeaturedServices(response.data.data || []);
    } catch (error) {
      console.error('Failed to load services:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      window.location.href = `/hotels?search=${encodeURIComponent(searchQuery)}`;
    }
  };

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <h1>Discover Your Perfect Journey</h1>
          <p>Book hotels, restaurants, and trips with ease. Your adventure starts here.</p>
          
          <form className="search-form" onSubmit={handleSearch}>
            <div className="search-input-wrapper">
              <FiSearch className="search-icon" />
              <input
                type="text"
                placeholder="Search hotels, restaurants, trips..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <button type="submit" className="search-btn">Search</button>
          </form>
        </div>
      </section>

      {/* Service Categories */}
      <section className="categories-section">
        <div className="container">
          <h2>Explore Our Services</h2>
          <div className="categories-grid">
            <Link to="/hotels" className="category-card hotels">
              <div className="category-icon">ð¨</div>
              <h3>Hotels</h3>
              <p>Find the perfect accommodation for your stay</p>
            </Link>
            <Link to="/restaurants" className="category-card restaurants">
              <div className="category-icon">ð½ï¸</div>
              <h3>Restaurants</h3>
              <p>Discover amazing dining experiences</p>
            </Link>
            <Link to="/trips" className="category-card trips">
              <div className="category-icon">ð</div>
              <h3>Trips</h3>
              <p>Explore exciting destinations and adventures</p>
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Services */}
      <section className="featured-section">
        <div className="container">
          <h2>Featured Services</h2>
          {loading ? (
            <div className="loading">Loading services...</div>
          ) : featuredServices.length > 0 ? (
            <div className="services-grid">
              {featuredServices.map((service) => (
                <Link to={`/services/${service.id}`} key={service.id} className="service-card">
                  <div className="service-image">
                    {service.images && service.images[0] ? (
                      <img src={service.images[0].imageUrl} alt={service.name} />
                    ) : (
                      <div className="placeholder-image">No Image</div>
                    )}
                  </div>
                  <div className="service-info">
                    <span className="service-type">{service.type}</span>
                    <h3>{service.name}</h3>
                    <p className="service-location">
                      <FiMapPin /> {service.location}
                    </p>
                    <div className="service-rating">
                      <FiStar /> {service.rating || '4.5'} ({service.reviewCount || 0} reviews)
                    </div>
                    <p className="service-price">
                      From <strong>${service.basePrice}</strong>/night
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="no-services">No services available at the moment.</div>
          )}
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="container">
          <h2>Why Choose Ziyarah?</h2>
          <div className="features-grid">
            <div className="feature-card">
              <FiShield className="feature-icon" />
              <h3>Secure Booking</h3>
              <p>Your transactions are protected with industry-standard security</p>
            </div>
            <div className="feature-card">
              <FiClock className="feature-icon" />
              <h3>Instant Confirmation</h3>
              <p>Get immediate booking confirmations via email and SMS</p>
            </div>
            <div className="feature-card">
              <FiUsers className="feature-icon" />
              <h3>24/7 Support</h3>
              <p>Our support team is always ready to assist you</p>
            </div>
            <div className="feature-card">
              <FiStar className="feature-icon" />
              <h3>Best Prices</h3>
              <p>Competitive rates with exclusive discounts and offers</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="container">
          <h2>Ready to Start Your Journey?</h2>
          <p>Join thousands of travelers who trust Ziyarah for their bookings</p>
          <Link to="/register" className="cta-btn">Create Free Account</Link>
        </div>
      </section>
    </div>
  );
}

export default HomePage;