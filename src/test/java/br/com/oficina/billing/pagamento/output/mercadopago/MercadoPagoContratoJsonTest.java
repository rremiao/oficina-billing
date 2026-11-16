package br.com.oficina.billing.pagamento.output.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderAdditionalInfo;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderAddress;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderConfig;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderConfigOnline;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderIdentification;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderInstallment;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderInterestFee;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderItem;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderPayer;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderPaymentMethod;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderPhone;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderRequest;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTrack;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTrackValue;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTravelPassenger;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTransactionPayment;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTransactionPaymentMethod;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTransactions;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request.MercadoPagoOrderTravelRoute;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find.MercadoPagoFindOrderResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contrato JSON dos DTOs do Mercado Pago, com o Jackson 3 (o mesmo que o Feign usa no Spring Boot 4).
 * Cobre os campos em snake_case e as datas no formato da API real (ISO 8601 com fuso, por isso
 * {@code OffsetDateTime}). Os poucos campos que fogem do snake_case não são usados pelo adaptador e
 * estão anotados para revisão em docs/plano-implementacao.md.
 */
class MercadoPagoContratoJsonTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void pedidoDeOrdemSaiEmSnakeCaseComTodosOsBlocos() {
		JsonNode json = jsonMapper.valueToTree(pedidoCompleto());

