package br.com.oficina.billing.pagamento.entrypoint.mensageria;

import br.com.oficina.billing.mensageria.application.EnvelopeMensagem;
import br.com.oficina.billing.mensageria.application.LeitorDados;
import br.com.oficina.billing.mensageria.application.ManipuladorComando;
import br.com.oficina.billing.mensageria.application.TipoComando;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoCobranca;
import br.com.oficina.billing.pagamento.application.usecase.CobrarPagamento;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** {@code CobrarPagamento}: {@code dados} = {@code orcamentoId}. O cliente aprovou na API do OS Service. */
@Component
class CobrarPagamentoManipulador implements ManipuladorComando {

	private final CobrarPagamento cobrarPagamento;
	private final JsonMapper jsonMapper;

	CobrarPagamentoManipulador(CobrarPagamento cobrarPagamento, JsonMapper jsonMapper) {
		this.cobrarPagamento = cobrarPagamento;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public TipoComando tipo() {
		return TipoComando.COBRAR_PAGAMENTO;
	}

	@Override
	public void manipular(EnvelopeMensagem envelope) {
		Dados dados = LeitorDados.ler(jsonMapper, envelope, Dados.class);
		cobrarPagamento.executar(new SolicitacaoCobranca(envelope.sagaId(),
				LeitorDados.exigir(dados.orcamentoId(), "orcamentoId", envelope), envelope.correlationId()));
	}

	record Dados(Long orcamentoId) {
	}
}
