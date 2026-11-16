# ADR 0002 — Pagamento pela Orders API do Mercado Pago

- **Status:** aceita
- **Data:** 2026-11-14

## Contexto

O enunciado pede a integração com o Mercado Pago para o pagamento do orçamento aprovado. O cliente
aprova o orçamento na API do OS Service, que manda `CobrarPagamento` ao Billing, e espera
`PagamentoConfirmado` ou `PagamentoRecusado`. A compensação da Saga é `EstornarPagamento`.

O código herdado da branch tinha os DTOs da Orders API, mas autenticava por `/oauth/token` com pedido
vazio, montava o pedido vazio e usava uma chave de idempotência fixa (`"testKey"`).

## Decisão

- **Orders API com checkout online por Pix.** O Billing cria a ordem (`POST /v1/orders`, `type: online`)
  já com a transação Pix (`payment_method: {id: pix, type: bank_transfer}`) e o pagador, que a API exige
  na criação, e guarda o `ticket_url` do Pix (o `checkout_url` fica de reserva). O cliente consulta esse
  link em `GET /pagamentos/orcamento/{id}` e paga. O pagamento não termina na chamada: termina no aviso
  do webhook.
- **Webhook com consulta.** O aviso do Mercado Pago (`POST /pagamentos/webhook/mercadopago`) é público,
  sem JWT, e tem a assinatura `x-signature` (HMAC-SHA256 com o segredo do webhook) conferida. Mesmo
  com assinatura válida, o aviso só diz qual ordem mudou: o status vem de uma consulta à API
  (`GET /v1/orders/{id}`). Um aviso forjado ou repetido nunca confirma pagamento sozinho.
- **Access token.** A autenticação é o access token das credenciais da aplicação
  (`MERCADOPAGO_ACCESS_TOKEN`). O fluxo OAuth serve para marketplace, agindo em nome de terceiros, e
  saiu junto com os DTOs dele.
- **Idempotência em três camadas.** Um pagamento por orçamento (índice único `ux_pagamento_orcamento`);
  `X-Idempotency-Key` fixa por orçamento na cobrança (`cobranca-orcamento-{id}`) e por pagamento no
  estorno (`estorno-pagamento-{id}`), então uma falha entre a chamada ao provedor e a gravação não
  cobra nem estorna duas vezes; e versão otimista na tabela, para webhook e estorno simultâneos não se
  sobrescreverem.
- **Status da ordem.** `processed` confirma; `failed`, `canceled` e `expired` recusam (a ordem que o
  cliente não pagou expira e vira `PagamentoRecusado`); `refunded` é estorno; os demais esperam.

## Consequências

- O provedor fica atrás da porta `ProvedorPagamento`: os casos de uso não conhecem o Mercado Pago.
- O webhook precisa ser configurado no painel do Mercado Pago (tópico "Order"), apontando para
  `/billing/v1/pagamentos/webhook/mercadopago` no API Gateway, e precisa estar fora da validação de
  JWT do Gateway também.
- **Não validado contra o sandbox.** Os testes usam a Orders API simulada (WireMock), com o formato
  dos DTOs herdados. Antes de produção, falta uma rodada com credenciais de teste do Mercado Pago, para
  confirmar o formato real das respostas (inclusive se o pedido online exige `payer`).
- Sem histórico de tentativas: a tabela guarda o status atual e a última resposta do provedor
  (`resposta_provedor`, em `jsonb`).
