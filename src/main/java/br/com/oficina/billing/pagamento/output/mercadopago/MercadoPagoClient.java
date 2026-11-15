package br.com.oficina.billing.pagamento.output.mercadopago;

import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderRequest;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente HTTP da Orders API do Mercado Pago. A autenticação é o access token das credenciais da
 * aplicação ({@code Authorization: Bearer}); toda operação que cria algo leva {@code X-Idempotency-Key}.
 */
@FeignClient(name = "mercadopago", url = "${mercadopago.url}")
interface MercadoPagoClient {

	String AUTORIZACAO = "Authorization";
	String IDEMPOTENCIA = "X-Idempotency-Key";

	@PostMapping("/v1/orders")
	MercadoPagoOrderResponse criarOrdem(@RequestHeader(AUTORIZACAO) String autorizacao,
			@RequestHeader(IDEMPOTENCIA) String chaveIdempotencia, @RequestBody MercadoPagoOrderRequest ordem);

	@GetMapping("/v1/orders/{idOrdem}")
	MercadoPagoFindOrderResponse consultarOrdem(@RequestHeader(AUTORIZACAO) String autorizacao,
			@PathVariable("idOrdem") String idOrdem);

	/** Estorno total: sem corpo, devolve a ordem com o reembolso em {@code transactions.refunds}. */
	@PostMapping("/v1/orders/{idOrdem}/refund")
	MercadoPagoFindOrderResponse estornarOrdem(@RequestHeader(AUTORIZACAO) String autorizacao,
			@RequestHeader(IDEMPOTENCIA) String chaveIdempotencia, @PathVariable("idOrdem") String idOrdem);
}
