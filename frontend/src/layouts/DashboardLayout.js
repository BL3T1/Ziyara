import React from 'react';
import Sidebar from '../components/dashboard/Sidebar';
import DashboardHeader from '../components/dashboard/DashboardHeader';
import './DashboardLayout.css';

const DashboardLayout = ({ children, title, breadcrumbs, role }) => {
    return (
        <div className="dashboard-layout">
            <Sidebar role={role} />
            <div className="dashboard-main">
                <DashboardHeader title={title} breadcrumbs={breadcrumbs} />
                <main className="dashboard-content">
                    {children}
                </main>
            </div>
        </div>
    );
};

export default DashboardLayout;
