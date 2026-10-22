# Decisões de negócio

> Registro das decisões de negócio da POC. Cada decisão indica a origem (lote de perguntas do
> levantamento P-001) e se a recomendação inicial da IA foi aceita.
> As decisões técnicas iniciais (DT-01..DT-06) estão no prompt P-001 ([histórico](ia/historico-de-prompts.md)),
> suas alterações nas entradas T-xx e a estratégia de tenant no [ADR-0001](adr/0001-estrategia-multitenant.md).

## Resumo

| ID | Decisão | Origem | Recomendação da IA |
|---|---|---|---|
| D-01 | Acesso definido somente por objetivo | P-001, lote 1 | Não adotada |
| D-02 | Contribuição realizada, imutável, corrigida por estorno | P-001, lote 1 | Aceita |
| D-03 | Registro próprio ou pelo dono em nome do participante | P-001, lote 1 | Aceita |
| D-04 | Toda meta é valor-alvo + contribuições | P-001, lote 1 | Aceita |
| D-05 | Papel = acesso; `contribui` = participação financeira | P-001, lote 2 | Aceita |
| D-06 | Vários donos, mínimo de um | P-001, lote 2 | Aceita |
| D-07 | Permissões do MEMBRO e do DONO | P-001, lote 2 | Aceita |
| D-08 | Ciclo de vida ATIVO / CONCLUÍDO / ARQUIVADO | P-001, lote 2 | Aceita |
| D-09 | 404 sem leitura, 403 com leitura | P-001, lote 3 | Aceita |
| D-10 | Cenários negativos demonstrados ao vivo | P-001, lote 3 | Sem recomendação (escolha múltipla) |
| D-11 | Famílias e membros somente por seeds | P-001, lote 3 | Aceita |
| D-12 | BRL com centavos; excedente aceito | P-001, lote 3 | Aceita |
| D-13 | No máximo um estorno por contribuição, como registro próprio (era PV-05) | P-003 | Aceita |
| C-01 | Convenção de idioma | P-001, revisão do plano | Definida pelo usuário |
| C-02 | Nome do projeto: Vesta; pacote `com.vesta` | P-003 | Definida pelo usuário |
| T-01 | Atualização das versões da stack (DT-01) | P-003 | Definida pelo usuário |

---

## D-01 — Acesso definido somente por objetivo

- **Decisão:** não existe papel familiar (responsável/membro). Quem cria o objetivo é DONO e pode
  mantê-lo só para si ou adicionar membros da família. No objetivo há dois papéis, DONO e MEMBRO,
  definidos ao adicionar o membro e editáveis pelo dono; as permissões mais sérias (editar, excluir)
  ficam com o dono. Exemplo: o pai cria o objetivo do aluguel e adiciona só a mãe; os filhos não o veem.
- **Opções consideradas:** papel familiar + ACL por objetivo (recomendação da IA); somente ACL por
  objetivo; responsável vê todos os objetivos.
- **Justificativa:** o usuário preferiu um modelo sem regras implícitas, em que toda visibilidade
  vem de um vínculo explícito com o objetivo.
- **Consequências:** qualquer membro pode criar objetivos; a distinção entre pais e filhos é
  expressa caso a caso pelo dono. Não há como um responsável ver automaticamente o objetivo
  individual de um filho.

## D-02 — Contribuição realizada, imutável, corrigida por estorno

- **Decisão:** a contribuição é o registro declarativo de dinheiro efetivamente guardado ou pago.
  Não é editada nem excluída; corrige-se por estorno (autor, data, motivo) seguido de um novo
  lançamento. O acumulado soma só as contribuições não estornadas. "Contribuição coletiva" é a
  agregação das contribuições individuais.
- **Opções consideradas:** realizada e imutável com estorno; realizada e editável/excluível;
  compromisso + realização.
- **Justificativa:** preserva a rastreabilidade exigida pelo RF-04 sem precisar de auditoria
  separada, com escopo menor que o modelo de compromissos.

## D-03 — Quem registra contribuições

- **Decisão:** o participante registra as próprias contribuições; o DONO pode registrar em nome de
  qualquer participante. O sistema guarda contribuinte e "registrado por" separadamente.
