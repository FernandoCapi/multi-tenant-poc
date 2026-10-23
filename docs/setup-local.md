# Setup do ambiente local

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 21 (Temurin 21.0.12.1+1 usado no desenvolvimento) | Só para build e testes fora do Docker. |
| Docker + Docker Compose | Docker 29.x, Compose v5.x | O daemon precisa estar rodando: o Compose e os testes (Testcontainers) dependem dele. |
| Maven | não é necessário instalar | O projeto usa o Maven Wrapper (`./mvnw`), fixado no Maven 3.10.0. |
| Node.js | 20.19+ | Opcional, só para rodar a coleção Bruno pela CLI. |

O PostgreSQL roda **apenas em container** (`postgres:18.6`). Não é preciso instalá-lo na máquina.

### JDK 21 sem alterar a configuração global

Se a máquina já tem outro JDK configurado globalmente para outros projetos, **não altere**
`JAVA_HOME` nem o `PATH` permanentes. Ative o JDK 21 só no terminal em uso:

```powershell
# PowerShell
$env:JAVA_HOME = "<pasta do JDK 21>"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version   # deve mostrar 21
```

```bash
# Git Bash
export JAVA_HOME="<pasta do JDK 21>"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Configuração

```bash
cp .env.example .env
```

O `.env` (fora do Git) define os segredos locais:

| Variável | Uso |
|---|---|
| `POSTGRES_PORT` | Porta do Postgres no host (padrão 5432). Mude se outra base local já usar a 5432; o backend fora do Docker usa a mesma variável. |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Superusuário de bootstrap. Só é usado pelo entrypoint da imagem para criar as roles. |
| `APP_OWNER_PASSWORD` | Role `app_owner`, dona do schema, usada pelo Flyway. |
| `APP_USER_PASSWORD` | Role `app_user`, usada pelo backend. Não é superuser, não tem BYPASSRLS e não é dona das tabelas. |
| `JWT_SECRET` | Segredo HS256 que assina os tokens, com no mínimo 32 bytes ([ADR-0002](adr/0002-autenticacao-jwt.md)). O backend não sobe sem ele. |

As roles (`app_owner`, `app_user` e `app_auth`, a role sem login dona da função de lookup de login)
são criadas por `infra/postgres/init/01-create-roles.sh` **apenas na primeira inicialização** do
volume.

> **Atualizando de uma versão anterior à P-006?** A role `app_auth` é nova, e o script só roda num
> volume vazio. Recrie o volume local (apaga os dados locais): `docker compose down -v`.

## Subir o ambiente

```bash
docker compose up -d --build
docker compose ps                              # postgres e backend "healthy"
curl http://localhost:8080/actuator/health     # {"status":"UP",...}
docker compose logs backend | grep -i flyway   # V1..V4 e "demo families" aplicadas
```

No Compose, o backend roda com o perfil `local`, que carrega os dados de demonstração.

Só o banco: `docker compose up -d postgres`.

### Usuários de demonstração

Todos os usuários demo têm a senha **`vesta-demo`**. Eles existem só com o perfil `local`
(`backend/src/main/resources/db/seed/R__demo_families.sql`).

| Família | Usuário | E-mail | Objetivos em que participa |
|---|---|---|---|
| Silva | Carlos (pai) | `carlos@silva.demo` | Viagem (dono), Aluguel (dono), Parcela do carro (dono) |
| Silva | Ana (mãe) | `ana@silva.demo` | Viagem (dona), Aluguel (membro), Parcela do carro (dona, sem contribuir) |
| Silva | Pedro (filho) | `pedro@silva.demo` | Viagem (membro), Videogame (individual, dono) |
| Silva | Júlia (filha) | `julia@silva.demo` | Viagem (membro) |
| Silva | Lucas (filho) | `lucas@silva.demo` | Viagem (membro) |
| Souza | Marcos | `marcos@souza.demo` | Reforma da cozinha (dono) |
| Souza | Beatriz | `beatriz@souza.demo` | Reforma da cozinha (dona) |

Os filhos não participam do objetivo Aluguel: é o cenário "filho × aluguel" da demonstração (N-4).
Pedro recebe 404 no Aluguel e 403 ao editar a Viagem; Marcos recebe 404 em qualquer objetivo da Silva.

```bash
# Login e dados do usuário logado
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"carlos@silva.demo","password":"vesta-demo"}' | jq -r .accessToken)
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
```

### Backend fora do Docker (IDE ou terminal)

Fora do Docker, o backend lê os segredos do `.env` da raiz do repositório. Funciona tanto com a raiz
quanto com `backend/` como diretório de trabalho, então não é preciso configurar variáveis de
ambiente na IDE.

```bash
docker compose up -d postgres     # só o banco; ou pare o backend do Compose: docker compose stop backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No IntelliJ, execute `VestaBackendApplication` com **Active profiles: `local`** na Run Configuration
para carregar os usuários demo. Sem o perfil, o backend também sobe, mas não aplica os seeds. Se o
container `backend` estiver rodando, ele ocupa a porta 8080: pare-o ou defina `SERVER_PORT=8081`.

