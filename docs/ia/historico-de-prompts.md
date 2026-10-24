# Histórico de prompts

> Registro cronológico dos prompts usados no desenvolvimento com o Claude Code.
> Cada entrada traz o prompt (ou referência a ele), as respostas relevantes do usuário e os artefatos
> produzidos. Só é registrado o que de fato aconteceu na sessão.

## Índice

| ID | Data | Objetivo | Artefatos |
|---|---|---|---|
| P-001 | 2026-10-21 | Levantamento de requisitos e decisões de negócio | `docs/requisitos.md`, `docs/modelo-de-dominio.md`, `docs/decisoes-de-negocio.md`, `docs/adr/0001-estrategia-multitenant.md`, `docs/ia/*`, `CLAUDE.md`, `README.md` |
| P-002 | 2026-10-22 | Aprovação da documentação, convenções de Git e commits | `CLAUDE.md` (seção Git), commits na branch `docs/p-001-especificacao-inicial` |
| P-003 | 2026-10-22 | Estrutura do monorepo, Docker Compose, banco com RLS e primeiros testes de isolamento | `backend/`, `infra/`, `bruno/`, `docker-compose.yml`, `docs/setup-local.md`, ADR-0001 (complemento), commits na branch `feat/p-003-fundacao-rls` |
| P-004 | 2026-10-22 | Ajustes do ambiente local: repositório local, credenciais e leitura do `.env` | `backend/src/main/resources/application.yml`, `docs/setup-local.md` |
| P-005 | 2026-10-22 | Avisos de inspeção na entidade `Goal` | `backend/src/main/java/com/vesta/goal/Goal.java` |
| P-006 | 2026-10-22 | Planejamento das próximas etapas e implementação da autenticação, do tenant vindo do JWT e dos seeds | `backend/` (auth, tenant, user, family, V4, seed), `infra/postgres/init`, `bruno/auth`, ADR-0001, ADR-0002, `docs/setup-local.md`, README |
| P-007 | 2026-10-23 | Objetivos, membros, autorização por objetivo e progresso (só leitura) | `backend/` (goal, family, V5), `bruno/goals`, `docker-compose.yml` (porta configurável), decisões D-14..D-16, ADR-0001, README, setup |
| P-008 | 2026-10-23 | Contribuições e estornos (registro, estorno, listagem paginada) | `backend/` (contribution), `bruno/contributions`, decisões D-17..D-19, ADR-0001, README, setup |
| P-009 | 2026-10-23 | Frontend de demonstração (React + Vite + TS) | `frontend/`, `docker-compose.yml` (serviço frontend), D-20, T-02, README, setup |

---

## P-001 — Definição dos requisitos da POC

- **Data:** 2026-10-21
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5, em modo de planejamento (plan mode).
- **Branch:** `FernandoCapi/pelican`

### Fluxo da interação

1. O usuário enviou o prompt abaixo, pedindo análise, consolidação de requisitos e perguntas, sem código.
2. A IA verificou o repositório (apenas `README.md` com título) e entregou: resumo da POC, requisitos
   consolidados, decisões estabelecidas, ambiguidades, questões pendentes, premissas e riscos.
3. Três lotes de perguntas de escolha, cada um com opções, trade-offs e recomendação inicial.
4. A IA propôs um plano de documentação. Na primeira submissão do plano, o usuário o rejeitou para
   acrescentar a convenção de idioma (C-01). O plano revisado foi aprovado.
5. A IA produziu os documentos listados no índice.

### Respostas do usuário

**Lote 1**

| Pergunta | Resposta |
|---|---|
| Modelo de permissões | Somente ACL por objetivo (recomendação da IA não adotada). Texto do usuário: *"Somente ACL por objetivo. Podemos isolar baseado em quem criou o objetivo, ter opção de selecionar membros da familia participantes, ou manter somente para si mesmo. No exemplo do aluguel, o pai pode ter criado e adicionado somente a mãe, de forma que as crianças não terão acesso. Temos que ter permissão de dono do objetivo e de membro/contribuinte, que será decidido quando adicionar ao objetivo e pode ser editado pelo dono, definiria permissões mais sérias como editar e deletar registros"* → D-01 |
| Natureza da contribuição | Realizado, imutável, com estorno → D-02 |
| Quem registra | Próprio ou gestor → D-03 |
| Parcelas e recorrência | Tudo é meta com valor-alvo → D-04 |

**Lote 2**

| Pergunta | Resposta |
|---|---|
| Acesso × participação (RF-03) | Papel + flag `contribui` → D-05 |
| Número de donos | Vários donos, mínimo 1 → D-06 |
| Permissões do membro | Estorna a própria e pode sair → D-07 |
| Ciclo de vida | Ativo/Concluído/Arquivado → D-08 |

