package br.com.oficina.billing.pagamento.output.orcamento;

import br.com.oficina.billing.orcamento.application.gateway.OrcamentoGateway;
import br.com.oficina.billing.orcamento.domain.Orcamento;
import br.com.oficina.billing.pagamento.application.gateway.OrcamentoParaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.ItemCobranca;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Liga o pagamento aos orçamentos do próprio Billing, traduzindo as linhas em itens de cobrança. */
@Component
class OrcamentoParaCobrancaAdapter implements OrcamentoParaCobranca {

	private final OrcamentoGateway orcamentoGateway;

	OrcamentoParaCobrancaAdapter(OrcamentoGateway orcamentoGateway) {
		this.orcamentoGateway = orcamentoGateway;
	}

	@Override
	public Optional<OrcamentoACobrar> buscar(Long orcamentoId) {
		return orcamentoGateway.buscarPorId(orcamentoId).map(OrcamentoParaCobrancaAdapter::paraCobranca);
	}

	private static OrcamentoACobrar paraCobranca(Orcamento orcamento) {
		Orcamento.Cabecalho cabecalho = orcamento.getCabecalho();
		List<ItemCobranca> itens = Stream.concat(
				orcamento.getServicos().stream()
						.map(s -> new ItemCobranca("SERVICO-" + s.servicoId(), s.nome(), 1, s.valorUnitario())),
				orcamento.getItens().stream()
						.map(i -> new ItemCobranca("ITEM-" + i.itemId(), i.nome(), i.quantidade(), i.valorUnitario())))
				.toList();
		return new OrcamentoACobrar(orcamento.getId(), cabecalho.sagaId(), cabecalho.ordemServicoId(),
				cabecalho.codigoOrdemServico(), cabecalho.clienteId(), orcamento.getValorTotal(), itens);
	}
}
