import React from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import { StatCard, PieChartWidget, RecentActivity } from '../../components/dashboard/widgets/Widgets';
import { LuUsers, LuUserCheck, LuCalendarX, LuLayers } from 'react-icons/lu';
import './DashboardPages.css';

const HRDashboard = () => {
    const stats = [
        { title: 'Total Employees', value: '156', trend: '+8 this month', icon: <LuUsers />, color: '#3b82f6' },
        { title: 'Attendance Today', value: '142/156', trend: '91% rate', icon: <LuUserCheck />, color: '#10b981' },
        { title: 'Leave Requests', value: '12', trend: '8 pending', icon: <LuCalendarX />, color: '#ef4444' },
        { title: 'Departments', value: '8', trend: 'Stable', icon: <LuLayers />, color: '#8b5cf6' },
    ];

    const distributionData = [
        { name: 'Engineering', value: 45 },
        { name: 'Customer Support', value: 32 },
        { name: 'Sales & Marketing', value: 28 },
        { name: 'Finance', value: 18 },
        { name: 'Operations', value: 18 },
        { name: 'HR & Admin', value: 15 },
    ];

    const distributionColors = ['#2563eb', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6'];

    const recentActivity = [
        { time: '14:32', user: 'Sarah Ahmed', action: 'Checked In', target: 'Engineering' },
        { time: '14:15', user: 'Mohammed Hassan', action: 'Checked Out', target: 'Sales' },
        { time: '13:45', user: 'Aisha Omar', action: 'Leave Approved', target: 'HR' },
        { time: '13:20', user: 'Khalid Ibrahim', action: 'New Hire Onboarding', target: 'Customer Support' },
        { time: '12:50', user: 'Nora Ahmed', action: 'Training Completed', target: 'Operations' },
    ];

    return (
        <DashboardLayout
            role="hr"
            title="HR Dashboard"
            breadcrumbs={['HR', 'Dashboard']}
        >
            <div className="dashboard-grid">
                {stats.map((stat, idx) => (
                    <StatCard key={idx} {...stat} />
                ))}
            </div>

            <div className="dashboard-section-grid">
                <div className="dashboard-card charts-section">
                    <PieChartWidget
                        title="Employee Distribution by Department"
                        data={distributionData}
                        colors={distributionColors}
                    />
                </div>

                <div className="dashboard-card activity-section">
                    <RecentActivity activities={recentActivity} />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default HRDashboard;
