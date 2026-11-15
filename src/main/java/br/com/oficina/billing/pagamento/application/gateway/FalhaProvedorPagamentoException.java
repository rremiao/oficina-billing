package br.com.oficina.billing.pagamento.application.gateway;

/**
 * O provedor de pagamento não respondeu ou respondeu com erro. É transitória: o comando da Saga é
 * tentado de novo, e a chave de idempotência impede cobrança ou estorno em dobro.
 */
public class FalhaProvedorPagamentoException extends RuntimeException {

	public FalhaProvedorPagamentoException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
