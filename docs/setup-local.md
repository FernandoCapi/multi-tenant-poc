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

O `.env` (fora do Git) define as senhas locais:

| Variável | Uso |
|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Superusuário de bootstrap. Só é usado pelo entrypoint da imagem para criar as roles. |
| `APP_OWNER_PASSWORD` | Role `app_owner`, dona do schema, usada pelo Flyway. |
| `APP_USER_PASSWORD` | Role `app_user`, usada pelo backend. Não é superuser, não tem BYPASSRLS e não é dona das tabelas. |

As roles são criadas por `infra/postgres/init/01-create-roles.sh` **apenas na primeira inicialização**
do volume.

## Subir o ambiente

```bash
docker compose up -d --build
docker compose ps                              # postgres e backend "healthy"
curl http://localhost:8080/actuator/health     # {"status":"UP",...}
docker compose logs backend | grep -i flyway   # migrations V1..V3 aplicadas
```

Só o banco: `docker compose up -d postgres`.

Para rodar o backend fora do Docker, com o banco do Compose:

```bash
cd backend
DB_APP_PASSWORD=app-user-dev DB_OWNER_PASSWORD=app-owner-dev ./mvnw spring-boot:run
```

## Testes

```bash
cd backend
./mvnw verify
```

Os testes sobem um PostgreSQL 18.6 próprio via Testcontainers, com o mesmo script de roles do
Compose. Não dependem do banco do Compose, mas exigem o Docker rodando.

| Classe | O que demonstra |
|---|---|
| `DatabaseSecurityTest` | `app_user` sem superuser/BYPASSRLS e sem tabelas próprias; toda tabela com RLS forçada e policy; `app_user` não consegue desligar a RLS. |
| `RowLevelSecurityTest` | SQL sem `WHERE family_id` só vê a família da transação; sem tenant, nada é visível; inserção com `family_id` de outra família é bloqueada; FK composta impede referência cruzada; contribuições e estornos são imutáveis; no máximo um estorno por contribuição. |
| `TenantIsolationIntegrationTest` | Pelo Spring: repositório e SQL nativo isolados por família; nenhum vazamento entre transações na mesma conexão do pool; sem tenant, leitura vazia e escrita rejeitada. |

## Inspecionar o banco

```bash
# Como a role da aplicação: sem tenant na transação, nenhuma linha é visível
docker compose exec -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -c "select count(*) from goals"

# A role da aplicação não consegue desligar a RLS
docker compose exec -e PGPASSWORD=app-user-dev postgres \
  psql -h localhost -U app_user -d vesta -c "alter table goals disable row level security"
```

Ainda não há dados de demonstração; os seeds entram junto com a autenticação.

## Coleção Bruno

Abra a pasta `bruno/` no Bruno e selecione o ambiente `local`, ou rode pela CLI:

```bash
cd bruno
npx -y @usebruno/cli@4.3.0 run --env local
```

## Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| Testes falham com erro de conexão ao Docker | Docker Desktop parado | Iniciar o Docker e repetir. |
| `password authentication failed for user "app_user"` | O volume foi criado com outras senhas; o script de roles roda só na primeira inicialização | `docker compose down -v` (apaga os dados locais) e `docker compose up -d`. |
| `java -version` mostra 8 ou 17 | JDK 21 não ativado neste terminal | Ativar o JDK 21 na sessão (seção acima). |
| Porta 5432 ou 8080 ocupada | Outro serviço local | Liberar a porta ou ajustar o mapeamento em `docker-compose.yml`. |
