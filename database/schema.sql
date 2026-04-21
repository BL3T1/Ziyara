-- ============================================================================
-- ZIYARAH PLATFORM - DATABASE SCHEMA
-- PostgreSQL 15+ DDL Script
-- Version: 1.0
-- Date: 2026-02-05
-- ============================================================================

-- Drop existing tables (in reverse dependency order)
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS ticket_comments CASCADE;
DROP TABLE IF EXISTS internal_tickets CASCADE;
DROP TABLE IF EXISTS complaint_comments CASCADE;
DROP TABLE IF EXISTS complaints CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS refunds CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS taxi_bookings CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS discount_codes CASCADE;
DROP TABLE IF EXISTS service_images CASCADE;
DROP TABLE IF EXISTS services CASCADE;
DROP TABLE IF EXISTS service_providers CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS groups CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS exchange_rates CASCADE;

-- ============================================================================
-- ENUMERATION TYPES
-- ============================================================================

-- User Role Enum
CREATE TYPE user_role_enum AS ENUM (
    'CUSTOMER',
    'SUPER_ADMIN',
    'SALES_MANAGER',
    'SALES_REPRESENTATIVE',
    'FINANCE_MANAGER',
    'ACCOUNTANT',
    'SUPPORT_MANAGER',
    'SUPPORT_AGENT',
    'CEO',
    'GENERAL_MANAGER',
    'HR_MANAGER',
    'PROVIDER_MANAGER',
    'PROVIDER_FINANCE',
    'PROVIDER_STAFF',
    'TAXI_OPERATOR'
);

-- User Status Enum
CREATE TYPE user_status_enum AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'FROZEN',
    'PENDING_VERIFICATION',
    'DELETED'
);

-- Employee Level Enum
CREATE TYPE employee_level_enum AS ENUM (
    'SUPER_ADMIN',
    'MANAGER',
    'EMPLOYEE',
    'EXECUTIVE'
);

-- Service Type Enum
CREATE TYPE service_type_enum AS ENUM (
    'HOTEL',
    'RESORT',
    'RESTAURANT',
    'TAXI',
    'TRIP'
);

-- Service Status Enum
CREATE TYPE service_status_enum AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED',
    'PENDING_APPROVAL'
);

-- Provider Status Enum
CREATE TYPE provider_status_enum AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED',
    'PENDING_APPROVAL'
);

-- Booking Status Enum
CREATE TYPE booking_status_enum AS ENUM (
    'PENDING',
    'CONFIRMED',
    'ACTIVE',
    'COMPLETED',
    'CANCELLED',
    'EXPIRED',
    'REFUNDING',
    'REFUNDED',
    'REFUND_FAILED',
    'MANUAL_REVIEW',
    'REVIEW_PENDING',
    'REVIEWED',
    'CLOSED'
);

-- Payment Method Enum
CREATE TYPE payment_method_enum AS ENUM (
    'CREDIT_CARD',
    'DEBIT_CARD',
    'WALLET',
    'BANK_TRANSFER',
    'CASH'
);

-- Payment Status Enum
CREATE TYPE payment_status_enum AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
);

-- Refund Status Enum
CREATE TYPE refund_status_enum AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);

-- Taxi Status Enum
CREATE TYPE taxi_status_enum AS ENUM (
    'PENDING',
    'CONFIRMED',
    'DRIVER_ASSIGNED',
    'EN_ROUTE',
    'ARRIVED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);

-- Vehicle Type Enum
CREATE TYPE vehicle_type_enum AS ENUM (
    'STANDARD',
    'PREMIUM',
    'LUXURY',
    'SUV',
    'VAN'
);

-- Complaint Priority Enum
CREATE TYPE complaint_priority_enum AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

-- Complaint Status Enum
CREATE TYPE complaint_status_enum AS ENUM (
    'SUBMITTED',
    'ACKNOWLEDGED',
    'ASSIGNED',
    'IN_PROGRESS',
    'PENDING_INFO',
    'ESCALATED',
    'RESOLVED',
    'REJECTED',
    'CLOSED',
    'REOPENED'
);

-- Discount Status Enum
CREATE TYPE discount_status_enum AS ENUM (
    'PENDING_APPROVAL',
    'ACTIVE',
    'USED',
    'EXPIRED',
    'DEACTIVATED',
    'REJECTED'
);

-- Review Status Enum
CREATE TYPE review_status_enum AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'HIDDEN'
);

