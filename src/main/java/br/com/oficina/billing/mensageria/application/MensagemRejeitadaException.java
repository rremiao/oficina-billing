package br.com.oficina.billing.mensageria.application;

/**
 * Rejeição definitiva: o adaptador do broker manda a mensagem direto para a DLQ, sem nova tentativa.
 * Qualquer outra exceção do {@link ConsumidorComandos} é falha transitória.
 */
public class MensagemRejeitadaException extends RuntimeException {

	public MensagemRejeitadaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
