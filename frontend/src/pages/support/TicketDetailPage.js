import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FiArrowLeft, FiSend, FiUser, FiClock, FiCheckCircle, FiAlertCircle, FiXCircle, FiLoader } from 'react-icons/fi';
import { ticketApi } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import './TicketDetailPage.css';

function TicketDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [ticket, setTicket] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [newComment, setNewComment] = useState('');

  useEffect(() => {
    loadTicket();
  }, [id]);

  const loadTicket = async () => {
    try {
      const [ticketRes, commentsRes] = await Promise.all([
        ticketApi.getTicketById(id),
        ticketApi.getComments(id)
      ]);
      setTicket(ticketRes.data.data);
      setComments(commentsRes.data.data || []);
    } catch (error) {
      console.error('Failed to load ticket:', error);
      toast.error('Failed to load ticket details');
    } finally {
      setLoading(false);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSubmitting(true);
    try {
      const response = await ticketApi.addComment(id, { comment: newComment });
      setComments(prev => [...prev, response.data.data]);
      setNewComment('');
      toast.success('Comment added');
    } catch (error) {
      console.error('Failed to add comment:', error);
      toast.error('Failed to add comment');
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status?.toUpperCase()) {
      case 'OPEN': return <FiAlertCircle className="status-icon open" />;
      case 'IN_PROGRESS': return <FiClock className="status-icon in-progress" />;
      case 'RESOLVED': return <FiCheckCircle className="status-icon resolved" />;
      case 'CLOSED': return <FiXCircle className="status-icon closed" />;
      default: return null;
    }
  };

  const getStatusClass = (status) => {
    return status?.toLowerCase().replace('_', '-');
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return <div className="loading-page">Loading ticket details...</div>;
  }

  if (!ticket) {
    return (
      <div className="not-found-page">
        <h2>Ticket not found</h2>
        <Link to="/support">Back to Support</Link>
      </div>
    );
  }

  return (
    <div className="ticket-detail-page">
      <div className="container">
        {/* Back Link */}
        <Link to="/support" className="back-link">
          <FiArrowLeft /> Back to Support
        </Link>

        {/* Ticket Header */}
        <div className="ticket-header-card">
          <div className="ticket-header-top">
            <div className="ticket-info">
              <h1>{ticket.subject}</h1>
              <div className="ticket-meta">
                <span className="ticket-id">Ticket #{ticket.id}</span>
                <span className="ticket-type">{ticket.type}</span>
                <span className={`priority-badge ${ticket.priority?.toLowerCase()}`}>
                  {ticket.priority} Priority
                </span>
              </div>
            </div>
            <div className="ticket-status">
              {getStatusIcon(ticket.status)}
              <span className={`status-badge ${getStatusClass(ticket.status)}`}>
                {ticket.status?.replace('_', ' ')}
              </span>
            </div>
          </div>

          <div className="ticket-details">
            <div className="detail-item">
              <span className="label">Created</span>
              <span className="value">{formatDate(ticket.createdAt)}</span>
            </div>
            {ticket.assignedTo && (
              <div className="detail-item">
                <span className="label">Assigned To</span>
                <span className="value">{ticket.assignedTo.name}</span>
              </div>
            )}
            {ticket.resolvedAt && (
              <div className="detail-item">
                <span className="label">Resolved</span>
                <span className="value">{formatDate(ticket.resolvedAt)}</span>
              </div>
            )}
          </div>

          <div className="ticket-description">
            <h3>Description</h3>
            <p>{ticket.description}</p>
          </div>
        </div>

        {/* Comments Section */}
        <div className="comments-section">
          <h3>Comments ({comments.length})</h3>

          {comments.length > 0 ? (
            <div className="comments-list">
              {comments.map((comment) => (
                <div key={comment.id} className={`comment ${comment.isInternal ? 'internal' : ''}`}>
                  <div className="comment-header">
                    <div className="comment-author">
                      <FiUser className="author-icon" />
                      <span>{comment.user?.name || 'User'}</span>
                      {comment.isInternal && <span className="internal-badge">Internal</span>}
                    </div>
                    <span className="comment-date">{formatDate(comment.createdAt)}</span>
                  </div>
                  <p className="comment-text">{comment.comment}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="no-comments">
              <p>No comments yet. Be the first to comment!</p>
            </div>
          )}

          {/* Add Comment Form */}
          {ticket.status !== 'CLOSED' && (
            <form onSubmit={handleAddComment} className="comment-form">
              <textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                placeholder="Add a comment..."
                rows={4}
              />
              <button type="submit" disabled={submitting || !newComment.trim()}>
                {submitting ? (
                  <>
                    <FiLoader className="spinner" /> Sending...
                  </>
                ) : (
                  <>
                    <FiSend /> Send Comment
                  </>
                )}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default TicketDetailPage;