**Lote 3**

| Pergunta | Resposta |
|---|---|
| 403 × 404 | 404 sem leitura, 403 com leitura → D-09 |
| Cenários negativos ao vivo | Os quatro: entre famílias, filho × aluguel, membro sem permissão, autenticação inválida → D-10 |
| Origem dos usuários | Somente seeds → D-11 |
| Valores | BRL, aceita excedente → D-12 |

**Revisão do plano**

> *"Em questão de código, tudo relacionado a interface e conteúdo apresentado ao usuário será em
> português, porém, nome de entidades, classes, métodos e comentários dentro do código devem ser
> todos em inglês"* → C-01

### Prompt integral

<details>
<summary>P-001 — texto enviado pelo usuário</summary>

```text
P-001 — Definição dos requisitos da POC de SaaS financeiro multitenant
1. Contexto do projeto
Quero construir uma POC de um SaaS financeiro voltado para famílias que desejam organizar objetivos financeiros, acompanhar pagamentos e contabilizar contribuições individuais e coletivas.
O projeto será desenvolvido com apoio do Claude Code. Além de demonstrar a aplicação funcionando, a POC deverá demonstrar como a IA pode apoiar a análise, as decisões arquiteturais, a implementação, os testes, a revisão e a documentação de um software.
O repositório GitHub já existe, mas está vazio.
Neste primeiro momento, não implemente código, não crie a estrutura do projeto e não instale dependências. O objetivo é estruturar a ideia, consolidar os requisitos obrigatórios, registrar as decisões iniciais e esclarecer as questões de negócio que ainda estão pendentes.
2. Visão do produto
A aplicação permitirá que uma família organize objetivos financeiros, como:
Juntar dinheiro para uma viagem.
Acompanhar parcelas de um carro.
Organizar o pagamento de despesas recorrentes.
Criar objetivos financeiros individuais para cada membro.
Registrar quanto cada participante contribuiu e quanto ainda falta alcançar.
Cada objetivo terá um valor-alvo e um acompanhamento do seu progresso financeiro.
Uma família pode ter, por exemplo, dois pais e três filhos. Todos podem participar de um objetivo de viagem, enquanto somente os pais poderão visualizar o objetivo relacionado ao pagamento do aluguel.
Portanto, o sistema precisa considerar tanto o isolamento entre famílias quanto as permissões específicas de cada membro dentro da própria família.
3. Requisitos funcionais obrigatórios da POC
A POC deverá contemplar os seguintes requisitos:
RF-01 — Famílias e membros
Cada família será um tenant.
Uma família poderá possuir múltiplos membros.
Os membros deverão ter identidade individual para autenticação e autorização.
O modelo deverá permitir distinguir a família à qual o membro pertence.
A POC começará com a premissa de que cada usuário pertence a uma única família.
RF-02 — Objetivos financeiros
Um membro poderá ter objetivos financeiros individuais.
Deverá ser possível criar objetivos compartilhados por dois ou mais membros.
Nem todos os membros da família precisarão participar de todos os objetivos.
Cada objetivo deverá possuir, no mínimo, identificação, descrição, valor-alvo, status e informações necessárias para acompanhar seu progresso.
A aplicação deverá calcular e apresentar o valor acumulado e o valor restante para alcançar o objetivo.
RF-03 — Participação e visibilidade
A participação em um objetivo deverá ser definida explicitamente.
A aplicação deverá controlar quais membros podem visualizar e quais podem administrar cada objetivo.
A participação financeira e a permissão de acesso deverão ser conceitos distintos.
Os membros não poderão acessar objetivos para os quais não tenham autorização, mesmo quando pertencerem à mesma família.
Listagens, detalhes, totais e demais informações financeiras também deverão respeitar as permissões de acesso.
RF-04 — Contribuições financeiras
Deverá ser possível registrar contribuições vinculadas a objetivos.
O sistema deverá identificar quem realizou cada contribuição.
O progresso deverá ser atualizado de acordo com as regras financeiras definidas para a POC.
As contribuições deverão estar vinculadas ao tenant e ao objetivo correspondente.
O modelo deverá permitir rastrear as contribuições que compõem o total acumulado.
RF-05 — Autenticação
A aplicação deverá possuir login real.
As senhas deverão ser armazenadas com hash adequado, nunca em texto puro.
A autenticação utilizará JWT.
As rotas de negócio deverão exigir autenticação e um contexto de tenant válido.
RF-06 — Isolamento e autorização
Um membro de uma família nunca poderá consultar ou modificar dados pertencentes a outra família.
A autorização deverá ser aplicada no backend, não apenas na interface.
A aplicação deverá proteger operações de leitura, criação, atualização, exclusão, listagem e agregação financeira.
Para recursos inexistentes ou aos quais o usuário não tem acesso, a estratégia preferencial será retornar 404 Not Found, evitando revelar a existência de recursos protegidos.
A POC deverá demonstrar, por meio de testes automatizados, que as proteções funcionam.
RF-07 — Interface de demonstração
O frontend será uma interface mínima para demonstrar os principais fluxos.
Deverá permitir apresentar autenticação, visualização dos objetivos autorizados, acompanhamento do progresso e registro de contribuições.
A interface não precisa ter acabamento visual de produto comercial, mas deve possibilitar demonstrar os requisitos funcionais e de segurança.
4. Decisões técnicas iniciais já estabelecidas
As seguintes decisões são premissas do projeto e não precisam ser rediscutidas sem que seja identificada uma razão técnica relevante.
DT-01 — Stack
Monorepo hospedado no GitHub.
Backend em Java com Spring Boot 3.
Banco de dados PostgreSQL.
Frontend em React, Vite e TypeScript.
Flyway para migrations.
Docker Compose para execução local.
Bruno para organizar requisições e cenários de teste.
Claude Code como ferramenta de apoio ao desenvolvimento.
DT-02 — Estratégia multitenant
Tabelas compartilhadas entre os tenants.
family_id nas entidades de negócio pertencentes a uma família.
Índices compostos iniciados por family_id, respeitando os padrões de acesso.
Restrições de integridade para evitar referências cruzadas entre famílias, quando aplicável.
DT-03 — Defesa em profundidade
O isolamento não deverá depender de uma única camada.
A estratégia inicial deverá combinar:
Contexto de tenant na aplicação, por meio de um TenantContext associado ao ciclo de vida da requisição.
Mecanismos de persistência que reforcem o isolamento em leituras e escritas.
Row-Level Security (RLS) no PostgreSQL.
Testes automatizados que demonstrem o isolamento efetivo.
O contexto do banco será definido com SET LOCAL app.tenant_id dentro da transação. A role utilizada pela aplicação não deverá possuir superuser nem BYPASSRLS. Deverá ser avaliada a utilização de FORCE ROW LEVEL SECURITY.
A implementação também deverá considerar o comportamento do pool de conexões, o gerenciamento transacional do Spring e os riscos de contexto ausente ou incorreto.
DT-04 — Resolução do tenant
Na POC:
Um usuário estará associado a uma única família.
Não haverá troca de família ou de contexto de tenant.
O tenant será resolvido pelo backend durante o login.
O JWT poderá conter o identificador do tenant resolvido pelo servidor.
O sistema não confiará cegamente em family_id ou tenant_id recebido do cliente.
A alternativa de resolver o tenant a partir do identificador do usuário autenticado (sub) e consultar sua associação à família durante cada requisição deverá ser documentada no ADR, com seus benefícios, custos e implicações.
DT-05 — Testes e dados de demonstração
Utilizar Flyway para versionamento do esquema.
Criar dados de demonstração para pelo menos duas famílias.
Desenvolver testes automatizados de isolamento entre tenants.
Incluir um cenário em que a RLS impeça o acesso indevido mesmo sem um WHERE family_id = ... explícito na consulta da aplicação.
Testar também as permissões entre membros de uma mesma família.
Organizar uma coleção Bruno com cenários positivos e negativos.
DT-06 — Ecossistema e documentação
O projeto deverá possuir:
README com arquitetura, estrutura, decisões relevantes, configuração e execução.
ADR para a estratégia de tenant, alternativas consideradas e respectivos trade-offs.
Documentação de setup do ambiente local.
Instruções persistentes para o Claude Code.
Histórico dos prompts utilizados durante o desenvolvimento.
Registro das decisões arquiteturais influenciadas pela IA.
Registro das sugestões incorretas ou inadequadas da IA, incluindo identificação do problema, evidências, decisão tomada e aprendizado.
AI Development Report cumulativo, atualizado durante o projeto.
Documentação viva, mantida conforme código, testes e decisões evoluírem.
Não devemos afirmar que uma atividade, teste, captura de prompt ou validação aconteceu sem evidência de que foi realizada.
5. Objetivo desta etapa: esclarecer as decisões pendentes
Analise os requisitos acima e identifique quais decisões de negócio ainda precisam ser definidas antes de iniciarmos a implementação.
Faça perguntas objetivas, priorizadas pela importância para o modelo de domínio, para as regras financeiras e para a segurança.
Considere especialmente os seguintes temas:
Diferença entre proprietário, administrador, participante e membro com acesso de leitura.
Criação de objetivos pessoais e compartilhados.
Quem poderá criar, editar, arquivar e excluir objetivos.
Como um objetivo passa a ser visível para determinados membros.
Como os membros entram ou saem de um objetivo.
O que exatamente significa uma contribuição financeira na POC.
Se o registro representa dinheiro efetivamente recebido, um pagamento realizado ou apenas um compromisso de contribuição.
Como tratar correções, cancelamentos e exclusões de contribuições.
Como calcular o progresso e o valor restante.
Como representar objetivos concluídos, cancelados ou reabertos.
Qual moeda será utilizada e se haverá suporte a valores decimais.
Se metas possuem data-limite.
Quais fluxos precisam estar demonstráveis na interface.
Quais cenários negativos deverão obrigatoriamente ser demonstrados na apresentação da POC.
Esses tópicos são exemplos de áreas que precisam ser avaliadas. Não presuma que todas precisam necessariamente de uma regra complexa. Prefira o comportamento mais simples que atenda aos objetivos da POC sem comprometer segurança, consistência financeira ou capacidade de evolução.
6. Como conduzir o levantamento
Siga este procedimento:
Consolide os requisitos fornecidos, separando requisitos funcionais, requisitos de segurança, decisões técnicas já estabelecidas e questões pendentes.
Identifique ambiguidades, contradições e dependências entre decisões.
Separe questões bloqueadoras para a definição do domínio de questões que podem ser adiadas sem comprometer a POC.
Faça as perguntas necessárias ao usuário. Agrupe perguntas relacionadas e explique brevemente por que cada decisão importa.
Sempre que houver alternativas relevantes, apresente opções claras, seus principais trade-offs e uma recomendação inicial, sem tratar a recomendação como uma decisão já aprovada.
Evite perguntar novamente sobre decisões técnicas já estabelecidas.
Não invente requisitos para preencher lacunas.
Não faça perguntas sobre detalhes de implementação que dependam de decisões de negócio ainda não tomadas.
Não gere uma lista excessivamente extensa de questões de baixo impacto. Priorize as decisões que podem alterar o domínio, as permissões, os cálculos financeiros ou a arquitetura.
Quando uma decisão puder ser adiada de forma segura, registre-a como pendente em vez de bloquear desnecessariamente o projeto.
Organize as perguntas em lotes pequenos e coerentes. Comece pelas decisões de negócio mais importantes e aguarde minhas respostas antes de avançar para novas questões dependentes delas.
7. Entregáveis desta primeira interação
Na sua primeira resposta, apresente:
Resumo da POC: uma descrição objetiva do produto e do problema que ele resolve.
Requisitos consolidados: requisitos funcionais e de segurança identificados, com critérios iniciais de aceitação quando possível.
Decisões já estabelecidas: lista das decisões iniciais que deverão ser preservadas.
Questões pendentes: agrupadas por domínio, autorização, contribuições financeiras, experiência de demonstração e outros temas relevantes.
Primeiro lote de perguntas: somente as perguntas prioritárias para as quais preciso tomar uma decisão agora.
Premissas e riscos: hipóteses identificadas, riscos de segurança e pontos que poderão exigir validação técnica.
Não implemente código nem crie arquivos nesta interação. Primeiro, vamos fechar as decisões de negócio essenciais e consolidar o escopo da POC.
Depois que as respostas forem obtidas, prepararemos a especificação inicial de requisitos, o modelo de domínio, os critérios de aceitação e os registros de decisões necessários para orientar a implementação incremental.
```

