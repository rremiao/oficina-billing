package br.com.oficina.billing.pagamento.entrypoint.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Confere o cabeçalho {@code x-signature} dos avisos do Mercado Pago: {@code ts=<instante>,v1=<hmac>},
 * onde o hmac é o HMAC-SHA256, com o segredo do webhook, de
 * {@code id:<data.id>;request-id:<x-request-id>;ts:<instante>;}. Sem segredo configurado, nenhum aviso
 * é aceito.
 */
@Component
class AssinaturaMercadoPago {

	private static final String ALGORITMO = "HmacSHA256";

	private final String segredo;

	AssinaturaMercadoPago(@Value("${mercadopago.webhook-secret}") String segredo) {
		this.segredo = segredo;
	}

	boolean valida(String assinatura, String requestId, String dataId) {
		if (segredo == null || segredo.isBlank() || assinatura == null || dataId == null) {
			return false;
		}
		String instante = null;
		String recebida = null;
		for (String parte : assinatura.split(",")) {
			String[] chaveValor = parte.trim().split("=", 2);
			if (chaveValor.length == 2 && "ts".equals(chaveValor[0])) {
				instante = chaveValor[1];
			} else if (chaveValor.length == 2 && "v1".equals(chaveValor[0])) {
				recebida = chaveValor[1];
			}
		}
		if (instante == null || recebida == null) {
			return false;
		}
		byte[] esperada = HexFormat.of().formatHex(hmac(manifesto(dataId, requestId, instante)))
				.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(esperada, recebida.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
	}

	/** O Mercado Pago assina o id em minúsculas e deixa de fora o que não veio no aviso. */
	static String manifesto(String dataId, String requestId, String instante) {
		StringBuilder manifesto = new StringBuilder("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
		if (requestId != null) {
			manifesto.append("request-id:").append(requestId).append(';');
		}
		return manifesto.append("ts:").append(instante).append(';').toString();
	}

	private byte[] hmac(String conteudo) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), ALGORITMO));
			return mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HMAC-SHA256 indisponível na JVM", e);
		}
	}
}
