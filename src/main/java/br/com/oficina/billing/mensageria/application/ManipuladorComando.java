package br.com.oficina.billing.mensageria.application;

/**
 * Reação a um tipo de comando do OS Service. Lança {@link MensagemInvalidaException} ou
 * {@code RegraNegocioException} quando o comando nunca vai poder ser processado (vai para a DLQ) e
 * qualquer outra exceção quando a falha é transitória (é tentado de novo). O caso de uso chamado é
 * idempotente: a segunda entrega não tem efeito além de responder de novo.
 */
public interface ManipuladorComando {

	TipoComando tipo();

	void manipular(EnvelopeMensagem envelope);
}
