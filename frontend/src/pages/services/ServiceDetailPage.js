import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { FiMapPin, FiStar, FiCalendar, FiUsers, FiCheck, FiX, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { serviceApi } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import './ServiceDetailPage.css';

function ServiceDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [service, setService] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(0);
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [guests, setGuests] = useState(1);

  useEffect(() => {
    loadService();
  }, [id]);

  const loadService = async () => {
    try {
      const response = await serviceApi.getServiceById(id);
      setService(response.data.data);
    } catch (error) {
      console.error('Failed to load service:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleBooking = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/booking/${id}` } });
      return;
    }
    navigate(`/booking/${id}?checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}`);
  };

  const nextImage = () => {
    if (service?.images?.length > 0) {
      setSelectedImage((prev) => (prev + 1) % service.images.length);
    }
  };

  const prevImage = () => {
    if (service?.images?.length > 0) {
      setSelectedImage((prev) => (prev - 1 + service.images.length) % service.images.length);
    }
  };

  if (loading) {
    return <div className="loading-page">Loading service details...</div>;
  }

  if (!service) {
    return (
      <div className="not-found-page">
        <h2>Service not found</h2>
        <Link to="/">Go back home</Link>
      </div>
    );
  }

  const getServiceTypeLabel = (type) => {
    switch (type) {
      case 'HOTEL': return 'Hotel';
      case 'RESTAURANT': return 'Restaurant';
      case 'TRIP': return 'Trip';
      default: return type;
    }
  };

  return (
    <div className="service-detail-page">
      {/* Image Gallery */}
      <div className="image-gallery">
        {service.images && service.images.length > 0 ? (
          <>
            <img 
              src={service.images[selectedImage]?.imageUrl} 
              alt={service.name} 
              className="main-image"
            />
            {service.images.length > 1 && (
              <>
                <button className="gallery-nav prev" onClick={prevImage}>
                  <FiChevronLeft />
                </button>
                <button className="gallery-nav next" onClick={nextImage}>
                  <FiChevronRight />
                </button>
                <div className="image-indicators">
                  {service.images.map((_, index) => (
                    <button
                      key={index}
                      className={`indicator ${index === selectedImage ? 'active' : ''}`}
                      onClick={() => setSelectedImage(index)}
                    />
                  ))}
                </div>
              </>
            )}
          </>
        ) : (
          <div className="placeholder-image">No Image Available</div>
        )}
      </div>

      <div className="service-detail-content">
        <div className="main-info">
          {/* Header */}
          <div className="service-header">
            <span className="service-type-badge">{getServiceTypeLabel(service.type)}</span>
            <h1>{service.name}</h1>
            <p className="service-location">
              <FiMapPin /> {service.location}
            </p>
            <div className="service-rating">
              <FiStar className="star-icon" /> {service.rating || '4.5'} ({service.reviewCount || 0} reviews)
            </div>
          </div>

          {/* Description */}
          <div className="service-section">
            <h2>Description</h2>
            <p>{service.description}</p>
          </div>

          {/* Amenities */}
          {service.amenities && service.amenities.length > 0 && (
            <div className="service-section">
              <h2>Amenities</h2>
              <div className="amenities-grid">
                {service.amenities.map((amenity, index) => (
                  <div key={index} className="amenity-item">
                    <FiCheck className="check-icon" /> {amenity}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Policies */}
          {service.policies && (
            <div className="service-section">
              <h2>Policies</h2>
              <div className="policies">
                {service.policies.checkIn && (
                  <p><strong>Check-in:</strong> {service.policies.checkIn}</p>
                )}
                {service.policies.checkOut && (
                  <p><strong>Check-out:</strong> {service.policies.checkOut}</p>
                )}
                {service.policies.cancellation && (
                  <p><strong>Cancellation:</strong> {service.policies.cancellation}</p>
                )}
              </div>
            </div>
          )}

          {/* Reviews Section */}
          <div className="service-section">
            <h2>Reviews</h2>
            <div className="reviews-placeholder">
              <p>Reviews will be displayed here</p>
            </div>
          </div>
        </div>

        {/* Booking Sidebar */}
        <div className="booking-sidebar">
          <div className="booking-card">
            <div className="price-info">
              <span className="price">${service.basePrice}</span>
              <span className="price-unit">
                {service.type === 'HOTEL' ? '/night' : service.type === 'RESTAURANT' ? '/person' : '/person'}
              </span>
            </div>

            <div className="booking-form">
              <div className="form-group">
                <label>Check-in</label>
                <input
                  type="date"
                  value={checkIn}
                  onChange={(e) => setCheckIn(e.target.value)}
                  min={new Date().toISOString().split('T')[0]}
                />
              </div>
              <div className="form-group">
                <label>Check-out</label>
                <input
                  type="date"
                  value={checkOut}
                  onChange={(e) => setCheckOut(e.target.value)}
                  min={checkIn || new Date().toISOString().split('T')[0]}
                />
              </div>
              <div className="form-group">
                <label>Guests</label>
                <select value={guests} onChange={(e) => setGuests(parseInt(e.target.value))}>
                  {[1, 2, 3, 4, 5, 6, 7, 8].map(n => (
                    <option key={n} value={n}>{n} {n === 1 ? 'Guest' : 'Guests'}</option>
                  ))}
                </select>
              </div>
            </div>

            <button 
              className="book-btn"
              onClick={handleBooking}
              disabled={!checkIn || !checkOut}
            >
              {isAuthenticated ? 'Book Now' : 'Login to Book'}
            </button>

            <p className="booking-note">You won't be charged yet</p>
          </div>

          {/* Contact Info */}
          <div className="contact-card">
            <h3>Contact Information</h3>
            <p><strong>Phone:</strong> {service.contactPhone || 'Not available'}</p>
            <p><strong>Email:</strong> {service.contactEmail || 'Not available'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ServiceDetailPage;