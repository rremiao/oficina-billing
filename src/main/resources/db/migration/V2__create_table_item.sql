-- Catálogo de peças e insumos com preço de venda, vindo do monólito (migration V4 da Fase 3). O saldo
-- (tabela estoque) e as movimentações ficaram com o serviço de Execução.
CREATE TABLE item
(
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120)   NOT NULL,
    descricao        VARCHAR(255) NULL,
    tipo             VARCHAR(20)    NOT NULL CHECK (tipo IN ('PECA', 'INSUMO')),
    valor_unitario   NUMERIC(10, 2) NOT NULL CHECK (valor_unitario > 0),
    unidade_medida   VARCHAR(10)    NOT NULL,
    ativo            BOOLEAN        NOT NULL DEFAULT TRUE,
    data_criacao     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NULL
);

-- Item duplicado é o mesmo tipo, nome e unidade, sem diferenciar maiúsculas. No monólito a regra usava
-- a descrição, que é opcional: dois itens diferentes sem descrição colidiam.
CREATE UNIQUE INDEX ux_item_tipo_nome_unidade ON item (tipo, lower(nome), lower(unidade_medida));
