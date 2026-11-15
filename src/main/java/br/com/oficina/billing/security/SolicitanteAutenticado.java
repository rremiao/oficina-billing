package br.com.oficina.billing.security;

import br.com.oficina.billing.shared.application.Solicitante;
import org.springframework.security.core.Authentication;

/** Traduz a autenticação do Spring Security no {@link Solicitante} que os casos de uso entendem. */
public final class SolicitanteAutenticado {

	private SolicitanteAutenticado() {
	}

	public static Solicitante de(Authentication autenticacao) {
		return autenticacao.getPrincipal() instanceof ClienteAutenticado cliente
				? Solicitante.cliente(cliente.id())
				: Solicitante.funcionario();
	}
}
