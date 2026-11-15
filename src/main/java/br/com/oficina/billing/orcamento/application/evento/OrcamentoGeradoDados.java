package br.com.oficina.billing.orcamento.application.evento;

import java.math.BigDecimal;

/** {@code dados} do evento {@code OrcamentoGerado}; o OS Service só exige o {@code orcamentoId}. */
public record OrcamentoGeradoDados(Long orcamentoId, BigDecimal valorServicos, BigDecimal valorItens,
		BigDecimal valorTotal) {
}
