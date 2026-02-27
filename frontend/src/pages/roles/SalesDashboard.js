import React from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import { StatCard, PieChartWidget, BarChartWidget } from '../../components/dashboard/widgets/Widgets';
import { LuDollarSign, LuCalendar, LuBriefcase, LuStar } from 'react-icons/lu';
import './DashboardPages.css';

const SalesDashboard = () => {
    const stats = [
        { title: 'Total Revenue', value: '$458.2K', trend: '+18.5%', icon: <LuDollarSign />, color: '#3b82f6' },
        { title: 'Bookings (MTD)', value: '3,842', trend: '+12.3%', icon: <LuCalendar />, color: '#10b981' },
        { title: 'Active Providers', value: '284', trend: '+8 new', icon: <LuBriefcase />, color: '#8b5cf6' },
        { title: 'Avg Rating', value: '4.8', trend: 'Stable', icon: <LuStar />, color: '#f59e0b' },
    ];

    const revenueByService = [
        { name: 'Hotels', value: 45 },
        { name: 'Restaurants', value: 25 },
        { name: 'Taxis', value: 12 },
        { name: 'Trips', value: 18 },
    ];

    const serviceColors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];

    const bookingsByRegion = [
        { name: 'Saudi Arabia', value: 1500 },
        { name: 'UAE', value: 1100 },
        { name: 'Qatar', value: 650 },
        { name: 'Kuwait', value: 450 },
        { name: 'Bahrain', value: 380 },
    ];

    return (
        <DashboardLayout
            role="sales"
            title="Sales Dashboard"
            breadcrumbs={['Sales', 'Dashboard']}
        >
            <div className="dashboard-grid">
                {stats.map((stat, idx) => (
                    <StatCard key={idx} {...stat} />
                ))}
            </div>

            <div className="dashboard-section-grid">
                <div className="dashboard-card charts-section">
                    <PieChartWidget
                        title="Revenue by Service"
                        data={revenueByService}
                        colors={serviceColors}
                    />
                </div>

                <div className="dashboard-card charts-section">
                    <BarChartWidget
                        title="Bookings by Region"
                        data={bookingsByRegion}
                        color="#3b82f6"
                    />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default SalesDashboard;
