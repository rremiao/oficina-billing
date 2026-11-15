package br.com.oficina.billing.pagamento.entrypoint.webhook;

import br.com.oficina.billing.pagamento.application.usecase.AtualizarSituacaoPagamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos do Mercado Pago sobre as ordens (configurados no painel da aplicação, tópico "Order").
 * Público, sem JWT: a origem é conferida pela assinatura. O aviso só diz qual ordem mudou; a situação
 * vem de uma consulta à API do Mercado Pago, feita no caso de uso.
 */
@RestController
@RequestMapping("/pagamentos/webhook")
@Tag(name = "Webhook do Mercado Pago", description = "Avisos de mudança nas ordens de pagamento")
public class MercadoPagoWebhookController {

	private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoWebhookController.class);
	private static final String TOPICO_ORDEM = "order";

	private final AssinaturaMercadoPago assinatura;
	private final AtualizarSituacaoPagamento atualizarSituacao;

	MercadoPagoWebhookController(AssinaturaMercadoPago assinatura, AtualizarSituacaoPagamento atualizarSituacao) {
		this.assinatura = assinatura;
		this.atualizarSituacao = atualizarSituacao;
	}

	@PostMapping("/mercadopago")
	@SecurityRequirements
	@Operation(summary = "Receber aviso do Mercado Pago",
			description = "Exige x-signature válido. Responde 200 também para ordem desconhecida, para o Mercado "
					+ "Pago não reenviar.")
	public ResponseEntity<Object> receber(
			@RequestHeader(name = "x-signature", required = false) String xSignature,
			@RequestHeader(name = "x-request-id", required = false) String xRequestId,
			@RequestParam(name = "data.id", required = false) String dataIdNaUrl,
			@RequestParam(name = "type", required = false) String tipoNaUrl,
			@RequestBody(required = false) AvisoMercadoPago aviso) {
		String dataId = dataIdNaUrl != null ? dataIdNaUrl : aviso == null ? null : aviso.idDaOrdem();
		String tipo = tipoNaUrl != null ? tipoNaUrl : aviso == null ? null : aviso.type();

		if (!assinatura.valida(xSignature, xRequestId, dataId)) {
			ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
					"Assinatura do aviso ausente ou inválida");
			problema.setTitle("Nao autenticado");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
		}

		// O log não leva nada que veio do aviso (público): só o que está no banco
		if (TOPICO_ORDEM.equals(tipo)) {
			atualizarSituacao.executar(dataId).ifPresentOrElse(
					pagamento -> LOG.info("webhook_mercadopago pagamentoId={} status={}", pagamento.id(), pagamento.status()),
					() -> LOG.info("webhook_mercadopago_ordem_desconhecida"));
		} else {
			LOG.info("webhook_mercadopago_ignorado topico_diferente_de_order");
		}
		return ResponseEntity.ok().build();
	}

	/** Corpo do aviso: {@code {"type": "order", "action": "order.processed", "data": {"id": "..."}}}. */
	public record AvisoMercadoPago(String type, String action, Dados data) {

		String idDaOrdem() {
			return data == null ? null : data.id();
		}

		public record Dados(String id) {
		}
	}
}
