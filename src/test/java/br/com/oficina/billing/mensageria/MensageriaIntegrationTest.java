package br.com.oficina.billing.mensageria;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import br.com.oficina.billing.apoio.BancoDeTeste;
import br.com.oficina.billing.apoio.MercadoPagoSimulado;
import br.com.oficina.billing.apoio.OsServiceSimulado;
import br.com.oficina.billing.config.IntegrationTest;
import br.com.oficina.billing.mensageria.output.outbox.Broker;
import br.com.oficina.billing.mensageria.output.outbox.Broker.FalhaPublicacaoException;
import br.com.oficina.billing.mensageria.output.outbox.Broker.MensagemSaida;
import br.com.oficina.billing.mensageria.output.outbox.OutboxEnvio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A Saga pelo RabbitMQ de verdade: o comando do OS Service chega na fila, o caso de uso roda, o evento
 * vai para o outbox e sai em {@code billing.eventos}. Mensagem que nunca vai dar certo vai para a DLQ;
 * falha transitória é tentada de novo.
 */
@IntegrationTest
class MensageriaIntegrationTest {

	private static final Duration ESPERA = Duration.ofSeconds(15);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin admin;

	@Autowired
	private OutboxEnvio outboxEnvio;

	@Autowired
	private Broker broker;

	@Autowired
	private JdbcTemplate jdbc;

	private OsServiceSimulado os;

	@BeforeEach
	void ambienteLimpo() {
		BancoDeTeste.limpar(jdbc);
		MercadoPagoSimulado.reiniciar();
		jdbc.update("INSERT INTO servico (id, nome, valor) VALUES (30, 'Troca de pastilhas', 150.00)");
		jdbc.update("INSERT INTO item (id, nome, tipo, valor_unitario, unidade_medida) "
				+ "VALUES (20, 'Pastilha de freio', 'PECA', 89.90, 'JG')");
		os = new OsServiceSimulado(rabbitTemplate, admin);
		os.limpar();
	}

	private static Map<String, Object> pedidoDeOrcamento(long itemId) {
		return Map.of("clienteId", 7, "veiculoId", 3,
				"servicos", List.of(Map.of("servicoId", 30)),
				"itens", List.of(Map.of("itemId", itemId, "quantidade", 2)));
	}

	private int contar(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	@Test
	void pedidoDeOrcamentoViraOrcamentoEVoltaComoOrcamentoGerado() {
		UUID saga = UUID.randomUUID();
		String correlationId = os.enviar("SolicitarOrcamento", saga, 42L, pedidoDeOrcamento(20));

		await().atMost(ESPERA).until(() -> contar("SELECT count(*) FROM outbox_evento") == 1);
		assertThat(outboxEnvio.enviarPendentes()).isEqualTo(1);

		Message evento = os.proximoEvento();
		assertThat(evento).isNotNull();
		assertThat(evento.getMessageProperties().getType()).isEqualTo("OrcamentoGerado");
		assertThat(evento.getMessageProperties().getMessageId()).isEqualTo(
				UUID.nameUUIDFromBytes(("billing:OrcamentoGerado:" + saga).getBytes(StandardCharsets.UTF_8)).toString());
		assertThat(evento.getMessageProperties().<String>getHeader("x-correlation-id")).isEqualTo(correlationId);
		JsonNode corpo = os.corpo(evento);
		assertThat(corpo.get("sagaId").asString()).isEqualTo(saga.toString());
		assertThat(corpo.get("ordemServicoId").asLong()).isEqualTo(42L);
		assertThat(corpo.get("correlationId").asString()).isEqualTo(correlationId);
		assertThat(corpo.at("/dados/orcamentoId").asLong()).isPositive();
		assertThat(corpo.at("/dados/valorTotal").decimalValue()).isEqualByComparingTo("329.80");
	}

	@Test
	void pedidoRepetidoNaoDuplicaERespondeDeNovoComOMesmoMessageId() {
		UUID saga = UUID.randomUUID();
		os.enviar("SolicitarOrcamento", saga, 42L, pedidoDeOrcamento(20));
		await().atMost(ESPERA).until(() -> contar("SELECT count(*) FROM outbox_evento") == 1);
		outboxEnvio.enviarPendentes();
		String primeiraResposta = os.proximoEvento().getMessageProperties().getMessageId();

		os.enviar("SolicitarOrcamento", saga, 42L, pedidoDeOrcamento(20));
		await().atMost(ESPERA).until(() -> contar("SELECT count(*) FROM outbox_evento WHERE publicado_em IS NULL") == 1);
		outboxEnvio.enviarPendentes();
		String segundaResposta = os.proximoEvento().getMessageProperties().getMessageId();

		assertThat(segundaResposta).isEqualTo(primeiraResposta);
		assertThat(contar("SELECT count(*) FROM orcamento")).isEqualTo(1);
		assertThat(contar("SELECT count(*) FROM outbox_evento")).isEqualTo(1);
	}

	@Test
	void mensagemInvalidaVaiParaADlqSemEfeito() {
		os.enviarCorpo("SolicitarOrcamento", "isto não é json", "corr-invalida");
		assertThat(os.proximaNaDlq()).isNotNull();

		os.enviarCorpo("SolicitarOrcamento", "{\"tipo\": \"SolicitarOrcamento\", \"ordemServicoId\": 42}", "corr-sem-saga");
		assertThat(os.proximaNaDlq()).isNotNull();

		assertThat(contar("SELECT count(*) FROM orcamento")).isZero();
	}

	@Test
	void pedidoComItemForaDoCatalogoVaiParaADlqSemResposta() {
		os.enviar("SolicitarOrcamento", UUID.randomUUID(), 42L, pedidoDeOrcamento(999));

		Message rejeitada = os.proximaNaDlq();
		assertThat(rejeitada).isNotNull();
		assertThat(os.corpo(rejeitada).get("tipo").asString()).isEqualTo("SolicitarOrcamento");
		assertThat(contar("SELECT count(*) FROM orcamento")).isZero();
		assertThat(outboxEnvio.enviarPendentes()).isZero();
	}

	@Test
	void falhaDoMercadoPagoETentadaTresVezesEDepoisVaiParaADlq() {
		UUID saga = UUID.randomUUID();
		os.enviar("SolicitarOrcamento", saga, 42L, pedidoDeOrcamento(20));
		await().atMost(ESPERA).until(() -> contar("SELECT count(*) FROM orcamento") == 1);
		long orcamentoId = jdbc.queryForObject("SELECT id FROM orcamento", Long.class);
		MercadoPagoSimulado.falhaAoCriarOrdem();

		os.enviar("CobrarPagamento", saga, 42L, Map.of("orcamentoId", orcamentoId));

		assertThat(os.proximaNaDlq()).isNotNull();
		MercadoPagoSimulado.servidor().verify(3, postRequestedFor(urlEqualTo("/v1/orders")));
		assertThat(contar("SELECT count(*) FROM pagamento")).isZero();
	}

	@Test
	void brokerQueNaoConfirmaViraFalhaDePublicacao() {
		MensagemSaida paraLugarNenhum = new MensagemSaida(UUID.randomUUID(), "OrcamentoGerado", "exchange-que-nao-existe",
				"OrcamentoGerado", 42L, "corr", "{}", LocalDateTime.now());

		assertThatThrownBy(() -> broker.enviar(paraLugarNenhum)).isInstanceOf(FalhaPublicacaoException.class);
	}
}
