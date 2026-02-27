import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FiMapPin, FiStar, FiFilter, FiX } from 'react-icons/fi';
import { serviceApi } from '../../services/api';
import './ServicesPage.css';

function RestaurantsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [restaurants, setRestaurants] = useState([]);
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
    loadRestaurants();
  }, [filters]);

  const loadRestaurants = async () => {
    setLoading(true);
    try {
      const params = {
        type: 'RESTAURANT',
        ...filters
      };
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === null) {
          delete params[key];
        }
      });
      
      const response = await serviceApi.getServices(params);
      setRestaurants(response.data.data || []);
    } catch (error) {
      console.error('Failed to load restaurants:', error);
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
      <div className="page-header restaurants-header">
        <div className="container">
          <h1>Restaurants</h1>
          <p>Discover amazing dining experiences</p>
        </div>
      </div>

      <div className="services-content">
        <div className="container">
          <div className="filter-bar">
            <div className="search-input">
              <input
                type="text"
                placeholder="Search restaurants..."
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

          {loading ? (
            <div className="loading">Loading restaurants...</div>
          ) : restaurants.length > 0 ? (
            <>
              <p className="results-count">{restaurants.length} restaurants found</p>
              <div className="services-grid">
                {restaurants.map((restaurant) => (
                  <Link to={`/services/${restaurant.id}`} key={restaurant.id} className="service-card">
                    <div className="service-image">
                      {restaurant.images && restaurant.images[0] ? (
                        <img src={restaurant.images[0].imageUrl} alt={restaurant.name} />
                      ) : (
                        <div className="placeholder-image">ð½ï¸</div>
                      )}
                    </div>
                    <div className="service-info">
                      <h3>{restaurant.name}</h3>
                      <p className="service-location">
                        <FiMapPin /> {restaurant.location}
                      </p>
                      <p className="service-description">{restaurant.description?.substring(0, 100)}...</p>
                      <div className="service-rating">
                        <FiStar className="star-icon" /> {restaurant.rating || '4.5'}
                      </div>
                      <div className="service-price">
                        Average <strong>${restaurant.basePrice}</strong>/person
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            </>
          ) : (
            <div className="no-results">
              <h3>No restaurants found</h3>
              <p>Try adjusting your search or filters</p>
              <button onClick={clearFilters}>Clear Filters</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default RestaurantsPage;