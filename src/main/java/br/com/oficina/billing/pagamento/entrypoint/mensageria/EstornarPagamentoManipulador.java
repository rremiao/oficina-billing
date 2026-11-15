package br.com.oficina.billing.pagamento.entrypoint.mensageria;

import br.com.oficina.billing.mensageria.application.EnvelopeMensagem;
import br.com.oficina.billing.mensageria.application.LeitorDados;
import br.com.oficina.billing.mensageria.application.ManipuladorComando;
import br.com.oficina.billing.mensageria.application.TipoComando;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoEstorno;
import br.com.oficina.billing.pagamento.application.usecase.EstornarPagamento;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** {@code EstornarPagamento}: {@code dados} = {@code pagamentoId} e {@code motivo}. Compensação da Saga. */
@Component
class EstornarPagamentoManipulador implements ManipuladorComando {

	private final EstornarPagamento estornarPagamento;
	private final JsonMapper jsonMapper;

	EstornarPagamentoManipulador(EstornarPagamento estornarPagamento, JsonMapper jsonMapper) {
		this.estornarPagamento = estornarPagamento;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public TipoComando tipo() {
		return TipoComando.ESTORNAR_PAGAMENTO;
	}

	@Override
	public void manipular(EnvelopeMensagem envelope) {
		Dados dados = LeitorDados.ler(jsonMapper, envelope, Dados.class);
		estornarPagamento.executar(new SolicitacaoEstorno(envelope.sagaId(),
				LeitorDados.exigir(dados.pagamentoId(), "pagamentoId", envelope), dados.motivo()));
	}

	record Dados(Long pagamentoId, String motivo) {
	}
}
