import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FiCheck, FiMapPin, FiCalendar, FiUsers, FiDownload, FiHome, FiList } from 'react-icons/fi';
import { bookingApi } from '../../services/api';
import './BookingConfirmationPage.css';

function BookingConfirmationPage() {
  const { bookingId } = useParams();
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadBooking();
  }, [bookingId]);

  const loadBooking = async () => {
    try {
      const response = await bookingApi.getBookingById(bookingId);
      setBooking(response.data.data);
    } catch (error) {
      console.error('Failed to load booking:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (loading) {
    return <div className="loading-page">Loading booking details...</div>;
  }

  if (!booking) {
    return (
      <div className="not-found-page">
        <h2>Booking not found</h2>
        <Link to="/">Go back home</Link>
      </div>
    );
  }

  return (
    <div className="confirmation-page">
      <div className="confirmation-container">
        {/* Success Header */}
        <div className="success-header">
          <div className="success-icon">
            <FiCheck />
          </div>
          <h1>Booking Confirmed!</h1>
          <p>Your booking has been successfully confirmed. A confirmation email has been sent to your email address.</p>
        </div>

        {/* Booking Details Card */}
        <div className="booking-details-card">
          <div className="booking-header">
            <div className="booking-id">
              <span>Booking ID:</span>
              <strong>#{booking.id}</strong>
            </div>
            <span className={`status-badge ${booking.status?.toLowerCase()}`}>
              {booking.status}
            </span>
          </div>

          <div className="booking-content">
            {/* Service Info */}
            <div className="service-section">
              {booking.service?.images && booking.service.images[0] && (
                <img 
                  src={booking.service.images[0].imageUrl} 
                  alt={booking.service.name}
                  className="service-image"
                />
              )}
              <div className="service-details">
                <h2>{booking.service?.name || 'Service'}</h2>
                <p className="location">
                  <FiMapPin /> {booking.service?.location || 'Location not specified'}
                </p>
              </div>
            </div>

            {/* Booking Info Grid */}
            <div className="info-grid">
              <div className="info-item">
                <FiCalendar className="info-icon" />
                <div className="info-content">
                  <span className="info-label">Check-in</span>
                  <span className="info-value">{formatDate(booking.checkIn)}</span>
                </div>
              </div>
              <div className="info-item">
                <FiCalendar className="info-icon" />
                <div className="info-content">
                  <span className="info-label">Check-out</span>
                  <span className="info-value">{formatDate(booking.checkOut)}</span>
                </div>
              </div>
              <div className="info-item">
                <FiUsers className="info-icon" />
                <div className="info-content">
                  <span className="info-label">Guests</span>
                  <span className="info-value">{booking.guests} {booking.guests === 1 ? 'Guest' : 'Guests'}</span>
                </div>
              </div>
            </div>

            {/* Special Requests */}
            {booking.specialRequests && (
              <div className="special-requests">
                <h4>Special Requests</h4>
                <p>{booking.specialRequests}</p>
              </div>
            )}

            {/* Payment Summary */}
            <div className="payment-summary">
              <h4>Payment Summary</h4>
              <div className="summary-row">
                <span>Base Amount</span>
                <span>${booking.baseAmount?.toFixed(2)}</span>
              </div>
              {booking.discountAmount > 0 && (
                <div className="summary-row discount">
                  <span>Discount</span>
                  <span>-${booking.discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="summary-row total">
                <span>Total Paid</span>
                <span>${booking.totalAmount?.toFixed(2)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="confirmation-actions">
          <button className="action-btn download">
            <FiDownload /> Download Voucher
          </button>
          <Link to="/my-bookings" className="action-btn bookings">
            <FiList /> View My Bookings
          </Link>
          <Link to="/" className="action-btn home">
            <FiHome /> Back to Home
          </Link>
        </div>

        {/* Help Section */}
        <div className="help-section">
          <h3>Need Help?</h3>
          <p>If you have any questions about your booking, please contact our support team.</p>
          <Link to="/support" className="support-link">Contact Support</Link>
        </div>
      </div>
    </div>
  );
}

export default BookingConfirmationPage;