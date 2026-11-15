package br.com.oficina.billing.pagamento.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

class PagamentoTest {

	private static final Pagamento.Origem ORIGEM = new Pagamento.Origem(UUID.randomUUID(), 42L, "OS-2026-000042", "corr-1");

	private static Pagamento aguardando() {
		return Pagamento.aguardando(10L, 7L, ORIGEM, new BigDecimal("329.8"), "ORD-1", "https://mp/checkout", "{}");
	}

	private static Pagamento comStatus(StatusPagamento status) {
		return Pagamento.existente(new Pagamento.Dados(5L, 10L, 7L, ORIGEM, new BigDecimal("329.80"), "ORD-1", null,
				status, null, null, null, null, null, null, null));
	}

	@Test
	void pagamentoNovoAguardaOClienteComValorDeDuasCasas() {
		Pagamento pagamento = aguardando();

		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.AGUARDANDO_PAGAMENTO);
		assertThat(pagamento.getValor()).hasToString("329.80");
		assertThat(pagamento.getLinkPagamento()).isEqualTo("https://mp/checkout");
		assertThat(pagamento.getUltimaRespostaProvedor()).isEqualTo("{}");
	}

	@Test
	void confirmarSoMudaQuemAindaAguarda() {
		Pagamento pagamento = aguardando();

		assertThat(pagamento.confirmar("pix", "{\"status\":\"processed\"}")).isTrue();
		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CONFIRMADO);
		assertThat(pagamento.getMetodo()).isEqualTo("pix");
		assertThat(pagamento.confirmar("pix", "{}")).isFalse();
		assertThat(pagamento.recusar("tarde demais", "{}")).isFalse();
		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CONFIRMADO);
	}

	@Test
	void recusarSoMudaQuemAindaAguarda() {
		Pagamento pagamento = aguardando();

		assertThat(pagamento.recusar("expired", "{}")).isTrue();
		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
		assertThat(pagamento.getMotivo()).isEqualTo("expired");
		assertThat(pagamento.confirmar("pix", "{}")).isFalse();
	}

	@Test
	void estornoDePagamentoConfirmadoGuardaOReembolso() {
		Pagamento pagamento = comStatus(StatusPagamento.CONFIRMADO);

		assertThat(pagamento.precisaEstornar()).isTrue();
		pagamento.registrarEstorno("REF-1", new BigDecimal("329.80"), "Peça danificada", "{}");

		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
		assertThat(pagamento.getIdEstorno()).isEqualTo("REF-1");
		assertThat(pagamento.getMotivo()).isEqualTo("Peça danificada");
		assertThat(pagamento.precisaEstornar()).isFalse();
	}

	@Test
	void semValorDoProvedorOEstornoValeOValorPago() {
		Pagamento pagamento = comStatus(StatusPagamento.CONFIRMADO);

		pagamento.registrarEstorno(null, null, "motivo", null);

		assertThat(pagamento.getValorEstornado()).isEqualByComparingTo("329.80");
	}

	@Test
	void estornarDeNovoNaoMudaNada() {
		Pagamento pagamento = comStatus(StatusPagamento.ESTORNADO);

		pagamento.registrarEstorno("REF-2", BigDecimal.ONE, "outro", null);

		assertThat(pagamento.getIdEstorno()).isNull();
	}

	@Test
	void soPagamentoConfirmadoPodeSerEstornado() {
		assertThatThrownBy(() -> comStatus(StatusPagamento.AGUARDANDO_PAGAMENTO).precisaEstornar())
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Só pagamento confirmado pode ser estornado (pagamento 5 está AGUARDANDO_PAGAMENTO)");
		assertThatThrownBy(() -> comStatus(StatusPagamento.RECUSADO).precisaEstornar())
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void pertenceSoAoClienteDoOrcamento() {
		assertThat(aguardando().pertenceAoCliente(7L)).isTrue();
		assertThat(aguardando().pertenceAoCliente(8L)).isFalse();
	}
}
