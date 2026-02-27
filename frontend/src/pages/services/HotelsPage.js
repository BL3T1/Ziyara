import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FiMapPin, FiStar, FiFilter, FiX } from 'react-icons/fi';
import { serviceApi } from '../../services/api';
import './ServicesPage.css';

function HotelsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [hotels, setHotels] = useState([]);
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
    loadHotels();
  }, [filters]);

  const loadHotels = async () => {
    setLoading(true);
    try {
      const params = {
        type: 'HOTEL',
        ...filters
      };
      // Remove empty filters
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === null) {
          delete params[key];
        }
      });
      
      const response = await serviceApi.getServices(params);
      setHotels(response.data.data || []);
    } catch (error) {
      console.error('Failed to load hotels:', error);
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
      <div className="page-header">
        <div className="container">
          <h1>Hotels</h1>
          <p>Find the perfect accommodation for your stay</p>
        </div>
      </div>

      <div className="services-content">
        <div className="container">
          {/* Search and Filter Bar */}
          <div className="filter-bar">
            <div className="search-input">
              <input
                type="text"
                placeholder="Search hotels..."
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

          {/* Expanded Filters */}
          {showFilters && (
            <div className="filters-panel">
              <div className="filter-group">
                <label>Location</label>
                <input
                  type="text"
                  placeholder="City or area"
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

          {/* Results */}
          {loading ? (
            <div className="loading">Loading hotels...</div>
          ) : hotels.length > 0 ? (
            <>
              <p className="results-count">{hotels.length} hotels found</p>
              <div className="services-grid">
                {hotels.map((hotel) => (
                  <Link to={`/services/${hotel.id}`} key={hotel.id} className="service-card">
                    <div className="service-image">
                      {hotel.images && hotel.images[0] ? (
                        <img src={hotel.images[0].imageUrl} alt={hotel.name} />
                      ) : (
                        <div className="placeholder-image">ð¨</div>
                      )}
                    </div>
                    <div className="service-info">
                      <h3>{hotel.name}</h3>
                      <p className="service-location">
                        <FiMapPin /> {hotel.location}
                      </p>
                      <p className="service-description">{hotel.description?.substring(0, 100)}...</p>
                      <div className="service-rating">
                        <FiStar className="star-icon" /> {hotel.rating || '4.5'}
                      </div>
                      <div className="service-price">
                        From <strong>${hotel.basePrice}</strong>/night
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            </>
          ) : (
            <div className="no-results">
              <h3>No hotels found</h3>
              <p>Try adjusting your search or filters</p>
              <button onClick={clearFilters}>Clear Filters</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default HotelsPage;