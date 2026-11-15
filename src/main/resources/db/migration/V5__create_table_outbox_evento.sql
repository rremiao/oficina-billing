-- Transactional Outbox: o evento para o OS Service é gravado na mesma transação da mudança que o
-- provocou, e um envio agendado publica depois, com confirmação do broker. Broker fora não perde
-- evento: ele fica pendente (publicado_em nulo) e sai na próxima rodada.
--
-- O message_id é determinístico (o mesmo fato tem sempre o mesmo id): responder de novo a um comando
-- repetido só marca o evento para reenvio, e o OS Service descarta a repetição pelo id.
CREATE TABLE outbox_evento
(
    id               BIGSERIAL PRIMARY KEY,
    message_id       UUID         NOT NULL,
    tipo             VARCHAR(60)  NOT NULL,
    exchange         VARCHAR(100) NOT NULL,
    routing_key      VARCHAR(100) NOT NULL,
    saga_id          UUID         NOT NULL,
    ordem_servico_id BIGINT       NOT NULL,
    correlation_id   VARCHAR(128) NOT NULL,
    payload          TEXT         NOT NULL,
    criado_em        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_em     TIMESTAMP NULL,
    tentativas       INT          NOT NULL DEFAULT 0,
    ultimo_erro      VARCHAR(500) NULL,
    CONSTRAINT ux_outbox_evento_message_id UNIQUE (message_id)
);

-- O envio só lê pendentes, em ordem de criação: o índice parcial fica pequeno mesmo com o histórico
CREATE INDEX ix_outbox_evento_pendente ON outbox_evento (id) WHERE publicado_em IS NULL;
