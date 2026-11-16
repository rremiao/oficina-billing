package br.com.oficina.billing.pagamento.output.mercadopago;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.apoio.MercadoPagoSimulado;
import br.com.oficina.billing.config.IntegrationTest;
import br.com.oficina.billing.pagamento.application.gateway.FalhaProvedorPagamentoException;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.CobrancaCriada;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.EstornoRealizado;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.ItemCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.NovaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.Situacao;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.SituacaoCobranca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

/**
 * O cliente Feign de verdade contra a Orders API simulada: o JSON que sai, os cabeçalhos de
 * autenticação e idempotência, e a tradução da resposta.
 */
@IntegrationTest
class MercadoPagoProvedorIntegrationTest {

	@Autowired
	private ProvedorPagamento provedor;

	@BeforeEach
	void limparSimulador() {
		MercadoPagoSimulado.reiniciar();
	}

	@Test
	void criaOrdemPixComAccessTokenEChaveDeIdempotencia() {
		MercadoPagoSimulado.ordemCriada("ORD01", "https://mp/pix/ORD01");

		CobrancaCriada cobranca = provedor.criarCobranca(new NovaCobranca("orcamento-10", "Orçamento da OS-2026-000042",
				new BigDecimal("329.80"), List.of(new ItemCobranca("ITEM-20", "Pastilha", 2, new BigDecimal("89.90"))),
				"cobranca-orcamento-10"));

		assertThat(cobranca.id()).isEqualTo("ORD01");
		assertThat(cobranca.linkPagamento()).isEqualTo("https://mp/pix/ORD01");
		assertThat(cobranca.situacao()).isEqualTo(Situacao.PENDENTE);
		assertThat(cobranca.resposta()).contains("\"id\":\"ORD01\"");
		MercadoPagoSimulado.servidor().verify(postRequestedFor(urlEqualTo("/v1/orders"))
				.withHeader("Authorization", equalTo("Bearer " + MercadoPagoSimulado.ACCESS_TOKEN))
				.withHeader("X-Idempotency-Key", equalTo("cobranca-orcamento-10"))
				.withRequestBody(equalToJson("""
						{"type": "online", "processing_mode": "automatic", "total_amount": "329.80",
						 "external_reference": "orcamento-10", "description": "Orçamento da OS-2026-000042",
						 "items": [{"external_code": "ITEM-20", "title": "Pastilha", "quantity": 2, "unit_price": "89.90"}],
						 "payer": {"email": "pagamentos@oficina.com.br"},
						 "transactions": {"payments": [{"amount": "329.80",
						   "payment_method": {"id": "pix", "type": "bank_transfer"}}]}}
						""", true, true)));
	}

	@Test
	void consultaTraduzOStatusEPegaOMeioDePagamento() {
		MercadoPagoSimulado.ordemConsultada("ORD01", "processed", "accredited", "pix");

		SituacaoCobranca situacao = provedor.consultar("ORD01");

		assertThat(situacao.situacao()).isEqualTo(Situacao.APROVADA);
		assertThat(situacao.metodo()).isEqualTo("pix");
		assertThat(situacao.detalhe()).isEqualTo("accredited");
	}

	@Test
	void estornoDevolveOReembolsoEMandaAChaveDeIdempotencia() {
		MercadoPagoSimulado.ordemEstornada("ORD01", "REF01", "329.80");

		EstornoRealizado estorno = provedor.estornar("ORD01", "estorno-pagamento-5");

		assertThat(estorno.id()).isEqualTo("REF01");
		assertThat(estorno.valor()).isEqualByComparingTo("329.80");
		MercadoPagoSimulado.servidor().verify(postRequestedFor(urlEqualTo("/v1/orders/ORD01/refund"))
				.withHeader("X-Idempotency-Key", equalTo("estorno-pagamento-5")));
	}

	@Test
	void erroDoMercadoPagoViraFalhaTransitoria() {
		MercadoPagoSimulado.falhaAoCriarOrdem();

		assertThatThrownBy(() -> provedor.criarCobranca(new NovaCobranca("orcamento-10", "x", BigDecimal.TEN, List.of(),
				"chave")))
				.isInstanceOf(FalhaProvedorPagamentoException.class)
				.hasMessage("Mercado Pago respondeu 500");
	}
}
