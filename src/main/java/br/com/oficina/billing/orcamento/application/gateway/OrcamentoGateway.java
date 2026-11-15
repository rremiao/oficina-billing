package br.com.oficina.billing.orcamento.application.gateway;

import br.com.oficina.billing.orcamento.domain.Orcamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos orçamentos. */
public interface OrcamentoGateway {

	/**
	 * Grava e devolve o orçamento com id e data. Um segundo orçamento para o mesmo {@code sagaId} é
	 * recusado pelo banco ({@code DataIntegrityViolationException}): duas entregas simultâneas do mesmo
	 * pedido gravam um orçamento só.
	 */
	Orcamento salvar(Orcamento orcamento);

	Optional<Orcamento> buscarPorId(Long id);

	Optional<Orcamento> buscarPorSagaId(UUID sagaId);

	/** Orçamentos de uma OS, do mais recente para o mais antigo (um por ciclo da Saga). */
	List<Orcamento> listarPorOrdemServico(Long ordemServicoId);
}
