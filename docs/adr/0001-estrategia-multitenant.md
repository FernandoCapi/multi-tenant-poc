# ADR-0001 — Estratégia multitenant e isolamento entre famílias

- **Status:** Proposto
- **Data:** 2026-10-21
- **Etapa:** P-001 (especificação); complementado em P-003 (2026-10-22) com a implementação e as
  evidências da fundação — ver [Implementação](#implementação-p-003) e [Evidências](#evidências)
- **Relacionados:** DT-02, DT-03, DT-04, DT-05; [requisitos](../requisitos.md) RS-01..RS-07; [modelo de domínio](../modelo-de-dominio.md)

## Contexto

Cada família é um tenant. Um membro nunca pode consultar ou modificar dados de outra família, e o
isolamento não pode depender de uma única camada (DT-03). Dentro da família, o acesso é controlado
por objetivo (D-01, D-05).

Restrições já estabelecidas:

- PostgreSQL, Spring Boot, Flyway (DT-01). Versões em uso desde P-003: PostgreSQL 18.6,
  Spring Boot 4.1.1 (Hibernate 7.4), Java 21. A DT-01 original citava Spring Boot 3.
- Tabelas compartilhadas com `family_id` (DT-02).
- Contexto de tenant na aplicação, reforço na persistência e Row-Level Security (DT-03).
- Usuário em uma única família, tenant resolvido no login e incluído no JWT (DT-04).

## Decisão

### 1. Modelo de dados compartilhado

- Toda tabela de negócio tem `family_id NOT NULL`.
- Toda tabela referenciada tem `UNIQUE (family_id, id)`, e as FKs entre tabelas de negócio são
  **compostas** com `family_id` (ex.: `(family_id, goal_id) → goals(family_id, id)`). Isso torna
  impossível, no banco, uma contribuição de uma família apontar para um objetivo de outra.
- Índices compostos começam por `family_id`, seguindo os padrões de acesso.
- O contribuinte de uma contribuição referencia `users` (não `goal_members`), porque a contribuição
  permanece depois que o membro sai do objetivo (INV-07).

### 2. Contexto de tenant na aplicação (`TenantContext`)

- Preenchido a partir do JWT validado, por um filtro executado **depois** da autenticação, e sempre
  limpo ao fim da requisição (`finally`).
- Nunca é preenchido com valores vindos do corpo, da query ou de cabeçalhos do cliente (RS-04).
- Rotas de negócio sem `TenantContext` válido são rejeitadas.
- Escopo da requisição/thread: não é propagado para execução assíncrona. Operações de tenant em
  threads separadas ficam proibidas na POC.

### 3. Reforço na persistência

- Repositórios de negócio sempre filtram pelo tenant do contexto e, quando aplicável, pelo vínculo
  do usuário com o objetivo (`GoalMember`).
- Escritas atribuem `family_id` a partir do `TenantContext`, nunca de um DTO.
- Mecanismo escolhido em P-003: **JPA + Hibernate com `@TenantId`** (discriminador de tenant).
  O Hibernate acrescenta o filtro de `family_id` nas consultas de entidades e preenche o campo na
  inserção a partir do `TenantContext`. Consultas nativas não recebem esse filtro; nelas, só a RLS
  protege, e um teste cobre exatamente esse caso.
- Sem tenant no contexto, o resolver devolve o UUID nulo (`00000000-0000-0000-0000-000000000000`),
  que não corresponde a nenhuma família: as consultas do Hibernate falham fechadas.

### 4. Row-Level Security no PostgreSQL

- RLS habilitada **e forçada** (`ENABLE` + `FORCE ROW LEVEL SECURITY`) em todas as tabelas de negócio.
- Policy de leitura e escrita (`USING` e `WITH CHECK`) comparando `family_id` com o tenant da transação:

  ```sql
  family_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid
  ```

  O `NULLIF` é necessário porque, depois que uma variável customizada foi definida em uma sessão,
  `current_setting(..., true)` passa a devolver string vazia (e não `NULL`) quando ela não está
  definida; sem o `NULLIF`, o cast falharia com erro em vez de simplesmente não retornar linhas.
  Sem contexto, a comparação com `NULL` nunca é verdadeira: **falha fechada** (RS-06).
- O tenant é definido **dentro da transação**, no início de cada transação de negócio, com:

  ```sql
  SELECT set_config('app.tenant_id', :familyId, true);
  ```

  O terceiro argumento `true` dá o mesmo efeito de `SET LOCAL` (vale só até o fim da transação),
  mas aceita parâmetro vinculado, evitando concatenar o valor no SQL.
- Os identificadores são `uuid`, confirmado nas migrations (P-003).

### 5. Roles do banco

| Role | Uso | Características |
|---|---|---|
| `app_owner` | Dono do schema; usada pelo Flyway. | Cria e altera objetos. Sujeita à RLS por causa do `FORCE`. Não usada em tempo de execução. |
| `app_user` | Usada pela aplicação. | `NOSUPERUSER`, `NOBYPASSRLS`, sem ser dona das tabelas; apenas privilégios de DML necessários. |

Superusers sempre ignoram a RLS; por isso a aplicação nunca se conecta como superuser.

Privilégios de `app_user` (P-003): somente leitura em `families` e `users`; leitura e escrita em
`goals` e `goal_members`; **somente `SELECT` e `INSERT`** em `contributions` e
`contribution_reversals`. Assim, a imutabilidade das contribuições (D-02) também é garantida pelo banco.

### 6. Resolução do tenant: claim no JWT

No login, o servidor busca o usuário, valida a senha e emite um JWT com `sub` (id do usuário) e o
id da família. Nas requisições seguintes, o `TenantContext` usa a claim, sem consultar o banco.

### 7. Autorização por objetivo

A RLS garante o isolamento **entre famílias**. O controle **por objetivo** (DONO/MEMBRO, `contribui`)
fica na camada de aplicação, coberto por testes automatizados (RS-02). Listagens e agregações
aplicam o mesmo filtro de vínculo usado no detalhe.

## Alternativas consideradas

### A. Banco ou schema por tenant

- **Benefícios:** isolamento físico forte; um bug de consulta não vaza entre tenants.
- **Custos:** migrations multiplicadas, pool por tenant, provisionamento mais complexo.
- **Motivo da rejeição:** contraria DT-02 e está fora do objetivo da POC, que é demonstrar isolamento em tabelas compartilhadas.

### B. Tabelas compartilhadas sem RLS (só aplicação)

- **Benefícios:** mais simples; nenhuma dependência de recursos do PostgreSQL.
- **Custos:** uma única consulta esquecida sem filtro vaza dados.
- **Motivo da rejeição:** contraria DT-03 (defesa em profundidade).

### C. Resolver o tenant a cada requisição a partir do `sub` (exigida por DT-04)

| Aspecto | Claim no JWT (decisão) | Consulta por `sub` a cada requisição |
|---|---|---|
| Custo por requisição | Nenhuma consulta extra. | Uma consulta (ou cache) por requisição. |
| Revogação/mudança de vínculo | Só tem efeito quando o token expira. | Efeito imediato. |
| Confiança | Depende da integridade da assinatura do JWT. | Depende só do banco; o token carrega apenas identidade. |
| Login antes do tenant | Mesmo problema nas duas opções (ver Riscos). | Também precisa ler o vínculo do usuário antes de existir contexto de tenant. |
| Evolução para várias famílias por usuário | Exige reemitir token ou adicionar claim de família ativa. | Mais natural: o servidor valida o vínculo com a família escolhida a cada requisição. |

**Implicações para a POC:** como não há troca de família nem cadastro (D-11), a desatualização do
vínculo no token tem risco baixo; a expiração curta do token limita a janela. Se a POC evoluir para
múltiplas famílias por usuário ou revogação imediata, a consulta por `sub` (com cache) passa a ser
preferível.

### D. RLS também para a autorização por objetivo

- **Descrição:** definir também `app.user_id` na transação e criar policies que exijam vínculo em `goal_members`.
- **Benefícios:** a ACL por objetivo também passa a ser garantida pelo banco.
- **Custos:** policies mais complexas (gestão de membros, último dono, agregações), dependência
  circular entre `goals` e `goal_members` nas policies e depuração mais difícil.
- **Situação:** não adotada na POC. Fica como evolução a ser avaliada depois que os testes de
  autorização por objetivo existirem.

## Riscos e mitigações

A coluna de validação descreve o teste esperado. A seção [Evidências](#evidências) registra quais
deles já foram executados.

| Risco | Mitigação proposta | Validação |
|---|---|---|
| **Login antes do tenant:** a busca do usuário por e-mail acontece sem `app.tenant_id`, e a RLS bloquearia a leitura. | A definir na implementação. Opções: (a) tabela global de credenciais sem RLS, contendo só e-mail, hash, `user_id` e `family_id`; (b) função `SECURITY DEFINER` restrita a esse lookup. A opção (a) é a mais simples; a escolha será registrada neste ADR. | Teste de login e teste de que a tabela de credenciais não expõe dados de negócio. |
| **Pool de conexões:** um `SET` de sessão vazaria o tenant para a próxima requisição que usar a mesma conexão. | Usar apenas `set_config(..., true)` dentro da transação; nunca `SET` de sessão. | Teste de requisições sucessivas de tenants diferentes na mesma conexão (RS-07). **Feito em P-003.** |
| **Acesso fora de transação:** sem transação, a configuração local não se aplica. | Toda operação de negócio dentro de transação; `spring.jpa.open-in-view=false`; leituras lazy fora da transação falham fechadas por causa da RLS. | Teste de consulta sem contexto retornando zero linhas (RS-06). **Feito em P-003.** |
| **Dono das tabelas ignora a RLS sem `FORCE`.** | `FORCE ROW LEVEL SECURITY`; `app_user` não é dona das tabelas. | Teste executando consulta sem `WHERE` com `app_user` (RS-05). **Feito em P-003.** |
| **Uso acidental de superuser ou role com BYPASSRLS.** | Credenciais separadas para Flyway e aplicação; checagem dos atributos da role. | Teste ou verificação que confirme `rolsuper = false` e `rolbypassrls = false` para `app_user`. **Feito em P-003.** |
| **Escrita com `family_id` de outro tenant.** | `WITH CHECK` na policy, `family_id` vindo só do `TenantContext` e FKs compostas. | Teste com `family_id` forjado (N-2). **Feito no banco em P-003**; pela API, na etapa de endpoints. |
| **Claim de tenant desatualizada no JWT.** | Expiração curta; risco baixo sem troca de família. | Documentado; sem teste específico na POC. |

## Implementação (P-003)

| Elemento | Onde |
|---|---|
| Roles `app_owner` e `app_user` | `infra/postgres/init/01-create-roles.sh` (Compose e Testcontainers usam o mesmo script) |
| Esquema com `family_id`, `UNIQUE (family_id, id)` e FKs compostas | `backend/src/main/resources/db/migration/V1__create_core_schema.sql` |
| Função `app_current_family_id()`, RLS `ENABLE` + `FORCE` e policies | `V2__enable_row_level_security.sql` |
| Privilégios mínimos de `app_user` | `V3__grant_app_user_privileges.sql` |
| `TenantContext` (ThreadLocal) | `backend/src/main/java/com/vesta/tenant/TenantContext.java` |
| Resolver do Hibernate (`@TenantId`) | `TenantIdentifierResolver.java`, registrado em `TenantPersistenceConfiguration.java` |
| `set_config('app.tenant_id', ?, true)` no início de cada transação | `TenantAwareJpaTransactionManager.java`: estende `JpaTransactionManager` e substitui o gerenciador de transações padrão. Se a definição do tenant falhar, a transação é desfeita e não é entregue. |
| Configuração | `application.yml`: datasource `app_user`, Flyway `app_owner`, `open-in-view=false`, `ddl-auto=validate` |

Ainda não implementado: preenchimento do `TenantContext` a partir do JWT (etapa de autenticação),
lookup de login (risco em aberto abaixo) e autorização por objetivo.

## Evidências

Executadas em 2026-10-22, com JDK 21.0.12.1, Maven 3.10.0 e PostgreSQL 18.6 via Testcontainers.

**Suíte `./mvnw verify`:** 31 testes, 0 falhas.

| Classe | Testes | Cobre |
|---|---|---|
| `DatabaseSecurityTest` | 4 | `app_user` não é superuser, não tem BYPASSRLS e não é dona de tabelas; todas as tabelas com RLS forçada e policy; `app_user` não consegue desligar a RLS. |
| `RowLevelSecurityTest` | 22 | RS-05/N-3 (SQL sem `WHERE` em cada tabela); RS-06 (sem tenant, inclusive o caso `''` na mesma conexão); RS-04/N-2 no banco (`WITH CHECK`); `UPDATE`/`DELETE` entre famílias afetam 0 linhas; FK composta; tenant inválido gera erro; imutabilidade; um estorno por contribuição. |
| `TenantIsolationIntegrationTest` | 5 | Repositório e SQL nativo isolados; RS-07 (A → B → sem tenant na mesma conexão do pool, mesmo `pg_backend_pid`); `family_id` vindo do contexto; sem tenant, leitura vazia e escrita rejeitada. |

**Testes de mutação** (alterações temporárias, desfeitas e não commitadas), para confirmar que os
testes detectam a ausência de cada proteção:

| Alteração | Resultado |
|---|---|
| Remover RLS e policy de `goals` | 7 de 26 testes de banco falharam, incluindo o teste de catálogo. |
| Remover RLS e policy de `goals` (testes Spring) | 2 de 5 falharam: os de SQL nativo. O teste do repositório **passou**, o que mostra que o filtro do Hibernate isola sozinho. |
| Não definir o tenant na transação | 4 de 5 testes Spring quebraram (3 falhas e 1 erro). A RLS bloqueia até o caminho do Hibernate quando o tenant não é definido. |

**Ambiente Compose:** Flyway aplicou V1..V3 como `app_owner`; `app_user` vê 0 linhas em `goals` sem
tenant; `alter table goals disable row level security` como `app_user` falha com
`must be owner of table goals`.

## Consequências

- O isolamento entre famílias depende de três camadas independentes (aplicação, persistência, RLS) e
  das FKs compostas, e cada uma deve ser demonstrada por testes.
- A aplicação precisa garantir que toda operação de negócio aconteça em transação com o tenant definido.
- A autorização por objetivo depende da aplicação e dos testes, não da RLS.
- Este ADR permanece **Proposto** até que a implementação e os testes de RS-01..RS-07 existam e
  passem; só então seu status pode mudar para **Aceito**, com referência às evidências. Situação em
  P-003: RS-04 (no banco), RS-05, RS-06 e RS-07 têm evidência; RS-01 pela API, RS-02, RS-03 e o
  lookup de login ainda faltam.
