import React from 'react';
import { NavLink } from 'react-router-dom';
import {
    LuLayoutDashboard,
    LuPieChart,
    LuHotel,
    LuUtensils,
    LuCar,
    LuMap,
    LuUsers,
    LuBriefcase,
    LuCalendarDays,
    LuCreditCard,
    LuTicketPercent,
    LuLifeBuoy
} from 'react-icons/lu';
import './Sidebar.css';

const Sidebar = ({ role }) => {
    const menuItems = [
        {
            section: 'MAIN', items: [
                { name: 'Dashboard', icon: <LuLayoutDashboard />, path: `/${role}/dashboard` },
                { name: 'Analytics', icon: <LuPieChart />, path: `/${role}/analytics` },
            ]
        },
        {
            section: 'SERVICES', items: [
                { name: 'Hotels', icon: <LuHotel />, path: '/hotels' },
                { name: 'Restaurants', icon: <LuUtensils />, path: '/restaurants' },
                { name: 'Taxis', icon: <LuCar />, path: '/taxis' },
                { name: 'Trips', icon: <LuMap />, path: '/trips' },
            ]
        },
        {
            section: 'MANAGEMENT', items: [
                { name: 'Users', icon: <LuUsers />, path: '/users' },
                { name: 'Providers', icon: <LuBriefcase />, path: '/providers' },
                { name: 'Bookings', icon: <LuCalendarDays />, path: '/bookings' },
                { name: 'Payments', icon: <LuCreditCard />, path: '/payments' },
                { name: 'Discounts', icon: <LuTicketPercent />, path: '/discounts' },
            ]
        },
        {
            section: 'SUPPORT', items: [
                { name: 'Support', icon: <LuLifeBuoy />, path: '/support' },
            ]
        }
    ];

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <h2>Ziyarah</h2>
            </div>
            <nav className="sidebar-nav">
                {menuItems.map((section, idx) => (
                    <div key={idx} className="nav-section">
                        <span className="section-title">{section.section}</span>
                        <ul>
                            {section.items.map((item, itemIdx) => (
                                <li key={itemIdx}>
                                    <NavLink
                                        to={item.path}
                                        className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}
                                    >
                                        <span className="nav-icon">{item.icon}</span>
                                        <span className="nav-name">{item.name}</span>
                                    </NavLink>
                                </li>
                            ))}
                        </ul>
                    </div>
                ))}
            </nav>
        </aside>
    );
};

export default Sidebar;
