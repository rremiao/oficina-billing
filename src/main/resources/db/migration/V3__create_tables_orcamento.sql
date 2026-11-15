-- Orçamento de uma OS, montado a partir do pedido do OS Service (SolicitarOrcamento) com os preços do
-- catálogo. Cada linha guarda uma cópia do nome e do preço da época: mudar o catálogo depois não muda
-- orçamento já gerado. Referências ao OS Service (OS, cliente, veículo) são ids lógicos, sem FK.
CREATE TABLE orcamento
(
    id                   BIGSERIAL PRIMARY KEY,
    saga_id              UUID           NOT NULL,
    ordem_servico_id     BIGINT         NOT NULL,
    codigo_ordem_servico VARCHAR(20) NULL,
    cliente_id           BIGINT         NOT NULL,
    veiculo_id           BIGINT NULL,
    valor_servicos       NUMERIC(10, 2) NOT NULL CHECK (valor_servicos >= 0),
    valor_itens          NUMERIC(10, 2) NOT NULL CHECK (valor_itens >= 0),
    valor_total          NUMERIC(10, 2) NOT NULL CHECK (valor_total > 0),
    data_criacao         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Um orçamento por ciclo da Saga: o pedido repetido (entrega "pelo menos uma vez") não duplica
    CONSTRAINT ux_orcamento_saga UNIQUE (saga_id)
);

CREATE INDEX ix_orcamento_ordem_servico ON orcamento (ordem_servico_id);

CREATE TABLE orcamento_servico
(
    id             BIGSERIAL PRIMARY KEY,
    orcamento_id   BIGINT         NOT NULL REFERENCES orcamento (id),
    servico_id     BIGINT         NOT NULL REFERENCES servico (id),
    nome           VARCHAR(120)   NOT NULL,
    valor_unitario NUMERIC(10, 2) NOT NULL CHECK (valor_unitario > 0)
);

CREATE INDEX ix_orcamento_servico_orcamento ON orcamento_servico (orcamento_id);

CREATE TABLE orcamento_item
(
    id             BIGSERIAL PRIMARY KEY,
    orcamento_id   BIGINT         NOT NULL REFERENCES orcamento (id),
    item_id        BIGINT         NOT NULL REFERENCES item (id),
    nome           VARCHAR(120)   NOT NULL,
    quantidade     INTEGER        NOT NULL CHECK (quantidade > 0),
    valor_unitario NUMERIC(10, 2) NOT NULL CHECK (valor_unitario > 0),
    valor_total    NUMERIC(10, 2) NOT NULL CHECK (valor_total > 0)
);

CREATE INDEX ix_orcamento_item_orcamento ON orcamento_item (orcamento_id);
