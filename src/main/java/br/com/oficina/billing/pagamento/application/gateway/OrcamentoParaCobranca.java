package br.com.oficina.billing.pagamento.application.gateway;

import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.ItemCobranca;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** O que o pagamento precisa do orçamento: quanto cobrar, de quem e com quais linhas. */
public interface OrcamentoParaCobranca {

	Optional<OrcamentoACobrar> buscar(Long orcamentoId);

	record OrcamentoACobrar(Long id, UUID sagaId, Long ordemServicoId, String codigoOrdemServico, Long clienteId,
			BigDecimal valorTotal, List<ItemCobranca> itens) {
	}
}