Como as propriedades são resolvidas:

| Propriedade | Ordem de busca | Padrão |
|---|---|---|
| Usuário da aplicação | `DB_APP_USER` | `app_user` |
| Senha da aplicação | `DB_APP_PASSWORD` → `APP_USER_PASSWORD` (do `.env`) | vazio |
| Usuário do Flyway | `DB_OWNER_USER` | `app_owner` |
| Senha do Flyway | `DB_OWNER_PASSWORD` → `APP_OWNER_PASSWORD` (do `.env`) | vazio |
| URL | `DB_URL` | `jdbc:postgresql://localhost:${POSTGRES_PORT}/vesta` (porta padrão 5432) |
| Segredo do JWT | `JWT_SECRET` (ambiente ou `.env`) | vazio: o backend não sobe |

Variáveis de ambiente reais têm precedência sobre o `.env`; é assim que o Docker Compose configura
o container. As variáveis `POSTGRES_*` não são lidas pelo backend: elas servem só para criar o
banco na imagem do Postgres. Nunca configure a aplicação para conectar como `app_owner` ou como
superusuário, porque isso desligaria proteções do ADR-0001.

## Testes

```bash
cd backend
./mvnw verify
```

Os testes sobem um PostgreSQL 18.6 próprio via Testcontainers, com o mesmo script de roles do
Compose. Não dependem do banco do Compose nem do `.env`, mas exigem o Docker rodando.

| Classe | O que demonstra |
|---|---|
| `DatabaseSecurityTest` | `app_user` sem superuser/BYPASSRLS e sem tabelas próprias; toda tabela com RLS forçada e policy; toda função `SECURITY DEFINER` em lista explícita e com `search_path` fixo; `app_user` não consegue desligar a RLS. |
| `RowLevelSecurityTest` | SQL sem `WHERE family_id` só vê a família da transação; sem tenant, nada é visível; inserção com `family_id` de outra família é bloqueada; FK composta impede referência cruzada; contribuições e estornos são imutáveis; no máximo um estorno por contribuição. |
| `LoginCredentialsDatabaseTest` | Lookup de login sem tenant pela função `SECURITY DEFINER`; `app_user` não lê hashes de senha; atributos da função e da role `app_auth`. |
| `TenantIsolationIntegrationTest` | Pelo Spring: repositório e SQL nativo isolados por família; nenhum vazamento entre transações na mesma conexão do pool; sem tenant, leitura vazia e escrita rejeitada. |
| `LoginIntegrationTest` | Login devolve token com usuário e família; e-mail inexistente e senha errada recebem a mesma resposta; dados em branco → 422. |
| `MeIntegrationTest` | `/api/me` usa só a família do token (`family_id` do cliente é ignorado); sem token, expirado, adulterado, com outra chave ou sem `family_id` → 401. |
| `TenantContextFilterTest` | Tenant do token só durante a requisição, limpo mesmo com erro. |
| `DemoDataIntegrationTest` | O perfil `local` carrega as famílias demo, e seus usuários fazem login. |
| `GoalAuthorizationIntegrationTest` | Matriz de permissões por objetivo: dono, membro, membro sem contribuir, sem vínculo e outra família (404/403). |
| `GoalListingIntegrationTest` | Listagem só com vínculo, arquivados com filtro, `familyId` forjado ignorado na criação. |
| `GoalLifecycleIntegrationTest` | Transições de status, edição só em ATIVO, exclusão e alteração concorrente. |
| `GoalMembershipIntegrationTest` | Gestão de membros, invariante do último dono e saída do objetivo. |
| `GoalProgressIntegrationTest` / `GoalProgressTest` | Acumulado sem estornos, restante, excedente, percentual e total por membro. |

