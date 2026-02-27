import React from 'react';
import { useNavigate } from 'react-router-dom';
import SuperAdminDashboard from './SuperAdminDashboard';
import SalesDashboard from './SalesDashboard';
import FinanceDashboard from './FinanceDashboard';
import SupportDashboard from './SupportDashboard';
import ExecutiveDashboard from './ExecutiveDashboard';
import HRDashboard from './HRDashboard';

const DashboardPlaceholder = ({ roleTitle, color }) => {
    const navigate = useNavigate();
    return (
        <div style={{ padding: '4rem', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>
            <h1 style={{ color: color, fontSize: '3rem', marginBottom: '1rem' }}>{roleTitle} Dashboard</h1>
            <p style={{ fontSize: '1.25rem', color: '#64748b', marginBottom: '2rem' }}>
                Welcome to the {roleTitle} workspace. This dashboard is coming soon.
            </p>
            <button
                onClick={() => navigate('/role-selection')}
                style={{
                    padding: '0.75rem 1.5rem',
                    backgroundColor: '#1A237E',
                    color: 'white',
                    border: 'none',
                    borderRadius: '0.5rem',
                    cursor: 'pointer',
                    fontWeight: '600'
                }}
            >
                Back to Role Selection
            </button>
        </div>
    );
};

export {
    SuperAdminDashboard,
    SalesDashboard,
    FinanceDashboard,
    SupportDashboard,
    ExecutiveDashboard,
    HRDashboard
};
