# Sugestões incorretas ou inadequadas da IA

> Registro de sugestões da IA que se mostraram erradas, inseguras ou inadequadas, com evidência,
> decisão tomada e aprendizado. Uma entrada só é criada quando houver evidência concreta
> (saída de teste, erro de build, revisão, documentação oficial etc.).

## Situação atual

Até a etapa P-007 (2026-10-23): **2 sugestões incorretas ou inadequadas registradas** (SI-001, SI-002).

A recomendação não adotada em P-001 (IA-01, papel familiar + ACL) não entra aqui: foi uma
preferência de modelagem do usuário, não um erro técnico.
Ver [decisoes-influenciadas-pela-ia.md](decisoes-influenciadas-pela-ia.md).

## Modelo de registro

```markdown
### SI-NNN — <título curto>

- **Data / etapa:** AAAA-MM-DD, P-NNN
- **Sugestão da IA:** o que foi sugerido ou implementado.
- **Problema:** por que estava errado ou inadequado.
- **Evidência:** saída de teste, mensagem de erro, trecho de revisão ou referência (link/commit).
- **Decisão tomada:** o que foi feito no lugar.
- **Aprendizado:** o que muda daqui em diante (e se gerou regra no CLAUDE.md).
```

## Registro

### SI-001 — Instalar o JDK 21 via winget

- **Data / etapa:** 2026-10-22, P-003 (planejamento).
- **Sugestão da IA:** o plano listava como pré-requisito instalar o JDK 21 com
  `winget install EclipseAdoptium.Temurin.21.JDK`, porque só o JDK 17 estava disponível.
- **Problema:** a máquina tem outro projeto que depende do Java 17 e do Gradle 8.3 globais. O pacote
  winget do Temurin instala com `ADDLOCAL=ALL`, que sobrescreve `PATH`, `JAVA_HOME` e o registro
  JavaSoft, e quebraria esse projeto. A IA verificou as versões instaladas, mas não perguntou se a
  configuração global podia ser alterada antes de propor uma instalação global.
- **Evidência:** o usuário rejeitou o plano e enviou o resumo de ambiente com a restrição e a
  explicação sobre `ADDLOCAL=ALL` (texto integral em
  [historico-de-prompts.md, P-003](historico-de-prompts.md#p-003--fundação-técnica-monorepo-compose-rls-e-testes-de-isolamento)).
  A instalação não chegou a ser executada.
- **Decisão tomada:** o JDK 21 e o Maven 3.10.0 ficam em pastas do usuário e são ativados apenas na
  sessão do comando. O projeto usa o Maven Wrapper, e nada global é alterado.
- **Aprendizado:** antes de propor instalações ou mudanças de configuração globais, verificar e
  perguntar sobre restrições da máquina. Preferir ferramentas isoladas por projeto ou sessão.
  Regra registrada no `CLAUDE.md` (não alterar JDK, Maven ou variáveis globais).

### SI-002 — Resultado esperado errado no plano da P-007

- **Data / etapa:** 2026-10-23, P-007 (verificação).
- **Sugestão da IA:** o plano listava como critério de verificação "Carlos vê 4 objetivos" nos dados demo.
- **Problema:** Carlos tem vínculo com 3 objetivos (Viagem, Aluguel, Parcela do carro). O Videogame é
  o objetivo individual do Pedro, e pela D-01 só quem tem vínculo o vê. O comportamento esperado
  correto é 3; a expectativa do plano contrariava a regra que a própria etapa implementa.
- **Evidência:** saída de `GET /api/goals` com o token do Carlos no ambiente Compose (3 objetivos) e
  `R__demo_families.sql` (Carlos não é membro do Videogame).
- **Decisão tomada:** manter a implementação; corrigir a expectativa no report e no ADR-0001. O cenário
  Bruno verifica explicitamente que o Videogame não aparece para o Carlos.
- **Aprendizado:** derivar números esperados dos dados de origem (seeds) e das regras, não de memória.
