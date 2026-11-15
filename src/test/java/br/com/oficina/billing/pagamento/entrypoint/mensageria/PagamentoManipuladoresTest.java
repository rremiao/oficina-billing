package br.com.oficina.billing.pagamento.entrypoint.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.oficina.billing.mensageria.application.EnvelopeMensagem;
import br.com.oficina.billing.mensageria.application.MensagemInvalidaException;
import br.com.oficina.billing.mensageria.application.TipoComando;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoCobranca;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoEstorno;
import br.com.oficina.billing.pagamento.application.usecase.CobrarPagamento;
import br.com.oficina.billing.pagamento.application.usecase.EstornarPagamento;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

class PagamentoManipuladoresTest {

	private static final UUID SAGA = UUID.randomUUID();
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final CobrarPagamento cobrarPagamento = mock(CobrarPagamento.class);
	private final EstornarPagamento estornarPagamento = mock(EstornarPagamento.class);
	private final CobrarPagamentoManipulador cobrar = new CobrarPagamentoManipulador(cobrarPagamento, JSON);
	private final EstornarPagamentoManipulador estornar = new EstornarPagamentoManipulador(estornarPagamento, JSON);

	private static EnvelopeMensagem envelope(String tipo, String dados) {
		return new EnvelopeMensagem(UUID.randomUUID(), tipo, SAGA, 42L, null, "corr-1", null, 1, JSON.readTree(dados));
	}

	@Test
	void cobrarPagamentoLevaOOrcamentoEOCorrelationId() {
		cobrar.manipular(envelope("CobrarPagamento", "{\"orcamentoId\": 10}"));

		assertThat(cobrar.tipo()).isEqualTo(TipoComando.COBRAR_PAGAMENTO);
		verify(cobrarPagamento).executar(new SolicitacaoCobranca(SAGA, 10L, "corr-1"));
	}

	@Test
	void estornarPagamentoLevaOPagamentoEOMotivo() {
		estornar.manipular(envelope("EstornarPagamento", "{\"pagamentoId\": 5, \"motivo\": \"Peça danificada\"}"));

		assertThat(estornar.tipo()).isEqualTo(TipoComando.ESTORNAR_PAGAMENTO);
		verify(estornarPagamento).executar(new SolicitacaoEstorno(SAGA, 5L, "Peça danificada"));
	}

	@Test
	void semOIdObrigatorioEMensagemInvalida() {
		assertThatThrownBy(() -> cobrar.manipular(envelope("CobrarPagamento", "{}")))
				.isInstanceOf(MensagemInvalidaException.class)
				.hasMessage("Comando CobrarPagamento sem orcamentoId");
		assertThatThrownBy(() -> estornar.manipular(envelope("EstornarPagamento", "{\"motivo\": \"x\"}")))
				.hasMessage("Comando EstornarPagamento sem pagamentoId");
	}
}
