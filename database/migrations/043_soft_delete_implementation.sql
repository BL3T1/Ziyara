-- Migration: 043_soft_delete_implementation.sql
-- Phase: 1 (Critical Security & Integrity)
-- Description: Implements soft delete pattern across all major tables to prevent accidental data loss
--              and ensure audit trails remain intact. Adds triggers to enforce logical deletion.

-- Start Transaction
BEGIN;

-- 1. Add soft delete columns to core tables
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(user_id);

ALTER TABLE organizations 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(user_id);

ALTER TABLE projects 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(user_id);

ALTER TABLE tasks 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(user_id);

ALTER TABLE comments 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(user_id);

ALTER TABLE attachments 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(user_id);

-- 2. Create indexes for performance on soft delete filtering
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orgs_deleted_at ON organizations(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_projects_deleted_at ON projects(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_comments_deleted_at ON comments(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_attachments_deleted_at ON attachments(deleted_at) WHERE deleted_at IS NOT NULL;

-- 3. Create a generic function to handle soft deletes
CREATE OR REPLACE FUNCTION soft_delete_record(
    p_table_name TEXT,
    p_id UUID,
    p_deleted_by UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_sql TEXT;
BEGIN
    -- Validate table name to prevent SQL injection (whitelist approach)
    IF p_table_name NOT IN ('users', 'organizations', 'projects', 'tasks', 'comments', 'attachments') THEN
        RAISE EXCEPTION 'Invalid table name for soft delete: %', p_table_name;
    END IF;

    v_sql := format(
        'UPDATE %I SET deleted_at = NOW(), deleted_by = $1 WHERE id = $2 AND deleted_at IS NULL',
        p_table_name
    );
    
    -- Note: We assume primary key is named based on table (e.g., user_id, organization_id)
    -- For simplicity in this migration, we map specific IDs. In production, use dynamic PK detection.
    IF p_table_name = 'users' THEN
        EXECUTE 'UPDATE users SET deleted_at = NOW(), deleted_by = $1 WHERE user_id = $2 AND deleted_at IS NULL' USING p_deleted_by, p_id;
    ELSIF p_table_name = 'organizations' THEN
        EXECUTE 'UPDATE organizations SET deleted_at = NOW(), deleted_by = $1 WHERE organization_id = $2 AND deleted_at IS NULL' USING p_deleted_by, p_id;
    ELSIF p_table_name = 'projects' THEN
        EXECUTE 'UPDATE projects SET deleted_at = NOW(), deleted_by = $1 WHERE project_id = $2 AND deleted_at IS NULL' USING p_deleted_by, p_id;
    ELSIF p_table_name = 'tasks' THEN
        EXECUTE 'UPDATE tasks SET deleted_at = NOW(), deleted_by = $1 WHERE task_id = $2 AND deleted_at IS NULL' USING p_deleted_by, p_id;
    ELSIF p_table_name = 'comments' THEN
        EXECUTE 'UPDATE comments SET deleted_at = NOW(), deleted_by = $1 WHERE comment_id = $2 AND deleted_at IS NULL' USING p_deleted_by, p_id;
    ELSIF p_table_name = 'attachments' THEN
        EXECUTE 'UPDATE attachments SET deleted_at = NOW(), deleted_by = $1 WHERE attachment_id = $2 AND deleted_at IS NULL' USING p_deleted_by, p_id;
    END IF;

    IF FOUND THEN
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Create Views for "Active" records only (Application should query these views)
CREATE OR REPLACE VIEW active_users AS
SELECT * FROM users WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_organizations AS
SELECT * FROM organizations WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_projects AS
SELECT * FROM projects WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_tasks AS
SELECT * FROM tasks WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_comments AS
SELECT * FROM comments WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_attachments AS
SELECT * FROM attachments WHERE deleted_at IS NULL;

-- 5. Add constraint to prevent updating deleted records (Optional strict mode)
-- This trigger function prevents modifications to logically deleted rows
CREATE OR REPLACE FUNCTION prevent_modification_of_deleted()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot modify a logically deleted record (ID: %)', 
            CASE 
                WHEN TG_TABLE_NAME = 'users' THEN OLD.user_id::TEXT
                WHEN TG_TABLE_NAME = 'organizations' THEN OLD.organization_id::TEXT
                WHEN TG_TABLE_NAME = 'projects' THEN OLD.project_id::TEXT
                WHEN TG_TABLE_NAME = 'tasks' THEN OLD.task_id::TEXT
                ELSE OLD.id::TEXT
            END;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to critical tables
DROP TRIGGER IF EXISTS trg_prevent_modify_deleted_users ON users;
CREATE TRIGGER trg_prevent_modify_deleted_users
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION prevent_modification_of_deleted();

DROP TRIGGER IF EXISTS trg_prevent_modify_deleted_orgs ON organizations;
CREATE TRIGGER trg_prevent_modify_deleted_orgs
    BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION prevent_modification_of_deleted();

DROP TRIGGER IF EXISTS trg_prevent_modify_deleted_projects ON projects;
CREATE TRIGGER trg_prevent_modify_deleted_projects
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION prevent_modification_of_deleted();

COMMIT;

-- Verification Queries
-- SELECT count(*) FROM active_users;
-- SELECT count(*) FROM users WHERE deleted_at IS NOT NULL;

-- ROLLBACK SCRIPT
/*
BEGIN;
DROP TRIGGER IF EXISTS trg_prevent_modify_deleted_projects ON projects;
DROP TRIGGER IF EXISTS trg_prevent_modify_deleted_orgs ON organizations;
DROP TRIGGER IF EXISTS trg_prevent_modify_deleted_users ON users;
DROP FUNCTION IF EXISTS prevent_modification_of_deleted();
DROP VIEW IF EXISTS active_attachments;
DROP VIEW IF EXISTS active_comments;
DROP VIEW IF EXISTS active_tasks;
DROP VIEW IF EXISTS active_projects;
DROP VIEW IF EXISTS active_organizations;
DROP VIEW IF EXISTS active_users;
DROP FUNCTION IF EXISTS soft_delete_record(TEXT, UUID, UUID);
DROP INDEX IF EXISTS idx_attachments_deleted_at;
DROP INDEX IF EXISTS idx_comments_deleted_at;
DROP INDEX IF EXISTS idx_tasks_deleted_at;
DROP INDEX IF EXISTS idx_projects_deleted_at;
DROP INDEX IF EXISTS idx_orgs_deleted_at;
DROP INDEX IF EXISTS idx_users_deleted_at;
ALTER TABLE attachments DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE comments DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE tasks DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE projects DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE organizations DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE users DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS deleted_at;
COMMIT;
*/
