# Decisões influenciadas pela IA

> Registro de como as sugestões da IA afetaram decisões de negócio e de arquitetura: o que foi
> sugerido, o que o usuário decidiu e onde está a evidência. Só entram itens com evidência na
> sessão correspondente.

## Legenda

- **Aceita:** a recomendação da IA virou a decisão sem alteração.
- **Modificada:** a decisão partiu da recomendação, com ajustes do usuário.
- **Não adotada:** o usuário escolheu outra opção.
- **Proposta:** ainda não decidida ou não validada.
- **Aceita em parte:** parte da sugestão foi implementada; o restante continua em aberto.
- **Rejeitada (inadequada):** a sugestão estava errada ou era inadequada para o contexto; ver
  [sugestoes-incorretas.md](sugestoes-incorretas.md).

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
| IA-13 | Estratégia multitenant | Detalhamento do ADR-0001: FKs compostas, `set_config(..., true)`, `NULLIF` na policy, roles separadas, tabela de credenciais para o login | Aceita em parte | Implementado em P-003, exceto o lookup de login (ainda em aberto); ADR segue Proposto | [ADR-0001](../adr/0001-estrategia-multitenant.md#evidências) |
| IA-14 | Persistência | JPA + Hibernate `@TenantId` | Aceita | ADR-0001, seção 3 | P-003 |
| IA-15 | Build | Maven Wrapper | Aceita | Wrapper fixado em Maven 3.10.0 | P-003 |
| IA-16 | Frontend | Adiar o frontend para a etapa da interface | Aceita | Sem `frontend/` em P-003 | P-003 |
| IA-17 | PV-05 | Confirmar um estorno por contribuição e garantir no banco (UNIQUE + grants sem UPDATE/DELETE) | Aceita | D-13 | P-003 |
| IA-18 | Versões da stack | Plano com Spring Boot 3.5 e PostgreSQL 17 | Não adotada | T-01: Spring Boot 4.1.1, PostgreSQL 18.6, Java 21 | P-003, revisão do plano |
| IA-19 | Instalação do JDK 21 | `winget install EclipseAdoptium.Temurin.21.JDK` | Rejeitada (inadequada) | JDK em pasta do usuário, ativado por sessão | [SI-001](sugestoes-incorretas.md#si-001--instalar-o-jdk-21-via-winget) |
| IA-20 | Vínculo do tenant à transação | Estender `JpaTransactionManager` e aplicar `set_config` no `doBegin`; UUID nulo como sentinela no Hibernate; teste de catálogo exigindo RLS em toda tabela; testes de mutação | Aceita (plano aprovado) | Implementado; evidências no ADR-0001 | P-003 |
| IA-21 | Configuração local | Fazer o backend ler o `.env` da raiz quando roda fora do Docker, mapeando `APP_USER_PASSWORD`/`APP_OWNER_PASSWORD` como fallback | Aceita | Implementado em `application.yml` | P-004 |
| IA-22 | Avisos de inspeção em `Goal` | Não tornar `final` nem local campos persistentes (a JPA proíbe e o Hibernate os preenche por reflexão); supressão pontual com justificativa | Aceita | `@SuppressWarnings` em `currency` e `status` | P-005 |

## Observações

- Dos 11 itens com recomendação explícita da IA decididos em P-001, 10 foram aceitos e 1 não foi
  adotado (IA-01). D-10 (cenários de demonstração) foi uma escolha múltipla sem recomendação, e
  C-01 (convenção de idioma) foi definida pelo usuário sem sugestão da IA.
- Em P-003, as 4 recomendações explícitas nas perguntas (IA-14 a IA-17) foram aceitas. A versão do
  Java e o pacote foram escolhas do usuário, sem recomendação da IA. Duas propostas do plano não
  foram adotadas: as versões (IA-18) e a instalação via `winget` (IA-19); a segunda foi registrada
  como sugestão inadequada.
- IA-01 não é considerada uma sugestão incorreta: foi uma preferência de modelagem do usuário,
  não um erro técnico. Ver [sugestoes-incorretas.md](sugestoes-incorretas.md).
- IA-13 foi implementada e testada em P-003, exceto o lookup de login.
