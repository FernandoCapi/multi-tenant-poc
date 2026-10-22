# AI Development Report

> Relatório cumulativo sobre o uso de IA (Claude Code) no desenvolvimento da POC. É atualizado a
> cada etapa. Toda afirmação deve ter evidência: prompt registrado, arquivo, commit, saída de teste
> ou decisão documentada.

## Visão geral

| Etapa | Data | Tipo de apoio | Resultado | Evidência |
|---|---|---|---|---|
| P-001 | 2026-10-21 | Análise de requisitos, levantamento de decisões, especificação e ADR proposto | 12 decisões de negócio, 1 convenção, 7 itens pendentes, ADR-0001 proposto | [historico-de-prompts.md](historico-de-prompts.md#p-001--definição-dos-requisitos-da-poc) |
| P-002 | 2026-10-22 | Convenções de Git no `CLAUDE.md` e commits da documentação | Documentação da P-001 aprovada e commitada em commits atômicos | [historico-de-prompts.md](historico-de-prompts.md#p-002--aprovação-da-documentação-e-convenções-de-git) |

## Indicadores acumulados

| Indicador | Valor | Observação |
|---|---|---|
| Etapas registradas | 2 | P-001, P-002 |
| Recomendações da IA decididas | 11 | 10 aceitas, 1 não adotada ([detalhes](decisoes-influenciadas-pela-ia.md)) |
| Sugestões incorretas evidenciadas | 0 | [sugestoes-incorretas.md](sugestoes-incorretas.md) |
| Linhas de código produzidas | 0 | Nenhum código foi escrito até agora |
| Testes automatizados | 0 | Nenhum teste existe ainda |

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
