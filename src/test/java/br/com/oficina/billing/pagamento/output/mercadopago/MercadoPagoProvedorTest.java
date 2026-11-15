package br.com.oficina.billing.pagamento.output.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.pagamento.application.gateway.FalhaProvedorPagamentoException;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.EstornoRealizado;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.SituacaoCobranca;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderTransaction;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Casos de borda do adaptador que o simulador HTTP não precisa reproduzir. */
class MercadoPagoProvedorTest {

	private final MercadoPagoClient client = mock(MercadoPagoClient.class);

	@Test
	void semAccessTokenNemChamaOMercadoPago() {
		MercadoPagoProvedor semToken = new MercadoPagoProvedor(client, JsonMapper.builder().build(), " ");

		assertThatThrownBy(() -> semToken.consultar("ORD01"))
				.isInstanceOf(FalhaProvedorPagamentoException.class)
				.hasMessage("MERCADOPAGO_ACCESS_TOKEN não configurado");
		verifyNoInteractions(client);
	}

	@Test
	void ordemSemPagamentoNemReembolsoDevolveCamposVazios() {
		MercadoPagoProvedor provedor = new MercadoPagoProvedor(client, JsonMapper.builder().build(), "token");
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
