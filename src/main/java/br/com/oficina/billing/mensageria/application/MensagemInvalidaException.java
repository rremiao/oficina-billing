package br.com.oficina.billing.mensageria.application;

/**
 * Mensagem que nunca vai ser processada, por mais que se tente: JSON inválido, campo obrigatório
 * ausente ou tipo sem manipulador. Vai direto para a DLQ.
 */
public class MensagemInvalidaException extends RuntimeException {

	public MensagemInvalidaException(String mensagem) {
		super(mensagem);
	}

	public MensagemInvalidaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
