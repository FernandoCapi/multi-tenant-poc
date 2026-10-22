-- Least-privilege grants for the runtime role (ADR-0001).
-- Families and users are seeded by the schema owner; the application only reads them.
GRANT SELECT ON families, users TO app_user;

GRANT SELECT, INSERT, UPDATE, DELETE ON goals, goal_members TO app_user;

-- Contributions and reversals are immutable (D-02): no UPDATE or DELETE for the application
GRANT SELECT, INSERT ON contributions, contribution_reversals TO app_user;

GRANT EXECUTE ON FUNCTION app_current_family_id() TO app_user;