-- Notification Type Enum
CREATE TYPE notification_type_enum AS ENUM (
    'BOOKING_CONFIRMATION',
    'BOOKING_CANCELLATION',
    'PAYMENT_RECEIVED',
    'REFUND_PROCESSED',
    'COMPLAINT_UPDATE',
    'DISCOUNT_CODE',
    'PROMOTIONAL',
    'SYSTEM',
    'REMINDER'
);

-- Notification Channel Enum
CREATE TYPE notification_channel_enum AS ENUM (
    'EMAIL',
    'SMS',
    'PUSH',
    'IN_APP'
);

-- Notification Status Enum
CREATE TYPE notification_status_enum AS ENUM (
    'PENDING',
    'SENT',
    'DELIVERED',
    'READ',
    'FAILED'
);

-- Internal Ticket Type Enum
CREATE TYPE ticket_type_enum AS ENUM (
    'BUG_REPORT',
    'FEATURE_REQUEST',
    'SYSTEM_ISSUE',
    'ACCESS_REQUEST',
    'DATA_REQUEST',
    'GENERAL_INQUIRY',
    'MAINTENANCE',
    'SECURITY_ISSUE'
);

-- Internal Ticket Priority Enum
CREATE TYPE ticket_priority_enum AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL',
    'URGENT'
);

-- Internal Ticket Status Enum
CREATE TYPE ticket_status_enum AS ENUM (
    'SUBMITTED',
    'ACKNOWLEDGED',
    'ASSIGNED',
    'IN_PROGRESS',
    'PENDING_INFO',
    'TESTING',
    'RESOLVED',
    'VERIFIED',
    'CLOSED',
    'REOPENED',
    'CANCELLED'
);

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Exchange Rates Table (for multi-currency support)
CREATE TABLE exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18, 6) NOT NULL,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_exchange_rates_currency_pair UNIQUE (from_currency, to_currency, effective_date)
);

-- Departments Table
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Groups Table (Organizational groups; platform uses Z1–Z7 in seed)
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Roles Table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(30) NOT NULL UNIQUE,
    description TEXT,
    level employee_level_enum NOT NULL DEFAULT 'EMPLOYEE',
    group_id UUID REFERENCES groups(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Permissions Table
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'ALL',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Role-Permission Mapping Table
CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_role_permissions UNIQUE (role_id, permission_id)
);

-- Users Table (Base table for all users)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role user_role_enum NOT NULL DEFAULT 'CUSTOMER',
    status user_status_enum NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_users_email_phone CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

-- User-Role Assignment Table (for RBAC)
CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    group_id UUID REFERENCES groups(id),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id)
);

-- Customers Table (Extended user info for customers)
CREATE TABLE customers (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    id_document_url VARCHAR(500),
    id_document_type VARCHAR(50),
    id_document_number VARCHAR(100),
    preferred_currency VARCHAR(3) DEFAULT 'USD',
    profile_image_url VARCHAR(500),
    date_of_birth DATE,
    nationality VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Employees Table (Extended user info for company staff)
CREATE TABLE employees (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    department_id UUID REFERENCES departments(id),
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    level employee_level_enum NOT NULL DEFAULT 'EMPLOYEE',
    hire_date DATE NOT NULL,
    termination_date DATE,
    job_title VARCHAR(100),
    salary DECIMAL(12, 2),
    manager_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Service Providers Table
CREATE TABLE service_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    website VARCHAR(255),
    description TEXT,
    logo_url VARCHAR(500),
    status provider_status_enum NOT NULL DEFAULT 'PENDING_APPROVAL',
    commission_rate DECIMAL(5, 2) DEFAULT 10.00,
    contract_start_date DATE,
    contract_end_date DATE,
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_routing_number VARCHAR(50),
    tax_id VARCHAR(50),
    created_by UUID REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Services Table (Hotels, Restaurants, Trips, etc.)
CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES service_providers(id),
    type service_type_enum NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    base_price DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status service_status_enum NOT NULL DEFAULT 'PENDING_APPROVAL',
    attributes JSONB,
    amenities JSONB,
    policies TEXT,
    star_rating INTEGER CHECK (star_rating >= 1 AND star_rating <= 5),
    total_rooms INTEGER,
    available_rooms INTEGER,
    max_guests INTEGER DEFAULT 1,
    check_in_time TIME DEFAULT '14:00:00',
    check_out_time TIME DEFAULT '11:00:00',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Service Images Table
CREATE TABLE service_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Discount Codes Table
CREATE TABLE discount_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    percentage DECIMAL(5, 2) NOT NULL CHECK (percentage > 0 AND percentage <= 100),
    max_discount DECIMAL(12, 2),
    min_spend DECIMAL(12, 2) DEFAULT 0,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    usage_limit INTEGER DEFAULT 1,
    used_count INTEGER DEFAULT 0,
    applicable_service_types service_type_enum[],
    applicable_service_ids UUID[],
    customer_id UUID REFERENCES users(id),
    created_by UUID NOT NULL REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    status discount_status_enum NOT NULL DEFAULT 'PENDING_APPROVAL',
    is_compensatory BOOLEAN DEFAULT FALSE,
    related_complaint_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bookings Table
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_reference VARCHAR(20) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES users(id),
    service_id UUID NOT NULL REFERENCES services(id),
    discount_code_id UUID REFERENCES discount_codes(id),
    check_in_date DATE NOT NULL,
    check_out_date DATE,
    guests INTEGER NOT NULL DEFAULT 1,
    rooms INTEGER DEFAULT 1,
    base_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) DEFAULT 0,
    tax_amount DECIMAL(12, 2) DEFAULT 0,
    commission_amount DECIMAL(12, 2) DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status booking_status_enum NOT NULL DEFAULT 'PENDING',
    special_requests TEXT,
    id_document_url VARCHAR(500),
    id_document_verified BOOLEAN DEFAULT FALSE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    cancelled_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_bookings_dates CHECK (check_out_date IS NULL OR check_out_date >= check_in_date),
    CONSTRAINT chk_bookings_guests CHECK (guests > 0)
);

-- Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    exchange_rate DECIMAL(18, 6) DEFAULT 1.0,
    original_amount DECIMAL(12, 2),
    original_currency VARCHAR(3),
    method payment_method_enum NOT NULL,
    status payment_status_enum NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(100) UNIQUE,
    gateway_response JSONB,
    gateway_transaction_id VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Refunds Table
CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    amount DECIMAL(12, 2) NOT NULL,
    penalty_amount DECIMAL(12, 2) DEFAULT 0,
    net_refund_amount DECIMAL(12, 2) NOT NULL,
    reason TEXT NOT NULL,
    status refund_status_enum NOT NULL DEFAULT 'PENDING',
    processed_by UUID REFERENCES users(id),
    processed_at TIMESTAMP WITH TIME ZONE,
    gateway_refund_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Taxi Bookings Table (matches TaxiBookingJpaEntity / bkg_taxi_bookings after prefix migration)
CREATE TABLE taxi_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    driver_id UUID REFERENCES users(id),
    vehicle_type VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    pickup_location TEXT,
    destination_location TEXT,
    pickup_latitude DOUBLE PRECISION,
    pickup_longitude DOUBLE PRECISION,
    destination_latitude DOUBLE PRECISION,
    destination_longitude DOUBLE PRECISION,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    estimated_distance NUMERIC(8, 2),
    actual_distance NUMERIC(8, 2),
    estimated_price NUMERIC(12, 2),
    actual_price NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'SEARCHING',
    license_plate VARCHAR(20),
    driver_name VARCHAR(255),
    vehicle_model VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Complaints Table
