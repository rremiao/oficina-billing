package br.com.oficina.billing.catalogo.servico.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class ServicoTest {

	@Test
	void servicoNovoNasceAtivoComNomeSemEspacosEValorComDuasCasas() {
		Servico servico = Servico.novo("  Alinhamento  ", "   ", new BigDecimal("150"));

		assertThat(servico.getId()).isNull();
		assertThat(servico.getNome()).isEqualTo("Alinhamento");
		assertThat(servico.getDescricao()).isNull();
		assertThat(servico.getValor()).isEqualByComparingTo("150.00").hasToString("150.00");
		assertThat(servico.isAtivo()).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void nomeEObrigatorio(String nome) {
		assertThatThrownBy(() -> Servico.novo(nome, null, BigDecimal.TEN))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Nome é obrigatório");
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "-1", "0.00"})
	void valorPrecisaSerPositivo(String valor) {
		assertThatThrownBy(() -> Servico.novo("Alinhamento", null, new BigDecimal(valor)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Valor deve ser maior que zero");
	}

	@Test
	void valorNuloNaoEAceito() {
		assertThatThrownBy(() -> Servico.novo("Alinhamento", null, null))
				.hasMessage("Valor deve ser maior que zero");
	}

	@Test
	void valorComTresCasasNaoEArredondado() {
		assertThatThrownBy(() -> Servico.novo("Alinhamento", null, new BigDecimal("10.005")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Valor deve ter no máximo duas casas decimais");
	}

	@Test
	void zerosAMaisNaoContamComoCasaDecimal() {
		assertThat(Servico.novo("Alinhamento", null, new BigDecimal("10.500")).getValor()).hasToString("10.50");
	}

	@Test
	void atualizarTrocaOsDadosComAsMesmasRegras() {
		Servico servico = Servico.novo("Alinhamento", null, BigDecimal.TEN);

		servico.atualizar(" Balanceamento ", " Rodas dianteiras ", new BigDecimal("80.5"));

		assertThat(servico.getNome()).isEqualTo("Balanceamento");
		assertThat(servico.getDescricao()).isEqualTo("Rodas dianteiras");
		assertThat(servico.getValor()).hasToString("80.50");
		assertThatThrownBy(() -> servico.atualizar("", null, BigDecimal.TEN)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void inativarDesligaOServico() {
		Servico servico = Servico.novo("Alinhamento", null, BigDecimal.TEN);

		servico.inativar();

		assertThat(servico.isAtivo()).isFalse();
	}

	@Test
	void servicoExistenteMantemIdEDatas() {
		LocalDateTime criacao = LocalDateTime.of(2026, 11, 1, 10, 0);
		LocalDateTime atualizacao = LocalDateTime.of(2026, 11, 2, 10, 0);

		Servico servico = Servico.existente(7L, "Alinhamento", "desc", BigDecimal.TEN, false, criacao, atualizacao);

		assertThat(servico.getId()).isEqualTo(7L);
		assertThat(servico.isAtivo()).isFalse();
		assertThat(servico.getDataCriacao()).isEqualTo(criacao);
		assertThat(servico.getDataAtualizacao()).isEqualTo(atualizacao);
	}
}
