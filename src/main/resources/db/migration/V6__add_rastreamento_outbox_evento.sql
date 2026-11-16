-- Cabeçalhos do trace (traceparent, tracestate, newrelic) da transação que gravou o evento, em JSON.
-- O envio do outbox roda depois, em outra thread; sem guardar o contexto aqui, o trace terminaria no
-- banco e o OS Service começaria um trace novo ao receber a resposta do Billing.
ALTER TABLE outbox_evento ADD COLUMN rastreamento TEXT NULL;
