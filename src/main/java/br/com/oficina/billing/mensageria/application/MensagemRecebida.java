package br.com.oficina.billing.mensageria.application;

/**
 * Mensagem entregue pelo broker, sem nada específico de broker.
 *
 * @param corpo         envelope em JSON, como chegou
 * @param tipo          tipo nas propriedades do broker (pode faltar em mensagem malformada)
 * @param correlationId id de correlação do cabeçalho, ou nulo
 */
public record MensagemRecebida(String corpo, String tipo, String correlationId) {
}
