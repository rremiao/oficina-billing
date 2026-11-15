package br.com.oficina.billing.orcamento.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.mensageria.application.EventoSaga;
import br.com.oficina.billing.mensageria.application.PublicadorEventos;
import br.com.oficina.billing.mensageria.application.TipoEvento;
import br.com.oficina.billing.orcamento.application.dto.OrcamentoResponse;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento.ItemSolicitado;
import br.com.oficina.billing.orcamento.application.evento.OrcamentoGeradoDados;
import br.com.oficina.billing.orcamento.application.gateway.CatalogoPrecos;
import br.com.oficina.billing.orcamento.application.gateway.CatalogoPrecos.Preco;
import br.com.oficina.billing.orcamento.application.gateway.OrcamentoGateway;
import br.com.oficina.billing.orcamento.domain.LinhaServico;
import br.com.oficina.billing.orcamento.domain.Orcamento;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class GerarOrcamentoTest {

	private static final UUID SAGA = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");

	private final OrcamentoGateway orcamentoGateway = mock(OrcamentoGateway.class);
	private final CatalogoPrecos catalogoPrecos = mock(CatalogoPrecos.class);
	private final PublicadorEventos publicador = mock(PublicadorEventos.class);
	private final GerarOrcamento gerar = new GerarOrcamento(orcamentoGateway, catalogoPrecos, publicador);

	@BeforeEach
	void catalogo() {
		when(orcamentoGateway.buscarPorSagaId(any())).thenReturn(Optional.empty());
		when(orcamentoGateway.salvar(any())).thenAnswer(i -> i.getArgument(0));
		when(catalogoPrecos.servicos(anyCollection())).thenReturn(Map.of(
				30L, new Preco("Troca de pastilhas", new BigDecimal("150.00"), true),
				31L, new Preco("Serviço antigo", new BigDecimal("10.00"), false)));
		when(catalogoPrecos.itens(anyCollection())).thenReturn(Map.of(
				20L, new Preco("Pastilha de freio", new BigDecimal("89.90"), true),
				21L, new Preco("Item antigo", new BigDecimal("1.00"), false)));
	}

	private static SolicitacaoOrcamento pedido(List<Long> servicos, List<ItemSolicitado> itens) {
		return new SolicitacaoOrcamento(SAGA, 42L, "OS-2026-000042", 7L, 3L, servicos, itens);
	}

	@Test
	void montaOOrcamentoComNomeEPrecoDoCatalogo() {
		OrcamentoResponse resposta = gerar.executar(pedido(List.of(30L), List.of(new ItemSolicitado(20L, 2))));

		assertThat(resposta.sagaId()).isEqualTo(SAGA);
		assertThat(resposta.ordemServicoId()).isEqualTo(42L);
		assertThat(resposta.servicos()).singleElement()
				.satisfies(s -> assertThat(s.nome()).isEqualTo("Troca de pastilhas"));
		assertThat(resposta.itens()).singleElement()
				.satisfies(i -> assertThat(i.valorTotal()).hasToString("179.80"));
		assertThat(resposta.valorTotal()).hasToString("329.80");
	}

	@Test
	void respondeOrcamentoGeradoComOsTotaisEAChaveDaSaga() {
		gerar.executar(pedido(List.of(30L), List.of(new ItemSolicitado(20L, 2))));

		ArgumentCaptor<EventoSaga> evento = ArgumentCaptor.forClass(EventoSaga.class);
		verify(publicador).publicar(evento.capture());
		assertThat(evento.getValue().tipo()).isEqualTo(TipoEvento.ORCAMENTO_GERADO);
		assertThat(evento.getValue().chave()).isEqualTo(SAGA.toString());
		assertThat(evento.getValue().sagaId()).isEqualTo(SAGA);
		assertThat(evento.getValue().ordemServicoId()).isEqualTo(42L);
		assertThat(evento.getValue().dados()).isEqualTo(new OrcamentoGeradoDados(null, new BigDecimal("150.00"),
				new BigDecimal("179.80"), new BigDecimal("329.80")));
	}

	@Test
	void pedidoRepetidoDaMesmaSagaDevolveOOrcamentoJaGeradoSemGravarDeNovo() {
		Orcamento jaGerado = Orcamento.existente(5L, new Orcamento.Cabecalho(SAGA, 42L, null, 7L, null),
				List.of(new LinhaServico(30L, "Troca de pastilhas", new BigDecimal("140.00"))), List.of(), null);
		when(orcamentoGateway.buscarPorSagaId(SAGA)).thenReturn(Optional.of(jaGerado));

		OrcamentoResponse resposta = gerar.executar(pedido(List.of(30L), List.of()));

		assertThat(resposta.id()).isEqualTo(5L);
		assertThat(resposta.valorTotal()).hasToString("140.00");
		verify(orcamentoGateway, never()).salvar(any());
		// Responde de novo: o OS só reenvia quando não recebeu a resposta
		verify(publicador).publicar(any());
	}

	@Test
	void mesmaSagaVindaDeOutraOsEPedidoInconsistente() {
		Orcamento deOutraOs = Orcamento.existente(5L, new Orcamento.Cabecalho(SAGA, 99L, null, 7L, null),
				List.of(new LinhaServico(30L, "X", BigDecimal.TEN)), List.of(), null);
		when(orcamentoGateway.buscarPorSagaId(SAGA)).thenReturn(Optional.of(deOutraOs));

		assertThatThrownBy(() -> gerar.executar(pedido(List.of(30L), List.of())))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O sagaId " + SAGA + " já gerou orçamento para outra ordem de serviço");
	}

	@Test
	void servicoOuItemForaDoCatalogoNaoGeraOrcamento() {
		assertThatThrownBy(() -> gerar.executar(pedido(List.of(99L), List.of())))
				.hasMessage("Serviço 99 não existe no catálogo");
		assertThatThrownBy(() -> gerar.executar(pedido(List.of(), List.of(new ItemSolicitado(98L, 1)))))
				.hasMessage("Item 98 não existe no catálogo");
		verify(orcamentoGateway, never()).salvar(any());
	}

	@Test
	void servicoOuItemInativoNaoEntraEmOrcamentoNovo() {
		assertThatThrownBy(() -> gerar.executar(pedido(List.of(31L), List.of())))
				.hasMessage("Serviço 31 está inativo e não entra em orçamento");
		assertThatThrownBy(() -> gerar.executar(pedido(List.of(), List.of(new ItemSolicitado(21L, 1)))))
				.hasMessage("Item 21 está inativo e não entra em orçamento");
	}

	@Test
	void idNuloNaListaViraRegraDeNegocio() {
		assertThatThrownBy(() -> gerar.executar(pedido(Arrays.asList(30L, null), List.of())))
				.hasMessage("Toda linha do orçamento precisa do id do catálogo");
		assertThatThrownBy(() -> gerar.executar(pedido(List.of(), Arrays.asList(new ItemSolicitado(20L, 1), null))))
				.hasMessage("Toda linha do orçamento precisa do id do catálogo");
	}

	@Test
	void pedidoSemSagaNaoConsultaNada() {
		SolicitacaoOrcamento semSaga = new SolicitacaoOrcamento(null, 42L, null, 7L, null, List.of(30L), null);

		assertThatThrownBy(() -> gerar.executar(semSaga)).hasMessage("O pedido de orçamento precisa do sagaId");
		verify(orcamentoGateway, never()).buscarPorSagaId(any());
	}

	@Test
	void pedidoSemListasViraOrcamentoVazioERecusado() {
		SolicitacaoOrcamento vazio = new SolicitacaoOrcamento(SAGA, 42L, null, 7L, null, null, null);

		assertThatThrownBy(() -> gerar.executar(vazio)).hasMessage("O orçamento precisa de ao menos um serviço ou item");
	}

	@Test
	void gravaODominioComAsLinhasNaOrdemDoPedido() {
		gerar.executar(pedido(List.of(30L), List.of(new ItemSolicitado(20L, 1))));

		ArgumentCaptor<Orcamento> gravado = ArgumentCaptor.forClass(Orcamento.class);
		verify(orcamentoGateway).salvar(gravado.capture());
		assertThat(gravado.getValue().getCabecalho().codigoOrdemServico()).isEqualTo("OS-2026-000042");
		assertThat(gravado.getValue().getItens()).extracting(i -> i.itemId()).containsExactly(20L);
	}
}
