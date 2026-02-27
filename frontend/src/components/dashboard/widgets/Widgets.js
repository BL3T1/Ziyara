import React from 'react';
import {
    LuTrendingUp,
    LuTrendingDown
} from 'react-icons/lu';
import {
    PieChart as RePieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip,
    Legend,
    BarChart as ReBarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    LineChart as ReLineChart,
    Line
} from 'recharts';
import './Widgets.css';

export const StatCard = ({ title, value, trend, icon, color }) => {
    const isPositive = trend.startsWith('+');

    return (
        <div className="stat-card">
            <div className="stat-header">
                <div className="stat-icon" style={{ backgroundColor: `${color}15`, color: color }}>
                    {icon}
                </div>
                <div className={`stat-trend ${isPositive ? 'positive' : 'negative'}`}>
                    {isPositive ? <LuTrendingUp /> : <LuTrendingDown />}
                    <span>{trend}</span>
                </div>
            </div>
            <div className="stat-content">
                <span className="stat-title">{title}</span>
                <h3 className="stat-value">{value}</h3>
            </div>
        </div>
    );
};

export const HealthMetric = ({ label, value, color }) => {
    return (
        <div className="health-metric">
            <div className="metric-info">
                <span className="metric-label">{label}</span>
                <span className="metric-value">{value}%</span>
            </div>
            <div className="progress-bar-bg">
                <div
                    className="progress-bar-fill"
                    style={{ width: `${value}%`, backgroundColor: color }}
                ></div>
            </div>
        </div>
    );
};

export const AuditLogTable = ({ logs }) => {
    return (
        <div className="audit-log-container">
            <div className="table-header">
                <h3>Recent Audit Logs</h3>
                <div className="table-actions">
                    <button className="btn-secondary">Filter</button>
                    <button className="btn-secondary">Export</button>
                </div>
            </div>
            <table className="audit-table">
                <thead>
                    <tr>
                        <th>TIMESTAMP</th>
                        <th>USER</th>
                        <th>ACTION</th>
                        <th>RESOURCE</th>
                        <th>STATUS</th>
                    </tr>
                </thead>
                <tbody>
                    {logs.map((log, idx) => (
                        <tr key={idx}>
                            <td>{log.timestamp}</td>
                            <td>{log.user}</td>
                            <td>{log.action}</td>
                            <td>{log.resource}</td>
                            <td>
                                <span className={`status-badge ${log.status.toLowerCase()}`}>
                                    {log.status}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export const PieChartWidget = ({ title, data, colors }) => {
    return (
        <div className="chart-widget">
            <h3>{title}</h3>
            <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer>
                    <RePieChart>
                        <Pie
                            data={data}
                            cx="50%"
                            cy="50%"
                            innerRadius={60}
                            outerRadius={80}
                            paddingAngle={5}
                            dataKey="value"
                        >
                            {data.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
                            ))}
                        </Pie>
                        <Tooltip
                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                        />
                        <Legend verticalAlign="bottom" height={36} />
                    </RePieChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export const BarChartWidget = ({ title, data, color }) => {
    return (
        <div className="chart-widget">
            <h3>{title}</h3>
            <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer>
                    <ReBarChart data={data}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                        <XAxis
                            dataKey="name"
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#64748b', fontSize: 12 }}
                            dy={10}
                        />
                        <YAxis
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#64748b', fontSize: 12 }}
                        />
                        <Tooltip
                            cursor={{ fill: '#f8fafc' }}
                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                        />
                        <Bar dataKey="value" fill={color} radius={[4, 4, 0, 0]} barSize={40} />
                    </ReBarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export const LineChartWidget = ({ title, data, lines }) => {
    return (
        <div className="chart-widget">
            <h3>{title}</h3>
            <div style={{ width: '100%', height: 350 }}>
                <ResponsiveContainer>
                    <ReLineChart data={data}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                        <XAxis
                            dataKey="name"
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#64748b', fontSize: 12 }}
                            dy={10}
                        />
                        <YAxis
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#64748b', fontSize: 12 }}
                        />
                        <Tooltip
                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                        />
                        <Legend verticalAlign="top" align="right" iconType="circle" />
                        {lines.map((line, idx) => (
                            <Line
                                key={idx}
                                type="monotone"
                                dataKey={line.key}
                                stroke={line.color}
                                strokeWidth={3}
                                dot={{ r: 4, fill: line.color, strokeWidth: 2, stroke: '#fff' }}
                                activeDot={{ r: 6 }}
                            />
                        ))}
                    </ReLineChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};
export const HorizontalBarChart = ({ title, data, color }) => {
    return (
        <div className="chart-widget">
            <h3>{title}</h3>
            <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer>
                    <ReBarChart layout="vertical" data={data}>
                        <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f1f5f9" />
                        <XAxis type="number" hide />
                        <YAxis
                            dataKey="name"
                            type="category"
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#64748b', fontSize: 13 }}
                            width={140}
                        />
                        <Tooltip
                            cursor={{ fill: '#f8fafc' }}
                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                        />
                        <Bar dataKey="value" fill={color} radius={[0, 4, 4, 0]} barSize={24} />
                    </ReBarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export const LiveChatQueue = ({ chats }) => {
    return (
        <div className="chat-queue-card">
            <div className="card-header">
                <h3>Live Chat Queue</h3>
            </div>
            <div className="chat-list">
                {chats.map((chat, idx) => (
                    <div key={idx} className="chat-item">
                        <div className="chat-info">
                            <span className="chat-user">{chat.name}</span>
                            <span className="chat-subject">{chat.subject}</span>
                        </div>
                        <span className="chat-wait-time">{chat.waitTime}</span>
                    </div>
                ))}
            </div>
            <button className="btn-primary-full">View All ({chats.length})</button>
        </div>
    );
};

export const RecentActivity = ({ activities }) => {
    return (
        <div className="activity-card">
            <div className="card-header">
                <h3>Recent Activity</h3>
            </div>
            <div className="activity-list">
                {activities.map((activity, idx) => (
                    <div key={idx} className="activity-item">
                        <div className="activity-time">{activity.time}</div>
                        <div className="activity-marker"></div>
                        <div className="activity-content">
                            <span className="activity-user">{activity.user}</span>
                            <span className="activity-text">{activity.action}</span>
                            {activity.target && <span className="activity-target">• {activity.target}</span>}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};
