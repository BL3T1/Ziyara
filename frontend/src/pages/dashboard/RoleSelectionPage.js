import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
    LuShieldCheck,
    LuTrendingUp,
    LuDollarSign,
    LuHeadphones,
    LuBarChart3,
    LuUsers
} from 'react-icons/lu';
import './RoleSelectionPage.css';

const roles = [
    {
        id: 'super-admin',
        title: 'Super Admin',
        description: 'System monitoring, user management, and API oversight.',
        icon: <LuShieldCheck />,
        path: '/admin/dashboard',
        color: 'blue'
    },
    {
        id: 'sales-manager',
        title: 'Sales Manager',
        description: 'Revenue tracking, bookings, and provider management.',
        icon: <LuTrendingUp />,
        path: '/sales/dashboard',
        color: 'green'
    },
    {
        id: 'finance-manager',
        title: 'Finance Manager',
        description: 'Financial metrics, payments, and commission tracking.',
        icon: <LuDollarSign />,
        path: '/finance/dashboard',
        color: 'purple'
    },
    {
        id: 'support-manager',
        title: 'Support Manager',
        description: 'Customer support, tickets, and complaint resolution.',
        icon: <LuHeadphones />,
        path: '/support/dashboard',
        color: 'orange'
    },
    {
        id: 'executive',
        title: 'Executive',
        description: 'High-level overview and key business metrics.',
        icon: <LuBarChart3 />,
        path: '/executive/dashboard',
        color: 'pink'
    },
    {
        id: 'hr-manager',
        title: 'HR Manager',
        description: 'Employee management, attendance, and leave requests.',
        icon: <LuUsers />,
        path: '/hr/dashboard',
        color: 'teal'
    }
];

const RoleSelectionPage = () => {
    const navigate = useNavigate();

    return (
        <div className="role-selection-container">
            <header className="role-selection-header">
                <h1>Ziyarah Dashboard</h1>
                <p>Select your role to access the dashboard</p>
            </header>

            <div className="role-grid">
                {roles.map((role) => (
                    <div
                        key={role.id}
                        className={`role-card ${role.color}`}
                        onClick={() => navigate(role.path)}
                    >
                        <div className="role-icon-wrapper">
                            {role.icon}
                        </div>
                        <div className="role-content">
                            <h3>{role.title}</h3>
                            <p>{role.description}</p>
                        </div>
                    </div>
                ))}
            </div>

            <footer className="role-selection-footer">
                <p>Ziyarah Travel & Tourism Platform • Admin Dashboard v1.0</p>
            </footer>
        </div>
    );
};

export default RoleSelectionPage;
