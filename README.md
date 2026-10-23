# Vesta

POC de um SaaS financeiro multitenant para famílias: objetivos financeiros individuais e
compartilhados, contribuições por membro e acompanhamento de progresso, com isolamento entre
famílias (tenants) e controle de acesso por objetivo dentro de cada família.

A POC também documenta como o Claude Code apoiou análise, arquitetura, implementação, testes,
revisão e documentação.

> **Estado atual (P-007):** fundação, autenticação e objetivos prontos: esquema com Row-Level
> Security forçada, login com JWT, tenant vindo do token, API de objetivos e membros com autorização
> por objetivo, progresso financeiro (só leitura), duas famílias de demonstração e testes
> automatizados de isolamento. Ainda não há registro de contribuições (P-008) nem frontend (P-009).

## API disponível

| Método e rota | Acesso | Descrição |
|---|---|---|
| `POST /api/auth/login` | público | `{email, password}` → `{accessToken, tokenType, expiresIn}` |
| `GET /api/me` | token | Usuário logado e sua família |
| `GET /api/goals?status=` | token | Objetivos em que o usuário tem vínculo, com progresso; sem `status`, exclui arquivados |
| `POST /api/goals` | token | Cria objetivo (o criador vira dono); `members` opcional |
| `GET /api/goals/{id}` | vinculados | Detalhe, membros, progresso e total por membro |
| `PATCH /api/goals/{id}` | dono, só ATIVO | Título, descrição, valor-alvo, data-limite |
| `POST /api/goals/{id}/complete` · `reopen` · `archive` · `unarchive` | dono | Transições de status (D-14) |
| `DELETE /api/goals/{id}` | dono | Só objetivos sem contribuições |
| `POST /api/goals/{id}/members` | dono | Adiciona membro da família (`userId`, `role`, `contributes`) |
| `PATCH /api/goals/{id}/members/{userId}` | dono | Papel e/ou participação financeira |
| `DELETE /api/goals/{id}/members/{userId}` | dono, ou o próprio membro (sair) | O último dono não sai |
| `GET /api/family/members` | token | Membros da família do usuário |
| `GET /actuator/health` | público | Saúde da aplicação |

Erros: 404 para objetivo inexistente, de outra família ou sem vínculo; 403 para quem vê mas não pode;
409 para conflito de estado; 422 para dados inválidos.

Usuários de demonstração (senha `vesta-demo`) em [docs/setup-local.md](docs/setup-local.md#usuários-de-demonstração).

## Stack

| Componente | Versão |
|---|---|
| Java | 21 (Temurin) |
| Spring Boot | 4.1.1 (Spring Framework 7, Hibernate 7) |
| Maven | 3.10.0, via Maven Wrapper |
| PostgreSQL | 18.6 (Docker) |
| Flyway | migrations do esquema |
| Testcontainers | testes de integração com PostgreSQL real |
| Docker Compose | ambiente local |
| Bruno | coleção de requisições |
| React + Vite + TypeScript | frontend (etapa futura) |

## Estrutura

```
backend/                 API Spring Boot (pacote com.vesta)
  src/main/java/com/vesta/auth/     login, emissão e validação do JWT, configuração de segurança
  src/main/java/com/vesta/tenant/   TenantContext, filtro que o preenche pelo token e vínculo à transação
  src/main/java/com/vesta/user/     entidade User e /api/me
  src/main/java/com/vesta/family/   entidade Family
  src/main/java/com/vesta/goal/     objetivos, membros, autorização por objetivo e progresso
  src/main/resources/db/migration/  migrations Flyway (esquema, RLS, privilégios, lookup de login)
  src/main/resources/db/seed/       famílias de demonstração (só no perfil local)
  src/test/java/com/vesta/          testes de isolamento, autenticação e dados demo
infra/postgres/init/     criação das roles app_owner, app_user e app_auth
bruno/                   coleção Bruno
docs/                    requisitos, modelo de domínio, decisões, ADRs e registros de IA
docker-compose.yml       PostgreSQL + backend
```

## Arquitetura de isolamento

O isolamento entre famílias não depende de uma única camada ([ADR-0001](docs/adr/0001-estrategia-multitenant.md)):

1. **Aplicação:** a família vem só do JWT assinado no login ([ADR-0002](docs/adr/0002-autenticacao-jwt.md)).
   O `TenantContextFilter` a coloca no `TenantContext` durante a requisição; valores enviados pelo
   cliente são ignorados.
2. **Persistência:** o Hibernate (`@TenantId`) filtra e preenche `family_id` nas entidades.
3. **Banco:** cada transação define `app.tenant_id` com `set_config(..., true)`, e a RLS forçada
   restringe todas as tabelas, inclusive em SQL nativo. A aplicação usa a role `app_user`, que não é
   superuser, não tem BYPASSRLS e não é dona das tabelas. As migrations rodam como `app_owner`.
4. **Integridade:** chaves estrangeiras compostas com `family_id` impedem referências entre famílias.
   O login encontra o usuário antes de existir tenant por uma única função `SECURITY DEFINER`; a
   aplicação não lê hashes de senha.
5. **Testes:** a suíte prova cada uma dessas garantias.

## Como executar

Passo a passo completo, incluindo pré-requisitos, em [docs/setup-local.md](docs/setup-local.md).

```bash
cp .env.example .env
docker compose up -d --build                  # PostgreSQL + backend (perfil local, com dados demo)
curl http://localhost:8080/actuator/health    # {"status":"UP",...}
curl -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"carlos@silva.demo","password":"vesta-demo"}'

cd backend && ./mvnw verify                   # testes (exige Docker em execução e JDK 21)
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [Setup local](docs/setup-local.md) | Pré-requisitos, execução, testes e solução de problemas |
| [Requisitos](docs/requisitos.md) | Requisitos funcionais e de segurança, critérios de aceitação, matriz de permissões, cenários de demonstração |
| [Modelo de domínio](docs/modelo-de-dominio.md) | Glossário PT → código, entidades, invariantes, ciclo de vida e cálculos |
| [Decisões de negócio](docs/decisoes-de-negocio.md) | Decisões D-xx, convenções e itens pendentes |
| [ADR-0001 — Estratégia multitenant](docs/adr/0001-estrategia-multitenant.md) | Isolamento por `family_id`, TenantContext, RLS, lookup de login, alternativas, riscos e evidências |
| [ADR-0002 — Autenticação com JWT](docs/adr/0002-autenticacao-jwt.md) | HS256, validade, claims, alternativas e consequências |
| [Histórico de prompts](docs/ia/historico-de-prompts.md) | Prompts usados no desenvolvimento |
| [Decisões influenciadas pela IA](docs/ia/decisoes-influenciadas-pela-ia.md) | Sugestões da IA aceitas e rejeitadas |
| [Sugestões incorretas da IA](docs/ia/sugestoes-incorretas.md) | Erros da IA com evidência e aprendizado |
| [AI Development Report](docs/ia/ai-development-report.md) | Relatório cumulativo do uso de IA |
| [CLAUDE.md](CLAUDE.md) | Instruções persistentes para o Claude Code |
