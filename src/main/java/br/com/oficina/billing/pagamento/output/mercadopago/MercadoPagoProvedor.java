package br.com.oficina.billing.pagamento.output.mercadopago;

import br.com.oficina.billing.pagamento.application.gateway.FalhaProvedorPagamentoException;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderItem;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderRequest;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderPayment;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderRefund;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderTransaction;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link ProvedorPagamento} na Orders API do Mercado Pago, com checkout online: a ordem criada
 * devolve o {@code checkout_url} onde o cliente paga, e a confirmação chega depois pelo webhook.
 */
@Component
class MercadoPagoProvedor implements ProvedorPagamento {

	private static final String TIPO_ONLINE = "online";
	private static final String PROCESSAMENTO_AUTOMATICO = "automatic";

	private final MercadoPagoClient client;
	private final JsonMapper jsonMapper;
	private final String accessToken;

	MercadoPagoProvedor(MercadoPagoClient client, JsonMapper jsonMapper,
			@Value("${mercadopago.access-token}") String accessToken) {
		this.client = client;
		this.jsonMapper = jsonMapper;
		this.accessToken = accessToken;
	}

	@Override
	public CobrancaCriada criarCobranca(NovaCobranca cobranca) {
		MercadoPagoOrderResponse ordem = chamar(() ->
				client.criarOrdem(autorizacao(), cobranca.chaveIdempotencia(), paraOrdem(cobranca)));
		return new CobrancaCriada(ordem.getId(), ordem.getCheckoutUrl(), situacao(ordem.getStatus()),
				jsonMapper.writeValueAsString(ordem));
	}

	@Override
	public SituacaoCobranca consultar(String idCobranca) {
		MercadoPagoFindOrderResponse ordem = chamar(() -> client.consultarOrdem(autorizacao(), idCobranca));
		return new SituacaoCobranca(situacao(ordem.getStatus()), metodo(ordem), ordem.getStatusDetail(),
				jsonMapper.writeValueAsString(ordem));
	}

	@Override
	public EstornoRealizado estornar(String idCobranca, String chaveIdempotencia) {
		MercadoPagoFindOrderResponse ordem = chamar(() ->
				client.estornarOrdem(autorizacao(), chaveIdempotencia, idCobranca));
		MercadoPagoFindOrderRefund reembolso = ultimoReembolso(ordem);
		return new EstornoRealizado(
				reembolso == null ? null : reembolso.getId(),
				reembolso == null || reembolso.getAmount() == null ? null : new BigDecimal(reembolso.getAmount()),
				jsonMapper.writeValueAsString(ordem));
	}

	/**
	 * Status da ordem na Orders API: {@code processed} é pago; {@code failed}, {@code canceled} e
	 * {@code expired} encerram sem pagamento; {@code refunded} é estornado. Os demais
	 * ({@code created}, {@code processing}, {@code action_required}) ainda esperam o cliente.
	 */
	static Situacao situacao(String status) {
		if (status == null) {
			return Situacao.PENDENTE;
		}
		return switch (status) {
			case "processed" -> Situacao.APROVADA;
			case "failed", "canceled", "cancelled", "expired" -> Situacao.RECUSADA;
			case "refunded" -> Situacao.ESTORNADA;
			default -> Situacao.PENDENTE;
		};
	}

	private static MercadoPagoOrderRequest paraOrdem(NovaCobranca cobranca) {
		MercadoPagoOrderRequest ordem = new MercadoPagoOrderRequest();
		ordem.setType(TIPO_ONLINE);
		ordem.setProcessingMode(PROCESSAMENTO_AUTOMATICO);
		ordem.setTotalAmount(cobranca.valor().toPlainString());
		ordem.setExternalReference(cobranca.referenciaExterna());
		ordem.setDescription(cobranca.descricao());
		ordem.setItems(cobranca.itens().stream().map(MercadoPagoProvedor::paraItem).toList());
		return ordem;
	}

	private static MercadoPagoOrderItem paraItem(ItemCobranca item) {
		MercadoPagoOrderItem itemOrdem = new MercadoPagoOrderItem();
		itemOrdem.setExternalCode(item.codigo());
		itemOrdem.setTitle(item.titulo());
		itemOrdem.setQuantity(item.quantidade());
		itemOrdem.setUnitPrice(item.valorUnitario().toPlainString());
		return itemOrdem;
	}

	private static String metodo(MercadoPagoFindOrderResponse ordem) {
		List<MercadoPagoFindOrderPayment> pagamentos = ordem.getTransactions() == null
				? null : ordem.getTransactions().getPayments();
		if (pagamentos == null || pagamentos.isEmpty() || pagamentos.get(0).getPaymentMethod() == null) {
			return null;
		}
		return pagamentos.get(0).getPaymentMethod().getId();
	}

	private static MercadoPagoFindOrderRefund ultimoReembolso(MercadoPagoFindOrderResponse ordem) {
		MercadoPagoFindOrderTransaction transacoes = ordem.getTransactions();
		if (transacoes == null || transacoes.getRefunds() == null || transacoes.getRefunds().isEmpty()) {
			return null;
		}
		return transacoes.getRefunds().getLast();
	}

	private String autorizacao() {
		if (accessToken == null || accessToken.isBlank()) {
			throw new FalhaProvedorPagamentoException("MERCADOPAGO_ACCESS_TOKEN não configurado", null);
		}
		return "Bearer " + accessToken;
	}

	private static <T> T chamar(Supplier<T> chamada) {
		try {
			return chamada.get();
		} catch (FeignException e) {
			throw new FalhaProvedorPagamentoException("Mercado Pago respondeu " + e.status(), e);
		}
	}
}
