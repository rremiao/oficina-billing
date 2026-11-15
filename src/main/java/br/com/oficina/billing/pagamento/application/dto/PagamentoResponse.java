package br.com.oficina.billing.pagamento.application.dto;

import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.pagamento.domain.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamento como a API mostra. A resposta bruta do provedor fica só no banco, para auditoria. */
public record PagamentoResponse(
		Long id,
		Long orcamentoId,
		Long ordemServicoId,
		StatusPagamento status,
		BigDecimal valor,
		String idCobranca,
		String linkPagamento,
		String metodo,
		String motivo,
		String idEstorno,
		BigDecimal valorEstornado,
		LocalDateTime dataCriacao,
		LocalDateTime dataAtualizacao
) {

	public static PagamentoResponse de(Pagamento pagamento) {
		return new PagamentoResponse(
				pagamento.getId(),
				pagamento.getOrcamentoId(),
				pagamento.getOrigem().ordemServicoId(),
				pagamento.getStatus(),
				pagamento.getValor(),
				pagamento.getIdCobranca(),
				pagamento.getLinkPagamento(),
				pagamento.getMetodo(),
				pagamento.getMotivo(),
				pagamento.getIdEstorno(),
				pagamento.getValorEstornado(),
				pagamento.getDataCriacao(),
				pagamento.getDataAtualizacao());
	}
}
