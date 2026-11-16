package br.com.oficina.billing.mensageria.output.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.mensageria.config.MensageriaProperties;
import br.com.oficina.billing.mensageria.output.outbox.Broker.FalhaPublicacaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import org.mockito.ArgumentCaptor;
import br.com.oficina.billing.mensageria.output.outbox.Broker.MensagemSaida;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class OutboxEnvioTest {

	private final OutboxEventoRepository repository = mock(OutboxEventoRepository.class);
	private final Broker broker = mock(Broker.class);
	private final TransactionTemplate transacao = mock(TransactionTemplate.class);
	private final MensageriaProperties propriedades = new MensageriaProperties("os.eventos", "billing.eventos",
			"oficina-billing-comandos", new MensageriaProperties.Outbox(false, 1000, 50, 7));
	private final OutboxEnvio envio = new OutboxEnvio(repository, broker, transacao, propriedades,
			JsonMapper.builder().build());

	@BeforeEach
	@SuppressWarnings("unchecked")
	void transacaoExecutaOCallback() {
		when(transacao.execute(any())).thenAnswer(i -> ((TransactionCallback<Integer>) i.getArgument(0)).doInTransaction(null));
	}

	private static OutboxEvento evento(String tipo) {
		OutboxEvento evento = new OutboxEvento();
		evento.setMessageId(UUID.randomUUID());
		evento.setTipo(tipo);
		evento.setExchange("billing.eventos");
		evento.setRoutingKey(tipo);
		evento.setOrdemServicoId(42L);
		evento.setCorrelationId("corr");
		evento.setPayload("{}");
		evento.aoCriar();
		return evento;
	}

	@Test
	void enviaOsPendentesEMarcaComoEnviados() {
		OutboxEvento primeiro = evento("OrcamentoGerado");
		OutboxEvento segundo = evento("PagamentoConfirmado");
		when(repository.travarPendentes(50)).thenReturn(List.of(primeiro, segundo));

		assertThat(envio.enviarPendentes()).isEqualTo(2);
		assertThat(primeiro.getPublicadoEm()).isNotNull();
		assertThat(segundo.getPublicadoEm()).isNotNull();
		verify(broker, times(2)).enviar(any());
	}

	@Test
	void eventoLevaOTraceGravadoEIgnoraRastreamentoInvalido() {
		OutboxEvento comTrace = evento("OrcamentoGerado");
		comTrace.setRastreamento("{\"traceparent\":\"00-abc-def-01\"}");
		OutboxEvento invalido = evento("PagamentoConfirmado");
		invalido.setRastreamento("não é json");
		OutboxEvento semTrace = evento("PagamentoEstornado");
		when(repository.travarPendentes(50)).thenReturn(List.of(comTrace, invalido, semTrace));

		assertThat(envio.enviarPendentes()).isEqualTo(3);

		ArgumentCaptor<MensagemSaida> enviadas = ArgumentCaptor.forClass(MensagemSaida.class);
		verify(broker, times(3)).enviar(enviadas.capture());
		assertThat(enviadas.getAllValues()).extracting(MensagemSaida::rastreamento)
				.containsExactly(Map.of("traceparent", "00-abc-def-01"), Map.of(), Map.of());
	}

	@Test
	void falhaRegistraATentativaEParaARodadaParaNaoInverterAOrdem() {
		OutboxEvento primeiro = evento("OrcamentoGerado");
		OutboxEvento segundo = evento("PagamentoConfirmado");
		when(repository.travarPendentes(50)).thenReturn(List.of(primeiro, segundo));
		doThrow(new FalhaPublicacaoException("x".repeat(600), null)).when(broker).enviar(any());

		assertThat(envio.enviarPendentes()).isZero();
		assertThat(primeiro.getPublicadoEm()).isNull();
		assertThat(primeiro.getTentativas()).isEqualTo(1);
		assertThat(primeiro.getUltimoErro()).hasSize(500);
		verify(broker, times(1)).enviar(any());
	}

	@Test
	void erroCurtoFicaInteiro() {
		OutboxEvento pendente = evento("OrcamentoGerado");
		pendente.setPublicadoEm(null);
		when(repository.travarPendentes(50)).thenReturn(List.of(pendente));
		doThrow(new FalhaPublicacaoException("canal fechado", null)).when(broker).enviar(any());

		envio.enviarPendentes();

		assertThat(pendente.getUltimoErro()).isEqualTo("canal fechado");
	}

	@Test
	void transacaoSemResultadoContaZero() {
		doReturn(null).when(transacao).execute(any());

		assertThat(envio.enviarPendentes()).isZero();
	}

	@Test
	void agendadorEnviaELimpaOsEnviadosForaDaRetencao() {
		OutboxAgendador agendador = new OutboxAgendador(envio, repository, propriedades);
		when(repository.travarPendentes(50)).thenReturn(List.of());

		agendador.enviar();
		agendador.limpar();

		verify(repository).travarPendentes(50);
		verify(repository).removerEnviadosAntesDe(any(LocalDateTime.class));
	}
}
