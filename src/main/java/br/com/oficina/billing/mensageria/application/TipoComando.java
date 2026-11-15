package br.com.oficina.billing.mensageria.application;

/** Comandos que o OS Service manda ao Billing (routing key em {@code os.eventos} = nome). */
public enum TipoComando {

	SOLICITAR_ORCAMENTO("SolicitarOrcamento"),

	COBRAR_PAGAMENTO("CobrarPagamento"),

	ESTORNAR_PAGAMENTO("EstornarPagamento");

	private final String nome;

	TipoComando(String nome) {
		this.nome = nome;
	}

	public String nome() {
		return nome;
	}
}
