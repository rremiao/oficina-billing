package br.com.oficina.billing.orcamento.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.orcamento.application.dto.OrcamentoResponse;
import br.com.oficina.billing.orcamento.application.gateway.OrcamentoGateway;
import br.com.oficina.billing.orcamento.domain.LinhaServico;
import br.com.oficina.billing.orcamento.domain.Orcamento;
import br.com.oficina.billing.shared.application.Solicitante;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ConsultarOrcamentosTest {

	private final OrcamentoGateway gateway = mock(OrcamentoGateway.class);
	private final ConsultarOrcamentos consultar = new ConsultarOrcamentos(gateway);

	private static Orcamento orcamento(Long id, Long clienteId) {
		return Orcamento.existente(id, new Orcamento.Cabecalho(UUID.randomUUID(), 42L, null, clienteId, null),
				List.of(new LinhaServico(30L, "Alinhamento", BigDecimal.TEN)), List.of(), null);
	}

	@Test
	void funcionarioVeQualquerOrcamento() {
		when(gateway.buscarPorId(1L)).thenReturn(Optional.of(orcamento(1L, 7L)));

		assertThat(consultar.buscarPorId(1L, Solicitante.funcionario()).clienteId()).isEqualTo(7L);
	}

	@Test
	void clienteVeOProprioOrcamento() {
		when(gateway.buscarPorId(1L)).thenReturn(Optional.of(orcamento(1L, 7L)));

		assertThat(consultar.buscarPorId(1L, Solicitante.cliente(7L)).id()).isEqualTo(1L);
	}

	@Test
	void orcamentoDeOutroClienteRespondeComoSeNaoExistisse() {
		when(gateway.buscarPorId(1L)).thenReturn(Optional.of(orcamento(1L, 7L)));

		assertThatThrownBy(() -> consultar.buscarPorId(1L, Solicitante.cliente(8L)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Orçamento não encontrado");
	}

	@Test
	void orcamentoInexistenteResponde404() {
		when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> consultar.buscarPorId(9L, Solicitante.funcionario()))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listaPorOsMostraAoClienteSoOsProprios() {
		when(gateway.listarPorOrdemServico(42L)).thenReturn(List.of(orcamento(2L, 7L), orcamento(1L, 8L)));

		assertThat(consultar.listarPorOrdemServico(42L, Solicitante.funcionario()))
				.extracting(OrcamentoResponse::id).containsExactly(2L, 1L);
		assertThat(consultar.listarPorOrdemServico(42L, Solicitante.cliente(7L)))
				.extracting(OrcamentoResponse::id).containsExactly(2L);
	}
}
