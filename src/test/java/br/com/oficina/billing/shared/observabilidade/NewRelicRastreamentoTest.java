package br.com.oficina.billing.shared.observabilidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.newrelic.api.agent.HeaderType;
import org.junit.jupiter.api.Test;

import java.util.Map;

/** Sem o agent anexado (como nos testes), a API do New Relic é no-op: nada quebra e não há trace. */
class NewRelicRastreamentoTest {

	private final NewRelicRastreamento rastreamento = new NewRelicRastreamento();

	@Test
	void semAgentNaoHaTraceParaCapturarEContinuarNaoQuebra() {
		assertThat(rastreamento.capturar()).isEmpty();
		assertThatNoException().isThrownBy(() -> {
			rastreamento.continuar(Map.of("traceparent", "00-abc-def-01"));
			rastreamento.continuar(Map.of());
			rastreamento.continuar(null);
			rastreamento.nomearTransacao("Mensagem", "CobrarPagamento");
		});
	}

	@Test
	void cabecalhosSaoDoTipoMensagemEGuardamUmValorPorNome() {
		CabecalhosRastreamento cabecalhos = new CabecalhosRastreamento(Map.of("traceparent", "00-abc"));
		cabecalhos.setHeader("newrelic", "eyJ");
		cabecalhos.addHeader("tracestate", "nr");

		assertThat(cabecalhos.getHeaderType()).isEqualTo(HeaderType.MESSAGE);
		assertThat(cabecalhos.getHeader("traceparent")).isEqualTo("00-abc");
		assertThat(cabecalhos.getHeaders("newrelic")).containsExactly("eyJ");
		assertThat(cabecalhos.getHeaders("ausente")).isEmpty();
		assertThat(cabecalhos.containsHeader("tracestate")).isTrue();
		assertThat(cabecalhos.getHeaderNames()).containsExactlyInAnyOrder("traceparent", "newrelic", "tracestate");
		assertThat(cabecalhos.comoMapa()).hasSize(3);
	}
}
