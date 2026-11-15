-- Catálogo de serviços com preço, vindo do monólito (migration V3 da Fase 3), sem mudança de colunas.
CREATE TABLE servico
(
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120)   NOT NULL,
    descricao        VARCHAR(255) NULL,
    valor            NUMERIC(10, 2) NOT NULL CHECK (valor > 0),
    ativo            BOOLEAN        NOT NULL DEFAULT TRUE,
    data_criacao     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NULL
);

-- Nome único sem diferenciar maiúsculas: a checagem da aplicação sozinha deixa passar dois cadastros
-- simultâneos com o mesmo nome.
CREATE UNIQUE INDEX ux_servico_nome ON servico (lower(nome));
