import React from 'react';
import { useAuth } from '../../context/AuthContext';
import {
    LuBell,
    LuMail,
    LuSearch,
    LuChevronRight
} from 'react-icons/lu';
import './DashboardHeader.css';

const DashboardHeader = ({ title, breadcrumbs = [] }) => {
    const { user } = useAuth();

    return (
        <header className="dashboard-header">
            <div className="header-left">
                <div className="breadcrumbs">
                    {breadcrumbs.map((crumb, idx) => (
                        <React.Fragment key={idx}>
                            <span className="crumb">{crumb}</span>
                            {idx < breadcrumbs.length - 1 && <LuChevronRight className="breadcrumb-separator" />}
                        </React.Fragment>
                    ))}
                </div>
                <h1 className="page-title">{title}</h1>
            </div>

            <div className="header-right">
                <div className="search-box">
                    <LuSearch className="search-icon" />
                    <input type="text" placeholder="Search..." />
                </div>

                <div className="header-actions">
                    <button className="action-btn" title="Notifications">
                        <LuBell />
                        <span className="notification-badge"></span>
                    </button>
                    <button className="action-btn" title="Messages">
                        <LuMail />
                    </button>

                    <div className="user-profile">
                        <div className="user-info">
                            <span className="user-name">{user?.firstName || 'Admin'}</span>
                            <span className="user-role">{user?.role?.replace('_', ' ') || 'Super Admin'}</span>
                        </div>
                        <div className="user-avatar">
                            {user?.firstName?.charAt(0) || 'A'}
                        </div>
                    </div>
                </div>
            </div>
        </header>
    );
};

export default DashboardHeader;