- **Opções consideradas:** próprio ou dono; somente o próprio participante; qualquer um com leitura.
- **Justificativa:** permite que um responsável lance a contribuição de um filho pequeno, sem
  misturar acesso com participação.

## D-04 — Toda meta é valor-alvo + contribuições

- **Decisão:** não há cronograma de parcelas nem recorrência na POC. "Parcela do carro" é um
  objetivo com valor-alvo. A data-limite é opcional e apenas informativa.
- **Opções consideradas:** tudo é meta com alvo; meta com parcelas planejadas; meta com recorrência.
- **Justificativa:** mantém o domínio simples e focado em isolamento e consistência financeira.
  Parcelas e recorrência ficam como evolução futura.

## D-05 — Acesso e participação financeira são distintos

- **Decisão:** o papel (DONO/MEMBRO) define o acesso; o indicador `contribui` (padrão sim) define
  a participação financeira. Só quem tem `contribui = sim` pode ser contribuinte.
- **Opções consideradas:** papel + flag `contribui`; somente DONO e MEMBRO; DONO, MEMBRO e LEITOR.
- **Justificativa:** atende literalmente o RF-03 com um único campo booleano. Permite, por exemplo,
  um filho que vê a viagem sem contribuir, ou uma mãe dona que não contribui.
- **Observação:** esta decisão resolveu a tensão entre D-01 ("membro/contribuinte") e o RF-03,
  identificada na análise da IA.

## D-06 — Vários donos, mínimo de um

- **Decisão:** um objetivo pode ter vários donos. O objetivo nunca fica sem dono: o último dono não
  pode sair, ser rebaixado nem ser removido.
- **Opções consideradas:** vários donos com mínimo de um; dono único (criador); dono único transferível.

## D-07 — Permissões do MEMBRO e do DONO

- **Decisão:**
  - MEMBRO: ver o objetivo, registrar contribuição própria, estornar contribuição própria e sair do objetivo.
  - DONO: tudo o que o membro faz, mais editar o objetivo, gerenciar membros, papéis e `contribui`,
    registrar em nome de outro, estornar qualquer contribuição, concluir, reabrir, arquivar e excluir.
  - Contribuições de quem sai ou é removido permanecem no histórico e no acumulado.
- **Opções consideradas:** membro estorna a própria e pode sair; só o dono estorna; membro não pode sair.

## D-08 — Ciclo de vida do objetivo

- **Decisão:** status ATIVO, CONCLUÍDO e ARQUIVADO. "Meta atingida" é um indicador calculado; a
  conclusão é manual, feita pelo dono, que também pode reabrir. Contribuições só entram em objetivos
  ATIVOS. Arquivar oculta sem apagar. A exclusão física só é permitida se o objetivo nunca teve
  contribuições.
- **Opções consideradas:** ATIVO/CONCLUÍDO/ARQUIVADO com conclusão manual; conclusão automática;
  exclusão sempre lógica.
- **Justificativa:** evita regras de reabertura automática quando um estorno faz o acumulado voltar
  abaixo do alvo, e evita condições de corrida.

## D-09 — Política de 404 e 403

- **Decisão:** 404 para recurso inexistente, de outra família ou que o usuário não vê; 403 quando
  o usuário vê o recurso mas não tem permissão para a ação.
- **Opções consideradas:** 404 sem leitura e 403 com leitura; 404 sempre.
- **Justificativa:** o 403 não revela nada que o usuário já não saiba e deixa a interface e o
  diagnóstico mais claros.

## D-10 — Cenários negativos demonstrados ao vivo

- **Decisão:** os quatro grupos serão mostrados na apresentação (interface ou Bruno):
  acesso entre famílias (incluindo RLS sem WHERE e `family_id` forjado), filho × objetivo do aluguel,
  membro sem permissão (403 e contribuição em objetivo concluído) e autenticação inválida (401).
  Todos também serão cobertos por testes automatizados.
