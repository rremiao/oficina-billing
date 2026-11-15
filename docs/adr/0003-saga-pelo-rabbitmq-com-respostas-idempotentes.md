# ADR 0003 — Saga pelo RabbitMQ, com respostas idempotentes

- **Status:** aceita
- **Data:** 2026-11-14

## Contexto

A Saga é orquestrada pelo OS Service, que já roda em RabbitMQ (ADR 0002 do OS Service). Ele publica
`SolicitarOrcamento`, `CobrarPagamento` e `EstornarPagamento` em `os.eventos` e espera `OrcamentoGerado`,
`PagamentoConfirmado`, `PagamentoRecusado` e `PagamentoEstornado` em `billing.eventos`. A entrega é
"pelo menos uma vez", e o OS Service reenvia `SolicitarOrcamento` (5 min) e `EstornarPagamento` (10 min)
com o mesmo `messageId`, pedindo que o Billing **responda de novo** se já tinha processado.

## Decisão

- **RabbitMQ, mesma topologia do OS Service.** Fila `oficina-billing-comandos` ligada a `os.eventos`
  pelos três comandos, com DLX e DLQ (`oficina-billing-comandos-dlq`). O Billing publica em
  `billing.eventos` com a routing key igual ao tipo.
- **Transactional Outbox.** O caso de uso grava o evento na tabela `outbox_evento` na mesma transação da
  mudança; um envio agendado publica com publisher confirm. Broker fora não perde evento.
- **Idempotência nos casos de uso, sem tabela de mensagens processadas.** Cada caso de uso já é
  idempotente pelo próprio dado: orçamento por `sagaId`, pagamento por orçamento, estorno pelo status.
  Todo comando que chega passa pelo caso de uso, que, se o fato já existe, só responde de novo. Assim a
  regra "responda de novo" do contrato vale sem lógica extra.
- **`messageId` determinístico nas respostas.** O id do evento sai do tipo e do fato
  (`UUID.nameUUIDFromBytes("billing:<tipo>:<sagaId ou pagamentoId>")`). Responder de novo só marca o
  mesmo registro do outbox para reenvio, e o OS Service descarta a repetição pelo `messageId`, em vez de
  mandá-la para a DLQ dele.
- **Falhas.** Mensagem inválida (JSON quebrado, sem `sagaId`, `tipo` ou `ordemServicoId`, sem campo
  obrigatório nos `dados`) e regra de negócio (item fora do catálogo, orçamento de outra Saga) vão direto
  para a DLQ. Falha transitória (banco, Mercado Pago) ganha 3 tentativas com espera crescente e depois vai
  para a DLQ. Mensagem na DLQ nunca volta sozinha.

## Consequências

- Duas entregas simultâneas do mesmo comando podem esbarrar num índice único (orçamento por `sagaId`,
  pagamento por orçamento); a perdedora falha como transitória, é tentada de novo e responde o já gravado.
- Pelo contrato, não há evento de falha de orçamento: pedido com item fora do catálogo vai para a DLQ
  sem resposta, e o OS Service reenvia e depois libera a OS.
- Para a Saga de ponta a ponta, Billing e OS Service precisam do mesmo broker. O `docker-compose` do
  Billing sobe um RabbitMQ próprio só para rodar o serviço sozinho.
