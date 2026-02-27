import React from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import { StatCard, HorizontalBarChart, LiveChatQueue } from '../../components/dashboard/widgets/Widgets';
import { LuTicket, LuClock, LuCheckCircle, LuTimer } from 'react-icons/lu';
import './DashboardPages.css';

const SupportDashboard = () => {
    const stats = [
        { title: 'Open Tickets', value: '145', trend: '-12 from yesterday', icon: <LuTicket />, color: '#3b82f6' },
        { title: 'Pending Response', value: '68', trend: '+8.2%', icon: <LuClock />, color: '#f59e0b' },
        { title: 'Resolved Today', value: '92', trend: '+18.5%', icon: <LuCheckCircle />, color: '#10b981' },
        { title: 'Avg Response Time', value: '2.4h', trend: '-0.3h', icon: <LuTimer />, color: '#8b5cf6' },
    ];

    const complaintsByCategory = [
        { name: 'Booking Issues', value: 45 },
        { name: 'Payment Problems', value: 32 },
        { name: 'Service Quality', value: 28 },
        { name: 'Cancellation', value: 20 },
        { name: 'Technical', value: 15 },
    ];

    const liveChats = [
        { name: 'Mohammed Hassan', subject: 'Booking inquiry', waitTime: '2 min' },
        { name: 'Aisha Ahmed', subject: 'Payment issue', waitTime: '5 min' },
        { name: 'Khalid Omar', subject: 'General question', waitTime: '8 min' },
        { name: 'Nora Ibrahim', subject: 'Cancellation request', waitTime: '12 min' },
    ];

    return (
        <DashboardLayout
            role="support"
            title="Support Workspace"
            breadcrumbs={['Support', 'Dashboard']}
        >
            <div className="dashboard-grid">
                {stats.map((stat, idx) => (
                    <StatCard key={idx} {...stat} />
                ))}
            </div>

            <div className="dashboard-section-grid">
                <div className="dashboard-card charts-section">
                    <HorizontalBarChart
                        title="Complaints by Category"
                        data={complaintsByCategory}
                        color="#3b82f6"
                    />
                </div>

                <div className="dashboard-card chat-section">
                    <LiveChatQueue chats={liveChats} />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default SupportDashboard;
