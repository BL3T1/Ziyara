import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FiPlus, FiSearch, FiFilter, FiMessageCircle, FiClock, FiCheckCircle, FiAlertCircle, FiXCircle } from 'react-icons/fi';
import { ticketApi } from '../../services/api';
import toast from 'react-hot-toast';
import './SupportPage.css';

function SupportPage() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    loadTickets();
  }, []);

  const loadTickets = async () => {
    try {
      const response = await ticketApi.getMyTickets();
      setTickets(response.data.data || []);
    } catch (error) {
      console.error('Failed to load tickets:', error);
      toast.error('Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status?.toUpperCase()) {
      case 'OPEN': return <FiAlertCircle className="status-icon open" />;
      case 'IN_PROGRESS': return <FiClock className="status-icon in-progress" />;
      case 'RESOLVED': return <FiCheckCircle className="status-icon resolved" />;
      case 'CLOSED': return <FiXCircle className="status-icon closed" />;
      default: return <FiMessageCircle className="status-icon" />;
    }
  };

  const getStatusClass = (status) => {
    return status?.toLowerCase().replace('_', '-');
  };

  const getPriorityClass = (priority) => {
    return priority?.toLowerCase();
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const filteredTickets = tickets.filter(ticket => {
    const matchesFilter = filter === 'all' || ticket.status?.toLowerCase().replace('_', '-') === filter;
    const matchesSearch = !searchQuery || 
      ticket.subject?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      ticket.description?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  return (
    <div className="support-page">
      <div className="page-header">
        <div className="container">
          <h1>Support Center</h1>
          <p>Get help with your issues and track your tickets</p>
        </div>
      </div>

      <div className="support-content">
        <div className="container">
          {/* Quick Actions */}
          <div className="quick-actions">
            <Link to="/support/tickets/new" className="new-ticket-btn">
              <FiPlus /> Create New Ticket
            </Link>
          </div>

          {/* Search and Filter */}
          <div className="filter-bar">
            <div className="search-input">
              <FiSearch className="search-icon" />
              <input
                type="text"
                placeholder="Search tickets..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <div className="filter-tabs">
              <button 
                className={`tab ${filter === 'all' ? 'active' : ''}`}
                onClick={() => setFilter('all')}
              >
                All
              </button>
              <button 
                className={`tab ${filter === 'open' ? 'active' : ''}`}
                onClick={() => setFilter('open')}
              >
                Open
              </button>
              <button 
                className={`tab ${filter === 'in-progress' ? 'active' : ''}`}
                onClick={() => setFilter('in-progress')}
              >
                In Progress
              </button>
              <button 
                className={`tab ${filter === 'resolved' ? 'active' : ''}`}
                onClick={() => setFilter('resolved')}
              >
                Resolved
              </button>
            </div>
          </div>

          {/* Tickets List */}
          {loading ? (
            <div className="loading">Loading tickets...</div>
          ) : filteredTickets.length > 0 ? (
            <div className="tickets-list">
              {filteredTickets.map((ticket) => (
                <Link to={`/support/tickets/${ticket.id}`} key={ticket.id} className="ticket-card">
                  <div className="ticket-icon">
                    {getStatusIcon(ticket.status)}
                  </div>
                  <div className="ticket-content">
                    <div className="ticket-header">
                      <h3>{ticket.subject}</h3>
                      <span className={`status-badge ${getStatusClass(ticket.status)}`}>
                        {ticket.status?.replace('_', ' ')}
                      </span>
                    </div>
                    <p className="ticket-description">{ticket.description?.substring(0, 150)}...</p>
                    <div className="ticket-meta">
                      <span className="ticket-type">{ticket.type}</span>
                      <span className={`priority-badge ${getPriorityClass(ticket.priority)}`}>
                        {ticket.priority}
                      </span>
                      <span className="ticket-date">{formatDate(ticket.createdAt)}</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="no-tickets">
              <FiMessageCircle className="no-tickets-icon" />
              <h3>No tickets found</h3>
              <p>
                {searchQuery || filter !== 'all' 
                  ? 'Try adjusting your search or filter'
                  : "You haven't created any support tickets yet"}
              </p>
              {!searchQuery && filter === 'all' && (
                <Link to="/support/tickets/new" className="create-btn">
                  Create Your First Ticket
                </Link>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default SupportPage;