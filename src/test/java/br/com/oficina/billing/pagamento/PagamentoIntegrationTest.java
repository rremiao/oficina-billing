package br.com.oficina.billing.pagamento;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.billing.apoio.BancoDeTeste;
import br.com.oficina.billing.apoio.MercadoPagoSimulado;
import br.com.oficina.billing.apoio.TokensDeTeste;
import br.com.oficina.billing.config.IntegrationTest;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento.ItemSolicitado;
import br.com.oficina.billing.orcamento.application.usecase.GerarOrcamento;
import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoCobranca;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoEstorno;
import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.application.usecase.CobrarPagamento;
import br.com.oficina.billing.pagamento.application.usecase.EstornarPagamento;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.pagamento.domain.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Pagamento de ponta a ponta com PostgreSQL real e o Mercado Pago simulado: cobrança, link para o
 * cliente, aviso do webhook, recusa e estorno.
 */
@IntegrationTest
class PagamentoIntegrationTest {

	private static final String ADMIN = TokensDeTeste.bearer(TokensDeTeste.admin());

	@Autowired
	private GerarOrcamento gerarOrcamento;

	@Autowired
	private CobrarPagamento cobrarPagamento;

	@Autowired
	private EstornarPagamento estornarPagamento;

	@Autowired
	private PagamentoGateway pagamentoGateway;

	@Autowired
	private TransactionTemplate transacao;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID saga;
	private Long orcamentoId;

	@BeforeEach
	void orcamentoGerado() {
		BancoDeTeste.limpar(jdbc);
		MercadoPagoSimulado.reiniciar();
		jdbc.update("INSERT INTO servico (id, nome, valor) VALUES (30, 'Troca de pastilhas', 150.00)");
		jdbc.update("INSERT INTO item (id, nome, tipo, valor_unitario, unidade_medida) "
				+ "VALUES (20, 'Pastilha de freio', 'PECA', 89.90, 'JG')");
		saga = UUID.randomUUID();
		orcamentoId = gerarOrcamento.executar(new SolicitacaoOrcamento(saga, 42L, "OS-2026-000042", 7L, 3L,
				List.of(30L), List.of(new ItemSolicitado(20L, 2)))).id();
	}

	private PagamentoResponse cobrar() {
		MercadoPagoSimulado.ordemCriada("ORD01", "https://mp/checkout/ORD01");
		return cobrarPagamento.executar(new SolicitacaoCobranca(saga, orcamentoId, "corr-1"));
	}

