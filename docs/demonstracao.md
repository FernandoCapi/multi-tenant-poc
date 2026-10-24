# Roteiro de demonstração

Guia para apresentar a POC Vesta: os fluxos principais na interface, os cenários negativos N-1 a N-9
(D-10) e onde cada garantia é comprovada por teste automatizado. Pré-requisitos e solução de
problemas estão em [setup-local.md](setup-local.md).

## 1. Preparação (5 minutos antes)

```bash
cp .env.example .env              # se ainda não existir
docker compose down -v            # ATENÇÃO: apaga os dados locais, para começar com os dados demo limpos
docker compose up -d --build      # PostgreSQL + backend (perfil local) + frontend
docker compose ps                 # postgres e backend "healthy", frontend "Up"
```

- Interface: `http://localhost:3000` (ou a porta de `FRONTEND_PORT`).
- Senha de todos os usuários demo: `vesta-demo`.
- Deixe abertos: o navegador, um terminal na raiz do repositório e o Bruno (pasta `bruno/`, ambiente `local`).

| Usuário | E-mail | Papel na demonstração |
|---|---|---|
| Carlos | `carlos@silva.demo` | Pai, dono da Viagem, do Aluguel e da Parcela do carro |
| Pedro | `pedro@silva.demo` | Filho, membro da Viagem, sem acesso ao Aluguel |
| Ana | `ana@silva.demo` | Mãe, dona da Parcela do carro sem contribuir |
| Marcos | `marcos@souza.demo` | Outra família (Souza) |

## 2. Fluxo principal na interface

1. **Login do Carlos.** O topo mostra "Carlos · Família Silva", e a família vem do token, não da tela.
2. **Lista de objetivos:** Viagem, Aluguel e Parcela do carro, cada um com barra de progresso. O
   Videogame do Pedro não aparece, porque é individual e o Carlos não tem vínculo com ele.
3. **Viagem de férias:** progresso (R$ 3.450,00 de R$ 12.000,00), total por membro e contribuições,
   incluindo uma estornada (riscada, com motivo).
4. **Registrar contribuição em nome do Pedro:** o campo Valor formata em BRL durante a digitação, e a
   data não aceita dias futuros. A linha nova mostra contribuinte = Pedro e registrado por = Carlos (D-03).
5. **Estornar essa contribuição:** o motivo é obrigatório. A linha fica riscada e o acumulado volta.
6. **Aluguel:** a meta está atingida (R$ 2.500,00), mas o status continua Ativo, porque concluir é manual (D-08).
7. *(Opcional)* **Novo objetivo** com membros escolhidos (papel e "contribui").

## 3. Cenários negativos (D-10)

| # | Cenário | Como demonstrar ao vivo | Teste automatizado |
|---|---|---|---|
| N-1 | Outra família usa o id de um objetivo | Bruno `goals/Marcos acessa a Viagem da Silva` → 404 | `GoalAuthorizationIntegrationTest`, `ContributionAuthorizationIntegrationTest`, `RowLevelSecurityTest` |
| N-2 | `family_id` forjado pelo cliente | Bruno `auth/Me com family_id forjado` e `goals/Carlos cria objetivo com family_id forjado` | `MeIntegrationTest`, `GoalListingIntegrationTest`, `RowLevelSecurityTest` (`WITH CHECK`) |
| N-3 | RLS sem `WHERE family_id` | Script SQL (seção 4) | `RowLevelSecurityTest`, `TenantIsolationIntegrationTest` |
| N-4 | Filho × objetivo do aluguel | Interface: Pedro não vê o Aluguel; abrir `/goals/a2000000-0000-0000-0000-000000000002` → "Objetivo não encontrado." | `GoalAuthorizationIntegrationTest`, `GoalListingIntegrationTest`, `GoalDetailPage.test.tsx` |
| N-5 | Membro sem permissão | Interface: Pedro na Viagem não tem botões de dono nem "Estornar" nas contribuições dos outros; Bruno `goals/Pedro edita a Viagem` → 403 | `GoalAuthorizationIntegrationTest`, `ContributionAuthorizationIntegrationTest` |
| N-6 | Quem não contribui tenta registrar | Bruno `contributions/Ana tenta registrar` → 403; na interface, o formulário da Ana na Parcela do carro só oferece o Carlos | `ContributionAuthorizationIntegrationTest`, `GoalDetailPage.test.tsx` |
| N-7 | Contribuição em objetivo concluído | Interface: concluir um objetivo, e o formulário some; Bruno `contributions/Carlos tenta registrar no concluído` → 409 | `ContributionRulesIntegrationTest` |
| N-8 | Sem token, token expirado ou adulterado | Bruno `auth/Me sem token`, `auth/Me com token adulterado` → 401 | `MeIntegrationTest` (inclui token expirado e assinado com outra chave) |
| N-9 | Senha errada × e-mail inexistente | Interface: login errado mostra "E-mail ou senha inválidos."; Bruno `auth/Login com senha errada` e `auth/Login com e-mail inexistente` → mesma resposta | `LoginIntegrationTest` |

Para rodar todos os cenários de uma vez:

```bash
cd bruno
npx -y @usebruno/cli@4.3.0 run --env local      # 38 requisições
```

As pastas `goals/` e `contributions/` criam dados de teste. A de objetivos exclui o que cria; a de
contribuições deixa um objetivo arquivado por execução.

## 4. N-3 ao vivo: RLS sem `WHERE`

O script roda consultas **sem filtro de família**, como a role da aplicação, trocando apenas o tenant
da transação:

```bash
docker compose exec -T -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -f - < infra/demo/n3-rls-sem-where.sql
```

(Use o valor de `APP_USER_PASSWORD` do seu `.env`.) O que mostrar na saída:

1. Com o tenant da Silva, `SELECT ... FROM goals` traz só objetivos da Silva.
2. A mesma consulta com o tenant da Souza traz só a Reforma da cozinha.
3. Sem tenant, 0 linhas: a RLS falha fechada.
4. A Souza tentando alterar a Viagem da Silva: `UPDATE 0`.
5. `ALTER TABLE ... DISABLE ROW LEVEL SECURITY` como `app_user`: `must be owner of table goals`.

## 5. Evidências de qualidade

```bash
cd backend && ./mvnw verify          # 160 testes, PostgreSQL 18.6 real via Testcontainers
cd frontend && npm test              # 28 testes
```

- A CI (`.github/workflows/ci.yml`) roda backend e frontend a cada push e PR para a `main`.
- Testes de mutação (alterações temporárias que removem uma proteção e fazem os testes falharem)
  estão registrados no [ADR-0001](adr/0001-estrategia-multitenant.md#evidências-finais).
- O uso de IA está documentado em [docs/ia/](ia/ai-development-report.md), incluindo as sugestões
  incorretas da IA e como foram detectadas.

## 6. O que a POC não faz (por decisão)

- Parcelas com vencimento e despesas recorrentes (D-04); cadastro e convite de usuários (D-11); multimoeda (D-12).
- Usuário em mais de uma família ou troca de família (DT-04).
- Refresh token e revogação imediata: o token vale 60 minutos (ADR-0002).
- Gestão de membros pela interface depois da criação do objetivo: só pela API/Bruno (D-20).