CREATE TABLE complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES users(id),
    booking_id UUID REFERENCES bookings(id),
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority complaint_priority_enum NOT NULL DEFAULT 'MEDIUM',
    status complaint_status_enum NOT NULL DEFAULT 'SUBMITTED',
    category VARCHAR(100),
    assigned_agent_id UUID REFERENCES users(id),
    assigned_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    escalated_at TIMESTAMP WITH TIME ZONE,
    escalated_to UUID REFERENCES users(id),
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Complaint Comments Table
CREATE TABLE complaint_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    comment TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Internal Tickets Table (for bug reports, feature requests, and internal issues)
CREATE TABLE internal_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    reporter_id UUID NOT NULL REFERENCES users(id),
    type ticket_type_enum NOT NULL DEFAULT 'GENERAL_INQUIRY',
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority ticket_priority_enum NOT NULL DEFAULT 'MEDIUM',
    status ticket_status_enum NOT NULL DEFAULT 'SUBMITTED',
    module VARCHAR(100),
    sub_module VARCHAR(100),
    environment VARCHAR(50),
    browser VARCHAR(100),
    operating_system VARCHAR(100),
    steps_to_reproduce TEXT,
    expected_behavior TEXT,
    actual_behavior TEXT,
    attachments JSONB,
    assigned_to_id UUID REFERENCES users(id),
    assigned_at TIMESTAMP WITH TIME ZONE,
    estimated_hours DECIMAL(5, 2),
    actual_hours DECIMAL(5, 2),
    due_date TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    resolution_summary TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by UUID REFERENCES users(id),
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by UUID REFERENCES users(id),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID REFERENCES users(id),
    cancellation_reason TEXT,
    related_ticket_id UUID REFERENCES internal_tickets(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Ticket Comments Table
CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES internal_tickets(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    comment TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT TRUE,
    is_resolution BOOLEAN DEFAULT FALSE,
    attachments JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Reviews Table
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    service_id UUID NOT NULL REFERENCES services(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    response TEXT,
    responded_at TIMESTAMP WITH TIME ZONE,
    responded_by UUID REFERENCES users(id),
    status review_status_enum NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_reviews_booking UNIQUE (booking_id)
);

-- Notifications Table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type notification_type_enum NOT NULL,
    channel notification_channel_enum NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    status notification_status_enum NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Sessions Table (for JWT token management)
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(255) UNIQUE,
    device_type VARCHAR(50),
    device_info TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Audit Logs Table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    changes JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Users indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Customers indexes
CREATE INDEX idx_customers_name ON customers(first_name, last_name);

-- Employees indexes
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_code ON employees(employee_code);
CREATE INDEX idx_employees_manager ON employees(manager_id);

-- Service Providers indexes
CREATE INDEX idx_providers_status ON service_providers(status);
CREATE INDEX idx_providers_created_at ON service_providers(created_at);

-- Services indexes
CREATE INDEX idx_services_provider ON services(provider_id);
CREATE INDEX idx_services_type ON services(type);
CREATE INDEX idx_services_status ON services(status);
CREATE INDEX idx_services_location ON services(city, country);
CREATE INDEX idx_services_price ON services(base_price);
CREATE INDEX idx_services_coordinates ON services(latitude, longitude);

-- Bookings indexes
CREATE INDEX idx_bookings_customer ON bookings(customer_id);
CREATE INDEX idx_bookings_service ON bookings(service_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_dates ON bookings(check_in_date, check_out_date);
CREATE INDEX idx_bookings_reference ON bookings(booking_reference);
CREATE INDEX idx_bookings_created_at ON bookings(created_at);

-- Payments indexes
CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_ref ON payments(transaction_ref);

-- Refunds indexes
CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_refunds_booking ON refunds(booking_id);
CREATE INDEX idx_refunds_status ON refunds(status);

-- Taxi Bookings indexes
CREATE INDEX idx_taxi_bookings_booking ON taxi_bookings(booking_id);
CREATE INDEX idx_taxi_bookings_status ON taxi_bookings(status);
CREATE INDEX idx_taxi_bookings_scheduled_at ON taxi_bookings(scheduled_at);

-- Complaints indexes
CREATE INDEX idx_complaints_customer ON complaints(customer_id);
CREATE INDEX idx_complaints_booking ON complaints(booking_id);
CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_priority ON complaints(priority);
CREATE INDEX idx_complaints_assigned_agent ON complaints(assigned_agent_id);
CREATE INDEX idx_complaints_ticket ON complaints(ticket_number);
CREATE INDEX idx_complaints_created_at ON complaints(created_at);

-- Internal Tickets indexes
CREATE INDEX idx_internal_tickets_reporter ON internal_tickets(reporter_id);
CREATE INDEX idx_internal_tickets_assigned_to ON internal_tickets(assigned_to_id);
CREATE INDEX idx_internal_tickets_status ON internal_tickets(status);
CREATE INDEX idx_internal_tickets_priority ON internal_tickets(priority);
CREATE INDEX idx_internal_tickets_type ON internal_tickets(type);
CREATE INDEX idx_internal_tickets_module ON internal_tickets(module);
CREATE INDEX idx_internal_tickets_ticket_number ON internal_tickets(ticket_number);
CREATE INDEX idx_internal_tickets_due_date ON internal_tickets(due_date);
CREATE INDEX idx_internal_tickets_created_at ON internal_tickets(created_at);

-- Ticket Comments indexes
CREATE INDEX idx_ticket_comments_ticket ON ticket_comments(ticket_id);
CREATE INDEX idx_ticket_comments_user ON ticket_comments(user_id);
CREATE INDEX idx_ticket_comments_created_at ON ticket_comments(created_at);

-- Reviews indexes
CREATE INDEX idx_reviews_service ON reviews(service_id);
CREATE INDEX idx_reviews_customer ON reviews(customer_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_status ON reviews(status);

-- Notifications indexes
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- Sessions indexes
CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_token ON sessions(token_hash);
CREATE INDEX idx_sessions_expires ON sessions(expires_at);

-- Audit Logs indexes
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Discount Codes indexes
CREATE INDEX idx_discount_codes_code ON discount_codes(code);
CREATE INDEX idx_discount_codes_status ON discount_codes(status);
CREATE INDEX idx_discount_codes_expiry ON discount_codes(expiry_date);
CREATE INDEX idx_discount_codes_customer ON discount_codes(customer_id);

-- Exchange Rates indexes
CREATE INDEX idx_exchange_rates_currencies ON exchange_rates(from_currency, to_currency);
CREATE INDEX idx_exchange_rates_date ON exchange_rates(effective_date);

-- ============================================================================
-- FUNCTIONS AND TRIGGERS
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_employees_updated_at BEFORE UPDATE ON employees
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_service_providers_updated_at BEFORE UPDATE ON service_providers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_services_updated_at BEFORE UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_discount_codes_updated_at BEFORE UPDATE ON discount_codes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bookings_updated_at BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refunds_updated_at BEFORE UPDATE ON refunds
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_taxi_bookings_updated_at BEFORE UPDATE ON taxi_bookings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_complaints_updated_at BEFORE UPDATE ON complaints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_internal_tickets_updated_at BEFORE UPDATE ON internal_tickets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ticket_comments_updated_at BEFORE UPDATE ON ticket_comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exchange_rates_updated_at BEFORE UPDATE ON exchange_rates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to generate booking reference
CREATE OR REPLACE FUNCTION generate_booking_reference()
RETURNS VARCHAR(20) AS $$
DECLARE
    ref VARCHAR(20);
BEGIN
    ref := 'ZYB' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || LPAD(FLOOR(RANDOM() * 100000)::TEXT, 5, '0');
    RETURN ref;
END;
$$ LANGUAGE plpgsql;

-- Function to generate complaint ticket number
CREATE OR REPLACE FUNCTION generate_ticket_number()
RETURNS VARCHAR(20) AS $$
DECLARE
    ticket VARCHAR(20);
BEGIN
    ticket := 'TKT' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0');
    RETURN ticket;
END;
$$ LANGUAGE plpgsql;

-- Function to generate internal ticket number
CREATE OR REPLACE FUNCTION generate_internal_ticket_number()
RETURNS VARCHAR(20) AS $$
DECLARE
    ticket VARCHAR(20);
BEGIN
    ticket := 'ITK' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0');
    RETURN ticket;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS ON TABLES AND COLUMNS
-- ============================================================================

COMMENT ON TABLE users IS 'Base table for all system users including customers and staff';
COMMENT ON TABLE customers IS 'Extended profile information for customer users';
COMMENT ON TABLE employees IS 'Extended profile information for company employees';
COMMENT ON TABLE departments IS 'Organizational departments within the company';
COMMENT ON TABLE groups IS 'Organizational groups (platform Z1-Z7 in seed; custom C{n} etc.) for role categorization';
COMMENT ON TABLE roles IS 'User roles with associated permissions';
COMMENT ON TABLE permissions IS 'Granular system permissions';
COMMENT ON TABLE service_providers IS 'Service providers (hotels, restaurants, etc.)';
COMMENT ON TABLE services IS 'Bookable services offered by providers';
COMMENT ON TABLE bookings IS 'Customer booking records';
COMMENT ON TABLE payments IS 'Payment transaction records';
COMMENT ON TABLE refunds IS 'Refund transaction records';
COMMENT ON TABLE taxi_bookings IS 'Taxi booking add-on records';
COMMENT ON TABLE complaints IS 'Customer complaint tickets';
COMMENT ON TABLE internal_tickets IS 'Internal tickets for bug reports, feature requests, and system issues';
COMMENT ON TABLE ticket_comments IS 'Comments and updates on internal tickets';
COMMENT ON TABLE reviews IS 'Customer reviews for services';
COMMENT ON TABLE notifications IS 'User notification records';
COMMENT ON TABLE sessions IS 'User session and token management';
COMMENT ON TABLE audit_logs IS 'System activity audit trail';
COMMENT ON TABLE discount_codes IS 'Promotional and compensatory discount codes';
COMMENT ON TABLE exchange_rates IS 'Currency exchange rates for multi-currency support';
