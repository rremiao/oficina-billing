package br.com.oficina.billing.pagamento.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.mensageria.application.EventoSaga;
import br.com.oficina.billing.mensageria.application.PublicadorEventos;
import br.com.oficina.billing.mensageria.application.TipoEvento;
import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoCobranca;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoEstorno;
import br.com.oficina.billing.pagamento.application.evento.NotificadorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.OrcamentoParaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.OrcamentoParaCobranca.OrcamentoACobrar;
import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.CobrancaCriada;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.EstornoRealizado;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.ItemCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.NovaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.Situacao;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.SituacaoCobranca;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.pagamento.domain.StatusPagamento;
import br.com.oficina.billing.shared.application.Solicitante;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PagamentoCasosDeUsoTest {

	private static final UUID SAGA = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");
	private static final Pagamento.Origem ORIGEM = new Pagamento.Origem(SAGA, 42L, "OS-2026-000042", "corr-1");

	private final PagamentoGateway pagamentoGateway = mock(PagamentoGateway.class);
	private final OrcamentoParaCobranca orcamentos = mock(OrcamentoParaCobranca.class);
	private final ProvedorPagamento provedor = mock(ProvedorPagamento.class);
	private final PublicadorEventos publicador = mock(PublicadorEventos.class);
	private final NotificadorPagamento notificador = new NotificadorPagamento(publicador);

	private EventoSaga eventoPublicado() {
		ArgumentCaptor<EventoSaga> evento = ArgumentCaptor.forClass(EventoSaga.class);
		verify(publicador).publicar(evento.capture());
		return evento.getValue();
	}

	/** Como o banco: pagamento novo ganha id ao ser gravado. */
	@BeforeEach
	void gravacaoAtribuiId() {
		when(pagamentoGateway.salvar(any())).thenAnswer(i -> {
			Pagamento p = i.getArgument(0);
			return p.getId() != null ? p : Pagamento.existente(new Pagamento.Dados(99L, p.getOrcamentoId(),
					p.getClienteId(), p.getOrigem(), p.getValor(), p.getIdCobranca(), p.getLinkPagamento(),
					p.getStatus(), p.getMetodo(), p.getMotivo(), p.getIdEstorno(), p.getValorEstornado(),
					p.getUltimaRespostaProvedor(), null, null));
		});
	}

	private static Pagamento comStatus(StatusPagamento status) {
		return Pagamento.existente(new Pagamento.Dados(5L, 10L, 7L, ORIGEM, new BigDecimal("329.80"), "ORD-1",
				"https://mp/checkout", status, null, null, null, null, null, null, null));
	}

	@Nested
	class Cobrar {

		private final CobrarPagamento cobrar = new CobrarPagamento(pagamentoGateway, orcamentos, provedor, notificador);

		@BeforeEach
		void orcamentoAprovado() {
			when(orcamentos.buscar(10L)).thenReturn(Optional.of(new OrcamentoACobrar(10L, SAGA, 42L, "OS-2026-000042", 7L,
					new BigDecimal("329.80"), List.of(new ItemCobranca("SERVICO-30", "Troca de pastilhas", 1,
							new BigDecimal("150.00"))))));
			when(pagamentoGateway.buscarPorOrcamentoId(10L)).thenReturn(Optional.empty());
		}

		@Test
		void criaACobrancaComChaveFixaPorOrcamentoEGuardaOLink() {
			when(provedor.criarCobranca(any())).thenReturn(
					new CobrancaCriada("ORD-1", "https://mp/checkout", Situacao.PENDENTE, "{}"));

			PagamentoResponse resposta = cobrar.executar(new SolicitacaoCobranca(SAGA, 10L, "corr-1"));

			ArgumentCaptor<NovaCobranca> pedido = ArgumentCaptor.forClass(NovaCobranca.class);
			verify(provedor).criarCobranca(pedido.capture());
			assertThat(pedido.getValue().chaveIdempotencia()).isEqualTo("cobranca-orcamento-10");
			assertThat(pedido.getValue().referenciaExterna()).isEqualTo("orcamento-10");
			assertThat(pedido.getValue().descricao()).isEqualTo("Orçamento da OS-2026-000042");
			assertThat(pedido.getValue().valor()).hasToString("329.80");
			assertThat(resposta.status()).isEqualTo(StatusPagamento.AGUARDANDO_PAGAMENTO);
			assertThat(resposta.linkPagamento()).isEqualTo("https://mp/checkout");
			assertThat(resposta.ordemServicoId()).isEqualTo(42L);
			// Sem resultado ainda: o evento sai quando o Mercado Pago avisar
			verify(publicador, never()).publicar(any());
		}

		@Test
		void comandoRepetidoDevolveOPagamentoJaCriadoSemCobrarDeNovo() {
			when(pagamentoGateway.buscarPorOrcamentoId(10L)).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));

			PagamentoResponse resposta = cobrar.executar(new SolicitacaoCobranca(SAGA, 10L, "corr-1"));

			assertThat(resposta.id()).isEqualTo(5L);
			verify(provedor, never()).criarCobranca(any());
			EventoSaga evento = eventoPublicado();
			assertThat(evento.tipo()).isEqualTo(TipoEvento.PAGAMENTO_CONFIRMADO);
			assertThat(evento.chave()).isEqualTo("5");
			assertThat(evento.correlationId()).isEqualTo("corr-1");
		}

		@Test
		void cobrancaQueJaVoltaPagaOuRecusadaEntraComOStatusFinal() {
			when(provedor.criarCobranca(any())).thenReturn(new CobrancaCriada("ORD-1", null, Situacao.APROVADA, "{}"));
			assertThat(cobrar.executar(new SolicitacaoCobranca(SAGA, 10L, null)).status())
					.isEqualTo(StatusPagamento.CONFIRMADO);

			when(provedor.criarCobranca(any())).thenReturn(new CobrancaCriada("ORD-2", null, Situacao.RECUSADA, "{}"));
			assertThat(cobrar.executar(new SolicitacaoCobranca(SAGA, 10L, null)).status())
					.isEqualTo(StatusPagamento.RECUSADO);
		}

		@Test
		void semCodigoDaOsADescricaoUsaOId() {
			when(orcamentos.buscar(11L)).thenReturn(Optional.of(new OrcamentoACobrar(11L, SAGA, 43L, null, 7L,
					BigDecimal.TEN, List.of())));
			when(pagamentoGateway.buscarPorOrcamentoId(11L)).thenReturn(Optional.empty());
			when(provedor.criarCobranca(any())).thenReturn(new CobrancaCriada("ORD-3", null, Situacao.PENDENTE, "{}"));

			cobrar.executar(new SolicitacaoCobranca(SAGA, 11L, null));

			ArgumentCaptor<NovaCobranca> pedido = ArgumentCaptor.forClass(NovaCobranca.class);
			verify(provedor).criarCobranca(pedido.capture());
			assertThat(pedido.getValue().descricao()).isEqualTo("Orçamento da OS 43");
		}

		@Test
		void orcamentoInexistenteOuDeOutraSagaNaoCobra() {
			when(orcamentos.buscar(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> cobrar.executar(new SolicitacaoCobranca(SAGA, 99L, null)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Orçamento 99 não existe para ser cobrado");
			assertThatThrownBy(() -> cobrar.executar(new SolicitacaoCobranca(UUID.randomUUID(), 10L, null)))
					.hasMessage("O orçamento 10 é de outro ciclo da Saga");
			verify(provedor, never()).criarCobranca(any());
		}
	}

	@Nested
	class AtualizarSituacao {

		private final AtualizarSituacaoPagamento atualizar = new AtualizarSituacaoPagamento(pagamentoGateway, provedor, notificador);

		@Test
		void cobrancaAprovadaNoProvedorConfirmaOPagamento() {
			when(pagamentoGateway.buscarPorIdCobranca("ORD-1")).thenReturn(Optional.of(comStatus(StatusPagamento.AGUARDANDO_PAGAMENTO)));
			when(provedor.consultar("ORD-1")).thenReturn(new SituacaoCobranca(Situacao.APROVADA, "pix", "accredited", "{}"));

			Optional<PagamentoResponse> resposta = atualizar.executar("ORD-1");

			assertThat(resposta).get().satisfies(p -> {
				assertThat(p.status()).isEqualTo(StatusPagamento.CONFIRMADO);
				assertThat(p.metodo()).isEqualTo("pix");
			});
			verify(pagamentoGateway).salvar(any());
			assertThat(eventoPublicado().dados()).isEqualTo(new NotificadorPagamento.PagamentoConfirmadoDados(
					5L, 10L, new BigDecimal("329.80"), "pix"));
		}

		@Test
		void cobrancaRecusadaNoProvedorRecusaOPagamentoComODetalhe() {
			when(pagamentoGateway.buscarPorIdCobranca("ORD-1")).thenReturn(Optional.of(comStatus(StatusPagamento.AGUARDANDO_PAGAMENTO)));
			when(provedor.consultar("ORD-1")).thenReturn(new SituacaoCobranca(Situacao.RECUSADA, null, "expired", "{}"));

			assertThat(atualizar.executar("ORD-1")).get()
					.satisfies(p -> assertThat(p.motivo()).isEqualTo("expired"));
			assertThat(eventoPublicado().dados())
					.isEqualTo(new NotificadorPagamento.PagamentoRecusadoDados(10L, "expired"));
		}

		@Test
		void avisoRepetidoOuAindaPendenteNaoGravaNada() {
			when(pagamentoGateway.buscarPorIdCobranca("ORD-1")).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));
			when(provedor.consultar("ORD-1")).thenReturn(new SituacaoCobranca(Situacao.APROVADA, "pix", null, "{}"));
			atualizar.executar("ORD-1");

			when(pagamentoGateway.buscarPorIdCobranca("ORD-2")).thenReturn(Optional.of(comStatus(StatusPagamento.AGUARDANDO_PAGAMENTO)));
			when(provedor.consultar("ORD-2")).thenReturn(new SituacaoCobranca(Situacao.PENDENTE, null, null, "{}"));
			atualizar.executar("ORD-2");

			when(pagamentoGateway.buscarPorIdCobranca("ORD-3")).thenReturn(Optional.of(comStatus(StatusPagamento.ESTORNADO)));
			when(provedor.consultar("ORD-3")).thenReturn(new SituacaoCobranca(Situacao.ESTORNADA, null, null, "{}"));
			atualizar.executar("ORD-3");

			verify(pagamentoGateway, never()).salvar(any());
			verify(publicador, never()).publicar(any());
		}

		@Test
		void cobrancaDesconhecidaNaoConsultaOProvedor() {
			when(pagamentoGateway.buscarPorIdCobranca(anyString())).thenReturn(Optional.empty());

			assertThat(atualizar.executar("ORD-DE-OUTRA-INTEGRACAO")).isEmpty();
			verify(provedor, never()).consultar(anyString());
		}
	}

	@Nested
	class Estornar {

		private final EstornarPagamento estornar = new EstornarPagamento(pagamentoGateway, provedor, notificador);

		@Test
		void estornaPagamentoConfirmadoComChaveFixaPorPagamento() {
			when(pagamentoGateway.buscarPorId(5L)).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));
			when(provedor.estornar("ORD-1", "estorno-pagamento-5"))
					.thenReturn(new EstornoRealizado("REF-1", new BigDecimal("329.80"), "{}"));

			PagamentoResponse resposta = estornar.executar(new SolicitacaoEstorno(SAGA, 5L, "Peça danificada"));

			assertThat(resposta.status()).isEqualTo(StatusPagamento.ESTORNADO);
			assertThat(resposta.idEstorno()).isEqualTo("REF-1");
			assertThat(resposta.motivo()).isEqualTo("Peça danificada");
			assertThat(eventoPublicado().dados()).isEqualTo(new NotificadorPagamento.PagamentoEstornadoDados(
					5L, "REF-1", new BigDecimal("329.80")));
		}

		@Test
		void estornoRepetidoNaoChamaOProvedorDeNovo() {
			when(pagamentoGateway.buscarPorId(5L)).thenReturn(Optional.of(comStatus(StatusPagamento.ESTORNADO)));

			assertThat(estornar.executar(new SolicitacaoEstorno(SAGA, 5L, "x")).status()).isEqualTo(StatusPagamento.ESTORNADO);
			verify(provedor, never()).estornar(anyString(), anyString());
			verify(pagamentoGateway, never()).salvar(any());
			assertThat(eventoPublicado().tipo()).isEqualTo(TipoEvento.PAGAMENTO_ESTORNADO);
		}

		@Test
		void pagamentoInexistenteDeOutraSagaOuNaoConfirmadoNaoEstorna() {
			when(pagamentoGateway.buscarPorId(99L)).thenReturn(Optional.empty());
			assertThatThrownBy(() -> estornar.executar(new SolicitacaoEstorno(SAGA, 99L, "x")))
					.hasMessage("Pagamento 99 não existe para ser estornado");

			when(pagamentoGateway.buscarPorId(5L)).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));
			assertThatThrownBy(() -> estornar.executar(new SolicitacaoEstorno(UUID.randomUUID(), 5L, "x")))
					.hasMessage("O pagamento 5 é de outro ciclo da Saga");

			when(pagamentoGateway.buscarPorId(6L)).thenReturn(Optional.of(comStatus(StatusPagamento.AGUARDANDO_PAGAMENTO)));
			assertThatThrownBy(() -> estornar.executar(new SolicitacaoEstorno(SAGA, 6L, "x")))
					.isInstanceOf(RegraNegocioException.class);
			verify(provedor, never()).estornar(anyString(), anyString());
		}
	}

	@Nested
	class Consultar {

		private final ConsultarPagamentos consultar = new ConsultarPagamentos(pagamentoGateway);

		@Test
		void funcionarioEClienteDonoVeemOPagamento() {
			when(pagamentoGateway.buscarPorId(5L)).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));
			when(pagamentoGateway.buscarPorOrcamentoId(10L)).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));

			assertThat(consultar.buscarPorId(5L, Solicitante.funcionario()).id()).isEqualTo(5L);
			assertThat(consultar.buscarPorOrcamento(10L, Solicitante.cliente(7L)).linkPagamento())
					.isEqualTo("https://mp/checkout");
		}

		@Test
		void pagamentoDeOutroClienteOuInexistenteResponde404() {
			when(pagamentoGateway.buscarPorId(5L)).thenReturn(Optional.of(comStatus(StatusPagamento.CONFIRMADO)));
			when(pagamentoGateway.buscarPorOrcamentoId(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> consultar.buscarPorId(5L, Solicitante.cliente(8L)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Pagamento não encontrado");
			assertThatThrownBy(() -> consultar.buscarPorOrcamento(99L, Solicitante.funcionario()))
					.isInstanceOf(RecursoNaoEncontradoException.class);
		}
	}
}
