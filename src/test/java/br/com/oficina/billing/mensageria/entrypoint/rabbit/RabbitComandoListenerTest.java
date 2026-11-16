package br.com.oficina.billing.mensageria.entrypoint.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import br.com.oficina.billing.mensageria.application.ConsumidorComandos;
import br.com.oficina.billing.mensageria.application.MensagemRecebida;
import br.com.oficina.billing.mensageria.application.MensagemRejeitadaException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Map;

class RabbitComandoListenerTest {

	private final ConsumidorComandos consumidor = mock(ConsumidorComandos.class);
	private final RabbitComandoListener listener = new RabbitComandoListener(consumidor);

	private static Message mensagem(String correlationId) {
		MessageProperties propriedades = new MessageProperties();
		propriedades.setType("CobrarPagamento");
		if (correlationId != null) {
			propriedades.setHeader("x-correlation-id", correlationId);
		}
		return new Message("{\"tipo\": \"CobrarPagamento\"}".getBytes(StandardCharsets.UTF_8), propriedades);
	}

	@Test
	void converteCorpoTipoECorrelationId() {
		MensagemRecebida recebida = RabbitComandoListener.converter(mensagem("corr-1"));

		assertThat(recebida.corpo()).isEqualTo("{\"tipo\": \"CobrarPagamento\"}");
		assertThat(recebida.tipo()).isEqualTo("CobrarPagamento");
		assertThat(recebida.correlationId()).isEqualTo("corr-1");
		assertThat(RabbitComandoListener.converter(mensagem(null)).correlationId()).isNull();
		assertThat(recebida.rastreamento()).isEmpty();
	}

	@Test
	void levaSoOsCabecalhosDoTraceDistribuido() {
		Message comTrace = mensagem("corr-1");
		comTrace.getMessageProperties().setHeader("traceparent", "00-abc-def-01");
		comTrace.getMessageProperties().setHeader("newrelic", "eyJ2Ijo");
		comTrace.getMessageProperties().setHeader("x-ordem-servico-id", 42);

		assertThat(RabbitComandoListener.converter(comTrace).rastreamento())
				.containsOnly(Map.entry("traceparent", "00-abc-def-01"), Map.entry("newrelic", "eyJ2Ijo"));
	}

	@Test
	void rejeicaoDefinitivaVaiDiretoParaADlq() {
		doThrow(new MensagemRejeitadaException("inválida", null)).when(consumidor).receber(any());

		assertThatThrownBy(() -> listener.receber(mensagem("corr-1")))
				.isInstanceOf(AmqpRejectAndDontRequeueException.class)
				.hasMessage("inválida");
	}

	@Test
	void falhaTransitoriaSobeParaOContainerTentarDeNovo() {
		IllegalStateException bancoFora = new IllegalStateException("banco fora");
		doThrow(bancoFora).when(consumidor).receber(any());

		assertThatThrownBy(() -> listener.receber(mensagem("corr-1"))).isSameAs(bancoFora);
	}
}
