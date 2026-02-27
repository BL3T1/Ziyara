import React from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import { StatCard, LineChartWidget } from '../../components/dashboard/widgets/Widgets';
import { LuDollarSign, LuTrendingUp, LuUsers, LuAward } from 'react-icons/lu';
import './DashboardPages.css';

const ExecutiveDashboard = () => {
    const stats = [
        { title: 'Total Revenue YTD', value: '$1.84M', trend: '+32.5%', icon: <LuDollarSign />, color: '#3b82f6' },
        { title: 'Growth Rate YoY', value: '48.2%', trend: '+12.3% from last year', icon: <LuTrendingUp />, color: '#10b981' },
        { title: 'Total Customers', value: '45,231', trend: '+18.5%', icon: <LuUsers />, color: '#8b5cf6' },
        { title: 'NPS Score', value: '72', trend: '+5 points', icon: <LuAward />, color: '#f59e0b' },
    ];

    const trendData = [
        { name: 'Mar', target: 80000, actual: 85000 },
        { name: 'Apr', target: 95000, actual: 92000 },
        { name: 'May', target: 110000, actual: 115000 },
        { name: 'Jun', target: 130000, actual: 142000 },
        { name: 'Jul', target: 150000, actual: 168000 },
        { name: 'Aug', target: 175000, actual: 185000 },
        { name: 'Sep', target: 200000, actual: 215000 },
        { name: 'Oct', target: 230000, actual: 245000 },
        { name: 'Nov', target: 260000, actual: 280000 },
        { name: 'Dec', target: 300000, actual: 320000 },
        { name: 'Jan', target: 330000, actual: 350000 },
        { name: 'Feb', target: 360000, actual: 385000 },
    ];

    const chartLines = [
        { key: 'target', color: '#cbd5e1' },
        { key: 'actual', color: '#3b82f6' },
    ];

    return (
        <DashboardLayout
            role="executive"
            title="Executive Overview"
            breadcrumbs={['Executive', 'Dashboard']}
        >
            <div className="dashboard-grid">
                {stats.map((stat, idx) => (
                    <StatCard key={idx} {...stat} />
                ))}
            </div>

            <div className="dashboard-card">
                <LineChartWidget
                    title="Revenue Trend vs Target (Last 12 Months)"
                    data={trendData}
                    lines={chartLines}
                />
            </div>
        </DashboardLayout>
    );
};

export default ExecutiveDashboard;
