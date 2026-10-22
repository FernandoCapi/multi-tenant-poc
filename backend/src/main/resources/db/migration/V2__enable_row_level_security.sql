-- Row-level security per family (ADR-0001).
-- The tenant is bound per transaction by the application with
--   select set_config('app.tenant_id', '<family uuid>', true)
-- Without a bound tenant the function returns NULL and no row matches (fail closed).
-- NULLIF is required because, once the setting was used in a session, current_setting
-- returns '' (not NULL) after the transaction that set it ends.

CREATE FUNCTION app_current_family_id() RETURNS uuid
    LANGUAGE sql
    STABLE
AS $$
    SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid
$$;

-- FORCE makes the policies apply to the table owner as well
ALTER TABLE families ENABLE ROW LEVEL SECURITY;
ALTER TABLE families FORCE ROW LEVEL SECURITY;
CREATE POLICY families_tenant_isolation ON families
    USING (id = app_current_family_id())
    WITH CHECK (id = app_current_family_id());

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY users_tenant_isolation ON users
    USING (family_id = app_current_family_id())
    WITH CHECK (family_id = app_current_family_id());

ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE goals FORCE ROW LEVEL SECURITY;
CREATE POLICY goals_tenant_isolation ON goals
    USING (family_id = app_current_family_id())
    WITH CHECK (family_id = app_current_family_id());

ALTER TABLE goal_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE goal_members FORCE ROW LEVEL SECURITY;
CREATE POLICY goal_members_tenant_isolation ON goal_members
    USING (family_id = app_current_family_id())
    WITH CHECK (family_id = app_current_family_id());

ALTER TABLE contributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE contributions FORCE ROW LEVEL SECURITY;
CREATE POLICY contributions_tenant_isolation ON contributions
    USING (family_id = app_current_family_id())
    WITH CHECK (family_id = app_current_family_id());

ALTER TABLE contribution_reversals ENABLE ROW LEVEL SECURITY;
ALTER TABLE contribution_reversals FORCE ROW LEVEL SECURITY;
CREATE POLICY contribution_reversals_tenant_isolation ON contribution_reversals
    USING (family_id = app_current_family_id())
    WITH CHECK (family_id = app_current_family_id());
