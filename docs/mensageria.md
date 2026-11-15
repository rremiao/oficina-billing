# Contrato de mensageria do Billing

O que o Billing consome do OS Service, o que publica de volta e as garantias. Broker: RabbitMQ. O
envelope é o mesmo do OS Service (`docs/mensageria.md` do repositório `oficina-os-service`). Decisão em
[ADR 0003](adr/0003-saga-pelo-rabbitmq-com-respostas-idempotentes.md).

## Topologia

```text
os.eventos (topic) ─→ oficina-billing-comandos (SolicitarOrcamento, CobrarPagamento, EstornarPagamento)
                          └─ rejeitada → oficina-billing-comandos-dlx → oficina-billing-comandos-dlq
billing.eventos (topic) ← Billing publica; routing key = tipo
```

A declaração é idempotente: o Billing declara `os.eventos` e `billing.eventos` com os mesmos argumentos
do OS Service (topic, durável).

## O que o Billing consome

| Tipo | `dados` | O que faz |
| --- | --- | --- |
| `SolicitarOrcamento` | `clienteId` (obrigatório), `veiculoId`, `servicos[{servicoId}]`, `itens[{itemId, quantidade}]` | Monta o orçamento com os preços do catálogo e responde `OrcamentoGerado` |
| `CobrarPagamento` | `orcamentoId` (obrigatório) | Cria a cobrança no Mercado Pago. O resultado sai depois, pelo webhook |
| `EstornarPagamento` | `pagamentoId` (obrigatório), `motivo` | Estorna no Mercado Pago e responde `PagamentoEstornado` |

No envelope, `sagaId`, `tipo` e `ordemServicoId` são obrigatórios.

## O que o Billing publica

| Tipo | Quando | `dados` |
| --- | --- | --- |
| `OrcamentoGerado` | Orçamento montado (ou pedido repetido) | `orcamentoId`, `valorServicos`, `valorItens`, `valorTotal` |
| `PagamentoConfirmado` | O Mercado Pago confirmou o pagamento (webhook) | `pagamentoId`, `orcamentoId`, `valorPago`, `metodo` |
| `PagamentoRecusado` | Cobrança recusada, cancelada ou expirada (webhook) | `orcamentoId`, `motivo` |
| `PagamentoEstornado` | Estorno feito (ou pedido repetido) | `pagamentoId`, `refundId`, `valorEstornado` |

Todo evento devolve o `sagaId` e o `correlationId` do comando que o originou; os eventos de pagamento
disparados pelo webhook usam os do `CobrarPagamento`. Propriedades AMQP: `message_id`, `type`,
`correlation_id`, `content_type` JSON, entrega persistente, cabeçalhos `x-correlation-id` e
`x-ordem-servico-id`.

## Garantias

- **Publicação pelo outbox.** O evento é gravado na transação da mudança e enviado com publisher
  confirm. Broker fora não perde evento.
- **`messageId` determinístico.** O mesmo fato tem sempre o mesmo id (`OrcamentoGerado` por `sagaId`;
  os de pagamento pelo `pagamentoId`). Comando repetido é respondido de novo com o mesmo id, e o OS
  Service descarta a repetição.
- **Consumo idempotente.** Comando repetido não duplica orçamento, cobrança nem estorno.
- **Falhas.** Mensagem inválida ou recusada por regra de negócio (item fora do catálogo, inativo,
  orçamento ou pagamento de outra Saga, estorno de pagamento não confirmado) vai direto para a DLQ, sem
  resposta. Falha transitória (banco, Mercado Pago) é tentada 3 vezes (500 ms, 1 s) e depois vai para a
  DLQ. Para reprocessar, mova a mensagem pelo painel do RabbitMQ depois de corrigir a causa.
