import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom';
import { FiMapPin, FiStar, FiCreditCard, FiCheck, FiLoader } from 'react-icons/fi';
import { serviceApi, bookingApi, paymentApi } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import './BookingPage.css';

function BookingPage() {
  const { serviceId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [service, setService] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [step, setStep] = useState(1);

  const [bookingData, setBookingData] = useState({
    checkIn: searchParams.get('checkIn') || '',
    checkOut: searchParams.get('checkOut') || '',
    guests: parseInt(searchParams.get('guests')) || 1,
    specialRequests: '',
    discountCode: '',
    paymentMethod: 'CARD'
  });

  const [pricing, setPricing] = useState({
    baseAmount: 0,
    discountAmount: 0,
    totalAmount: 0,
    nights: 0
  });

  useEffect(() => {
    loadService();
  }, [serviceId]);

  useEffect(() => {
    calculatePricing();
  }, [bookingData.checkIn, bookingData.checkOut, service, bookingData.discountCode]);

  const loadService = async () => {
    try {
      const response = await serviceApi.getServiceById(serviceId);
      setService(response.data.data);
    } catch (error) {
      console.error('Failed to load service:', error);
      toast.error('Failed to load service details');
    } finally {
      setLoading(false);
    }
  };

  const calculatePricing = () => {
    if (!service || !bookingData.checkIn || !bookingData.checkOut) {
      return;
    }

    const checkIn = new Date(bookingData.checkIn);
    const checkOut = new Date(bookingData.checkOut);
    const nights = Math.ceil((checkOut - checkIn) / (1000 * 60 * 60 * 24));

    if (nights <= 0) return;

    const baseAmount = service.basePrice * nights * bookingData.guests;
    const discountAmount = 0; // Would be calculated from discount code
    const totalAmount = baseAmount - discountAmount;

    setPricing({
      baseAmount,
      discountAmount,
      totalAmount,
      nights
    });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setBookingData(prev => ({ ...prev, [name]: value }));
  };

  const validateStep1 = () => {
    if (!bookingData.checkIn || !bookingData.checkOut) {
      toast.error('Please select check-in and check-out dates');
      return false;
    }
    if (new Date(bookingData.checkOut) <= new Date(bookingData.checkIn)) {
      toast.error('Check-out date must be after check-in date');
      return false;
    }
    return true;
  };

  const handleNextStep = () => {
    if (step === 1 && validateStep1()) {
      setStep(2);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      // Create booking
      const bookingResponse = await bookingApi.createBooking({
        serviceId: parseInt(serviceId),
        checkIn: bookingData.checkIn,
        checkOut: bookingData.checkOut,
        guests: bookingData.guests,
        specialRequests: bookingData.specialRequests,
        discountCode: bookingData.discountCode || null
      });

      const bookingId = bookingResponse.data.data.id;

      // Process payment
      await paymentApi.processPayment({
        bookingId,
        amount: pricing.totalAmount,
        currency: 'USD',
        method: bookingData.paymentMethod
      });

      toast.success('Booking confirmed successfully!');
      navigate(`/booking/confirmation/${bookingId}`);
    } catch (error) {
      console.error('Booking failed:', error);
      toast.error(error.response?.data?.message || 'Failed to complete booking');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="loading-page">Loading booking details...</div>;
  }

  if (!service) {
    return (
      <div className="not-found-page">
        <h2>Service not found</h2>
        <Link to="/">Go back home</Link>
      </div>
    );
  }

  return (
    <div className="booking-page">
      <div className="booking-container">
        {/* Progress Steps */}
        <div className="progress-steps">
          <div className={`step ${step >= 1 ? 'active' : ''}`}>
            <span className="step-number">1</span>
            <span className="step-label">Details</span>
          </div>
          <div className="step-line"></div>
          <div className={`step ${step >= 2 ? 'active' : ''}`}>
            <span className="step-number">2</span>
            <span className="step-label">Payment</span>
          </div>
        </div>

        <div className="booking-content">
          {/* Main Form */}
          <div className="booking-form-section">
            <form onSubmit={handleSubmit}>
              {step === 1 && (
                <div className="form-card">
                  <h2>Booking Details</h2>
                  
                  <div className="form-row">
                    <div className="form-group">
                      <label>Check-in Date</label>
                      <input
                        type="date"
                        name="checkIn"
                        value={bookingData.checkIn}
                        onChange={handleInputChange}
                        min={new Date().toISOString().split('T')[0]}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Check-out Date</label>
                      <input
                        type="date"
                        name="checkOut"
                        value={bookingData.checkOut}
                        onChange={handleInputChange}
                        min={bookingData.checkIn || new Date().toISOString().split('T')[0]}
                        required
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label>Number of Guests</label>
                    <select
                      name="guests"
                      value={bookingData.guests}
                      onChange={handleInputChange}
                    >
                      {[1, 2, 3, 4, 5, 6, 7, 8].map(n => (
                        <option key={n} value={n}>{n} {n === 1 ? 'Guest' : 'Guests'}</option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Special Requests (Optional)</label>
                    <textarea
                      name="specialRequests"
                      value={bookingData.specialRequests}
                      onChange={handleInputChange}
                      placeholder="Any special requests or requirements..."
                      rows={4}
                    />
                  </div>

                  <div className="form-group">
                    <label>Discount Code (Optional)</label>
                    <input
                      type="text"
                      name="discountCode"
                      value={bookingData.discountCode}
                      onChange={handleInputChange}
                      placeholder="Enter discount code"
                    />
                  </div>

                  <button type="button" className="next-btn" onClick={handleNextStep}>
                    Continue to Payment
                  </button>
                </div>
              )}

              {step === 2 && (
                <div className="form-card">
                  <h2>Payment Details</h2>

                  <div className="form-group">
                    <label>Payment Method</label>
                    <div className="payment-methods">
                      <label className={`payment-method ${bookingData.paymentMethod === 'CARD' ? 'selected' : ''}`}>
                        <input
                          type="radio"
                          name="paymentMethod"
                          value="CARD"
                          checked={bookingData.paymentMethod === 'CARD'}
                          onChange={handleInputChange}
                        />
                        <FiCreditCard /> Credit/Debit Card
                      </label>
                      <label className={`payment-method ${bookingData.paymentMethod === 'CASH' ? 'selected' : ''}`}>
                        <input
                          type="radio"
                          name="paymentMethod"
                          value="CASH"
                          checked={bookingData.paymentMethod === 'CASH'}
                          onChange={handleInputChange}
                        />
                        Pay at Location
                      </label>
                    </div>
                  </div>

                  {bookingData.paymentMethod === 'CARD' && (
                    <div className="card-details">
                      <div className="form-group">
                        <label>Card Number</label>
                        <input type="text" placeholder="1234 5678 9012 3456" />
                      </div>
                      <div className="form-row">
                        <div className="form-group">
                          <label>Expiry Date</label>
                          <input type="text" placeholder="MM/YY" />
                        </div>
                        <div className="form-group">
                          <label>CVV</label>
                          <input type="text" placeholder="123" />
                        </div>
                      </div>
                      <div className="form-group">
                        <label>Cardholder Name</label>
                        <input type="text" placeholder="John Doe" />
                      </div>
                    </div>
                  )}

                  <div className="form-actions">
                    <button type="button" className="back-btn" onClick={() => setStep(1)}>
                      Back
                    </button>
                    <button type="submit" className="submit-btn" disabled={submitting}>
                      {submitting ? (
                        <>
                          <FiLoader className="spinner" /> Processing...
                        </>
                      ) : (
                        <>
                          <FiCheck /> Confirm Booking
                        </>
                      )}
                    </button>
                  </div>
                </div>
              )}
            </form>
          </div>

          {/* Booking Summary */}
          <div className="booking-summary-section">
            <div className="summary-card">
              <div className="service-preview">
                {service.images && service.images[0] ? (
                  <img src={service.images[0].imageUrl} alt={service.name} />
                ) : (
                  <div className="placeholder-image">No Image</div>
                )}
                <div className="service-info">
                  <h3>{service.name}</h3>
                  <p className="location"><FiMapPin /> {service.location}</p>
                  <p className="rating"><FiStar /> {service.rating || '4.5'}</p>
                </div>
              </div>

              <div className="price-breakdown">
                <h4>Price Details</h4>
                <div className="price-row">
                  <span>${service.basePrice} x {pricing.nights} nights</span>
                  <span>${pricing.baseAmount.toFixed(2)}</span>
                </div>
                {pricing.discountAmount > 0 && (
                  <div className="price-row discount">
                    <span>Discount</span>
                    <span>-${pricing.discountAmount.toFixed(2)}</span>
                  </div>
                )}
                <div className="price-row total">
                  <span>Total</span>
                  <span>${pricing.totalAmount.toFixed(2)}</span>
                </div>
              </div>

              <div className="booking-info">
                <div className="info-row">
                  <span>Guest</span>
                  <span>{user?.firstName} {user?.lastName}</span>
                </div>
                <div className="info-row">
                  <span>Email</span>
                  <span>{user?.email}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BookingPage;