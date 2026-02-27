import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FiMapPin, FiCalendar, FiUsers, FiX, FiEye, FiFilter } from 'react-icons/fi';
import { bookingApi } from '../../services/api';
import toast from 'react-hot-toast';
import './MyBookingsPage.css';

function MyBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    loadBookings();
  }, []);

  const loadBookings = async () => {
    try {
      const response = await bookingApi.getMyBookings();
      setBookings(response.data.data || []);
    } catch (error) {
      console.error('Failed to load bookings:', error);
      toast.error('Failed to load bookings');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) {
      return;
    }

    try {
      await bookingApi.cancelBooking(bookingId, { reason: 'Cancelled by user' });
      toast.success('Booking cancelled successfully');
      loadBookings();
    } catch (error) {
      console.error('Failed to cancel booking:', error);
      toast.error(error.response?.data?.message || 'Failed to cancel booking');
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const getStatusClass = (status) => {
    switch (status?.toUpperCase()) {
      case 'CONFIRMED': return 'confirmed';
      case 'PENDING': return 'pending';
      case 'CANCELLED': return 'cancelled';
      case 'COMPLETED': return 'completed';
      default: return '';
    }
  };

  const filteredBookings = bookings.filter(booking => {
    if (filter === 'all') return true;
    return booking.status?.toLowerCase() === filter;
  });

  if (loading) {
    return <div className="loading-page">Loading your bookings...</div>;
  }

  return (
    <div className="my-bookings-page">
      <div className="page-header">
        <div className="container">
          <h1>My Bookings</h1>
          <p>Manage your reservations</p>
        </div>
      </div>

      <div className="bookings-content">
        <div className="container">
          {/* Filter Tabs */}
          <div className="filter-tabs">
            <button 
              className={`tab ${filter === 'all' ? 'active' : ''}`}
              onClick={() => setFilter('all')}
            >
              All
            </button>
            <button 
              className={`tab ${filter === 'confirmed' ? 'active' : ''}`}
              onClick={() => setFilter('confirmed')}
            >
              Confirmed
            </button>
            <button 
              className={`tab ${filter === 'pending' ? 'active' : ''}`}
              onClick={() => setFilter('pending')}
            >
              Pending
            </button>
            <button 
              className={`tab ${filter === 'completed' ? 'active' : ''}`}
              onClick={() => setFilter('completed')}
            >
              Completed
            </button>
            <button 
              className={`tab ${filter === 'cancelled' ? 'active' : ''}`}
              onClick={() => setFilter('cancelled')}
            >
              Cancelled
            </button>
          </div>

          {/* Bookings List */}
          {filteredBookings.length > 0 ? (
            <div className="bookings-list">
              {filteredBookings.map((booking) => (
                <div key={booking.id} className="booking-card">
                  <div className="booking-image">
                    {booking.service?.images && booking.service.images[0] ? (
                      <img src={booking.service.images[0].imageUrl} alt={booking.service.name} />
                    ) : (
                      <div className="placeholder-image">No Image</div>
                    )}
                  </div>
                  
                  <div className="booking-details">
                    <div className="booking-header">
                      <div>
                        <h3>{booking.service?.name || 'Service'}</h3>
                        <p className="location">
                          <FiMapPin /> {booking.service?.location || 'Location not specified'}
                        </p>
                      </div>
                      <span className={`status-badge ${getStatusClass(booking.status)}`}>
                        {booking.status}
                      </span>
                    </div>

                    <div className="booking-info">
                      <div className="info-item">
                        <FiCalendar />
                        <span>{formatDate(booking.checkIn)} - {formatDate(booking.checkOut)}</span>
                      </div>
                      <div className="info-item">
                        <FiUsers />
                        <span>{booking.guests} {booking.guests === 1 ? 'Guest' : 'Guests'}</span>
                      </div>
                    </div>

                    <div className="booking-footer">
                      <div className="booking-price">
                        <span className="label">Total:</span>
                        <span className="amount">${booking.totalAmount?.toFixed(2)}</span>
                      </div>
                      <div className="booking-actions">
                        <Link to={`/booking/confirmation/${booking.id}`} className="btn view-btn">
                          <FiEye /> View Details
                        </Link>
                        {(booking.status === 'CONFIRMED' || booking.status === 'PENDING') && (
                          <button 
                            className="btn cancel-btn"
                            onClick={() => handleCancelBooking(booking.id)}
                          >
                            <FiX /> Cancel
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="no-bookings">
              <h3>No bookings found</h3>
              <p>You haven't made any bookings yet.</p>
              <Link to="/hotels" className="browse-btn">Browse Services</Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default MyBookingsPage;