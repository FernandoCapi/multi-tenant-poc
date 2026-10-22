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
