package br.com.oficina.billing.shared.persistence;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Reconhece a violação de um índice único pelo nome. Serve para traduzir em regra de negócio o
 * cadastro duplicado que passou pela checagem da aplicação porque outro cadastro igual gravou antes.
 */
public final class ViolacaoDeUnicidade {

	private ViolacaoDeUnicidade() {
	}

	public static boolean doIndice(DataIntegrityViolationException excecao, String indice) {
		Throwable causa = excecao;
		while (causa != null) {
			if (causa.getMessage() != null && causa.getMessage().contains(indice)) {
				return true;
			}
			causa = causa.getCause();
		}
		return false;
	}
}
