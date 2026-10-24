# Especificação de requisitos — POC SaaS financeiro multitenant

> **Status:** versão inicial, derivada do levantamento P-001 (2026-10-21).
> **Fontes:** prompt P-001 ([histórico](ia/historico-de-prompts.md)) e decisões D-01..D-12 ([decisões de negócio](decisoes-de-negocio.md)).
> Documento vivo: deve ser atualizado sempre que código, testes ou decisões evoluírem.

## 1. Visão

SaaS para famílias organizarem objetivos financeiros (viagem, carro, despesas da casa, metas pessoais).
Cada objetivo tem um valor-alvo; o sistema mostra quanto foi acumulado, quanto falta e quanto cada
membro contribuiu.

A segurança atua em dois níveis:

- **Entre famílias:** cada família é um tenant e nunca acessa dados de outra.
- **Dentro da família:** cada objetivo tem uma lista explícita de membros com acesso; quem não está
  na lista não vê o objetivo (ex.: filhos não veem o objetivo do aluguel).

Além da aplicação funcionando, a POC deve demonstrar, com evidências, como a IA apoiou análise,
arquitetura, implementação, testes, revisão e documentação.

## 2. Escopo

**Dentro da POC**

- Login real com JWT; famílias e membros criados por seeds.
- Objetivos individuais e compartilhados, com controle de acesso por objetivo.
- Contribuições realizadas, imutáveis, com estorno.
- Cálculo de acumulado, restante, excedente e percentual.
- Isolamento entre famílias com defesa em profundidade (aplicação + persistência + RLS).
- Interface mínima em português e coleção Bruno com cenários positivos e negativos.

**Fora da POC** (registrado como evolução futura)

- Parcelas com cronograma de vencimentos e despesas recorrentes por período (D-04).
- Cadastro de família, convite e gestão de usuários pela interface (D-11).
- Multimoeda (D-12).
- Usuário em mais de uma família ou troca de contexto de tenant (DT-04).
- Compromissos/promessas de contribuição (D-02).
- Integração bancária ou conciliação.

## 3. Glossário

