package br.com.oficina.billing.mensageria.application;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Formato comum de todas as mensagens entre os serviços (corpo JSON), igual ao do OS Service.
 *
 * @param messageId          id único da mensagem; o OS Service sempre manda e deduplica por ele
 * @param tipo               nome do comando ou evento (também é a routing key)
 * @param sagaId             ciclo da Saga; obrigatório, e quem responde devolve o mesmo
 * @param ordemServicoId     OS a que a mensagem se refere
 * @param codigoOrdemServico código legível da OS
 * @param correlationId      id que acompanha a requisição original por todos os serviços
 * @param ocorridoEm         momento em que o fato aconteceu
 * @param versao             versão do envelope (ausente vale 1)
 * @param dados              conteúdo específico do tipo
 */
public record EnvelopeMensagem(
		UUID messageId,
		String tipo,
		UUID sagaId,
		Long ordemServicoId,
		String codigoOrdemServico,
		String correlationId,
		Instant ocorridoEm,
		Integer versao,
		JsonNode dados
) {

	public static final int VERSAO_ATUAL = 1;
}
