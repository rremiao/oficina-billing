package br.com.oficina.billing.mensageria.application;

/**
 * Saída da Saga: registra o evento para o OS Service. Exige transação aberta, para o evento existir
 * só se a mudança que o provocou for gravada (Transactional Outbox). Publicar de novo o mesmo fato só
 * agenda o reenvio do evento já registrado.
 */
public interface PublicadorEventos {

	void publicar(EventoSaga evento);
}
