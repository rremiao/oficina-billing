package br.com.oficina.billing.pagamento.application.gateway;

import br.com.oficina.billing.pagamento.domain.Pagamento;

import java.util.Optional;

/** Porta de persistência dos pagamentos. */
public interface PagamentoGateway {

	/**
	 * Grava e devolve o pagamento com id e datas. Um segundo pagamento para o mesmo orçamento é
	 * recusado pelo banco, e duas atualizações simultâneas do mesmo pagamento também (a segunda falha
	 * e é tentada de novo): webhook e estorno ao mesmo tempo não se sobrescrevem.
	 */
	Pagamento salvar(Pagamento pagamento);

	Optional<Pagamento> buscarPorId(Long id);

	Optional<Pagamento> buscarPorOrcamentoId(Long orcamentoId);

	Optional<Pagamento> buscarPorIdCobranca(String idCobranca);
}
