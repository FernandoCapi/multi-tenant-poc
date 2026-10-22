# ADR-0002 — Autenticação com JWT

- **Status:** Aceito
- **Data:** 2026-10-22
- **Etapa:** P-006
- **Relacionados:** RF-05, RS-03, RS-04, DT-04; [ADR-0001](0001-estrategia-multitenant.md)

## Contexto

A POC exige login real, senhas com hash adequado e JWT (RF-05). O tenant é resolvido pelo servidor
no login e pode ir no token (DT-04); o cliente nunca define a família (RS-04). Um único serviço,
o backend, emite e valida os tokens. Não há cadastro nem troca de família (D-11).

## Decisão

| Tema | Decisão |
|---|---|
| Emissão e validação | O próprio backend, com o resource server do Spring Security (`NimbusJwtEncoder`/`NimbusJwtDecoder`). |
| Algoritmo | HS256, com segredo simétrico `JWT_SECRET` de no mínimo 32 bytes. A aplicação não sobe com um segredo menor. |
| Validade | 60 minutos, sem refresh token: ao expirar, o usuário faz login de novo. |
| Claims | `iss` = `vesta`, `sub` = id do usuário, `family_id`, `iat`, `exp`. |
| Validação | Assinatura, expiração (com a tolerância padrão de 60 s), emissor e `sub`/`family_id` em formato UUID. Qualquer falha → 401. |
| Senhas | `DelegatingPasswordEncoder` (bcrypt), com hash no formato `{bcrypt}...`. |
| Falha de login | Mesma resposta (401, "E-mail ou senha inválidos.") para e-mail inexistente e senha errada. Para e-mail inexistente, o backend compara a senha com um hash fictício, então os dois casos custam o mesmo tempo. |
| Sessão | Stateless; CSRF desligado, porque o token vai no cabeçalho `Authorization` e não é enviado automaticamente pelo navegador. |
| Erros | `application/problem+json` em português; o corpo nunca diz por que o token foi rejeitado. |
| Rotas públicas | `POST /api/auth/login` e `/actuator/health`. Todo o resto exige token. |

O tenant da requisição vem do `family_id` do token, aplicado pelo `TenantContextFilter`
([ADR-0001, seção 6](0001-estrategia-multitenant.md#6-resolução-do-tenant-claim-no-jwt)).

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| RS256 com par de chaves | Permitiria que outros serviços validassem o token só com a chave pública. Com um único serviço, só adicionaria a gestão de chaves. |
| Refresh token | Mais realista, mas exige endpoint, armazenamento e revogação. Para a demonstração, 60 minutos bastam. |
| Sessão no servidor (cookie) | Contraria RF-05 (JWT) e exigiria proteção contra CSRF. |
| Consultar o vínculo usuário–família a cada requisição | Discutida no ADR-0001, alternativa C: revogação imediata em troca de uma consulta por requisição. |

## Consequências

- Revogação só por expiração: um token emitido continua válido até expirar, mesmo que o vínculo
  mude. É aceitável na POC, que não tem troca de família nem remoção de usuário.
- Trocar o `JWT_SECRET` invalida todos os tokens emitidos.
- O segredo é único por ambiente e nunca deve ir para o Git; o `.env.example` traz só um valor local.
- Um token válido que combine um usuário com a família de outro, o que só aconteceria se o segredo
  vazasse, não dá acesso: o usuário não é visível naquela família e a resposta é 401
  (teste `tokenWhoseUserIsNotInItsFamilyIsUnauthorized`).

## Evidências

`LoginIntegrationTest` (4), `MeIntegrationTest` (8) e `TenantContextFilterTest` (3) passando em
2026-10-22, além da coleção Bruno `auth/` (7 requisições) contra o ambiente do Compose. Detalhes no
[ADR-0001, Evidências de P-006](0001-estrategia-multitenant.md#evidências-de-p-006).
