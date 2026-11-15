package br.com.oficina.billing.bdd;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.billing.apoio.BancoDeTeste;
import br.com.oficina.billing.apoio.MercadoPagoSimulado;
import br.com.oficina.billing.apoio.OsServiceSimulado;
import br.com.oficina.billing.apoio.TokensDeTeste;
import br.com.oficina.billing.mensageria.output.outbox.OutboxEnvio;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Passos dos cenários da Saga no Billing. O OS Service é simulado no RabbitMQ (comandos em
 * {@code os.eventos}, respostas capturadas em {@code billing.eventos}) e o Mercado Pago, no WireMock;
 * o resto é a aplicação de verdade.
 */
public class SagaBillingSteps {

	private static final Duration ESPERA = Duration.ofSeconds(15);
	private static final String ORDEM_MERCADO_PAGO = "ORD01BDD";

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin admin;

	@Autowired
	private OutboxEnvio outboxEnvio;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private OsServiceSimulado os;
	private UUID saga;
	private long ordemServicoId;
	private Map<String, Object> pedidoDeOrcamento;
	private long orcamentoId;
	private long pagamentoId;
	private String messageIdDoOrcamentoGerado;

	@ParameterType("\\d+,\\d{2}")
	public BigDecimal dinheiro(String valor) {
		return new BigDecimal(valor.replace(",", "."));
	}

	@Before
	public void ambienteLimpo() {
		BancoDeTeste.limpar(jdbc);
		MercadoPagoSimulado.reiniciar();
		os = new OsServiceSimulado(rabbitTemplate, admin);
		os.limpar();
		saga = UUID.randomUUID();
	}

	@Dado("o catálogo com o serviço {string} a R$ {dinheiro} e a peça {string} a R$ {dinheiro}")
	public void catalogo(String servico, BigDecimal valorServico, String peca, BigDecimal valorPeca) {
		jdbc.update("INSERT INTO servico (id, nome, valor) VALUES (30, ?, ?)", servico, valorServico);
		jdbc.update("INSERT INTO item (id, nome, tipo, valor_unitario, unidade_medida) VALUES (20, ?, 'PECA', ?, 'JG')",
				peca, valorPeca);
	}

	@Quando("o OS Service pede o orçamento da OS {long} com o serviço e {int} unidades da peça")
	public void pedirOrcamento(long os, int quantidade) {
		ordemServicoId = os;
		pedidoDeOrcamento = Map.of("clienteId", 7, "veiculoId", 3,
				"servicos", List.of(Map.of("servicoId", 30)),
				"itens", List.of(Map.of("itemId", 20, "quantidade", quantidade)));
		this.os.enviar("SolicitarOrcamento", saga, ordemServicoId, pedidoDeOrcamento);
		esperarRespostaPendente("OrcamentoGerado");
	}

	@Então("o Billing responde que o orçamento foi gerado com total de R$ {dinheiro}")
	public void orcamentoGerado(BigDecimal total) {
		Message evento = proximoEvento("OrcamentoGerado");
		JsonNode corpo = os.corpo(evento);
		assertThat(corpo.get("sagaId").asString()).isEqualTo(saga.toString());
		assertThat(corpo.at("/dados/valorTotal").decimalValue()).isEqualByComparingTo(total);
		orcamentoId = corpo.at("/dados/orcamentoId").asLong();
		messageIdDoOrcamentoGerado = evento.getMessageProperties().getMessageId();
	}

	@Quando("o OS Service pede a cobrança do orçamento")
	public void pedirCobranca() {
		MercadoPagoSimulado.ordemCriada(ORDEM_MERCADO_PAGO, "https://mp/checkout/" + ORDEM_MERCADO_PAGO);
		os.enviar("CobrarPagamento", saga, ordemServicoId, Map.of("orcamentoId", orcamentoId));
		await().atMost(ESPERA).until(() -> contar("SELECT count(*) FROM pagamento") == 1);
		pagamentoId = jdbc.queryForObject("SELECT id FROM pagamento", Long.class);
	}

