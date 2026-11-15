package br.com.oficina.billing.orcamento.entrypoint.mensageria;

import br.com.oficina.billing.mensageria.application.EnvelopeMensagem;
import br.com.oficina.billing.mensageria.application.LeitorDados;
import br.com.oficina.billing.mensageria.application.ManipuladorComando;
import br.com.oficina.billing.mensageria.application.TipoComando;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento.ItemSolicitado;
import br.com.oficina.billing.orcamento.application.usecase.GerarOrcamento;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * {@code SolicitarOrcamento}: {@code dados} = {@code clienteId}, {@code veiculoId},
 * {@code servicos[{servicoId}]} e {@code itens[{itemId, quantidade}]}, só ids, sem preço.
 */
@Component
class SolicitarOrcamentoManipulador implements ManipuladorComando {

	private final GerarOrcamento gerarOrcamento;
	private final JsonMapper jsonMapper;

	SolicitarOrcamentoManipulador(GerarOrcamento gerarOrcamento, JsonMapper jsonMapper) {
		this.gerarOrcamento = gerarOrcamento;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public TipoComando tipo() {
		return TipoComando.SOLICITAR_ORCAMENTO;
	}

	@Override
	public void manipular(EnvelopeMensagem envelope) {
		Dados dados = LeitorDados.ler(jsonMapper, envelope, Dados.class);
		List<Long> servicos = dados.servicos() == null ? List.of()
				: dados.servicos().stream().map(s -> s == null ? null : s.servicoId()).toList();
		List<ItemSolicitado> itens = dados.itens() == null ? List.of()
				: dados.itens().stream().map(i -> i == null ? null
						: new ItemSolicitado(i.itemId(), i.quantidade() == null ? 0 : i.quantidade())).toList();

		gerarOrcamento.executar(new SolicitacaoOrcamento(envelope.sagaId(), envelope.ordemServicoId(),
				envelope.codigoOrdemServico(), LeitorDados.exigir(dados.clienteId(), "clienteId", envelope),
				dados.veiculoId(), servicos, itens));
	}

	record Dados(Long clienteId, Long veiculoId, List<Servico> servicos, List<Item> itens) {
	}

	record Servico(Long servicoId) {
	}

	record Item(Long itemId, Integer quantidade) {
	}
}
