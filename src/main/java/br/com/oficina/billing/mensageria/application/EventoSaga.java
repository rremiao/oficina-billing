package br.com.oficina.billing.mensageria.application;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento do Billing para o OS Service, ainda sem nada de broker.
 *
 * @param tipo               qual evento
 * @param chave              o que identifica o fato (o sagaId do orçamento, o id do pagamento): o mesmo
 *                           fato gera sempre o mesmo {@code messageId}
 * @param sagaId             ciclo da Saga, devolvido como veio no comando
 * @param ordemServicoId     OS a que o evento se refere
 * @param codigoOrdemServico código legível da OS, opcional
 * @param correlationId      id da requisição original; nulo usa o da mensagem ou requisição atual
 * @param dados              conteúdo do evento, serializado em JSON
 */
public record EventoSaga(TipoEvento tipo, String chave, UUID sagaId, Long ordemServicoId, String codigoOrdemServico,
		String correlationId, Object dados) {

	public EventoSaga {
		Objects.requireNonNull(tipo);
		Objects.requireNonNull(chave);
		Objects.requireNonNull(sagaId);
		Objects.requireNonNull(ordemServicoId);
	}

	/**
	 * Determinístico: responder de novo a um comando repetido publica o mesmo id, e o OS Service
	 * descarta a repetição em vez de mandá-la para a DLQ.
	 */
	public UUID messageId() {
		return UUID.nameUUIDFromBytes(("billing:" + tipo.nome() + ":" + chave).getBytes(StandardCharsets.UTF_8));
	}
}
