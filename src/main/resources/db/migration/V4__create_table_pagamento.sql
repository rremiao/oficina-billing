-- Pagamento de um orçamento pelo provedor (Mercado Pago). Um pagamento por orçamento: o comando
-- CobrarPagamento repetido encontra o pagamento existente em vez de criar outro.
CREATE TABLE pagamento
(
    id                   BIGSERIAL PRIMARY KEY,
    orcamento_id         BIGINT         NOT NULL REFERENCES orcamento (id),
    cliente_id           BIGINT         NOT NULL,
    saga_id              UUID           NOT NULL,
    ordem_servico_id     BIGINT         NOT NULL,
    codigo_ordem_servico VARCHAR(20) NULL,
    correlation_id       VARCHAR(128) NULL,
    valor                NUMERIC(10, 2) NOT NULL CHECK (valor > 0),
    status               VARCHAR(25)    NOT NULL
        CHECK (status IN ('AGUARDANDO_PAGAMENTO', 'CONFIRMADO', 'RECUSADO', 'ESTORNADO')),
    id_cobranca          VARCHAR(64)    NOT NULL,
    link_pagamento       VARCHAR(500) NULL,
    metodo               VARCHAR(40) NULL,
    motivo               VARCHAR(255) NULL,
    id_estorno           VARCHAR(64) NULL,
    valor_estornado      NUMERIC(10, 2) NULL CHECK (valor_estornado > 0),
    -- Última resposta do provedor, como veio, para auditoria e suporte
    resposta_provedor    JSONB NULL,
    -- Trava otimista: webhook e estorno ao mesmo tempo não sobrescrevem um ao outro
    versao               BIGINT         NOT NULL DEFAULT 0,
    data_criacao         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao     TIMESTAMP NULL,
    CONSTRAINT ux_pagamento_orcamento UNIQUE (orcamento_id),
    CONSTRAINT ux_pagamento_cobranca UNIQUE (id_cobranca)
);