	@Então("o cliente encontra o link de pagamento do Mercado Pago")
	public void clienteEncontraOLink() throws Exception {
		mockMvc.perform(get("/pagamentos/orcamento/" + orcamentoId)
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(7L, "12345678909", "Maria"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linkPagamento").value("https://mp/checkout/" + ORDEM_MERCADO_PAGO));
	}

	@Quando("o Mercado Pago avisa que o pagamento foi aprovado por {string}")
	public void pagamentoAprovado(String metodo) throws Exception {
		MercadoPagoSimulado.ordemConsultada(ORDEM_MERCADO_PAGO, "processed", "accredited", metodo);
		avisoDoMercadoPago();
	}

	@Então("o Billing responde que o pagamento de R$ {dinheiro} foi confirmado")
	public void pagamentoConfirmado(BigDecimal valor) {
		JsonNode corpo = os.corpo(proximoEvento("PagamentoConfirmado"));
		assertThat(corpo.at("/dados/pagamentoId").asLong()).isEqualTo(pagamentoId);
		assertThat(corpo.at("/dados/valorPago").decimalValue()).isEqualByComparingTo(valor);
		assertThat(corpo.get("sagaId").asString()).isEqualTo(saga.toString());
	}

	@Dado("que o cliente já pagou o orçamento")
	public void clienteJaPagou() throws Exception {
		pedirCobranca();
		pagamentoAprovado("pix");
		proximoEvento("PagamentoConfirmado");
	}

	@Quando("o OS Service pede o estorno do pagamento porque {string}")
	public void pedirEstorno(String motivo) {
		MercadoPagoSimulado.ordemEstornada(ORDEM_MERCADO_PAGO, "REF01BDD", "329.80");
		os.enviar("EstornarPagamento", saga, ordemServicoId, Map.of("pagamentoId", pagamentoId, "motivo", motivo));
		esperarRespostaPendente("PagamentoEstornado");
	}

	@Então("o Billing responde que R$ {dinheiro} foram estornados")
	public void pagamentoEstornado(BigDecimal valor) {
		JsonNode corpo = os.corpo(proximoEvento("PagamentoEstornado"));
		assertThat(corpo.at("/dados/refundId").asString()).isEqualTo("REF01BDD");
		assertThat(corpo.at("/dados/valorEstornado").decimalValue()).isEqualByComparingTo(valor);
	}

	@Então("o Mercado Pago recebeu um único pedido de reembolso")
	public void umReembolso() {
		MercadoPagoSimulado.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/orders/" + ORDEM_MERCADO_PAGO + "/refund")));
	}

	@Quando("o Mercado Pago avisa que a cobrança expirou")
	public void cobrancaExpirou() throws Exception {
		MercadoPagoSimulado.ordemConsultada(ORDEM_MERCADO_PAGO, "expired", "expired", "pix");
		avisoDoMercadoPago();
	}

	@Então("o Billing responde que o pagamento foi recusado")
	public void pagamentoRecusado() {
		JsonNode corpo = os.corpo(proximoEvento("PagamentoRecusado"));
		assertThat(corpo.at("/dados/orcamentoId").asLong()).isEqualTo(orcamentoId);
		assertThat(corpo.at("/dados/motivo").asString()).isEqualTo("expired");
	}

	@Quando("o OS Service pede de novo o mesmo orçamento")
	public void pedirDeNovo() {
		os.enviar("SolicitarOrcamento", saga, ordemServicoId, pedidoDeOrcamento);
		esperarRespostaPendente("OrcamentoGerado");
	}

	@Então("o Billing responde de novo com a mesma mensagem")
	public void respondeDeNovo() {
		assertThat(proximoEvento("OrcamentoGerado").getMessageProperties().getMessageId())
				.isEqualTo(messageIdDoOrcamentoGerado);
	}

	@Então("existe um único orçamento para a OS {long}")
	public void umOrcamento(long os) {
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento WHERE ordem_servico_id = ?", Integer.class, os))
				.isEqualTo(1);
	}

	private void avisoDoMercadoPago() throws Exception {
		mockMvc.perform(post("/pagamentos/webhook/mercadopago")
						.param("data.id", ORDEM_MERCADO_PAGO)
						.param("type", "order")
						.header("x-request-id", "req-bdd")
						.header("x-signature", MercadoPagoSimulado.assinatura(ORDEM_MERCADO_PAGO, "req-bdd", "1704908010"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\": \"order\", \"data\": {\"id\": \"" + ORDEM_MERCADO_PAGO + "\"}}"))
				.andExpect(status().isOk());
	}

	/** O comando é consumido em outra thread: espera a resposta aparecer pendente no outbox. */
	private void esperarRespostaPendente(String tipo) {
		await().atMost(ESPERA).until(() -> jdbc.queryForObject(
				"SELECT count(*) FROM outbox_evento WHERE tipo = ? AND publicado_em IS NULL", Integer.class, tipo) == 1);
	}

	private Message proximoEvento(String tipo) {
		outboxEnvio.enviarPendentes();
		Message evento = os.proximoEvento();
		assertThat(evento).as("evento %s publicado pelo Billing", tipo).isNotNull();
		assertThat(evento.getMessageProperties().getType()).isEqualTo(tipo);
		return evento;
	}

	private int contar(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}
}
