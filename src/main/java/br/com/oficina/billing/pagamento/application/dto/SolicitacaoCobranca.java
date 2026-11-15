package br.com.oficina.billing.pagamento.application.dto;

import java.util.UUID;

/**
 * Comando {@code CobrarPagamento} do OS Service: o cliente aprovou o orçamento. OS, cliente e valor
 * vêm do próprio orçamento.
 *
 * @param sagaId        ciclo da Saga; precisa ser o mesmo do orçamento
 * @param orcamentoId   orçamento aprovado
 * @param correlationId id da requisição original, guardado para os eventos que vierem depois
 */
public record SolicitacaoCobranca(UUID sagaId, Long orcamentoId, String correlationId) {
}
