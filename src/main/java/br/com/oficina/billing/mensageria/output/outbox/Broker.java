package br.com.oficina.billing.mensageria.output.outbox;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Quem entrega a mensagem. Hoje é o RabbitMQ; o outbox não sabe disso. */
public interface Broker {

	/**
	 * Envia e só retorna depois da confirmação do broker.
	 *
	 * @throws FalhaPublicacaoException se o broker não confirmou
	 */
	void enviar(MensagemSaida mensagem);

	/**
	 * Mensagem pronta para sair.
	 *
	 * @param messageId       id único, chave de deduplicação de quem consome
	 * @param tipo            nome do evento
	 * @param destino         exchange que recebe
	 * @param chaveRoteamento routing key
	 * @param ordemServicoId  OS a que a mensagem se refere
	 * @param correlationId   id da requisição original
	 * @param corpo           envelope já serializado em JSON
	 * @param criadoEm        momento em que o evento foi registrado
	 * @param rastreamento    cabeçalhos do trace de quem gerou o evento (vazio sem trace)
	 */
	record MensagemSaida(UUID messageId, String tipo, String destino, String chaveRoteamento, Long ordemServicoId,
			String correlationId, String corpo, LocalDateTime criadoEm, Map<String, String> rastreamento) {

		public MensagemSaida {
			rastreamento = rastreamento == null ? Map.of() : Map.copyOf(rastreamento);
		}
	}

	/** O broker não confirmou: o evento continua pendente e sai na próxima rodada. */
	class FalhaPublicacaoException extends RuntimeException {

		public FalhaPublicacaoException(String mensagem, Throwable causa) {
			super(mensagem, causa);
		}
	}
}
