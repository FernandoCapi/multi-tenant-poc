# AI Development Report

> Relatório cumulativo sobre o uso de IA (Claude Code) no desenvolvimento da POC. É atualizado a
> cada etapa. Toda afirmação deve ter evidência: prompt registrado, arquivo, commit, saída de teste
> ou decisão documentada.

## Visão geral

| Etapa | Data | Tipo de apoio | Resultado | Evidência |
|---|---|---|---|---|
| P-001 | 2026-10-21 | Análise de requisitos, levantamento de decisões, especificação e ADR proposto | 12 decisões de negócio, 1 convenção, 7 itens pendentes, ADR-0001 proposto | [historico-de-prompts.md](historico-de-prompts.md#p-001--definição-dos-requisitos-da-poc) |
| P-002 | 2026-10-22 | Convenções de Git no `CLAUDE.md` e commits da documentação | Documentação da P-001 aprovada e commitada em commits atômicos | [historico-de-prompts.md](historico-de-prompts.md#p-002--aprovação-da-documentação-e-convenções-de-git) |
| P-003 | 2026-10-22 | Planejamento e implementação da fundação técnica | Monorepo, Compose, esquema com RLS forçada, vínculo do tenant à transação, 31 testes e 3 testes de mutação | [historico-de-prompts.md](historico-de-prompts.md#p-003--fundação-técnica-monorepo-compose-rls-e-testes-de-isolamento), [ADR-0001](../adr/0001-estrategia-multitenant.md#evidências) |
| P-004 | 2026-10-22 | Ajustes do ambiente local | Branch movida para o repositório local; backend lê o `.env` fora do Docker | [historico-de-prompts.md](historico-de-prompts.md#p-004--ajustes-do-ambiente-local) |
| P-005 | 2026-10-22 | Análise de avisos de inspeção | Falsos positivos de JPA suprimidos pontualmente, com justificativa | [historico-de-prompts.md](historico-de-prompts.md#p-005--avisos-de-inspeção-na-entidade-goal) |
| P-006 | 2026-10-22 | Roadmap P-006..P-010 e autenticação | Login JWT, lookup `SECURITY DEFINER`, tenant do token, `/api/me`, seeds, 56 testes, Bruno `auth/` | [historico-de-prompts.md](historico-de-prompts.md#p-006--roadmap-e-autenticação), [ADR-0002](../adr/0002-autenticacao-jwt.md) |
| P-007 | 2026-10-23 | Objetivos, membros, autorização por objetivo e progresso | API de objetivos/membros, 404/403, lock otimista/pessimista, progresso, 121 testes, Bruno `goals/` | [historico-de-prompts.md](historico-de-prompts.md#p-007--objetivos-membros-e-autorização-por-objetivo), [ADR-0001](../adr/0001-estrategia-multitenant.md#evidências-de-p-007) |

## Indicadores acumulados

| Indicador | Valor | Observação |
|---|---|---|
| Etapas registradas | 7 | P-001..P-007 |
| Recomendações da IA decididas | 25 | 24 aceitas, 1 não adotada ([detalhes](decisoes-influenciadas-pela-ia.md)); propostas de plano não adotadas: 2 (IA-18, IA-19) |
| Sugestões incorretas evidenciadas | 2 | SI-001, SI-002 ([sugestoes-incorretas.md](sugestoes-incorretas.md)) |
| Linhas de código produzidas | ≈ 5.090 | Até P-007: Java de produção 2.041, Java de teste 2.095, SQL 298, shell 36, Bruno 616 (`wc -l`, inclui linhas em branco e comentários) |
| Testes automatizados | 121 | Todos passando em 2026-10-23 (`./mvnw test`) |

---

## P-001 — Levantamento de requisitos (2026-10-21)

**Ferramenta:** Claude Code, modelo Claude Opus 5.5, em modo de planejamento.

### O que a IA fez

- Verificou o estado do repositório (apenas `README.md` com o título).
- Consolidou os requisitos do prompt em requisitos funcionais (RF) e de segurança (RS), com
  critérios iniciais de aceitação.
- Apontou ambiguidades e contradições no prompt, entre elas:
  - "parcelas do carro" e "despesas recorrentes" versus o modelo de valor-alvo + contribuições (→ D-04);
  - "contribuições coletivas" versus a exigência de identificar o autor de cada contribuição (→ D-02);
  - 404 universal versus ação sem permissão em recurso visível (→ D-09);
  - "exclusão" versus rastreabilidade financeira (→ D-02, D-08).
- Conduziu três lotes de perguntas (12 perguntas), com opções, trade-offs e recomendação inicial.
- Identificou, após as respostas do lote 1, a tensão entre D-01 e o RF-03, que levou à D-05.
- Propôs um plano de documentação; o usuário o revisou e acrescentou a convenção de idioma (C-01).
- Produziu a especificação de requisitos, o modelo de domínio, o registro de decisões, o ADR-0001
  (status Proposto), os registros de IA, o `CLAUDE.md` e o índice no `README.md`.

### Decisões do usuário que divergiram da IA

- **D-01:** o usuário preferiu ACL somente por objetivo à recomendação de papel familiar + ACL.

### O que não foi feito nesta etapa

- Nenhum código, estrutura de projeto, dependência, migration ou teste.
- Nada no ADR-0001 foi validado; as mitigações técnicas são propostas.
- Os padrões PV-01..PV-07 não foram aprovados explicitamente pelo usuário.
- Os documentos ainda não foram revisados pelo usuário nem commitados.

### Próximos passos sugeridos

- Revisar os documentos desta etapa e decidir os itens PV-01..PV-07.
- Planejar a primeira entrega incremental (estrutura do monorepo, Docker Compose, banco com RLS e
  primeiros testes de isolamento).

---

## P-002 — Aprovação e convenções de Git (2026-10-22)

**Ferramenta:** Claude Code, modelo Claude Opus 5.5.

### O que a IA fez

- Registrou no `CLAUDE.md` as regras de Git definidas pelo usuário: branch por feature/escopo a
  partir da `main`, Conventional Commits atômicos, sem `Co-Authored-By`, commit/push/PR somente
  quando solicitado.
- Criou a branch `docs/p-001-especificacao-inicial` a partir da `main` e commitou a documentação
  da P-001 em commits atômicos, um por documento ou grupo coeso.

### O que não foi feito nesta etapa

- Nenhum push nem PR (não solicitados).
- Os itens PV-01..PV-07 continuam pendentes.

---

## P-003 — Fundação técnica (2026-10-22)

**Ferramenta:** Claude Code, modelo Claude Opus 5.5. Planejamento em plan mode, implementação com
aprovação do plano.

### O que a IA fez

- Verificou o estado do Git e do ambiente antes de planejar e fez 6 perguntas técnicas (persistência,
  build, Java, frontend, PV-05, pacote).
- Planejou a etapa. O plano foi revisado depois que o usuário informou as versões e as restrições
  do ambiente (SI-001, IA-18).
- Gerou o projeto pelo Spring Initializr (Boot 4.1.1), baixado e inspecionado em pasta temporária
  antes de ser copiado. Antes de escrever código, confirmou nos jars os pacotes do Spring Boot 4 e
  do Hibernate 7 (`HibernatePropertiesCustomizer`, `TransactionManagerCustomizers`,
  `CurrentTenantIdentifierResolver`, `MultiTenancySettings`).
- Implementou:
  - roles do banco;
  - migrations V1..V3: esquema, RLS forçada e privilégios mínimos;
  - `TenantContext`, o resolver do Hibernate e o `TenantAwareJpaTransactionManager`;
  - a entidade `Goal`;
  - Dockerfile, Compose e o esqueleto da coleção Bruno.
- Escreveu 31 testes de integração com Testcontainers (PostgreSQL 18.6, mesmo script de roles do Compose).
- Fez 11 commits atômicos na branch `feat/p-003-fundacao-rls`.

### Verificações executadas (saídas lidas)

| Verificação | Resultado |
|---|---|
| `./mvnw -v` | Maven 3.10.0, Java 21.0.12.1 |
| `./mvnw test` (suíte completa) | 31 testes, 0 falhas, 0 erros |
| Mutação: sem RLS em `goals` (testes de banco) | 7 de 26 falharam |
| Mutação: sem RLS em `goals` (testes Spring) | 2 de 5 falharam (SQL nativo); o teste do repositório passou, porque o filtro do Hibernate isola sozinho |
| Mutação: tenant não definido na transação | 3 falhas e 1 erro em 5 testes Spring |
| `docker compose up -d --build` | `postgres` e `backend` healthy; `/actuator/health` → `UP` |
| Flyway no Compose | V1..V3 aplicadas por `app_owner` |
| `psql` como `app_user` sem tenant | 0 linhas em `goals`; `disable row level security` → `must be owner of table goals` |
| `./mvnw spring-boot:run` contra o banco do Compose (porta 8081) | Iniciou e respondeu `UP`; processo encerrado em seguida |
| Bruno CLI 4.3.0 (`run --env local`) | 1 requisição, 2/2 asserções |
| Ambiente global intacto, em terminal novo | `gradle -v` → Gradle 8.3 na JVM 17.0.17; `JAVA_HOME` continua no Temurin 17 |

As alterações das mutações foram desfeitas; o arquivo restaurado foi comparado com o original (`cmp`).

### Decisões do usuário que divergiram da IA

- Versões: Spring Boot 4.1.1 e PostgreSQL 18.6, em vez de 3.5 e 17 (IA-18 → T-01).
- Instalação do JDK: em pasta do usuário, ativado por sessão, em vez de `winget` (IA-19 → SI-001).

### O que não foi feito nesta etapa

- Autenticação/JWT, preenchimento do `TenantContext` pela requisição, endpoints de negócio,
  autorização por objetivo, seeds de demonstração e frontend.
- Lookup de login (risco em aberto no ADR-0001).
- Sem push nem PR (não solicitados). Os itens PV-01..PV-04, PV-06 e PV-07 continuam pendentes.

---

## P-004 — Ajustes do ambiente local (2026-10-22)

- **Repositório local:** a branch estava presa a um worktree separado, invisível para a IDE do
  usuário. Ela foi movida para o repositório local, e o Compose agora sobe a partir dele.
- **Credenciais:** o usuário tentou configurar o backend com as variáveis `POSTGRES_*` e `app_owner`.
  A IA explicou a separação das roles (ADR-0001) e, com aprovação, fez o backend ler o `.env` fora do Docker.
- **Verificações executadas:** 31 testes passando; backend iniciado sem variáveis de ambiente a
  partir da raiz e de `backend/`; caso negativo (pasta sem `.env` → senha vazia); container do
  Compose saudável após o rebuild.
- **Observação:** a mensagem de erro não indicava quais variáveis o backend lê, e o guia de setup
  só mencionava as variáveis no comando de terminal. O guia agora traz uma tabela de resolução das
  propriedades e uma entrada de solução de problemas para esse erro.

---

## P-006 — Roadmap e autenticação (2026-10-22)

**Ferramenta:** Claude Code, modelo Claude Opus 5.5.

### O que a IA fez

- Propôs o roadmap P-006..P-010 e fez 4 perguntas; as 4 recomendações foram aceitas.
- Confirmou nos metadados do Initializr e nos jars os nomes de starters e pacotes do Boot 4.1.1
  (security, resource server, `AutoConfigureMockMvc`).
- Gerou os hashes bcrypt de teste e de demonstração com o próprio encoder do Spring.
- Implementou:
  - role `app_auth` e a migration V4 (hash de senha, privilégio por coluna, função de lookup);
  - login com JWT HS256 e validação de `sub`/`family_id`;
  - `TenantContextFilter`, entidades `User`/`Family` e o endpoint `/api/me`;
  - seeds de duas famílias no perfil `local`;
  - cenários Bruno de autenticação;
  - ADR-0002.

### Verificações executadas (saídas lidas)

| Verificação | Resultado |
|---|---|
| `./mvnw test` | 56 testes, 0 falhas |
| `./mvnw test` sem `.env` presente | 42 testes passando no momento da checagem (antes de `/api/me` e seeds); a base de testes define todas as propriedades |
| Mutação: `app_auth` sem BYPASSRLS | 2 de 6 testes de lookup falharam |
| Mutação: `TenantContextFilter` não registrado | 2 de 8 testes de `/api/me` falharam |
| `docker compose down -v && up -d --build` | Healthy; Flyway V1..V4 + seed; login e `/api/me` corretos para as duas famílias; 401 sem token e com senha errada |
| Backend sem perfil `local` contra banco com seed | Sobe e autentica (padrão `repeatable:missing`) |
| Bruno CLI 4.3.0 | 8/8 requisições, 17/17 asserções |
| Comandos `psql` documentados | Executados; saída conforme o guia |

### Problemas encontrados durante a etapa

- **Testes passando pelo motivo errado:** a primeira rodada dos testes de login passou porque o
  segredo JWT vinha do `.env` local, já que a edição na base de testes não tinha sido aplicada. A
  IA percebeu o problema na saída da ferramenta, corrigiu e confirmou rodando os testes com o
  `.env` removido. Regra registrada no `CLAUDE.md`: testes não podem depender do `.env`.
- **README em UTF-16:** o arquivo estava em UTF-16 desde o primeiro commit do repositório, e as
  edições anteriores da IA (P-001 e P-003) mantiveram essa codificação sem perceber. Convertido
  para UTF-8 em commit próprio.
- **Ambiente Bruno não atualizado:** a primeira execução falhou em 3 requisições porque a edição do
  ambiente `local.bru` não tinha sido aplicada. Corrigido, e a segunda execução passou.
- **Edição em `docs/requisitos.md` não aplicada** no primeiro commit de documentação. Detectada,
  aplicada e incluída no mesmo commit, ainda local, com `--amend`.

### O que não foi feito nesta etapa

- Execução pela IDE do usuário com o perfil `local`: não é possível pela IA. Foram verificadas a
  execução por `java -jar` sem perfil e a execução no Compose com o perfil.
- Endpoints de objetivos e contribuições, autorização por objetivo e frontend (P-007 a P-009).
- Sem push nem PR (não solicitados).

---

## P-007 — Objetivos, membros e autorização por objetivo (2026-10-23)

**Ferramenta:** Claude Code, modelo Claude Opus 5.5.

### O que a IA fez

- Fez 4 perguntas (PV-02, PV-03, PV-06 e o escopo do progresso); as 4 recomendações foram aceitas.
- Implementou:
  - V5 (`version`);
  - domínio do objetivo (transições, `GoalMember`);
  - `GoalAccessService` (404/403);
  - API de objetivos e membros;
  - diretório da família;
  - progresso financeiro com total por membro;
  - cenários Bruno de objetivos.
- Removeu a supressão `FieldMayBeFinal` de `status` em `Goal`, como previsto na P-005, já que as
  transições agora alteram o campo.

### Verificações executadas (saídas lidas)

| Verificação | Resultado |
|---|---|
| `./mvnw test` | 121 testes, 0 falhas |
| Mutação: `requireOwner` sempre permite | 10 de 27 testes da matriz falharam |
| Mutação: listagem sem filtro de vínculo | 2 de 8 testes de listagem quebraram (por erro, não asserção) |
| Mutação: último dono com `< 1` | 1 de 10 testes de membros falhou |
| Mutação: acumulado sem excluir estornos | 4 de 4 testes de progresso falharam |
| Compose com dados demo | V5 aplicada; Carlos vê 3 objetivos; Pedro: 404 no Aluguel, 403 ao editar a Viagem; Marcos: 404 na Silva; Viagem = 3.450,00 |
| Bruno CLI 4.3.0, duas execuções seguidas | 23/23 requisições, 43/43 asserções, 3/3 testes nas duas |

### Problemas encontrados durante a etapa

- **Porta 5432 ocupada** pelo container `docker_svc-db_1`, de outro projeto do usuário. A IA não o
  parou nem o alterou. Tornou a porta do Postgres configurável (`POSTGRES_PORT`, padrão 5432) e usou
  5433 no `.env` local.
- **Expectativa errada no plano** ("Carlos vê 4 objetivos"): registrada como SI-002.
- **Script de documentação via heredoc** falhou no shell por causa das crases e aspas. Foi refeito
  como arquivo Python no scratchpad. Nenhuma alteração parcial ficou nos arquivos, porque o script
  não chegou a rodar.
- **Sessão interrompida pelo usuário** durante a documentação e retomada com
  "continue de onde foi parado".

### O que não foi feito nesta etapa

- Teste de concorrência real (dois donos saindo ao mesmo tempo): a proteção é o lock pessimista,
  sem teste dedicado.
- Registro e estorno de contribuições, e a restrição "quem não contribui não registra" (P-008).
- Sem push nem PR (não solicitados).