		assertThat(json.get("total_amount").asString()).isEqualTo("150.00");
		assertThat(json.get("external_reference").asString()).isEqualTo("orcamento-10");
		assertThat(json.get("processing_mode").asString()).isEqualTo("automatic");
		assertThat(json.at("/payer/first_name").asString()).isEqualTo("Maria");
		assertThat(json.at("/payer/phone/area_code").asString()).isEqualTo("11");
		assertThat(json.at("/payer/identification/type").asString()).isEqualTo("CPF");
		assertThat(json.at("/payer/address/zip_code").asString()).isEqualTo("01001000");
		assertThat(json.at("/items/0/unit_price").asString()).isEqualTo("75.00");
		assertThat(json.at("/transactions/payments/0/amount").asString()).isEqualTo("150.00");
		assertThat(json.at("/transactions/payments/0/payment_method/id").asString()).isEqualTo("pix");
		assertThat(json.at("/transactions/payments/0/payment_method/type").asString()).isEqualTo("bank_transfer");
		assertThat(json.has("capture_mode")).as("campos nulos ficam fora do pedido").isFalse();
		assertThat(json.at("/config/online/success_url").asString()).isEqualTo("https://oficina/ok");
		assertThat(json.at("/config/online/tracks/0/values/pixel_id").asString()).isEqualTo("px");
		assertThat(json.at("/config/payment_method/max_installments").asInt()).isEqualTo(3);
		assertThat(json.at("/config/payment_method/installments/interest_fee/type").asString()).isEqualTo("none");
		assertThat(json.get("additional_info").get("travel.passengers").get(0).get("first_name").asString())
				.isEqualTo("Maria");
		assertThat(json.get("additional_info").get("travel.routes").get(0).get("departure").asString())
				.isEqualTo("SP");
	}

	@Test
	void respostaDeCriacaoDaOrdemMapeiaOsBlocosAninhados() {
		String json = """
				{"id": "ORD-1", "type": "online", "processing_mode": "automatic", "status": "created",
				 "status_detail": "created", "external_reference": "orcamento-10", "total_amount": "150.00",
				 "total_paid_amount": "0.00", "checkout_url": "https://mp/checkout", "currency": "BRL",
				 "created_date": "2026-11-16T17:04:11.785Z", "last_updated_date": "2026-11-16T17:04:38.473Z",
				 "integration_data": {"application_id": "app", "sponsor": {"id": "sp-1"}},
				 "config": {
				   "online": {"callback_url": "https://oficina/cb", "retries": {"allowed": true}},
				   "payment_method": {"max_installments": 3, "default_type": "credit_card",
				     "installments": {"interest_free": {"type": "range", "values": [1, 3]},
				                      "available": {"type": "all"}}}},
				 "items": [{"external_code": "SRV-7", "title": "Troca de pastilhas", "quantity": 1,
				            "unit_price": "150.00"}],
				 "transactions": {"payments": [{"id": "PAY-1", "amount": "150.00", "status": "action_required",
				   "date_of_expiration": "2026-11-17T17:04:12.172+00:00",
				   "payment_method": {"id": "pix", "type": "bank_transfer", "ticket_url": "https://mp/pix/PAY-1"}}]}}
				""";

		MercadoPagoOrderResponse resposta = jsonMapper.readValue(json, MercadoPagoOrderResponse.class);

		assertThat(resposta.getId()).isEqualTo("ORD-1");
		assertThat(resposta.getExternalReference()).isEqualTo("orcamento-10");
		assertThat(resposta.getTotalAmount()).isEqualTo("150.00");
		assertThat(resposta.getCheckoutUrl()).isEqualTo("https://mp/checkout");
		assertThat(resposta.getIntegrationData().getSponsor().getId()).isEqualTo("sp-1");
		assertThat(resposta.getConfig().getOnline().getRetries().getAllowed()).isTrue();
		assertThat(resposta.getConfig().getPaymentMethod().getInstallments().getInterestFree().getValues())
				.containsExactly(1, 3);
		assertThat(resposta.getConfig().getPaymentMethod().getInstallments().getAvailable().getType()).isEqualTo("all");
		assertThat(resposta.getItems()).singleElement()
				.satisfies(item -> assertThat(item.getUnitPrice()).isEqualTo("150.00"));
		assertThat(resposta.getTransactions().getPayments()).singleElement()
				.satisfies(pagamento -> assertThat(pagamento.getPaymentMethod().getTicketUrl())
						.isEqualTo("https://mp/pix/PAY-1"));
		// Datas no formato da API real: com "Z" ou com deslocamento explícito
		assertThat(resposta.getCreatedDate()).isEqualTo(OffsetDateTime.parse("2026-11-16T17:04:11.785Z"));
		assertThat(resposta.getTransactions().getPayments().getFirst().getDateOfExpiration())
				.isEqualTo(OffsetDateTime.parse("2026-11-17T17:04:12.172+00:00"));
	}

	@Test
	void respostaDaConsultaDaOrdemMapeiaPagamentosEstornosEContestacoes() {
		String json = """
				{"id": "ORD-1", "status": "processed", "status_detail": "accredited",
				 "external_reference": "orcamento-10", "total_amount": "150.00", "total_paid_amount": "150.00",
				 "payer": {"email": "maria@exemplo.com"},
				 "integration_data": {"application_id": "app", "sponsor": {"id": "sp-1"}},
				 "transactions": {
				   "payments": [{"id": "PAY-1", "amount": "150.00", "paid_amount": "150.00", "status": "processed",
				                 "payment_method": {"id": "pix", "type": "bank_transfer", "qr_code": "000201"}}],
				   "refunds": [{"id": "REF-1", "transaction_id": "PAY-1", "amount": "150.00", "status": "processed"}],
				   "chargebacks": [{"id": "CB-1", "case_id": "case-9", "status": "open"}]},
				 "items": [{"external_code": "SRV-7", "unit_price": "150.00", "quantity": 1}],
				 "config": {"online": {"success_url": "https://oficina/ok", "retries": {"allowed": false}}}}
				""";

		MercadoPagoFindOrderResponse resposta = jsonMapper.readValue(json, MercadoPagoFindOrderResponse.class);

		assertThat(resposta.getStatusDetail()).isEqualTo("accredited");
		assertThat(resposta.getTotalPaidAmount()).isEqualTo("150.00");
		assertThat(resposta.getPayer().getEmail()).isEqualTo("maria@exemplo.com");
		assertThat(resposta.getIntegrationData().getSponsor().getId()).isEqualTo("sp-1");
		assertThat(resposta.getTransactions().getPayments()).singleElement()
				.satisfies(pagamento -> assertThat(pagamento.getPaymentMethod().getQrCode()).isEqualTo("000201"));
		assertThat(resposta.getTransactions().getRefunds()).singleElement()
				.satisfies(estorno -> assertThat(estorno.getTransactionId()).isEqualTo("PAY-1"));
		assertThat(resposta.getTransactions().getChargebacks()).singleElement()
				.satisfies(contestacao -> assertThat(contestacao.getCaseId()).isEqualTo("case-9"));
		assertThat(resposta.getItems()).singleElement()
				.satisfies(item -> assertThat(item.getExternalCode()).isEqualTo("SRV-7"));
		assertThat(resposta.getConfig().getOnline().getRetries().getAllowed()).isFalse();
	}

	private MercadoPagoOrderRequest pedidoCompleto() {
		MercadoPagoOrderRequest pedido = new MercadoPagoOrderRequest();
		pedido.setType("online");
		pedido.setTotalAmount("150.00");
		pedido.setExternalReference("orcamento-10");
		pedido.setProcessingMode("automatic");
		pedido.setPayer(pagador());
		pedido.setConfig(configuracao());
		pedido.setItems(List.of(item()));
		pedido.setTransactions(transacoes());
		pedido.setAdditionalInfo(informacoesAdicionais());
		return pedido;
	}

	private MercadoPagoOrderPayer pagador() {
		MercadoPagoOrderPhone telefone = new MercadoPagoOrderPhone();
		telefone.setAreaCode("11");
		telefone.setNumber("999999999");
		MercadoPagoOrderIdentification identificacao = new MercadoPagoOrderIdentification();
		identificacao.setType("CPF");
		identificacao.setNumber("12345678909");
		MercadoPagoOrderAddress endereco = new MercadoPagoOrderAddress();
		endereco.setZipCode("01001000");
		endereco.setStreetName("Praça da Sé");
		MercadoPagoOrderPayer pagador = new MercadoPagoOrderPayer();
		pagador.setEmail("maria@exemplo.com");
		pagador.setFirstName("Maria");
		pagador.setPhone(telefone);
		pagador.setIdentification(identificacao);
		pagador.setAddress(endereco);
		return pagador;
	}

	private MercadoPagoOrderConfig configuracao() {
		MercadoPagoOrderTrackValue valores = new MercadoPagoOrderTrackValue();
		valores.setPixelId("px");
		MercadoPagoOrderTrack rastreio = new MercadoPagoOrderTrack();
		rastreio.setType("facebook_ad");
		rastreio.setValues(valores);
		MercadoPagoOrderConfigOnline online = new MercadoPagoOrderConfigOnline();
		online.setSuccessUrl("https://oficina/ok");
		online.setTracks(List.of(rastreio));
		MercadoPagoOrderInterestFee juros = new MercadoPagoOrderInterestFee();
		juros.setType("none");
		juros.setValues(List.of(1));
		MercadoPagoOrderInstallment parcelamento = new MercadoPagoOrderInstallment();
		parcelamento.setInterestFee(juros);
		MercadoPagoOrderPaymentMethod meioDePagamento = new MercadoPagoOrderPaymentMethod();
		meioDePagamento.setMaxInstallments(3);
		meioDePagamento.setInstallments(parcelamento);
		MercadoPagoOrderConfig configuracao = new MercadoPagoOrderConfig();
		configuracao.setOnline(online);
		configuracao.setPaymentMethod(meioDePagamento);
		return configuracao;
	}

	private MercadoPagoOrderTransactions transacoes() {
		MercadoPagoOrderTransactionPaymentMethod pix = new MercadoPagoOrderTransactionPaymentMethod();
		pix.setId("pix");
		pix.setType("bank_transfer");
		MercadoPagoOrderTransactionPayment pagamento = new MercadoPagoOrderTransactionPayment();
		pagamento.setAmount("150.00");
		pagamento.setPaymentMethod(pix);
		MercadoPagoOrderTransactions transacoes = new MercadoPagoOrderTransactions();
		transacoes.setPayments(List.of(pagamento));
		return transacoes;
	}

	private MercadoPagoOrderItem item() {
		MercadoPagoOrderItem item = new MercadoPagoOrderItem();
		item.setExternalCode("SRV-7");
		item.setTitle("Troca de pastilhas");
		item.setQuantity(2);
		item.setUnitPrice("75.00");
		return item;
	}

	private MercadoPagoOrderAdditionalInfo informacoesAdicionais() {
		MercadoPagoOrderTravelPassenger passageiro = new MercadoPagoOrderTravelPassenger();
		passageiro.setFirstName("Maria");
		MercadoPagoOrderTravelRoute rota = new MercadoPagoOrderTravelRoute();
		rota.setDeparture("SP");
		MercadoPagoOrderAdditionalInfo informacoes = new MercadoPagoOrderAdditionalInfo();
		informacoes.setTravelPassengers(List.of(passageiro));
		informacoes.setTravelRoutes(List.of(rota));
		return informacoes;
	}
}
