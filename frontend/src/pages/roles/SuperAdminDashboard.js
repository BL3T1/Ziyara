import React from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import { StatCard, HealthMetric, AuditLogTable } from '../../components/dashboard/widgets/Widgets';
import { LuUsers, LuActivity, LuServer, LuAlertTriangle } from 'react-icons/lu';
import './DashboardPages.css';

const SuperAdminDashboard = () => {
    const stats = [
        { title: 'Total Users', value: '45,231', trend: '+12.5%', icon: <LuUsers />, color: '#3b82f6' },
        { title: 'API Requests (24h)', value: '1.2M', trend: '+8.2%', icon: <LuActivity />, color: '#10b981' },
        { title: 'Active Servers', value: '12/15', trend: 'Stable', icon: <LuServer />, color: '#8b5cf6' },
        { title: 'System Alerts', value: '3', trend: '-2 from yesterday', icon: <LuAlertTriangle />, color: '#ef4444' },
    ];

    const healthData = [
        { label: 'CPU Usage', value: 68, color: '#3b82f6' },
        { label: 'RAM Usage', value: 82, color: '#10b981' },
        { label: 'Disk Usage', value: 45, color: '#8b5cf6' },
        { label: 'Network', value: 56, color: '#f59e0b' },
    ];

    const recentLogs = [
        { timestamp: '2026-02-17 14:32:15', user: 'admin@ziyarah.com', action: 'User Role Updated', resource: 'users/12345', status: 'Success' },
        { timestamp: '2026-02-17 14:30:10', user: 'system', action: 'Database Backup', resource: 'db/production', status: 'Success' },
        { timestamp: '2026-02-17 14:28:45', user: 'dev_ops', action: 'Server Restart', resource: 'srv-04', status: 'Success' },
        { timestamp: '2026-02-17 14:25:20', user: 'admin@ziyarah.com', action: 'API Key Created', resource: 'auth/keys', status: 'Success' },
    ];

    return (
        <DashboardLayout
            role="super-admin"
            title="System Overview"
            breadcrumbs={['Super Admin', 'Dashboard']}
        >
            <div className="dashboard-grid">
                {stats.map((stat, idx) => (
                    <StatCard key={idx} {...stat} />
                ))}
            </div>

            <div className="dashboard-section-grid">
                <div className="dashboard-card health-section">
                    <h3>Server Health Metrics</h3>
                    <div className="health-metrics-container">
                        {healthData.map((item, idx) => (
                            <HealthMetric key={idx} {...item} />
                        ))}
                    </div>
                </div>

                <div className="dashboard-card logs-section">
                    <AuditLogTable logs={recentLogs} />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default SuperAdminDashboard;
