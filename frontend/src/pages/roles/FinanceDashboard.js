import React from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import { StatCard, LineChartWidget } from '../../components/dashboard/widgets/Widgets';
import { LuDollarSign, LuClock, LuRotateCcw, LuBarChart3 } from 'react-icons/lu';
import './DashboardPages.css';

const FinanceDashboard = () => {
    const stats = [
        { title: 'Total Revenue', value: '$892.4K', trend: '+24.3%', icon: <LuDollarSign />, color: '#3b82f6' },
        { title: 'Pending Payments', value: '$45.2K', trend: '-12.5%', icon: <LuClock />, color: '#f59e0b' },
        { title: 'Refunds Processed', value: '$12.8K', trend: '+5.2%', icon: <LuRotateCcw />, color: '#ef4444' },
        { title: 'Profit Margin', value: '32.5%', trend: '+2.1%', icon: <LuBarChart3 />, color: '#10b981' },
    ];

    const monthlyRevenueData = [
        { name: 'Aug', hotels: 55000, restaurants: 30000, taxi: 15000, trips: 25000 },
        { name: 'Sep', hotels: 65000, restaurants: 32000, taxi: 18000, trips: 28000 },
        { name: 'Oct', hotels: 62000, restaurants: 35000, taxi: 17000, trips: 30000 },
        { name: 'Nov', hotels: 75000, restaurants: 38000, taxi: 20000, trips: 35000 },
        { name: 'Dec', hotels: 82000, restaurants: 42000, taxi: 22000, trips: 38000 },
        { name: 'Jan', hotels: 90000, restaurants: 45000, taxi: 25000, trips: 42000 },
    ];

    const chartLines = [
        { key: 'hotels', color: '#3b82f6' },
        { key: 'restaurants', color: '#10b981' },
        { key: 'taxi', color: '#f59e0b' },
        { key: 'trips', color: '#8b5cf6' },
    ];

    return (
        <DashboardLayout
            role="finance"
            title="Financial Overview"
            breadcrumbs={['Finance', 'Dashboard']}
        >
            <div className="dashboard-grid">
                {stats.map((stat, idx) => (
                    <StatCard key={idx} {...stat} />
                ))}
            </div>

            <div className="dashboard-card">
                <LineChartWidget
                    title="Monthly Revenue by Service"
                    data={monthlyRevenueData}
                    lines={chartLines}
                />
            </div>
        </DashboardLayout>
    );
};

export default FinanceDashboard;
