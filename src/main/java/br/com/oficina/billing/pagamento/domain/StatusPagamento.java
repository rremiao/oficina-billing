package br.com.oficina.billing.pagamento.domain;

/**
 * Ciclo do pagamento:
 *
 * <pre>
 * AGUARDANDO_PAGAMENTO ─→ CONFIRMADO ─→ ESTORNADO
 *          └────────────→ RECUSADO
 * </pre>
 */
public enum StatusPagamento {

	/** Cobrança criada no provedor, esperando o cliente pagar no checkout. */
	AGUARDANDO_PAGAMENTO,

	CONFIRMADO,

	/** O provedor recusou, ou a cobrança expirou ou foi cancelada sem pagamento. */
	RECUSADO,

	/** Compensação da Saga: o valor voltou para o cliente. */
	ESTORNADO
}