</details>

---

## P-002 — Aprovação da documentação e convenções de Git

- **Data:** 2026-10-22
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5.
- **Branch criada:** `docs/p-001-especificacao-inicial`, a partir da `main`.

### Prompt integral

```text
Documentação aprovada. Crie uma nova branch a partir da main (sempre faça branches por feature/escopo de mudança), e faça o commit das mudanças. Use commit convencional de forma atomica, não utilize co-authored by nos commits.
Apenas dê push e crie commits quando solicitado. Adicione estas regras no claude.md.
```

### Resultado

- O usuário aprovou a documentação da etapa P-001. Os itens PV-01..PV-07 continuam pendentes:
  a aprovação foi da documentação, não desses padrões.
- Regras de Git adicionadas ao `CLAUDE.md`: branch por feature/escopo a partir da `main`,
  Conventional Commits atômicos, sem `Co-Authored-By`, commit/push/PR somente quando solicitado.
- Documentos da P-001 commitados em commits atômicos na nova branch. Não foi feito push.
- **Continuação:** em seguida o usuário pediu *"dê push na branch"*. A branch foi enviada ao
  `origin`, e o usuário abriu e mergeou o PR #1 na `main` (merge `7fc1131`).

---

## P-003 — Fundação técnica: monorepo, Compose, RLS e testes de isolamento

