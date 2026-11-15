package br.com.oficina.billing.shared.domain;

import br.com.oficina.billing.shared.exception.RegraNegocioException;

/** Normalização de texto de cadastro: sem espaços nas pontas, para "Alinhamento " não driblar o nome único. */
public final class Texto {

	private Texto() {
	}

	public static String obrigatorio(String valor, String mensagem) {
		if (valor == null || valor.isBlank()) {
			throw new RegraNegocioException(mensagem);
		}
		return valor.strip();
	}

	public static String opcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}
}
