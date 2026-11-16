package br.com.oficina.billing.mensageria.application;

import java.util.Map;

/**
 * Mensagem entregue pelo broker, sem nada específico de broker.
 *
 * @param corpo         envelope em JSON, como chegou
 * @param tipo          tipo nas propriedades do broker (pode faltar em mensagem malformada)
 * @param correlationId id de correlação do cabeçalho, ou nulo
 * @param rastreamento  cabeçalhos do trace de quem enviou (traceparent, newrelic etc.)
 */
public record MensagemRecebida(String corpo, String tipo, String correlationId, Map<String, String> rastreamento) {

	public MensagemRecebida {
		rastreamento = rastreamento == null ? Map.of() : Map.copyOf(rastreamento);
	}

	public MensagemRecebida(String corpo, String tipo, String correlationId) {
		this(corpo, tipo, correlationId, Map.of());
	}
}
