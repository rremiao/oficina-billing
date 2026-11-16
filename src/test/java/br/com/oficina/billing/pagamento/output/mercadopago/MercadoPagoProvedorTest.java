package br.com.oficina.billing.pagamento.output.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.pagamento.application.gateway.FalhaProvedorPagamentoException;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.EstornoRealizado;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.ItemCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.NovaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.SituacaoCobranca;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderRequest;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderPayment;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderPaymentMethod;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderTransaction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

/** Casos de borda do adaptador que o simulador HTTP não precisa reproduzir. */
class MercadoPagoProvedorTest {

	private static final String EMAIL = "pagamentos@oficina.com.br";

	private final MercadoPagoClient client = mock(MercadoPagoClient.class);

	@Test
	void comNomeDeTesteOPagadorVaiComOPrimeiroNome() {
		MercadoPagoProvedor provedor = new MercadoPagoProvedor(client, JsonMapper.builder().build(), "token", EMAIL, "APRO");
		MercadoPagoOrderResponse criada = new MercadoPagoOrderResponse();
		criada.setId("ORD01");
		criada.setStatus("action_required");
		when(client.criarOrdem(anyString(), anyString(), any())).thenReturn(criada);

		provedor.criarCobranca(cobranca());

		ArgumentCaptor<MercadoPagoOrderRequest> pedido = ArgumentCaptor.forClass(MercadoPagoOrderRequest.class);
		verify(client).criarOrdem(anyString(), anyString(), pedido.capture());
		assertThat(pedido.getValue().getPayer().getEmail()).isEqualTo(EMAIL);
		assertThat(pedido.getValue().getPayer().getFirstName()).isEqualTo("APRO");
	}

	@Test
	void semLinkDoPixUsaOCheckoutDaOrdem() {
		MercadoPagoProvedor provedor = new MercadoPagoProvedor(client, JsonMapper.builder().build(), "token", EMAIL, null);
		MercadoPagoOrderResponse semTransacoes = new MercadoPagoOrderResponse();
		semTransacoes.setCheckoutUrl("https://mp/checkout/ORD01");
		MercadoPagoOrderResponse pagamentoSemMeio = new MercadoPagoOrderResponse();
		pagamentoSemMeio.setCheckoutUrl("https://mp/checkout/ORD02");
		MercadoPagoFindOrderTransaction transacoes = new MercadoPagoFindOrderTransaction();
		transacoes.setPayments(List.of(new MercadoPagoFindOrderPayment()));
		pagamentoSemMeio.setTransactions(transacoes);
		MercadoPagoOrderResponse meioSemLink = new MercadoPagoOrderResponse();
		meioSemLink.setCheckoutUrl("https://mp/checkout/ORD03");
		MercadoPagoFindOrderPayment pagamento = new MercadoPagoFindOrderPayment();
		pagamento.setPaymentMethod(new MercadoPagoFindOrderPaymentMethod());
		MercadoPagoFindOrderTransaction comMeio = new MercadoPagoFindOrderTransaction();
		comMeio.setPayments(List.of(pagamento));
		meioSemLink.setTransactions(comMeio);
		MercadoPagoOrderResponse vazias = new MercadoPagoOrderResponse();
		vazias.setCheckoutUrl("https://mp/checkout/ORD04");
		MercadoPagoFindOrderTransaction semPagamentos = new MercadoPagoFindOrderTransaction();
		semPagamentos.setPayments(List.of());
		vazias.setTransactions(semPagamentos);
		when(client.criarOrdem(anyString(), anyString(), any()))
				.thenReturn(semTransacoes, pagamentoSemMeio, meioSemLink, vazias);

		assertThat(provedor.criarCobranca(cobranca()).linkPagamento()).isEqualTo("https://mp/checkout/ORD01");
		assertThat(provedor.criarCobranca(cobranca()).linkPagamento()).isEqualTo("https://mp/checkout/ORD02");
		assertThat(provedor.criarCobranca(cobranca()).linkPagamento()).isEqualTo("https://mp/checkout/ORD03");
		assertThat(provedor.criarCobranca(cobranca()).linkPagamento()).isEqualTo("https://mp/checkout/ORD04");
		ArgumentCaptor<MercadoPagoOrderRequest> pedido = ArgumentCaptor.forClass(MercadoPagoOrderRequest.class);
		verify(client, times(4)).criarOrdem(anyString(), anyString(), pedido.capture());
		assertThat(pedido.getValue().getPayer().getFirstName()).isNull();
	}

	private static NovaCobranca cobranca() {
		return new NovaCobranca("orcamento-1", "Orçamento", new BigDecimal("10.00"),
				List.of(new ItemCobranca("ITEM-1", "Pastilha", 1, new BigDecimal("10.00"))), "chave");
	}

	@Test
	void semAccessTokenNemChamaOMercadoPago() {
		MercadoPagoProvedor semToken = new MercadoPagoProvedor(client, JsonMapper.builder().build(), " ", EMAIL, "");

		assertThatThrownBy(() -> semToken.consultar("ORD01"))
				.isInstanceOf(FalhaProvedorPagamentoException.class)
				.hasMessage("MERCADOPAGO_ACCESS_TOKEN não configurado");
		verifyNoInteractions(client);
	}

	@Test
	void ordemSemPagamentoNemReembolsoDevolveCamposVazios() {
		MercadoPagoProvedor provedor = new MercadoPagoProvedor(client, JsonMapper.builder().build(), "token", EMAIL, "");
		MercadoPagoFindOrderResponse semTransacoes = new MercadoPagoFindOrderResponse();
		semTransacoes.setStatus("created");
		MercadoPagoFindOrderResponse transacoesVazias = new MercadoPagoFindOrderResponse();
		MercadoPagoFindOrderTransaction vazia = new MercadoPagoFindOrderTransaction();
		vazia.setPayments(List.of());
		vazia.setRefunds(List.of());
		transacoesVazias.setTransactions(vazia);
		when(client.consultarOrdem(anyString(), anyString())).thenReturn(semTransacoes, transacoesVazias);
		when(client.estornarOrdem(anyString(), anyString(), anyString())).thenReturn(semTransacoes, transacoesVazias);

		SituacaoCobranca primeira = provedor.consultar("ORD01");
		SituacaoCobranca segunda = provedor.consultar("ORD01");
		EstornoRealizado estornoSemTransacoes = provedor.estornar("ORD01", "chave");
		EstornoRealizado estornoSemReembolso = provedor.estornar("ORD01", "chave");

		assertThat(primeira.metodo()).isNull();
		assertThat(segunda.metodo()).isNull();
		assertThat(estornoSemTransacoes.id()).isNull();
		assertThat(estornoSemReembolso.valor()).isNull();
	}
}
