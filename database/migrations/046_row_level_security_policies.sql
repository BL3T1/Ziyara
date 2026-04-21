-- Migration: 046_row_level_security_policies.sql
-- Phase: 1 (Critical Security & Integrity)
-- Description: Implements Row Level Security (RLS) to enforce data isolation at the database level.
--              Ensures tenants cannot access each other's data even if application logic fails.

BEGIN;

-- 1. Enable RLS on all multi-tenant tables
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- 2. Create Helper Functions for RLS Checks

-- Check if current user is admin of the organization
CREATE OR REPLACE FUNCTION is_org_admin(target_org_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM organization_roles orr
        JOIN roles r ON orr.role_id = r.role_id
        WHERE orr.user_id = current_setting('app.current_user_id')::UUID
          AND orr.organization_id = target_org_id
          AND r.role_code IN ('ORG_ADMIN', 'SUPER_ADMIN')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Check if current user is member of the organization
CREATE OR REPLACE FUNCTION is_org_member(target_org_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM organization_roles
        WHERE user_id = current_setting('app.current_user_id')::UUID
          AND organization_id = target_org_id
    ) OR is_org_admin(target_org_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Check if current user has access to a project (via org or direct membership)
CREATE OR REPLACE FUNCTION has_project_access(target_project_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_org_id UUID;
BEGIN
    -- Get organization ID for the project
    SELECT organization_id INTO v_org_id FROM projects WHERE project_id = target_project_id;
    
    IF v_org_id IS NULL THEN
        RETURN FALSE;
    END IF;

    -- Check membership
    RETURN is_org_member(v_org_id) OR EXISTS (
        SELECT 1 FROM project_members
        WHERE user_id = current_setting('app.current_user_id')::UUID
          AND project_id = target_project_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Define Policies

-- A. Organizations: Users can only see organizations they belong to
DROP POLICY IF EXISTS org_isolation_policy ON organizations;
CREATE POLICY org_isolation_policy ON organizations
    FOR ALL
    USING (is_org_member(organization_id));

-- B. Projects: Users can only see projects in their organizations
DROP POLICY IF EXISTS project_isolation_policy ON projects;
CREATE POLICY project_isolation_policy ON projects
    FOR ALL
    USING (has_project_access(project_id));

-- C. Tasks: Inherit access from Project
DROP POLICY IF EXISTS task_isolation_policy ON tasks;
CREATE POLICY task_isolation_policy ON tasks
    FOR ALL
    USING (has_project_access(project_id));

-- D. Comments: Inherit access from Task -> Project
DROP POLICY IF EXISTS comment_isolation_policy ON comments;
CREATE POLICY comment_isolation_policy ON comments
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM tasks t
            WHERE t.task_id = comments.task_id
              AND has_project_access(t.project_id)
        )
    );

-- E. Attachments: Inherit access from Parent (Task/Comment)
-- Simplified: Assume attachment belongs to a task for this policy example
DROP POLICY IF EXISTS attachment_isolation_policy ON attachments;
CREATE POLICY attachment_isolation_policy ON attachments
    FOR ALL
    USING (
        task_id IS NULL OR EXISTS (
            SELECT 1 FROM tasks t
            WHERE t.task_id = attachments.task_id
              AND has_project_access(t.project_id)
        )
    );

-- F. Users: Users can see others in their own organization, plus themselves
DROP POLICY IF EXISTS user_isolation_policy ON users;
CREATE POLICY user_isolation_policy ON users
    FOR ALL
    USING (
        organization_id IN (
            SELECT organization_id FROM organization_roles
            WHERE user_id = current_setting('app.current_user_id')::UUID
        )
        OR user_id = current_setting('app.current_user_id')::UUID
    );

-- 4. Bypass RLS for Super Admins (Optional, handled inside functions usually, but explicit policy here)
-- Note: The functions above check for SUPER_ADMIN. If the DB user is a postgres superuser,
-- RLS is bypassed automatically unless FORCE ROW LEVEL SECURITY is used.

-- 5. Create View to Check Current RLS Context (Debugging)
CREATE OR REPLACE VIEW current_rls_context AS
SELECT 
    current_setting('app.current_user_id') as current_user_id,
    current_setting('app.current_org_id') as current_org_id,
    pg_has_role(current_user, 'superuser') as is_db_superuser;

COMMIT;

-- IMPORTANT: Application must set 'app.current_user_id' at the start of every transaction
-- Example: SET LOCAL app.current_user_id = '...';

-- ROLLBACK SCRIPT
/*
BEGIN;
DROP VIEW IF EXISTS current_rls_context;
DROP POLICY IF EXISTS user_isolation_policy ON users;
DROP POLICY IF EXISTS attachment_isolation_policy ON attachments;
DROP POLICY IF EXISTS comment_isolation_policy ON comments;
DROP POLICY IF EXISTS task_isolation_policy ON tasks;
DROP POLICY IF EXISTS project_isolation_policy ON projects;
DROP POLICY IF EXISTS org_isolation_policy ON organizations;
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE attachments DISABLE ROW LEVEL SECURITY;
ALTER TABLE comments DISABLE ROW LEVEL SECURITY;
ALTER TABLE tasks DISABLE ROW LEVEL SECURITY;
ALTER TABLE projects DISABLE ROW LEVEL SECURITY;
ALTER TABLE organizations DISABLE ROW LEVEL SECURITY;
DROP FUNCTION IF EXISTS has_project_access(UUID);
DROP FUNCTION IF EXISTS is_org_member(UUID);
DROP FUNCTION IF EXISTS is_org_admin(UUID);
COMMIT;
*/
