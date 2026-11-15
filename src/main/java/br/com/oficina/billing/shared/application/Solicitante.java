package br.com.oficina.billing.shared.application;

/**
 * Quem está consultando: um funcionário vê qualquer orçamento, um cliente só os próprios. Fica na
 * aplicação, e não no Spring Security, para a regra de posse ser testada sem a camada web.
 *
 * @param clienteId id do cliente autenticado, ou nulo para funcionário
 */
public record Solicitante(Long clienteId) {

	public static Solicitante funcionario() {
		return new Solicitante(null);
	}

	public static Solicitante cliente(Long clienteId) {
		return new Solicitante(clienteId);
	}

	public boolean ehCliente() {
		return clienteId != null;
	}
}
