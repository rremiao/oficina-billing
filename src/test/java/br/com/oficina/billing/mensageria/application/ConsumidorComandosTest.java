package br.com.oficina.billing.mensageria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import br.com.oficina.billing.shared.exception.RegraNegocioException;
import br.com.oficina.billing.shared.observabilidade.Rastreamento;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class ConsumidorComandosTest {

	private static final String SAGA = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";

	private final Rastreamento rastreamento = mock(Rastreamento.class);
	private final List<EnvelopeMensagem> recebidos = new ArrayList<>();
	private final List<String> correlacaoDuranteOProcessamento = new ArrayList<>();

	private ConsumidorComandos consumidor(Consumer<EnvelopeMensagem> reacao) {
		ManipuladorComando manipulador = new ManipuladorComando() {
			@Override
			public TipoComando tipo() {
				return TipoComando.COBRAR_PAGAMENTO;
			}

			@Override
			public void manipular(EnvelopeMensagem envelope) {
				correlacaoDuranteOProcessamento.add(MDC.get("correlationId"));
				recebidos.add(envelope);
				reacao.accept(envelope);
			}
		};
		return new ConsumidorComandos(List.of(manipulador), JsonMapper.builder().build(), rastreamento);
	}

	private static MensagemRecebida mensagem(String corpo) {
		return new MensagemRecebida(corpo, "CobrarPagamento", "corr-do-cabecalho");
	}

	private static String envelope(String tipo, String saga, String correlationId) {
		return """
				{"tipo": "%s", "sagaId": %s, "ordemServicoId": 42, "correlationId": %s, "dados": {"orcamentoId": 10}}
				""".formatted(tipo, saga == null ? "null" : "\"" + saga + "\"",
				correlationId == null ? "null" : "\"" + correlationId + "\"");
	}

	@Test
	void continuaOTraceDeQuemEnviouENomeiaATransacaoPeloComando() {
		Map<String, String> trace = Map.of("traceparent", "00-abc-def-01");

		consumidor(e -> { }).receber(new MensagemRecebida(envelope("CobrarPagamento", SAGA, "corr"),
				"CobrarPagamento", "corr", trace));

		var ordem = inOrder(rastreamento);
		ordem.verify(rastreamento).continuar(trace);
		ordem.verify(rastreamento).nomearTransacao("Mensagem", "CobrarPagamento");
		assertThat(recebidos).hasSize(1);
	}

	@Test
	void entregaAoManipuladorDoTipoComOCorrelationIdDoEnvelopeNoLog() {
		consumidor(e -> { }).receber(mensagem(envelope("CobrarPagamento", SAGA, "corr-do-envelope")));

		assertThat(recebidos).singleElement().satisfies(e -> {
			assertThat(e.sagaId()).hasToString(SAGA);
			assertThat(e.dados().get("orcamentoId").asLong()).isEqualTo(10L);
		});
		assertThat(correlacaoDuranteOProcessamento).containsExactly("corr-do-envelope");
		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	void semCorrelationIdNoEnvelopeUsaODoCabecalhoESemNenhumGeraUm() {
		consumidor(e -> { }).receber(mensagem(envelope("CobrarPagamento", SAGA, null)));
		consumidor(e -> { }).receber(new MensagemRecebida(envelope("CobrarPagamento", SAGA, null), null, null));

		assertThat(correlacaoDuranteOProcessamento.get(0)).isEqualTo("corr-do-cabecalho");
		assertThat(correlacaoDuranteOProcessamento.get(1)).isNotBlank();
	}

	@Test
	void mensagemQueNuncaVaiDarCertoERejeitadaDeVez() {
		ConsumidorComandos consumidor = consumidor(e -> { });

		assertThatThrownBy(() -> consumidor.receber(mensagem("não é json")))
				.isInstanceOf(MensagemRejeitadaException.class)
				.hasMessage("Corpo da mensagem não é um envelope JSON válido");
		assertThatThrownBy(() -> consumidor.receber(mensagem("null"))).hasMessage("Envelope sem tipo");
		assertThatThrownBy(() -> consumidor.receber(mensagem(envelope(" ", SAGA, null)))).hasMessage("Envelope sem tipo");
		assertThatThrownBy(() -> consumidor.receber(mensagem(envelope("CobrarPagamento", null, null))))
				.hasMessage("Envelope sem sagaId");
		assertThatThrownBy(() -> consumidor.receber(mensagem("{\"tipo\": \"CobrarPagamento\", \"sagaId\": \"" + SAGA + "\"}")))
				.hasMessage("Envelope sem ordemServicoId");
		assertThatThrownBy(() -> consumidor.receber(mensagem(envelope("OrcamentoGerado", SAGA, null))))
				.hasMessage("Nenhum manipulador para o comando OrcamentoGerado");
		assertThat(recebidos).isEmpty();
		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	void regraDeNegocioRecusadaTambemERejeitadaDeVez() {
		ConsumidorComandos consumidor = consumidor(e -> {
			throw new RegraNegocioException("Orçamento 10 não existe para ser cobrado");
		});

		assertThatThrownBy(() -> consumidor.receber(mensagem(envelope("CobrarPagamento", SAGA, null))))
				.isInstanceOf(MensagemRejeitadaException.class)
				.hasMessage("Orçamento 10 não existe para ser cobrado");
	}

	@Test
	void falhaTransitoriaSobeComoEstaParaSerTentadaDeNovo() {
		IllegalStateException bancoFora = new IllegalStateException("banco fora");
		ConsumidorComandos consumidor = consumidor(e -> {
			throw bancoFora;
		});

		assertThatThrownBy(() -> consumidor.receber(mensagem(envelope("CobrarPagamento", SAGA, null)))).isSameAs(bancoFora);
	}

	@Test
	void doisManipuladoresParaOMesmoComandoImpedemASubida() {
		ManipuladorComando um = new ManipuladorComando() {
			@Override
			public TipoComando tipo() {
				return TipoComando.SOLICITAR_ORCAMENTO;
			}

			@Override
			public void manipular(EnvelopeMensagem envelope) {
				// sem efeito
			}
		};

		assertThatThrownBy(() -> new ConsumidorComandos(List.of(um, um), JsonMapper.builder().build(), rastreamento))
				.isInstanceOf(IllegalStateException.class);
	}
}