- Detalhamento em [requisitos.md, seção 8](requisitos.md#8-cenários-de-demonstração).

## D-11 — Famílias e membros somente por seeds

- **Decisão:** sem cadastro nem convite. Famílias e membros, com senhas já em hash, vêm de seeds.
- **Opções consideradas:** somente seeds; cadastro de família + convite; seeds + membro cria login.
- **Justificativa:** mantém o foco no isolamento e nas regras financeiras e evita o problema de
  criar um tenant antes de existir contexto de tenant.

## D-12 — Moeda e excedente

- **Decisão:** moeda única BRL, com centavos e valores sempre positivos. Contribuições acima do
  alvo são aceitas: restante = max(alvo − acumulado, 0) e o excedente é exibido.
- **Opções consideradas:** BRL aceitando excedente; BRL bloqueando excedente; moeda por objetivo.
- **Justificativa:** não bloqueia o registro de dinheiro real e dispensa lock para contribuições concorrentes.

## C-01 — Convenção de idioma

- **Decisão:** interface, mensagens e todo conteúdo apresentado ao usuário em português, assim
  como a documentação. Nomes de entidades, tabelas, colunas, classes, métodos, variáveis, enums,
  endpoints e comentários no código em inglês.
- **Origem:** definida pelo usuário ao revisar o plano da etapa P-001.
- **Referência:** glossário em [modelo-de-dominio.md](modelo-de-dominio.md#1-glossário).

## D-13 — No máximo um estorno por contribuição

- **Decisão:** o estorno é um registro próprio (`ContributionReversal`), e cada contribuição tem no
  máximo um. A contribuição original nunca é alterada.
- **Origem:** era o padrão provisório PV-05; confirmado pelo usuário em P-003, quando o esquema foi criado.
- **Garantia no banco:** `UNIQUE (family_id, contribution_id)` em `contribution_reversals`, e a role
  da aplicação só tem `SELECT`/`INSERT` em contribuições e estornos (ADR-0001).

## C-02 — Nome do projeto e pacote

- **Decisão:** o projeto se chama **Vesta**. Pacote base Java: `com.vesta`; artefato `vesta-backend`;
  banco `vesta`.
- **Origem:** definida pelo usuário em P-003.

## T-01 — Versões da stack (altera DT-01)

- **Decisão:** Java 21 (Temurin 21.0.12.1+1), Spring Boot **4.1.1** (Spring Framework 7,
  Hibernate 7), Maven 3.10.0 via Maven Wrapper e PostgreSQL 18.6 (somente Docker).
- **Contexto:** a DT-01 citava Spring Boot 3. O usuário preparou o ambiente com versões atuais e
  definiu Spring Boot 4.1.1. O plano inicial da IA para P-003 previa Spring Boot 3.5 e PostgreSQL 17.
- **Restrição do ambiente:** a máquina tem outro projeto que depende do Java 17 e do Gradle 8.3
  globais. JDK 21 e Maven ficam em pastas do usuário e são ativados só na sessão; nada global é
  alterado (ver [setup local](setup-local.md)).
- **Origem:** definida pelo usuário em P-003.

---

## Itens pendentes de validação

Padrões adotados provisoriamente pela IA para não bloquear a especificação. **Não foram aprovados
explicitamente** e devem ser confirmados ou revistos antes da implementação da funcionalidade
correspondente.

| ID | Padrão provisório | Motivo |
|---|---|---|
| PV-01 | Estorno só em objetivo ATIVO; para corrigir um CONCLUÍDO, reabrir antes. | Mantém consistente a regra "conclusão manual" com um acumulado estável. |
| PV-02 | Transições: ATIVO→CONCLUÍDO, CONCLUÍDO→ATIVO, ATIVO/CONCLUÍDO→ARQUIVADO, ARQUIVADO→ATIVO (desarquivar, só dono). | Não houve decisão sobre desarquivar. |
| PV-03 | Arquivados ocultos por padrão e visíveis por filtro a quem tem acesso. | Detalhe de apresentação. |
| PV-04 | Data da contribuição informada pelo usuário, não futura; data de registro do servidor separada. | Permite lançar contribuições passadas. |
| ~~PV-05~~ | **Confirmado em P-003 → D-13.** | — |
| PV-06 | Dados do objetivo só podem ser editados em status ATIVO. | Evita alterar alvo de objetivo concluído. |
| PV-07 | Fluxos de interface além dos obrigatórios (estorno, criação/edição, gestão de membros) podem ser demonstrados só pela coleção Bruno. | RF-07 exige apenas login, objetivos, progresso e contribuição. |