- **Data:** 2026-10-22
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5. Planejamento em plan mode e depois implementação.
- **Branch:** `feat/p-003-fundacao-rls`, criada a partir de `origin/main` (`7fc1131`).

### Prompt integral

```text
A partir de uma nova branch através da main, planeje o próximo passo:
Estrutura do monorepo, Docker Compose, banco com RLS e os primeiros testes de isolamento
```

### Fluxo da interação

1. A IA verificou o estado do Git (PR #1 mergeado) e o ambiente: JDK 17 no `JAVA_HOME`, `java` 8
   no PATH, sem Maven, Gradle 8.3 global, Docker parado.
2. Lote de perguntas técnicas (respostas abaixo).
3. A IA apresentou um plano com Spring Boot 3.5, PostgreSQL 17 e instalação do JDK 21 via `winget`.
4. O usuário rejeitou o plano e enviou o resumo de ambiente abaixo: outro agente já havia preparado o
   ambiente com outras versões e restrições. O plano foi revisado e aprovado.
5. Implementação em 11 commits atômicos, com testes e testes de mutação (ver report).

### Respostas do usuário

| Pergunta | Resposta |
|---|---|
| Persistência | JPA + Hibernate `@TenantId` (recomendação da IA) |
| Build | Maven Wrapper (recomendação da IA) |
| Versão do Java | *"Java 21, Temurin jdk-21.0.12.1+1"* |
| Frontend nesta etapa | Só na etapa da interface (recomendação da IA) |
| Confirmar PV-05 no esquema | Confirmar PV-05 (recomendação da IA) → D-13 |
| Pacote base | *"com.vesta O nome do projeto será Vesta"* → C-02 |

### Resumo de ambiente enviado pelo usuário (revisão do plano)

<details>
<summary>Texto enviado pelo usuário (conteúdo literal; quebras de linha reconstituídas)</summary>

```text
Mudei algumas versões e preparei o ambiente utilizando outro agent. Segue o resumo abaixo:
# Ambiente preparado para o multi-tenant-poc (Windows 11)
## Versões escolhidas e por quê
| Componente | Versão | Motivo |
|---|---|---|
| JDK | Eclipse Temurin 21.0.12.1+1 (LTS) | Java 21 é a LTS atual com suporte longo; Spring Boot 4.1.1 exige Java 17 a 26. Temurin é a distribuição OpenJDK de referência, sem licença comercial. |
| Spring Boot | 4.1.1 | Versão estável corrente (Spring Framework 7). Não é instalação de sistema: entra como `spring-boot-starter-parent` no `pom.xml`. |
| Maven | 3.10.0 | Versão estável corrente; Spring Boot 4.1.1 exige 3.6.3+. |
| PostgreSQL | 18.6 (imagem Docker `postgres:18.6`) | Major atual. Roda apenas em container, consistente com a stack do projeto (Docker Compose para execução local). Nada foi instalado nativamente no Windows. |
## Restrição não negociável
Esta máquina tem outro projeto que depende do **Java 17 + Gradle 8.3** instalados globalmente
(`JAVA_HOME` de máquina aponta para o Temurin 17 do Chocolatey). **Nada disso pode ser alterado.**
Por isso o JDK 21 e o Maven NÃO foram instalados por winget, Chocolatey ou MSI: o pacote winget do
Temurin usa `ADDLOCAL=ALL`, que sobrescreve PATH, `JAVA_HOME` e o registro JavaSoft. Tudo ficou em
pastas do usuário, sem alterar PATH, variáveis globais ou registro. Verificado com snapshot antes e
depois (zero diferenças) e `gradle -v` continuando na JVM 17.
Regras para você, agent:
- **Nunca** proponha ou execute `winget install`, `choco install`, MSI ou alteração de `JAVA_HOME`,
  `PATH`, `M2_HOME` em escopo de usuário ou máquina.
- **Nunca** rode `choco upgrade all`.
- Não instale PostgreSQL nativo; use Docker.
## Onde está cada coisa
- JDK 21: `C:\Users\ferna\.jdks\jdk-21.0.12.1+1` (pasta que o IntelliJ já escaneia; o SDK aparece
  em Project Structure sem configuração global).
- Maven 3.10.0: `C:\Users\ferna\tools\apache-maven-3.10.0`
- Scripts de ativação: `C:\Users\ferna\tools\use-java21.ps1` e `C:\Users\ferna\tools\use-java21.cmd`
- Imagem Docker `postgres:18.6` já baixada. Docker 29.9 e Compose v5.6 instalados. Porta 5432 livre.
## Como rodar (obrigatório antes de qualquer comando Java/Maven)
O JDK 21 e o Maven só existem na sessão do terminal onde o script de ativação foi executado.
Terminais novos continuam com Java 17 (e `java` no PATH resolve para um Java 8 da Oracle, situação
pré-existente que não deve ser "corrigida").
PowerShell (dot-source, com o ponto no início):
    . C:\Users\ferna\tools\use-java21.ps1
cmd.exe:
    call C:\Users\ferna\tools\use-java21.cmd
Resultado esperado: `java -version` mostra `21.0.12.1` Temurin e `mvn -v` mostra `3.10.0` com
`Java version: 21.0.12.1`. Se mostrar 17 ou 1.8, o script não foi ativado nessa sessão.
Para subir o banco:
    docker compose up -d   (serviço com imagem `postgres:18.6`)
## O que falta e cabe a você ao criar o projeto
1. Gerar o backend com `spring-boot-starter-parent` 4.1.1 e `<java.version>21</java.version>`.
2. Incluir o **Maven Wrapper** fixado em 3.10.0 (`.mvn/wrapper/maven-wrapper.properties` com
   `distributionUrl` para `apache-maven-3.10.0-bin.zip`), para que `./mvnw` funcione mesmo sem o
   script de ativação. O `JAVA_HOME` ainda precisa vir do script.
3. Criar `docker-compose.yml` com serviço `postgres` na imagem `postgres:18.6`, porta `5432:5432`,
   volume nomeado e variáveis `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.
4. Atualizar docs/ADR do repositório, que ainda citam Spring Boot 3, para Spring Boot 4.1.1 / Java 21.
```

</details>

### Resultado

- Monorepo com `backend/` (Spring Boot 4.1.1, Java 21, Maven Wrapper 3.10.0), `infra/`, `bruno/` e
  Docker Compose (PostgreSQL 18.6 + backend).
- Esquema completo com RLS forçada, FKs compostas e privilégios mínimos; `TenantContext`, resolver
  do Hibernate e `TenantAwareJpaTransactionManager`.
- 31 testes automatizados passando; testes de mutação confirmaram que eles detectam a ausência das
  proteções (detalhes no [report](ai-development-report.md) e no ADR-0001).
- Sem push nem PR (não solicitados).

---

## P-004 — Ajustes do ambiente local

- **Data:** 2026-10-22
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5.
- **Branch:** `feat/p-003-fundacao-rls`. A mudança complementa a fundação ainda não mergeada.

### Prompts e resultado

1. *"não consigo enxergar a branch e as mudanças na minha IDE local (IntelliJ) possivelmente por
   estar em um workspace diferente. Por favor transfira para o local, e em diante, mantenha em local
   para que eu possa acompanhar as mudanças através da minha IDE"*
   → A branch estava em uso por um worktree separado. A IA desocupou a branch no worktree, fez o
   checkout dela no repositório local (`C:\Users\ferna\Music\multi-tenant-poc`), copiou o `.env` e
   subiu o Compose a partir da pasta local. Verificação: 31 testes passando e `/actuator/health` → `UP`.
   O trabalho passa a ser feito sempre no repositório local.
2. *"como foram definidas as credenciais do banco de dados, preciso cria-las?"*
   → Explicação: as senhas vêm do `.env`, e as roles são criadas pelo script de init na primeira
   inicialização do volume.
3. *"A run falhou com: ... The server requested SCRAM-based authentication, but the password is an
   empty string. Coloquei no env variables da IDE: POSTGRES_USER=app_owner;POSTGRES_PASSWORD=app-owner-dev"*
   → Diagnóstico: o backend lê `DB_APP_PASSWORD`/`DB_OWNER_PASSWORD`, não `POSTGRES_*`, e deve
   conectar como `app_user`, não como `app_owner`. A IA ofereceu ler o `.env` automaticamente.
4. *"sim, faça o backend ler o .env"*
   → `application.yml` importa `.env` da raiz ou de `../`, com fallback para
   `APP_USER_PASSWORD`/`APP_OWNER_PASSWORD`. Verificado: inicia sem variáveis de ambiente com a raiz
   e com `backend/` como diretório de trabalho; a partir de uma pasta sem `.env`, falha com a senha
   vazia (prova de que a senha vem do `.env`). Os 31 testes continuam passando e o container do
   Compose sobe saudável.

---

## P-005 — Avisos de inspeção na entidade Goal

- **Data:** 2026-10-22
- **Prompt:** *"A aplicação agora rodou com sucesso. Pequeno ajuste antes de prosseguirmos, o sonarqube
  trouxe reclamações no classe goal, entenda e faça as correções necessárias: Field can be converted
  to a local variable / Field 'currency' may be 'final' / Field 'status' may be 'final'"*
- **Análise da IA:** os três avisos são falsos positivos comuns em entidades JPA. Nenhum código altera
  `currency` e `status` depois da criação, mas o Hibernate os preenche por reflexão ao carregar do
  banco, e a especificação JPA proíbe campos persistentes `final`.
- **Perguntas:** qual campo recebeu o aviso de variável local (resposta: `currency`) e como tratar
  os avisos (resposta: supressão pontual, a recomendação da IA).
- **Resultado:** `@SuppressWarnings` só nos dois campos, com comentário justificando; a supressão em
  `status` deve sair quando as transições de status (D-08) forem implementadas. O espaçamento
  alterado acidentalmente no arquivo local foi normalizado. `./mvnw verify`: 31 testes passando.

---

## P-006 — Roadmap e autenticação

- **Data:** 2026-10-22
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5. Planejamento em plan mode, implementação após aprovação.
- **Branch:** `feat/p-006-autenticacao`, criada a partir da `main` (`c7c8869`, merge do PR #2) no repositório local.

### Prompt

```text
A branch foi mergeada na main.
Crie uma nova branch a partir da plan, e planeje os próximos passos
```

Não existe branch `plan`; a IA interpretou como `main` e disse isso ao usuário.

### Respostas do usuário (todas as recomendações da IA)

| Pergunta | Resposta |
|---|---|
| Lookup de login | Função `SECURITY DEFINER` com role dedicada |
| Escopo da P-006 | Auth + seeds + `/api/me` |
| Dados de demonstração | Flyway só no perfil `local` |
| Token | HS256, 60 min, sem refresh |

### Resultado

- Roadmap: P-006 autenticação → P-007 objetivos e autorização por objetivo → P-008 contribuições
  → P-009 frontend → P-010 consolidação.
- Implementação em 9 commits (o planejado, com o commit de dependências unido ao de autenticação,
  mais a conversão do README para UTF-8). 56 testes passando, testes de mutação, ambiente Compose
  recriado e coleção Bruno `auth/` (8/8 requisições).

---

## P-007 — Objetivos, membros e autorização por objetivo

- **Data:** 2026-10-23
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5. Planejamento em plan mode, implementação após aprovação.
- **Branch:** `feat/p-007-objetivos`, criada a partir da `main` (`00ad265`, merge do PR #3) no repositório local.

### Prompts

```text
Mudanças implementadas na main.
Crie a próxima branch e planeje os próximos passos
```

Durante a implementação o usuário interrompeu a sessão e depois pediu:

```text
continue de onde foi parado
```

O trabalho foi retomado no ponto em que estava: a atualização da documentação.

### Respostas do usuário (todas as recomendações da IA)

| Pergunta | Resposta |
|---|---|
| PV-02 — transições | Com desarquivar → D-14 |
| PV-03 — arquivados na listagem | Ocultos, com filtro → D-15 |
| PV-06 — edição | Só em ATIVO → D-16 |
| Progresso na P-007 | Sim, só leitura |

### Resultado

- API de objetivos e membros com autorização por objetivo (404/403), transições, exclusão,
  lock otimista e pessimista, progresso financeiro e total por membro.
- 121 testes passando; 4 testes de mutação; ambiente Compose e coleção Bruno (23/23 requisições,
  repetível) verificados.
- A porta 5432 estava ocupada por um banco de outro projeto do usuário. A IA não mexeu nele e tornou
  a porta do Vesta configurável (`POSTGRES_PORT`, padrão 5432; 5433 neste ambiente).

---

## Entre P-007 e P-008 — Acesso ao banco pelo DBeaver

- **Data:** 2026-10-23
- **Prompts (resumo):** o usuário não conseguia conectar pelo DBeaver em `localhost:5432` ("Connection
  refused") e depois perguntou qual era o acesso de superusuário para ver todos os registros.
- **Resposta da IA:** a porta do Vesta neste ambiente é 5433 (`POSTGRES_PORT`, definida na P-007 por
  causa do banco de outro projeto na 5432). Com `app_user`, as tabelas aparecem vazias por causa da
  RLS, a menos que `set_config('app.tenant_id', ...)` seja executado na transação. O superusuário de
  bootstrap (`postgres` / `postgres-dev`, do `.env`) ignora a RLS e serve só para inspeção, nunca
  para validar o isolamento nem para a aplicação.
- Sem alteração de código.

---

## P-008 — Contribuições e estornos

- **Data:** 2026-10-23
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5. Planejamento em plan mode, implementação após aprovação.
- **Branch:** `feat/p-008-contribuicoes`, criada a partir da `main` (`c2d51c5`, merge do PR #4) no repositório local.

### Prompt

```text
Mudança aprovada e mergeada. Crie a nova branch e prossiga com a mudança sequente
```

### Respostas do usuário

| Pergunta | Resposta |
|---|---|
| PV-01 — estorno em quais status | Só em ATIVO (recomendação da IA) → D-17 |
| PV-04 — data da contribuição | Informada, não futura (recomendação da IA) → D-18 |
| Listagem de contribuições | **Paginada** (a IA recomendava lista completa) → D-19 |
| Quem vê as contribuições | Todos os vinculados (recomendação da IA) |

### Resultado

- Registro (próprio ou pelo dono para um participante), estorno (pelo contribuinte ou pelo dono) e
  listagem paginada, com a ordem 404 → 403 → 422 → 409, lock da linha do objetivo e entidades
  insert-only.
- 160 testes passando; 2 testes de mutação; ambiente Compose e coleção Bruno (38/38 requisições, duas
  execuções seguidas) verificados.

---

## P-009 — Frontend de demonstração

- **Data:** 2026-10-23
- **Ferramenta/modelo:** Claude Code, modelo Claude Opus 5.5. Planejamento em plan mode, implementação
  após aprovação; verificação final no navegador com a extensão Claude in Chrome.
- **Branch:** `feat/p-009-frontend`, criada a partir da `main` (`2a926eb`, merge do PR #5) no repositório local.

### Prompt

```text
Mudança mergeada. Prossiga para a p-009
```

### Respostas do usuário (todas as recomendações da IA)

| Pergunta | Resposta |
|---|---|
| PV-07 — quais telas | Obrigatórios + estorno e criação → D-20 |
| Estilo | CSS próprio simples → T-02 |
| Token no navegador | `sessionStorage` → T-02 |
| Execução local | Vite dev + Compose com nginx → T-02 |

### Resultado

- Frontend com login, lista, detalhe (progresso, membros, total por membro, contribuições paginadas),
  registro e estorno de contribuição, transições de status e criação de objetivo; nginx no Compose.
- 21 testes de frontend, lint e build verdes; smoke test do nginx; roteiro no navegador com os
  usuários Carlos e Pedro.

---

## P-009 (ajuste) — Colisão de campos na interface

- **Data:** 2026-10-23
- **Prompts:** *"me de um exemplo de acesso para testar o FE"* (a IA listou os usuários demo e o que
  testar com cada um) e, em seguida, com uma captura de tela do detalhe da Parcela do carro:

```text
A UI simples é aceitável, mas ela não deve ter bugs visuais de colisão de campos
Faça as correções necessárias
```

- **Diagnóstico da IA:** no formulário "Registrar contribuição", os `input` mantinham a largura
  intrínseca do navegador dentro de colunas de grid de 160px, e os rótulos (itens do grid) não podiam
  encolher. Por isso o Valor invadia a Data, a Observação invadia o botão e o botão quebrava em duas linhas.
- **Resultado:** correção só de CSS (e um wrapper na tabela), commit
  `fix(frontend): prevent form fields from overlapping`. A verificação por medição de geometria no
  navegador encontrou e corrigiu um segundo defeito, não visível na captura: abaixo de ~600px, a
  coluna do detalhe ficava mais larga que a tela.

---

## P-009 (ajuste 2) — Data da contribuição sem dias futuros

- **Data:** 2026-10-23
- **Prompt:** *"No componente de data, bloqueie a opção de selecionar uma data superior a data de hoje, ao
  invés de avisar que não é possível após a tentativa"*
- **Resultado:** o campo de data do formulário de contribuição recebeu `max` igual ao dia de hoje no fuso
  America/Sao_Paulo (o mesmo do backend, D-18): o seletor não oferece dias futuros e uma data futura
  digitada impede o envio, sem requisição. A data-limite do objetivo (tela de criação) continua livre,
  porque é naturalmente futura. O backend continua rejeitando datas futuras (422).
- **Verificação:** 23 testes de frontend (2 novos: "hoje" em São Paulo e atributo `max` no formulário);
  no navegador, `max = 2026-10-23`, data 2026-10-24 → `rangeOverflow`, formulário inválido, 0 requisições POST.

---

## P-009 (ajuste 3) — Máscara de BRL nos campos monetários

- **Data:** 2026-10-23
- **Prompt:** *"Em todos os campos monetários, formate automaticamente para brl enquanto o usuário digita"*
- **Resultado:** componente `MoneyInput` usado no valor da contribuição e no valor-alvo do novo objetivo
  (os dois únicos campos monetários). Máscara no estilo calculadora: só dígitos contam e preenchem da
  direita para a esquerda (`2550` → R$ 25,50); Backspace remove o último dígito; colar "R$ 1.234,56"
  funciona; campo vazio continua vazio (o `required` segue valendo). O valor é mantido em centavos
  inteiros e convertido para o decimal da API só no envio, sem aritmética de ponto flutuante na tela.
- **Verificação:** 28 testes de frontend (novos: `MoneyInput.test.tsx` e `parseCents`/`centsToAmount`);
  no navegador, digitação real: `123456` → R$ 1.234,56, dois Backspaces → R$ 12,34; no novo objetivo,
  `1200000` → R$ 12.000,00. Nada foi enviado durante a verificação.
