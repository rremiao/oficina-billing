package br.com.oficina.billing.catalogo.item.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class ItemTest {

	@Test
	void itemNovoNasceAtivoComTextoSemEspacosEValorComDuasCasas() {
		Item item = Item.novo(new Item.Dados(" Pastilha de freio ", " ", TipoItem.PECA, new BigDecimal("89.9"), " UN "));

		assertThat(item.getId()).isNull();
		assertThat(item.getNome()).isEqualTo("Pastilha de freio");
		assertThat(item.getDescricao()).isNull();
		assertThat(item.getTipo()).isEqualTo(TipoItem.PECA);
		assertThat(item.getValorUnitario()).hasToString("89.90");
		assertThat(item.getUnidadeMedida()).isEqualTo("UN");
		assertThat(item.isAtivo()).isTrue();
	}

	@Test
	void tipoEObrigatorio() {
		assertThatThrownBy(() -> Item.novo(new Item.Dados("Óleo", null, null, BigDecimal.TEN, "L")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Tipo é obrigatório");
	}

	@Test
	void nomeEObrigatorio() {
		assertThatThrownBy(() -> Item.novo(new Item.Dados(" ", null, TipoItem.INSUMO, BigDecimal.TEN, "L")))
				.hasMessage("Nome é obrigatório");
	}

	@Test
	void unidadeDeMedidaEObrigatoria() {
		assertThatThrownBy(() -> Item.novo(new Item.Dados("Óleo", null, TipoItem.INSUMO, BigDecimal.TEN, null)))
				.hasMessage("Unidade de medida é obrigatória");
	}

	@Test
	void valorUnitarioPrecisaSerPositivoEComDuasCasas() {
		assertThatThrownBy(() -> Item.novo(new Item.Dados("Óleo", null, TipoItem.INSUMO, BigDecimal.ZERO, "L")))
				.hasMessage("Valor unitário deve ser maior que zero");
		assertThatThrownBy(() -> Item.novo(new Item.Dados("Óleo", null, TipoItem.INSUMO, new BigDecimal("1.234"), "L")))
				.hasMessage("Valor unitário deve ter no máximo duas casas decimais");
	}

	@Test
	void atualizarTrocaOsDadosComAsMesmasRegras() {
		Item item = Item.novo(new Item.Dados("Óleo", null, TipoItem.INSUMO, BigDecimal.TEN, "L"));

		item.atualizar(new Item.Dados("Óleo 5W30", "Sintético", TipoItem.INSUMO, new BigDecimal("45"), "L"));

		assertThat(item.getNome()).isEqualTo("Óleo 5W30");
		assertThat(item.getDescricao()).isEqualTo("Sintético");
		assertThat(item.getValorUnitario()).hasToString("45.00");
		assertThatThrownBy(() -> item.atualizar(new Item.Dados("Óleo", null, null, BigDecimal.TEN, "L")))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void inativarDesligaOItem() {
		Item item = Item.novo(new Item.Dados("Óleo", null, TipoItem.INSUMO, BigDecimal.TEN, "L"));

		item.inativar();

		assertThat(item.isAtivo()).isFalse();
	}

	@Test
	void itemExistenteMantemIdEDatas() {
		LocalDateTime criacao = LocalDateTime.of(2026, 11, 1, 10, 0);

		Item item = Item.existente(3L, new Item.Dados("Óleo", null, TipoItem.INSUMO, BigDecimal.TEN, "L"), false,
				criacao, null);

		assertThat(item.getId()).isEqualTo(3L);
		assertThat(item.isAtivo()).isFalse();
		assertThat(item.getDataCriacao()).isEqualTo(criacao);
		assertThat(item.getDataAtualizacao()).isNull();
	}
}
