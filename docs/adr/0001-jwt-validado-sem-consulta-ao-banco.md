# ADR 0001 — JWT validado sem consulta ao banco

- **Status:** aceita
- **Data:** 2026-11-14

## Contexto

O Billing recebe dois tipos de token, os dois assinados em HS256 com o mesmo segredo:

- **funcionário**, emitido pelo login do OS Service (`tipo = OPERADOR`, `role` = `ADMIN` ou
  `ATENDENTE`);
- **cliente**, emitido pela Lambda de autenticação por CPF (`tipo = CLIENTE`, `clienteId`).

O OS Service recarrega o funcionário da tabela `usuario` a cada requisição, e assim um funcionário
inativado perde o acesso na hora. O Billing não tem essa tabela, e consultar o banco do OS Service é
proibido pelo enunciado.

## Decisão

O Billing valida assinatura e expiração e monta a autenticação só a partir das claims:

- token de funcionário vale com `role` `ADMIN` ou `ATENDENTE`; qualquer outro papel não autentica;
- token de cliente vale sempre como `ROLE_CLIENTE`, qualquer que seja a claim `role`;
- token sem a claim `tipo` é tratado como de funcionário (tokens antigos do OS Service).

A recusa sai em `application/problem+json`: 401 sem token válido e 403 quando o papel não tem acesso
à rota.

## Consequências

- Um funcionário inativado no OS Service continua acessando o Billing até o token expirar (2 h, o
  `SECURITY_JWT_EXPIRATION` do OS Service). Custo aceito: as rotas do Billing são de catálogo e
  consulta de orçamento, e reduzir esse prazo é configuração do OS Service.
- O segredo precisa ser o mesmo nos três pontos (Lambda, OS Service e Billing). Segredo com menos de
  32 bytes impede a aplicação de subir.
- O LoadBalancer do EKS é público: o Billing revalida o token mesmo atrás do API Gateway, como o OS
  Service.