O glossário completo, com a correspondência entre os termos em português e os identificadores
usados no código, está em [modelo-de-dominio.md](modelo-de-dominio.md#1-glossário).

## 4. Requisitos funcionais

Os critérios de aceitação (CA) seguem o formato *Dado / Quando / Então*. Os códigos HTTP seguem a
política da [seção 7](#7-política-de-respostas-de-erro).

### RF-01 — Famílias e membros

- Cada família é um tenant; cada usuário pertence a exatamente uma família.
- Os membros têm identidade individual para autenticação e autorização.
- Famílias e membros existem apenas por seeds (D-11).

| CA | Critério |
|---|---|
| CA-01.1 | Dado um usuário semeado, quando faz login com credenciais válidas, então recebe um JWT que identifica o usuário (`sub`) e o tenant resolvido pelo servidor. |
| CA-01.2 | Todo usuário está vinculado a exatamente uma família (garantido por restrição no banco). |
| CA-01.3 | Não existe endpoint de cadastro de família ou de usuário. |
| CA-01.4 | Os seeds criam pelo menos duas famílias, com membros suficientes para todos os cenários da [seção 8](#8-cenários-de-demonstração). |

### RF-02 — Objetivos financeiros

- Atributos mínimos: título, descrição (opcional), valor-alvo, status, data-limite (opcional, só informativa — D-04).
- Status: ATIVO, CONCLUÍDO, ARQUIVADO (D-08).
- Um objetivo **individual** é simplesmente um objetivo sem outros membros além do criador.

| CA | Critério |
|---|---|
| CA-02.1 | Dado um usuário autenticado, quando cria um objetivo com título e valor-alvo > 0, então o objetivo nasce ATIVO, na família do usuário, e o criador é DONO com `contribui = sim`. **Testado (P-007):** `GoalListingIntegrationTest` |
| CA-02.2 | Dado um objetivo criado sem adicionar membros, então somente o criador o vê (objetivo individual). **Testado (P-007):** `GoalListingIntegrationTest` |
| CA-02.3 | O detalhe do objetivo apresenta valor-alvo, acumulado, restante, excedente, percentual e o indicador "meta atingida", calculados conforme a [seção 6](#6-regras-de-cálculo). **Testado (P-007):** `GoalProgressIntegrationTest`, `GoalProgressTest` |
| CA-02.4 | Dado um DONO, quando conclui um objetivo ATIVO, então o status passa a CONCLUÍDO; a conclusão é sempre manual, mesmo com a meta atingida. **Testado (P-007):** `GoalLifecycleIntegrationTest` |
| CA-02.5 | Dado um DONO, quando reabre um objetivo CONCLUÍDO, então o status volta a ATIVO. **Testado (P-007):** `GoalLifecycleIntegrationTest` |
| CA-02.6 | Dado um DONO, quando arquiva um objetivo ATIVO ou CONCLUÍDO, então ele fica oculto na listagem padrão e visível pelo filtro de arquivados; nenhum dado é apagado. **Testado (P-007):** `GoalListingIntegrationTest` |
| CA-02.7 | Dado um objetivo que nunca teve contribuições, quando o DONO o exclui, então ele é removido fisicamente. **Testado (P-007):** `GoalLifecycleIntegrationTest` |
| CA-02.8 | Dado um objetivo com pelo menos uma contribuição (inclusive estornada), quando o DONO tenta excluí-lo, então a operação é rejeitada com **409**. **Testado (P-007):** `GoalLifecycleIntegrationTest` |
| CA-02.9 | Dado um MEMBRO, quando tenta editar, concluir, reabrir, arquivar ou excluir o objetivo, então recebe **403**. **Testado (P-007):** `GoalAuthorizationIntegrationTest` |

### RF-03 — Participação e visibilidade

- O acesso é definido somente por objetivo, sem papel familiar (D-01).
- Papel no objetivo (DONO ou MEMBRO) define o **acesso**; o indicador `contribui` define a
  **participação financeira** (D-05). São conceitos distintos.
- Um objetivo sempre tem pelo menos um DONO (D-06).

| CA | Critério |
|---|---|
| CA-03.1 | Dado um usuário, quando lista objetivos, então recebe somente objetivos dos quais é membro (DONO ou MEMBRO). **Testado (P-007):** `GoalListingIntegrationTest` |
| CA-03.2 | Dado um DONO, quando adiciona um membro da mesma família informando papel e `contribui`, então o membro passa a ver o objetivo. **Testado (P-007):** `GoalMembershipIntegrationTest` |
| CA-03.3 | Dado um DONO, quando altera o papel ou o `contribui` de um membro, ou o remove, então a alteração é aplicada imediatamente às permissões. **Testado (P-007):** `GoalMembershipIntegrationTest` |
| CA-03.4 | Dado um DONO, quando tenta adicionar um usuário de outra família (ou um id inexistente), então recebe **404**, sem revelar se o usuário existe em outra família. **Testado (P-007):** `GoalMembershipIntegrationTest` |
| CA-03.5 | Dado o único DONO de um objetivo, quando tenta sair, ser rebaixado a MEMBRO ou ser removido, então a operação é rejeitada com **409**. **Testado (P-007):** `GoalMembershipIntegrationTest` |
| CA-03.6 | Dado um MEMBRO, quando sai do objetivo, então passa a receber **404** ao acessá-lo, e suas contribuições continuam no histórico e no acumulado. **Testado (P-007):** `GoalMembershipIntegrationTest`, `GoalProgressIntegrationTest` |
| CA-03.7 | Dado um membro com `contribui = não`, então ele vê o objetivo, as contribuições e os totais, mas não pode ser contribuinte. **Testado (P-007):** `GoalAuthorizationIntegrationTest` (visualização; a restrição de contribuir entra na P-008) |
| CA-03.8 | Todo total agregado apresentado a um usuário (ex.: soma dos seus objetivos) considera somente objetivos que ele pode ver. **Testado (P-007):** `GoalListingIntegrationTest`, `GoalProgressIntegrationTest` |

### RF-04 — Contribuições financeiras

- Uma contribuição é **dinheiro efetivamente guardado ou pago**, registrado de forma declarativa (D-02).
- É imutável: não há edição nem exclusão; a correção é feita por **estorno** seguido de novo lançamento.
- Guarda separadamente o **contribuinte** e **quem registrou** (D-03).
- Moeda única BRL, valor > 0 com no máximo duas casas decimais (D-12).

| CA | Critério |
|---|---|
| CA-04.1 | Dado um membro com `contribui = sim` em um objetivo ATIVO, quando registra uma contribuição própria com valor válido e data não futura, então ela é criada e o acumulado aumenta pelo valor. |
| CA-04.2 | Dado um DONO, quando registra uma contribuição em nome de um membro com `contribui = sim`, então o contribuinte é o membro e "registrado por" é o DONO. |
| CA-04.3 | Dado um MEMBRO, quando tenta registrar uma contribuição em nome de outra pessoa, então recebe **403**. |
| CA-04.4 | Dado um usuário com `contribui = não`, quando tenta registrar uma contribuição própria, então recebe **403**. |
| CA-04.5 | Dado um DONO, quando registra uma contribuição em nome de um membro com `contribui = não` ou de alguém que não é membro do objetivo, então recebe **422**. |
| CA-04.6 | Dado um objetivo CONCLUÍDO ou ARQUIVADO, quando alguém tenta registrar uma contribuição, então recebe **409**. |
| CA-04.7 | Valor ≤ 0, com mais de duas casas decimais ou com data futura é rejeitado com **422**. |
| CA-04.8 | Não existem operações para editar ou excluir uma contribuição. |
| CA-04.9 | Dado um membro, quando estorna uma contribuição da qual é o contribuinte, informando o motivo, então ela deixa de compor o acumulado e o estorno registra autor, data e motivo. |
| CA-04.10 | Dado um DONO, quando estorna qualquer contribuição do objetivo, então o efeito é o mesmo de CA-04.9. |
| CA-04.11 | Dado um MEMBRO, quando tenta estornar a contribuição de outra pessoa, então recebe **403**. |
| CA-04.12 | Estornar uma contribuição já estornada é rejeitado com **409**; estorno sem motivo é rejeitado com **422**. |
| CA-04.13 | A listagem de contribuições do objetivo mostra todas as contribuições, inclusive as estornadas (sinalizadas), com contribuinte, registrado por, valor e data; a soma das não estornadas é igual ao acumulado. |
| CA-04.14 | O detalhe do objetivo mostra o total contribuído por membro (contribuições não estornadas). |
| CA-04.15 | Toda contribuição e todo estorno pertencem à mesma família do objetivo. |

### RF-05 — Autenticação

| CA | Critério |
|---|---|
| CA-05.1 | Nenhuma senha é armazenada em texto puro; o hash usa um algoritmo adaptativo para senhas. **Implementado (P-006):** bcrypt via `DelegatingPasswordEncoder`; a aplicação não lê hashes diretamente. |
| CA-05.2 | Dado um e-mail inexistente ou uma senha errada, quando o login é tentado, então a resposta é **401** com a mesma mensagem nos dois casos. **Testado (P-006):** `LoginIntegrationTest`. |
| CA-05.3 | Dada uma rota de negócio, quando chamada sem token, com token expirado ou com token adulterado, então a resposta é **401**. **Testado (P-006):** `MeIntegrationTest`. |
| CA-05.4 | O JWT é assinado pelo servidor e tem expiração. **Implementado (P-006):** HS256, 60 minutos, sem refresh ([ADR-0002](adr/0002-autenticacao-jwt.md)). |

### RF-06 — Isolamento e autorização

Detalhado como requisitos de segurança RS-01 a RS-07 na [seção 5](#5-requisitos-de-segurança).

### RF-07 — Interface de demonstração

| CA | Critério |
|---|---|
| CA-07.1 | Fluxos obrigatórios na interface: login, listagem dos objetivos autorizados, detalhe com progresso (acumulado, restante, excedente, percentual, total por membro), registro de contribuição. |
| CA-07.2 | Todo texto apresentado ao usuário está em português; valores são exibidos em R$. |
| CA-07.3 | A interface pode ocultar ações não permitidas, mas o backend é sempre a autoridade: toda regra é validada no servidor. |
| CA-07.4 | Os erros 401, 403, 404, 409 e 422 são apresentados com mensagens compreensíveis em português. |

**Pendente de decisão:** se estorno, criação/edição de objetivo e gestão de membros também terão
tela ou serão demonstrados apenas pela coleção Bruno.

## 5. Requisitos de segurança

| ID | Requisito | Critérios de aceitação |
|---|---|---|
| RS-01 | **Isolamento entre famílias** em leitura, criação, atualização, exclusão, listagem e agregação. | Usuário da família A, usando ids de recursos da família B: detalhe, contribuições, estorno, alteração e exclusão → **404**; listagens e totais nunca incluem dados de B. **P-007:** objetivos e membros testados em `GoalAuthorizationIntegrationTest` e `GoalListingIntegrationTest`; contribuições na P-008. |
| RS-02 | **Autorização por objetivo** dentro da mesma família. | Filho sem vínculo com o objetivo do aluguel: o objetivo não aparece na listagem, o detalhe e as contribuições retornam **404** e os totais não o incluem. **P-007:** `GoalAuthorizationIntegrationTest`, `GoalListingIntegrationTest`. |
| RS-03 | **Autenticação obrigatória** nas rotas de negócio. | Ver RF-05. |
| RS-04 | **Tenant resolvido pelo servidor.** O `family_id` nunca é aceito como entrada do cliente. | Um `family_id` (ou equivalente) enviado no corpo, na query ou em cabeçalho é ignorado ou rejeitado; o recurso é sempre criado/consultado no tenant do usuário autenticado. |
| RS-05 | **Defesa em profundidade com RLS** ([ADR-0001](adr/0001-estrategia-multitenant.md)). | Consulta sem `WHERE family_id` executada com a role da aplicação retorna somente linhas do tenant do contexto; a role da aplicação não é superuser nem possui BYPASSRLS. |
| RS-06 | **Falha fechada sem contexto de tenant.** | Sem contexto de tenant definido, consultas retornam zero linhas (ou erro) e escritas falham; nunca retornam dados de outro tenant. |
| RS-07 | **Contexto não vaza entre requisições** (pool de conexões). | Requisições sucessivas de famílias diferentes, reaproveitando a mesma conexão do pool, só veem dados do próprio tenant. |

Todos os requisitos de segurança devem ser demonstrados por **testes automatizados** (DT-05).

## 6. Regras de cálculo

Valores em BRL, com precisão de centavos; nunca usar ponto flutuante.

| Grandeza | Fórmula |
|---|---|
| Acumulado | Soma dos valores das contribuições **não estornadas** do objetivo. |
| Restante | `max(alvo − acumulado, 0)` |
| Excedente | `max(acumulado − alvo, 0)` |
| Percentual | `acumulado ÷ alvo × 100`, podendo passar de 100% quando há excedente (a exibição pode limitar a barra em 100%). |
| Meta atingida | `acumulado ≥ alvo` — indicador calculado; **não** altera o status. |
| Total por membro | Soma das contribuições não estornadas em que o membro é o contribuinte. |

Contribuições acima do alvo são aceitas (D-12).

## 7. Política de respostas de erro

As verificações seguem esta ordem: autenticação → visibilidade → permissão → validação → estado.

| Código | Quando |
|---|---|
| **401** | Sem token, token inválido/expirado ou login com credenciais inválidas. |
| **404** | Recurso inexistente, de outra família ou que o usuário não pode ver (D-09). |
| **403** | O usuário vê o recurso, mas não tem permissão para a ação (D-09). |
| **422** | Dados inválidos: valor ≤ 0, mais de duas casas, data futura, contribuinte sem `contribui`, motivo de estorno ausente. |
| **409** | Conflito de estado: objetivo não ATIVO, último dono, exclusão de objetivo com contribuições, estorno duplicado. |

Mensagens de erro não revelam a existência de recursos protegidos.

### Matriz de permissões

| Ação | DONO | MEMBRO (`contribui = sim`) | MEMBRO (`contribui = não`) | Mesma família, sem vínculo | Outra família |
|---|---|---|---|---|---|
| Ver objetivo, contribuições e totais | ✅ | ✅ | ✅ | 404 | 404 |
| Editar dados do objetivo | ✅ | 403 | 403 | 404 | 404 |
| Concluir / reabrir | ✅ | 403 | 403 | 404 | 404 |
| Arquivar / desarquivar | ✅ | 403 | 403 | 404 | 404 |
| Excluir (sem contribuições) | ✅ | 403 | 403 | 404 | 404 |
| Adicionar/remover membro, alterar papel ou `contribui` | ✅ | 403 | 403 | 404 | 404 |
| Sair do objetivo | ✅ (se não for o último dono) | ✅ | ✅ | 404 | 404 |
| Registrar contribuição própria | ✅ se `contribui = sim` | ✅ | 403 | 404 | 404 |
| Registrar em nome de outro membro | ✅ (contribuinte com `contribui = sim`) | 403 | 403 | 404 | 404 |
| Estornar contribuição própria | ✅ | ✅ | ✅ | 404 | 404 |
| Estornar contribuição de outro | ✅ | 403 | 403 | 404 | 404 |

As restrições de estado (409) e de validação (422) se aplicam depois das permissões acima.

## 8. Cenários de demonstração

### Positivos (fluxo principal)

1. Login de um membro da família A.
2. Listagem dos objetivos autorizados e detalhe com progresso.
3. Registro de uma contribuição e atualização imediata de acumulado e restante.
4. DONO registra contribuição em nome de um filho; contribuinte e "registrado por" aparecem distintos.
5. Estorno de uma contribuição e recálculo do acumulado.

### Negativos obrigatórios ao vivo (D-10)

| # | Cenário | Resultado esperado |
|---|---|---|
| N-1 | Usuário da família A usa o id de um objetivo/contribuição da família B. | **404** |
| N-2 | Requisição com `family_id` da família B forjado no corpo. | Ignorado/rejeitado; nenhum efeito na família B. |
| N-3 | Consulta sem `WHERE family_id` com a role da aplicação (teste ou script de demonstração). | Somente linhas do tenant do contexto. |
| N-4 | Filho tenta acessar o objetivo do aluguel. | Ausente da listagem; **404** no detalhe. |
| N-5 | MEMBRO tenta editar o objetivo ou estornar a contribuição de outra pessoa. | **403** |
| N-6 | Usuário com `contribui = não` tenta registrar contribuição. | **403** |
| N-7 | Contribuição em objetivo CONCLUÍDO. | **409** |
| N-8 | Requisição sem token, com token expirado ou adulterado. | **401** |
| N-9 | Login com senha errada e com e-mail inexistente. | **401**, mesma mensagem. |

Todos os cenários acima também devem existir como testes automatizados e na coleção Bruno.

## 9. Itens pendentes de validação

Padrões adotados provisoriamente; detalhes em [decisoes-de-negocio.md](decisoes-de-negocio.md#itens-pendentes-de-validação).

- PV-01 Estorno só em objetivo ATIVO.
- ~~PV-02~~ Transições de status, incluindo desarquivar: **confirmado em P-007 (D-14)**.
- ~~PV-03~~ Arquivados ocultos por padrão e visíveis por filtro: **confirmado em P-007 (D-15)**.
- PV-04 Data da contribuição informada pelo usuário e não futura.
- ~~PV-05~~ Estorno como registro próprio, um por contribuição: **confirmado em P-003 (D-13)**.
- ~~PV-06~~ Edição dos dados do objetivo apenas em status ATIVO: **confirmado em P-007 (D-16)**.
- PV-07 Quais fluxos além dos obrigatórios terão tela (RF-07).

## 10. Rastreabilidade

| Requisito | Decisões |
|---|---|
| RF-01 | DT-04, D-11 |
| RF-02 | D-04, D-08, D-12, D-14, D-15, D-16 |
| RF-03 | D-01, D-05, D-06, D-07 |
| RF-04 | D-02, D-03, D-05, D-07, D-12, D-13 |
| RF-05 | DT-04 |
| RF-07 | D-10 |
| RS-01..RS-07 | DT-02, DT-03, DT-04, DT-05, D-09, D-10 |
