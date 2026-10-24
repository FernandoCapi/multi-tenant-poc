-- Live demonstration of scenario N-3 (docs/demonstracao.md): row-level security restricts SQL
-- that has no WHERE family_id at all. Run it as the application role, app_user:
--
--   docker compose exec -T -e PGPASSWORD=<APP_USER_PASSWORD> postgres \
--     psql -h localhost -U app_user -d vesta -f - < infra/demo/n3-rls-sem-where.sql
--
-- Read-only except for the UPDATE in step 4, which must affect 0 rows.

\pset footer on
\echo '== 1. Tenant = Família Silva: SELECT sem WHERE, só aparecem objetivos da Silva'
BEGIN;
SELECT set_config('app.tenant_id', 'a0000000-0000-0000-0000-000000000001', true) AS tenant;
SELECT title, family_id FROM goals ORDER BY title;
COMMIT;

\echo '== 2. Tenant = Família Souza: a mesma consulta, agora só a Souza'
BEGIN;
SELECT set_config('app.tenant_id', 'b0000000-0000-0000-0000-000000000001', true) AS tenant;
SELECT title, family_id FROM goals ORDER BY title;
COMMIT;

\echo '== 3. Sem tenant na transação: nenhuma linha (falha fechada)'
SELECT count(*) AS objetivos_visiveis FROM goals;

\echo '== 4. Tenant = Souza tentando alterar a Viagem da Silva: UPDATE 0'
BEGIN;
SELECT set_config('app.tenant_id', 'b0000000-0000-0000-0000-000000000001', true) AS tenant;
UPDATE goals SET title = 'Invadido' WHERE id = 'a2000000-0000-0000-0000-000000000001';
ROLLBACK;

\echo '== 5. A role da aplicação não consegue desligar a RLS'
\set ON_ERROR_STOP off
ALTER TABLE goals DISABLE ROW LEVEL SECURITY;
