import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FiArrowLeft, FiLoader, FiAlertCircle } from 'react-icons/fi';
import { ticketApi } from '../../services/api';
import toast from 'react-hot-toast';
import './CreateTicketPage.css';

function CreateTicketPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    type: 'BUG_REPORT',
    subject: '',
    description: '',
    priority: 'MEDIUM'
  });
  const [errors, setErrors] = useState({});

  const ticketTypes = [
    { value: 'BUG_REPORT', label: 'Bug Report' },
    { value: 'FEATURE_REQUEST', label: 'Feature Request' },
    { value: 'SUPPORT', label: 'Support Request' },
    { value: 'FEEDBACK', label: 'Feedback' },
    { value: 'OTHER', label: 'Other' }
  ];

  const priorities = [
    { value: 'LOW', label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH', label: 'High' },
    { value: 'CRITICAL', label: 'Critical' }
  ];

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.subject.trim()) {
      newErrors.subject = 'Subject is required';
    } else if (formData.subject.length < 10) {
      newErrors.subject = 'Subject must be at least 10 characters';
    }

    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    } else if (formData.description.length < 30) {
      newErrors.description = 'Description must be at least 30 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setLoading(true);
    try {
      const response = await ticketApi.createTicket(formData);
      toast.success('Ticket created successfully');
      navigate(`/support/tickets/${response.data.data.id}`);
    } catch (error) {
      console.error('Failed to create ticket:', error);
      toast.error(error.response?.data?.message || 'Failed to create ticket');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-ticket-page">
      <div className="container">
        <Link to="/support" className="back-link">
          <FiArrowLeft /> Back to Support
        </Link>

        <div className="form-card">
          <h1>Create New Ticket</h1>
          <p className="subtitle">Submit a support request and we'll get back to you as soon as possible.</p>

          <form onSubmit={handleSubmit} className="ticket-form">
            {/* Ticket Type */}
            <div className="form-group">
              <label htmlFor="type">Ticket Type</label>
              <select
                id="type"
                name="type"
                value={formData.type}
                onChange={handleChange}
              >
                {ticketTypes.map(type => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>

            {/* Subject */}
            <div className="form-group">
              <label htmlFor="subject">Subject</label>
              <input
                type="text"
                id="subject"
                name="subject"
                value={formData.subject}
                onChange={handleChange}
                placeholder="Brief summary of your issue"
                className={errors.subject ? 'error' : ''}
              />
              {errors.subject && (
                <span className="error-message">{errors.subject}</span>
              )}
            </div>

            {/* Priority */}
            <div className="form-group">
              <label htmlFor="priority">Priority</label>
              <select
                id="priority"
                name="priority"
                value={formData.priority}
                onChange={handleChange}
              >
                {priorities.map(priority => (
                  <option key={priority.value} value={priority.value}>{priority.label}</option>
                ))}
              </select>
              <span className="helper-text">Select the urgency of your request</span>
            </div>

            {/* Description */}
            <div className="form-group">
              <label htmlFor="description">Description</label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Please describe your issue in detail. Include any relevant information that might help us resolve your request."
                rows={8}
                className={errors.description ? 'error' : ''}
              />
              {errors.description && (
                <span className="error-message">{errors.description}</span>
              )}
              <span className="char-count">{formData.description.length} characters</span>
            </div>

            {/* Guidelines */}
            <div className="guidelines">
              <h4><FiAlertCircle /> Tips for a better response</h4>
              <ul>
                <li>Be specific about the issue you're experiencing</li>
                <li>Include any error messages you've received</li>
                <li>Mention the steps to reproduce the problem</li>
                <li>Attach screenshots if applicable (coming soon)</li>
              </ul>
            </div>

            {/* Submit */}
            <div className="form-actions">
              <button type="button" className="cancel-btn" onClick={() => navigate('/support')}>
                Cancel
              </button>
              <button type="submit" className="submit-btn" disabled={loading}>
                {loading ? (
                  <>
                    <FiLoader className="spinner" /> Creating...
                  </>
                ) : (
                  'Create Ticket'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default CreateTicketPage;