	private ResultActions avisar(String idOrdem, String xSignature) throws Exception {
		return mockMvc.perform(post("/pagamentos/webhook/mercadopago")
				.param("data.id", idOrdem)
				.param("type", "order")
				.header("x-request-id", "req-1")
				.header("x-signature", xSignature)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\": \"order\", \"action\": \"order.processed\", \"data\": {\"id\": \"%s\"}}"
						.formatted(idOrdem)));
	}

	private ResultActions avisarAssinado(String idOrdem) throws Exception {
		return avisar(idOrdem, MercadoPagoSimulado.assinatura(idOrdem, "req-1", "1704908010"));
	}

	@Test
	void cobrancaGuardaOLinkQueOClienteDonoConsulta() throws Exception {
		PagamentoResponse pagamento = cobrar();

		assertThat(pagamento.status()).isEqualTo(StatusPagamento.AGUARDANDO_PAGAMENTO);
		assertThat(pagamento.valor()).hasToString("329.80");

		String rota = "/pagamentos/orcamento/" + orcamentoId;
		mockMvc.perform(get(rota).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(7L, "12345678909", "Maria"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linkPagamento").value("https://mp/checkout/ORD01"))
				.andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
		mockMvc.perform(get(rota).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(8L, "98765432100", "João"))))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/pagamentos/" + pagamento.id()).header("Authorization", ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idCobranca").value("ORD01"));
	}

	@Test
	void cobrancaRepetidaNaoCriaOutraOrdemNoMercadoPago() {
		PagamentoResponse primeira = cobrar();
		PagamentoResponse repetida = cobrarPagamento.executar(new SolicitacaoCobranca(saga, orcamentoId, "corr-1"));

		assertThat(repetida.id()).isEqualTo(primeira.id());
		MercadoPagoSimulado.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/orders")));
	}

	@Test
	void avisoAssinadoConsultaOMercadoPagoEConfirmaOPagamento() throws Exception {
		PagamentoResponse pagamento = cobrar();
		MercadoPagoSimulado.ordemConsultada("ORD01", "processed", "accredited", "pix");

		avisarAssinado("ORD01").andExpect(status().isOk());

		mockMvc.perform(get("/pagamentos/" + pagamento.id()).header("Authorization", ADMIN))
				.andExpect(jsonPath("$.status").value("CONFIRMADO"))
				.andExpect(jsonPath("$.metodo").value("pix"));
		assertThat(jdbc.queryForObject("SELECT resposta_provedor ->> 'status' FROM pagamento WHERE id = ?", String.class,
				pagamento.id())).isEqualTo("processed");
	}

	@Test
	void avisoSemAssinaturaValidaNaoMudaNadaNemConsultaOMercadoPago() throws Exception {
		cobrar();

		avisar("ORD01", "ts=1704908010,v1=assinatura-falsa")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Assinatura do aviso ausente ou inválida"));

		MercadoPagoSimulado.servidor().verify(0, getRequestedFor(urlMatching("/v1/orders/.*")));
		assertThat(jdbc.queryForObject("SELECT status FROM pagamento", String.class)).isEqualTo("AGUARDANDO_PAGAMENTO");
	}

	@Test
	void avisoDeOrdemDesconhecidaOuDeOutroTopicoResponde200SemEfeito() throws Exception {
		avisarAssinado("ORD-DE-OUTRA-INTEGRACAO").andExpect(status().isOk());
		mockMvc.perform(post("/pagamentos/webhook/mercadopago")
						.param("data.id", "123")
						.param("type", "payment")
						.header("x-request-id", "req-1")
						.header("x-signature", MercadoPagoSimulado.assinatura("123", "req-1", "1704908010")))
				.andExpect(status().isOk());

		MercadoPagoSimulado.servidor().verify(0, getRequestedFor(urlMatching("/v1/orders/.*")));
	}

	@Test
	void ordemExpiradaRecusaOPagamento() throws Exception {
		cobrar();
		MercadoPagoSimulado.ordemConsultada("ORD01", "expired", "expired", "pix");

		avisarAssinado("ORD01").andExpect(status().isOk());

		assertThat(jdbc.queryForObject("SELECT status FROM pagamento", String.class)).isEqualTo("RECUSADO");
		assertThat(jdbc.queryForObject("SELECT motivo FROM pagamento", String.class)).isEqualTo("expired");
	}

	@Test
	void estornoDePagamentoConfirmadoChamaOMercadoPagoUmaVezSo() throws Exception {
		PagamentoResponse pagamento = cobrar();
		MercadoPagoSimulado.ordemConsultada("ORD01", "processed", "accredited", "pix");
		avisarAssinado("ORD01");
		MercadoPagoSimulado.ordemEstornada("ORD01", "REF01", "329.80");

		PagamentoResponse estornado = estornarPagamento.executar(new SolicitacaoEstorno(saga, pagamento.id(), "Peça danificada"));
		PagamentoResponse repetido = estornarPagamento.executar(new SolicitacaoEstorno(saga, pagamento.id(), "Peça danificada"));

		assertThat(estornado.status()).isEqualTo(StatusPagamento.ESTORNADO);
		assertThat(estornado.idEstorno()).isEqualTo("REF01");
		assertThat(estornado.valorEstornado()).isEqualByComparingTo("329.80");
		assertThat(repetido.idEstorno()).isEqualTo("REF01");
		MercadoPagoSimulado.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/orders/ORD01/refund")));
	}

	@Test
	void duasCobrancasSimultaneasDoMesmoOrcamentoGravamUmPagamentoSo() throws Exception {
		MercadoPagoSimulado.ordemCriada("ORD01", "https://mp/checkout/ORD01");
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<PagamentoResponse>> entregas = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			for (int i = 0; i < 2; i++) {
				entregas.add(executor.submit(() -> {
					largada.await();
					return cobrarPagamento.executar(new SolicitacaoCobranca(saga, orcamentoId, "corr-1"));
				}));
			}
			largada.countDown();
			for (Future<PagamentoResponse> entrega : entregas) {
				try {
					assertThat(entrega.get().idCobranca()).isEqualTo("ORD01");
				} catch (ExecutionException e) {
					// A perdedora esbarra no índice único; tentada de novo, devolve o pagamento já criado
					assertThat(e.getCause()).isInstanceOf(DataIntegrityViolationException.class);
				}
			}
		}

		assertThat(jdbc.queryForObject("SELECT count(*) FROM pagamento", Integer.class)).isEqualTo(1);
	}

	@Test
	void atualizacaoQueChegaDepoisDeOutraNaoSobrescreve() {
		Long pagamentoId = cobrar().id();
		TransactionTemplate outraTransacao = new TransactionTemplate(transacao.getTransactionManager());
		outraTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
			Pagamento lido = pagamentoGateway.buscarPorId(pagamentoId).orElseThrow();
			// Outra transação grava e confirma no meio do caminho (o webhook, por exemplo)
			outraTransacao.executeWithoutResult(s -> jdbc.update(
					"UPDATE pagamento SET versao = versao + 1, status = 'RECUSADO' WHERE id = ?", pagamentoId));
			lido.confirmar("pix", "{}");
			pagamentoGateway.salvar(lido);
		})).isInstanceOf(ObjectOptimisticLockingFailureException.class);

		assertThat(jdbc.queryForObject("SELECT status FROM pagamento", String.class)).isEqualTo("RECUSADO");
	}
}
