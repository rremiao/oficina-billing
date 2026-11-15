package br.com.oficina.billing.mensageria.application;

/** Eventos que o Billing publica para o OS Service (routing key em {@code billing.eventos} = nome). */
public enum TipoEvento {

	ORCAMENTO_GERADO("OrcamentoGerado"),

	PAGAMENTO_CONFIRMADO("PagamentoConfirmado"),

	PAGAMENTO_RECUSADO("PagamentoRecusado"),

	PAGAMENTO_ESTORNADO("PagamentoEstornado");

	private final String nome;

	TipoEvento(String nome) {
		this.nome = nome;
	}

	public String nome() {
		return nome;
	}
}
