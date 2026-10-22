# Decisões influenciadas pela IA

> Registro de como as sugestões da IA afetaram decisões de negócio e de arquitetura: o que foi
> sugerido, o que o usuário decidiu e onde está a evidência. Só entram itens com evidência na
> sessão correspondente.

## Legenda

- **Aceita:** a recomendação da IA virou a decisão sem alteração.
- **Modificada:** a decisão partiu da recomendação, com ajustes do usuário.
- **Não adotada:** o usuário escolheu outra opção.
- **Proposta:** ainda não decidida ou não validada.

## Registro

| ID | Tema | Sugestão da IA | Resultado | Decisão | Evidência |
|---|---|---|---|---|---|
| IA-01 | Modelo de permissões | Papel familiar (responsável/membro) + ACL por objetivo | Não adotada | D-01: somente ACL por objetivo, com DONO e MEMBRO | P-001, lote 1 |
| IA-02 | Natureza da contribuição | Valor realizado, imutável, com estorno | Aceita | D-02 | P-001, lote 1 |
| IA-03 | Quem registra contribuições | Próprio participante ou gestor em nome do participante | Aceita | D-03 | P-001, lote 1 |
| IA-04 | Parcelas e recorrência | Tudo é meta com valor-alvo; sem cronograma | Aceita | D-04 | P-001, lote 1 |
| IA-05 | Acesso × participação | Identificou a tensão entre D-01 ("membro/contribuinte") e o RF-03 e sugeriu papel + flag `contribui` | Aceita | D-05 | P-001, lote 2 |
| IA-06 | Donos | Vários donos, mínimo de um | Aceita | D-06 | P-001, lote 2 |
| IA-07 | Permissões do membro | Membro estorna a própria e pode sair | Aceita | D-07 | P-001, lote 2 |
| IA-08 | Ciclo de vida | ATIVO/CONCLUÍDO/ARQUIVADO, conclusão manual, exclusão só sem contribuições | Aceita | D-08 | P-001, lote 2 |
| IA-09 | Códigos de resposta | Identificou a ambiguidade de usar 404 para ação sem permissão em recurso visível; sugeriu 404 sem leitura e 403 com leitura | Aceita | D-09 | P-001, lote 3 |
| IA-10 | Origem dos usuários | Somente seeds | Aceita | D-11 | P-001, lote 3 |
| IA-11 | Moeda e excedente | BRL com centavos, aceitando excedente | Aceita | D-12 | P-001, lote 3 |
| IA-12 | Itens adiáveis | Padrões provisórios PV-01..PV-07 | Proposta | Pendentes de validação | [decisoes-de-negocio.md](../decisoes-de-negocio.md#itens-pendentes-de-validação) |
| IA-13 | Estratégia multitenant | Detalhamento do ADR-0001: FKs compostas, `set_config(..., true)`, `NULLIF` na policy, roles separadas, tabela de credenciais para o login | Proposta | ADR-0001 com status Proposto | [ADR-0001](../adr/0001-estrategia-multitenant.md) |

## Observações

- Dos 11 itens com recomendação explícita da IA decididos em P-001, 10 foram aceitos e 1 não foi
  adotado (IA-01). D-10 (cenários de demonstração) foi uma escolha múltipla sem recomendação, e
  C-01 (convenção de idioma) foi definida pelo usuário sem sugestão da IA.
- IA-01 não é considerada uma sugestão incorreta: foi uma preferência de modelagem do usuário,
  não um erro técnico. Ver [sugestoes-incorretas.md](sugestoes-incorretas.md).
- IA-13 descreve decisões técnicas **propostas**; nenhuma foi implementada ou testada ainda.
