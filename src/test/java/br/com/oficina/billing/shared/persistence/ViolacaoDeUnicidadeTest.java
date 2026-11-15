package br.com.oficina.billing.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ViolacaoDeUnicidadeTest {

	@Test
	void reconheceOIndiceNaCadeiaDeCausas() {
		DataIntegrityViolationException excecao = new DataIntegrityViolationException("could not execute statement",
				new IllegalStateException("duplicate key value violates unique constraint \"ux_servico_nome\""));

		assertThat(ViolacaoDeUnicidade.doIndice(excecao, "ux_servico_nome")).isTrue();
	}

	@Test
	void outraRestricaoNaoContaComoDuplicidade() {
		DataIntegrityViolationException excecao = new DataIntegrityViolationException("check constraint",
				new IllegalStateException((String) null));

		assertThat(ViolacaoDeUnicidade.doIndice(excecao, "ux_servico_nome")).isFalse();
	}
}