## Inspecionar o banco

```bash
# Como a role da aplicação: sem tenant na transação, nenhuma linha é visível
docker compose exec -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -c "select count(*) from goals"

# Com o tenant da Família Silva na transação, só os objetivos dela aparecem
docker compose exec -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -c "begin" \
  -c "select set_config('app.tenant_id', 'a0000000-0000-0000-0000-000000000001', true)" \
  -c "select title from goals" -c "commit"

# A role da aplicação não consegue desligar a RLS nem ler hashes de senha
docker compose exec -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -c "alter table goals disable row level security"
docker compose exec -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -c "select password_hash from users"
```

## Coleção Bruno

Abra a pasta `bruno/` no Bruno e selecione o ambiente `local`, ou rode pela CLI (com o Compose no ar):

```bash
cd bruno
npx -y @usebruno/cli@4.3.0 run --env local
```

A pasta `auth/` cobre login, `/api/me`, `family_id` forjado (N-2), token ausente ou adulterado (N-8)
e falhas de login com resposta idêntica (N-9). A pasta `goals/` cobre listagem, filho × aluguel (N-4),
membro sem permissão (N-5), outra família (N-1), criação com `familyId` forjado, membros e
transições; o objetivo criado é excluído no fim, então a coleção pode ser repetida.

## Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| Testes falham com erro de conexão ao Docker | Docker Desktop parado | Iniciar o Docker e repetir. |
| `password authentication failed for user "app_user"` | O volume foi criado com outras senhas; o script de roles roda só na primeira inicialização | `docker compose down -v` (apaga os dados locais) e `docker compose up -d`. |
| Flyway falha na V4 (`V4__add_login_credentials`) por falta da role `app_auth` | Volume criado antes da P-006 | `docker compose down -v` e `docker compose up -d --build`. |
| `vesta.jwt.secret (JWT_SECRET) must have at least 32 bytes` | Falta `JWT_SECRET` no `.env`, ou o valor é curto | Copiar a linha de `JWT_SECRET` do `.env.example`. |
| `The server requested SCRAM-based authentication, but the password is an empty string` | O backend não encontrou a senha: falta o `.env` na raiz do repositório, ou a IDE usa outro diretório de trabalho | Criar o `.env` (`cp .env.example .env`) e usar a raiz ou `backend/` como diretório de trabalho, ou definir `DB_APP_PASSWORD`/`DB_OWNER_PASSWORD`. |
| Login demo devolve 401 | Backend rodando sem o perfil `local` num banco sem seed | Rodar com o perfil `local` ao menos uma vez. |
| `java -version` mostra 8 ou 17 | JDK 21 não ativado neste terminal | Ativar o JDK 21 na sessão (seção acima). |
| `Bind for 0.0.0.0:5432 failed: port is already allocated` | Outra base local já usa a 5432 | Definir `POSTGRES_PORT=5433` (ou outra livre) no `.env`. |
| Porta 8080 ocupada | Outro serviço local | Liberar a porta ou ajustar o mapeamento em `docker-compose.yml`. |
