package br.com.oficina.billing.orcamento.application.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pedido de orçamento do OS Service: o envelope e os {@code dados} do comando
 * {@code SolicitarOrcamento}. Traz só ids e quantidades; os preços são do catálogo do Billing.
 *
 * @param sagaId             ciclo da Saga; devolvido no {@code OrcamentoGerado}
 * @param ordemServicoId     id da OS no OS Service
 * @param codigoOrdemServico código legível da OS, opcional
 * @param clienteId          dono da OS
 * @param veiculoId          veículo da OS, opcional
 * @param servicoIds         serviços do diagnóstico
 * @param itens              peças e insumos do diagnóstico, com quantidade
 */
public record SolicitacaoOrcamento(
		UUID sagaId,
		Long ordemServicoId,
		String codigoOrdemServico,
		Long clienteId,
		Long veiculoId,
		List<Long> servicoIds,
		List<ItemSolicitado> itens
) {

	/** Lista ausente vira vazia. Id nulo dentro da lista é mantido para virar regra de negócio no caso de uso. */
	public SolicitacaoOrcamento {
		servicoIds = servicoIds == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(servicoIds));
		itens = itens == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(itens));
	}

	public record ItemSolicitado(Long itemId, int quantidade) {
	}
}
