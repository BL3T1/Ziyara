import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FiMapPin, FiStar, FiFilter, FiX, FiClock, FiUsers } from 'react-icons/fi';
import { serviceApi } from '../../services/api';
import './ServicesPage.css';

function TripsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    search: searchParams.get('search') || '',
    minPrice: '',
    maxPrice: '',
    location: '',
    rating: '',
    sortBy: 'name',
    sortOrder: 'asc'
  });
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    loadTrips();
  }, [filters]);

  const loadTrips = async () => {
    setLoading(true);
    try {
      const params = {
        type: 'TRIP',
        ...filters
      };
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === null) {
          delete params[key];
        }
      });
      
      const response = await serviceApi.getServices(params);
      setTrips(response.data.data || []);
    } catch (error) {
      console.error('Failed to load trips:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const clearFilters = () => {
    setFilters({
      search: '',
      minPrice: '',
      maxPrice: '',
      location: '',
      rating: '',
      sortBy: 'name',
      sortOrder: 'asc'
    });
    setSearchParams({});
  };

  const hasActiveFilters = Object.values(filters).some(v => v !== '' && v !== 'name' && v !== 'asc');

  return (
    <div className="services-page">
      <div className="page-header trips-header">
        <div className="container">
          <h1>Trips</h1>
          <p>Explore exciting destinations and adventures</p>
        </div>
      </div>

      <div className="services-content">
        <div className="container">
          <div className="filter-bar">
            <div className="search-input">
              <input
                type="text"
                placeholder="Search trips..."
                value={filters.search}
                onChange={(e) => handleFilterChange('search', e.target.value)}
              />
            </div>
            <button 
              className={`filter-toggle ${showFilters ? 'active' : ''}`}
              onClick={() => setShowFilters(!showFilters)}
            >
              <FiFilter /> Filters
              {hasActiveFilters && <span className="filter-badge"></span>}
            </button>
          </div>

          {showFilters && (
            <div className="filters-panel">
              <div className="filter-group">
                <label>Location</label>
                <input
                  type="text"
                  placeholder="Destination"
                  value={filters.location}
                  onChange={(e) => handleFilterChange('location', e.target.value)}
                />
              </div>
              <div className="filter-group">
                <label>Min Price</label>
                <input
                  type="number"
                  placeholder="$ Min"
                  value={filters.minPrice}
                  onChange={(e) => handleFilterChange('minPrice', e.target.value)}
                />
              </div>
              <div className="filter-group">
                <label>Max Price</label>
                <input
                  type="number"
                  placeholder="$ Max"
                  value={filters.maxPrice}
                  onChange={(e) => handleFilterChange('maxPrice', e.target.value)}
                />
              </div>
              <div className="filter-group">
                <label>Rating</label>
                <select
                  value={filters.rating}
                  onChange={(e) => handleFilterChange('rating', e.target.value)}
                >
                  <option value="">Any</option>
                  <option value="5">5 Stars</option>
                  <option value="4">4+ Stars</option>
                  <option value="3">3+ Stars</option>
                </select>
              </div>
              <div className="filter-group">
                <label>Sort By</label>
                <select
                  value={filters.sortBy}
                  onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                >
                  <option value="name">Name</option>
                  <option value="basePrice">Price</option>
                  <option value="rating">Rating</option>
                </select>
              </div>
              {hasActiveFilters && (
                <button className="clear-filters" onClick={clearFilters}>
                  <FiX /> Clear Filters
                </button>
              )}
            </div>
          )}

          {loading ? (
            <div className="loading">Loading trips...</div>
          ) : trips.length > 0 ? (
            <>
              <p className="results-count">{trips.length} trips found</p>
              <div className="services-grid trips-grid">
                {trips.map((trip) => (
                  <Link to={`/services/${trip.id}`} key={trip.id} className="service-card trip-card">
                    <div className="service-image">
                      {trip.images && trip.images[0] ? (
                        <img src={trip.images[0].imageUrl} alt={trip.name} />
                      ) : (
                        <div className="placeholder-image">ð</div>
                      )}
                    </div>
                    <div className="service-info">
                      <h3>{trip.name}</h3>
                      <p className="service-location">
                        <FiMapPin /> {trip.location}
                      </p>
                      <p className="service-description">{trip.description?.substring(0, 100)}...</p>
                      <div className="trip-meta">
                        {trip.duration && (
                          <span><FiClock /> {trip.duration}</span>
                        )}
                        {trip.maxGroupSize && (
                          <span><FiUsers /> Max {trip.maxGroupSize}</span>
                        )}
                      </div>
                      <div className="service-rating">
                        <FiStar className="star-icon" /> {trip.rating || '4.5'}
                      </div>
                      <div className="service-price">
                        From <strong>${trip.basePrice}</strong>/person
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            </>
          ) : (
            <div className="no-results">
              <h3>No trips found</h3>
              <p>Try adjusting your search or filters</p>
              <button onClick={clearFilters}>Clear Filters</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default TripsPage;