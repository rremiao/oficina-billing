package br.com.oficina.billing.apoio;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Orders API do Mercado Pago simulada com WireMock, um servidor só para toda a suíte. Os testes de
 * integração apontam {@code mercadopago.url} para cá, e o cliente Feign faz chamadas HTTP de verdade.
 */
public final class MercadoPagoSimulado {

	public static final String ACCESS_TOKEN = "TEST-access-token";
	public static final String SEGREDO_WEBHOOK = "segredo-do-webhook-de-teste";

	private static final WireMockServer SERVIDOR = iniciar();

	private MercadoPagoSimulado() {
	}

	private static WireMockServer iniciar() {
		WireMockServer servidor = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		servidor.start();
		return servidor;
	}

	public static WireMockServer servidor() {
		return SERVIDOR;
	}

	public static String url() {
		return SERVIDOR.baseUrl();
	}

	public static void reiniciar() {
		SERVIDOR.resetAll();
	}

	/** Ordem Pix criada como a Orders API responde: aguardando a transferência, com o link do QR Code. */
	public static void ordemCriada(String idOrdem, String linkPix) {
		SERVIDOR.stubFor(post(urlEqualTo("/v1/orders")).willReturn(json("""
				{"id": "%s", "type": "online", "status": "action_required", "status_detail": "waiting_transfer",
				 "total_amount": "329.80",
				 "transactions": {"payments": [{"id": "PAY-1", "amount": "329.80", "status": "action_required",
				   "status_detail": "waiting_transfer",
				   "payment_method": {"id": "pix", "type": "bank_transfer", "ticket_url": "%s",
				     "qr_code": "00020126580014br.gov.bcb.pix"}}]}}
				""".formatted(idOrdem, linkPix))));
	}

	public static void ordemConsultada(String idOrdem, String status, String detalhe, String metodo) {
		SERVIDOR.stubFor(get(urlEqualTo("/v1/orders/" + idOrdem)).willReturn(json("""
				{"id": "%s", "status": "%s", "status_detail": "%s", "total_amount": "329.80",
				 "transactions": {"payments": [{"id": "PAY-1", "status": "%s", "amount": "329.80",
				   "payment_method": {"id": "%s", "type": "bank_transfer"}}]}}
				""".formatted(idOrdem, status, detalhe, status, metodo))));
	}

	public static void ordemEstornada(String idOrdem, String idReembolso, String valor) {
		SERVIDOR.stubFor(post(urlEqualTo("/v1/orders/" + idOrdem + "/refund")).willReturn(json("""
				{"id": "%s", "status": "refunded", "status_detail": "refunded",
				 "transactions": {"refunds": [{"id": "%s", "transaction_id": "PAY-1", "amount": "%s",
				   "status": "processed"}]}}
				""".formatted(idOrdem, idReembolso, valor))));
	}

	public static void falhaAoCriarOrdem() {
		SERVIDOR.stubFor(post(urlEqualTo("/v1/orders")).willReturn(aResponse().withStatus(500)));
	}

	/** Assina o aviso como o Mercado Pago, com o mesmo algoritmo documentado, escrito à parte da aplicação. */
	public static String assinatura(String dataId, String requestId, String instante) {
		String manifesto = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + instante + ";";
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SEGREDO_WEBHOOK.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return "ts=" + instante + ",v1=" + HexFormat.of().formatHex(mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	private static ResponseDefinitionBuilder json(String corpo) {
		return aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(corpo);
	}
}
