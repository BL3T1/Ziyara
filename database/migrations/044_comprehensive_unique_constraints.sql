-- Migration: 044_comprehensive_unique_constraints.sql
-- Phase: 1 (Critical Security & Integrity)
-- Description: Enforces business-level uniqueness to prevent duplicate data entries
--              that could lead to logic errors or security vulnerabilities.

BEGIN;

-- 1. Users: Ensure email is unique per organization (Multi-tenant safety)
-- If global uniqueness is required, use just (email). Here we assume multi-tenant.
ALTER TABLE users 
    ADD CONSTRAINT uk_users_email_organization 
    UNIQUE (email, organization_id);

-- 2. Users: Ensure username is globally unique if used for login
ALTER TABLE users 
    ADD CONSTRAINT uk_users_username_global 
    UNIQUE (username) 
    WHERE username IS NOT NULL;

-- 3. Projects: Prevent duplicate project names within an organization
ALTER TABLE projects 
    ADD CONSTRAINT uk_projects_name_organization 
    UNIQUE (name, organization_id);

-- 4. Tasks: Prevent duplicate task titles within a project (Optional, depending on business rules)
-- Often tasks have IDs, but if business requires unique titles per project:
-- ALTER TABLE tasks 
--     ADD CONSTRAINT uk_tasks_title_project 
--     UNIQUE (title, project_id);
-- Commented out as task titles often repeat. Instead, let's ensure User-Project roles are unique.

-- 5. Project Members: Ensure a user cannot be added twice to the same project
ALTER TABLE project_members 
    ADD CONSTRAINT uk_project_members_user_project 
    UNIQUE (user_id, project_id);

-- 6. Task Assignees: Ensure a user is not assigned twice to the same task
ALTER TABLE task_assignees 
    ADD CONSTRAINT uk_task_assignees_user_task 
    UNIQUE (user_id, task_id);

-- 7. User Roles: Ensure a user has only one role definition per scope (Org or Project)
-- Organization Roles
ALTER TABLE organization_roles 
    ADD CONSTRAINT uk_org_roles_user_org 
    UNIQUE (user_id, organization_id);

-- Project Roles (if separate table exists, otherwise handled in project_members)
-- Assuming a separate user_project_roles table might exist or is merged. 
-- If using project_members for roles:
-- ALTER TABLE project_members ADD CONSTRAINT uk_pm_user_project_role UNIQUE (user_id, project_id, role); 
-- (Handled by #5 if role is part of the membership identity)

-- 8. API Keys: Ensure API keys are globally unique
ALTER TABLE api_keys 
    ADD CONSTRAINT uk_api_keys_key_hash 
    UNIQUE (key_hash);

-- 9. Sessions: Ensure active session tokens are unique
ALTER TABLE user_sessions 
    ADD CONSTRAINT uk_sessions_token 
    UNIQUE (session_token);

-- 10. MFA Methods: Ensure a user doesn't have duplicate MFA methods of same type
ALTER TABLE user_mfa_methods 
    ADD CONSTRAINT uk_mfa_user_type 
    UNIQUE (user_id, method_type, identifier) 
    WHERE is_active = TRUE;

-- 11. Audit Logs: No specific unique constraint needed, but ensure indexing for time-based queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_desc 
    ON audit_logs(created_at DESC);

-- 12. Organizations: Ensure organization slug/name is unique globally (for URL routing)
ALTER TABLE organizations 
    ADD CONSTRAINT uk_organizations_slug 
    UNIQUE (slug);

ALTER TABLE organizations 
    ADD CONSTRAINT uk_organizations_name_global 
    UNIQUE (name);

COMMIT;

-- Verification Queries
-- SELECT conname FROM pg_constraint WHERE conname LIKE 'uk_%';

-- ROLLBACK SCRIPT
/*
BEGIN;
ALTER TABLE organizations DROP CONSTRAINT IF EXISTS uk_organizations_name_global;
ALTER TABLE organizations DROP CONSTRAINT IF EXISTS uk_organizations_slug;
DROP INDEX IF EXISTS idx_audit_logs_created_desc;
ALTER TABLE user_mfa_methods DROP CONSTRAINT IF EXISTS uk_mfa_user_type;
ALTER TABLE user_sessions DROP CONSTRAINT IF EXISTS uk_sessions_token;
ALTER TABLE api_keys DROP CONSTRAINT IF EXISTS uk_api_keys_key_hash;
ALTER TABLE organization_roles DROP CONSTRAINT IF EXISTS uk_org_roles_user_org;
ALTER TABLE task_assignees DROP CONSTRAINT IF EXISTS uk_task_assignees_user_task;
ALTER TABLE project_members DROP CONSTRAINT IF EXISTS uk_project_members_user_project;
-- ALTER TABLE tasks DROP CONSTRAINT IF EXISTS uk_tasks_title_project;
ALTER TABLE projects DROP CONSTRAINT IF EXISTS uk_projects_name_organization;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_username_global;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_email_organization;
COMMIT;
*/
