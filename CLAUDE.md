# CLAUDE.md — instruções persistentes do projeto

**Vesta**: POC de SaaS financeiro multitenant para famílias. Antes de qualquer tarefa, consulte:

- [docs/requisitos.md](docs/requisitos.md) — requisitos, critérios de aceitação, matriz de permissões e política de erros.
- [docs/modelo-de-dominio.md](docs/modelo-de-dominio.md) — glossário, entidades, invariantes e cálculos.
- [docs/decisoes-de-negocio.md](docs/decisoes-de-negocio.md) — decisões D-xx e itens pendentes PV-xx.
- [docs/adr/](docs/adr/) — decisões de arquitetura.

## Idioma (C-01)

- **Português:** interface, mensagens de erro e todo conteúdo exibido ao usuário; documentação.
- **Inglês:** entidades, tabelas, colunas, classes, métodos, variáveis, enums, endpoints e
  comentários no código.
- Use os identificadores do glossário em `docs/modelo-de-dominio.md` (ex.: `Goal`, `GoalMember`,
  `Contribution`, `ContributionReversal`, `OWNER`/`MEMBER`, `ACTIVE`/`COMPLETED`/`ARCHIVED`).

## Stack (DT-01)

Monorepo; backend Java 21 + Spring Boot 4.1.1 (Hibernate 7); Maven 3.10.0 via wrapper;
PostgreSQL 18.6 (somente Docker); Flyway; Testcontainers; frontend React + Vite + TypeScript;
Docker Compose para execução local; Bruno para requisições e cenários. Não troque componentes da
stack sem razão técnica relevante registrada em ADR. Pacote base: `com.vesta`.

## Estrutura e comandos

- `backend/` (Spring Boot), `infra/postgres/init/` (roles), `bruno/`, `docs/`, `docker-compose.yml`.
- Testes: `cd backend && ./mvnw verify` (exige Docker rodando e JDK 21 ativo na sessão).
- Ambiente: `docker compose up -d --build` (exige `.env`, copiado de `.env.example`).
- Não instale nem altere JDK, Maven ou variáveis globais (`JAVA_HOME`, `PATH`) da máquina; ative o
  JDK 21 apenas na sessão do comando (ver `docs/setup-local.md`).
- Migrations novas: toda tabela de negócio precisa de `family_id`, FKs compostas, RLS forçada,
  policy e grants mínimos para `app_user`. O `DatabaseSecurityTest` falha se faltar RLS.

## Regras de segurança (não negociáveis)

- Nunca aceite `family_id`/`tenant_id` vindo do cliente (corpo, query ou cabeçalho). O tenant vem
  sempre do `TenantContext`, preenchido a partir do JWT validado.
- Toda tabela de negócio tem `family_id`, FKs compostas com `family_id` e RLS habilitada e forçada.
- O tenant do banco é definido apenas com `set_config('app.tenant_id', ..., true)` dentro da
  transação; nunca use `SET` de sessão.
- A aplicação conecta com uma role sem superuser e sem BYPASSRLS; o Flyway usa outra role.
- Autorização sempre no backend. Respostas: 404 para inexistente/outra família/sem leitura;
  403 quando o usuário vê o recurso mas não tem permissão (D-09).
- Listagens e totais aplicam o mesmo filtro de acesso do detalhe.
- Valores monetários em decimal exato (BRL, duas casas); nunca ponto flutuante.
- Contribuições e estornos são imutáveis; não crie operações de edição ou exclusão de contribuição.
- Toda regra de isolamento ou permissão nova deve vir com teste automatizado.

## Evidências e honestidade

- Não afirme que um teste, build, captura de prompt ou validação aconteceu sem tê-lo executado
  e lido a saída.
- Ao relatar resultados, informe falhas e etapas puladas.
- Itens PV-xx são provisórios: não os trate como aprovados; peça confirmação ao implementá-los.

## Documentação viva

A cada etapa (prompt P-NNN):

1. Registre o prompt e as respostas relevantes em `docs/ia/historico-de-prompts.md`.
2. Atualize `docs/ia/decisoes-influenciadas-pela-ia.md` com sugestões aceitas, modificadas ou rejeitadas.
3. Registre em `docs/ia/sugestoes-incorretas.md` toda sugestão da IA que se mostrou errada, com evidência.
4. Atualize `docs/ia/ai-development-report.md`.
5. Mantenha requisitos, modelo de domínio, decisões, ADRs e README coerentes com o código e os testes.

## Git

- Crie commits, faça push e abra PRs **somente quando o usuário pedir**.
- Branch principal: `main`. Cada feature ou escopo de mudança tem sua própria branch, criada a
  partir da `main` (ex.: `docs/...`, `feat/...`, `fix/...`).
- Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat`, `fix`, `docs`, `test`,
  `refactor`, `chore`, `build`, `ci`), com escopo opcional (ex.: `docs(adr): ...`).
- Commits atômicos: cada commit contém uma única mudança coerente e deixa o repositório consistente.
- Não adicione `Co-Authored-By` nem outras linhas de atribuição nas mensagens de commit.
