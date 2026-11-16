package br.com.oficina.billing.mensageria.output.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.mensageria.application.EventoSaga;
import br.com.oficina.billing.mensageria.application.TipoEvento;
import br.com.oficina.billing.mensageria.config.MensageriaProperties;
import br.com.oficina.billing.shared.observabilidade.Rastreamento;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class OutboxPublicadorEventosTest {

	private final OutboxEventoRepository repository = mock(OutboxEventoRepository.class);
	private final Rastreamento rastreamento = mock(Rastreamento.class);
	private final OutboxPublicadorEventos publicador = new OutboxPublicadorEventos(repository,
			JsonMapper.builder().build(),
			new MensageriaProperties("os.eventos", "billing.eventos", "oficina-billing-comandos",
					new MensageriaProperties.Outbox(false, 1000, 50, 7)),
			rastreamento);

	private static EventoSaga evento() {
		return new EventoSaga(TipoEvento.ORCAMENTO_GERADO, "orcamento-10", UUID.randomUUID(), 42L, null, "corr",
				Map.of("orcamentoId", 10));
	}

	private OutboxEvento gravado() {
		ArgumentCaptor<OutboxEvento> registro = ArgumentCaptor.forClass(OutboxEvento.class);
		verify(repository).save(registro.capture());
		return registro.getValue();
	}

	@Test
	void guardaOTraceAtualParaOEnvioContinuarOMesmoTrace() {
		when(repository.findByMessageId(any())).thenReturn(Optional.empty());
		when(rastreamento.capturar()).thenReturn(Map.of("traceparent", "00-abc-def-01"));

		publicador.publicar(evento());

		assertThat(gravado().getRastreamento()).isEqualTo("{\"traceparent\":\"00-abc-def-01\"}");
	}

	@Test
	void semTraceAtivoNaoGuardaNada() {
		when(repository.findByMessageId(any())).thenReturn(Optional.empty());
		when(rastreamento.capturar()).thenReturn(Map.of());

		publicador.publicar(evento());

		assertThat(gravado().getRastreamento()).isNull();
	}
}
