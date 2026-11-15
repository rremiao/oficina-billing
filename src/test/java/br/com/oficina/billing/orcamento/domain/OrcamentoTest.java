package br.com.oficina.billing.orcamento.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class OrcamentoTest {

	private static final UUID SAGA = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");

	private static Orcamento.Cabecalho cabecalho() {
		return new Orcamento.Cabecalho(SAGA, 42L, "OS-2026-000042", 7L, 3L);
	}

	@Test
	void totaisSaemDasLinhasComDuasCasas() {
		Orcamento orcamento = Orcamento.gerar(cabecalho(),
				List.of(new LinhaServico(30L, "Troca de pastilhas", new BigDecimal("150")),
						new LinhaServico(31L, "Alinhamento", new BigDecimal("80.5"))),
				List.of(new LinhaItem(20L, "Pastilha", 2, new BigDecimal("89.90")),
						new LinhaItem(21L, "Fluido", 1, new BigDecimal("40"))));

		assertThat(orcamento.getValorServicos()).hasToString("230.50");
		assertThat(orcamento.getValorItens()).hasToString("219.80");
		assertThat(orcamento.getValorTotal()).hasToString("450.30");
		assertThat(orcamento.getItens().get(0).valorTotal()).hasToString("179.80");
		assertThat(orcamento.getId()).isNull();
	}

	@Test
	void orcamentoSoComServicosOuSoComItensEValido() {
		Orcamento soServicos = Orcamento.gerar(cabecalho(),
				List.of(new LinhaServico(30L, "Alinhamento", BigDecimal.TEN)), List.of());
		Orcamento soItens = Orcamento.gerar(cabecalho(), List.of(),
				List.of(new LinhaItem(20L, "Pastilha", 1, BigDecimal.TEN)));

		assertThat(soServicos.getValorItens()).hasToString("0.00");
		assertThat(soServicos.getValorTotal()).hasToString("10.00");
		assertThat(soItens.getValorServicos()).hasToString("0.00");
	}

	@Test
	void orcamentoSemLinhaNenhumaNaoExiste() {
		assertThatThrownBy(() -> Orcamento.gerar(cabecalho(), List.of(), List.of()))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O orçamento precisa de ao menos um serviço ou item");
	}

	@Test
	void cabecalhoExigeSagaOrdemDeServicoECliente() {
		assertThatThrownBy(() -> new Orcamento.Cabecalho(null, 42L, null, 7L, null))
				.hasMessage("O pedido de orçamento precisa do sagaId");
		assertThatThrownBy(() -> new Orcamento.Cabecalho(SAGA, null, null, 7L, null))
				.hasMessage("O pedido de orçamento precisa da ordem de serviço");
		assertThatThrownBy(() -> new Orcamento.Cabecalho(SAGA, 42L, null, null, null))
				.hasMessage("O pedido de orçamento precisa do cliente");
	}

	@Test
	void quantidadeDoItemPrecisaSerPositiva() {
		assertThatThrownBy(() -> new LinhaItem(20L, "Pastilha", 0, BigDecimal.TEN))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Quantidade do item 20 deve ser maior que zero");
	}

	@Test
	void linhaExigeIdNomeEPrecoPositivo() {
		assertThatThrownBy(() -> new LinhaServico(null, "Alinhamento", BigDecimal.TEN))
				.hasMessage("Toda linha do orçamento precisa do id do catálogo");
		assertThatThrownBy(() -> new LinhaItem(null, "Pastilha", 1, BigDecimal.TEN))
				.hasMessage("Toda linha do orçamento precisa do id do catálogo");
		assertThatThrownBy(() -> new LinhaServico(30L, " ", BigDecimal.TEN)).hasMessage("Nome é obrigatório");
		assertThatThrownBy(() -> new LinhaServico(30L, "Alinhamento", BigDecimal.ZERO))
				.hasMessage("Valor do serviço deve ser maior que zero");
	}

	@Test
	void totalQuePassaDoQueOBancoGuardaViraRegraDeNegocio() {
		assertThatThrownBy(() -> new LinhaItem(20L, "Pastilha", 2_000_000, new BigDecimal("89.90")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Valor do item passa do limite de 99.999.999,99");

		List<LinhaItem> itens = List.of(new LinhaItem(20L, "A", 1, new BigDecimal("60000000")),
				new LinhaItem(21L, "B", 1, new BigDecimal("60000000")));
		assertThatThrownBy(() -> Orcamento.gerar(cabecalho(), List.of(), itens))
				.hasMessage("Valor dos itens passa do limite de 99.999.999,99");
	}

	@Test
	void pertenceSoAoClienteDaOs() {
		Orcamento orcamento = Orcamento.existente(1L, cabecalho(),
				List.of(new LinhaServico(30L, "Alinhamento", BigDecimal.TEN)), List.of(),
				LocalDateTime.of(2026, 11, 14, 10, 0));

		assertThat(orcamento.pertenceAoCliente(7L)).isTrue();
		assertThat(orcamento.pertenceAoCliente(8L)).isFalse();
		assertThat(orcamento.getId()).isEqualTo(1L);
		assertThat(orcamento.getDataCriacao()).isEqualTo(LocalDateTime.of(2026, 11, 14, 10, 0));
	}

	@Test
	void linhasNaoPodemSerAlteradasDepoisDeGerado() {
		Orcamento orcamento = Orcamento.gerar(cabecalho(),
				List.of(new LinhaServico(30L, "Alinhamento", BigDecimal.TEN)), List.of());

		assertThatThrownBy(() -> orcamento.getServicos().add(new LinhaServico(31L, "X", BigDecimal.ONE)))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
