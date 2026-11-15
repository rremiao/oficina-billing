package br.com.oficina.billing.pagamento.application.dto;

import java.util.UUID;

/**
 * Comando {@code EstornarPagamento} do OS Service: compensação da Saga.
 *
 * @param sagaId      ciclo da Saga; precisa ser o mesmo do pagamento
 * @param pagamentoId pagamento a estornar
 * @param motivo      por que a OS está sendo compensada
 */
public record SolicitacaoEstorno(UUID sagaId, Long pagamentoId, String motivo) {
}
