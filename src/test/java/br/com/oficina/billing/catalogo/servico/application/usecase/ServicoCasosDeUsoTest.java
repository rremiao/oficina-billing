package br.com.oficina.billing.catalogo.servico.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.catalogo.servico.application.dto.ServicoRequest;
import br.com.oficina.billing.catalogo.servico.application.dto.ServicoResponse;
import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.catalogo.servico.domain.Servico;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

class ServicoCasosDeUsoTest {

	private final ServicoGateway gateway = mock(ServicoGateway.class);

	private static Servico gravado(Long id, String nome, String valor, boolean ativo) {
		return Servico.existente(id, nome, null, new BigDecimal(valor), ativo, null, null);
	}

	@Nested
	class Cadastrar {

		private final CadastrarServico cadastrar = new CadastrarServico(gateway);

		@Test
		void gravaOServicoComONomeNormalizado() {
			when(gateway.salvar(any())).thenAnswer(i -> gravado(1L, ((Servico) i.getArgument(0)).getNome(), "150", true));

			ServicoResponse resposta = cadastrar.executar(new ServicoRequest(" Alinhamento ", null, new BigDecimal("150")));

			assertThat(resposta.id()).isEqualTo(1L);
			assertThat(resposta.nome()).isEqualTo("Alinhamento");
			verify(gateway).existeComNome("Alinhamento", null);
		}

		@Test
		void nomeRepetidoNaoGrava() {
			when(gateway.existeComNome("Alinhamento", null)).thenReturn(true);

			assertThatThrownBy(() -> cadastrar.executar(new ServicoRequest("Alinhamento", null, BigDecimal.TEN)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Já existe serviço cadastrado com este nome");
			verify(gateway, never()).salvar(any());
		}
	}

	@Nested
	class Atualizar {

		private final AtualizarServico atualizar = new AtualizarServico(gateway);

		@Test
		void atualizaEGravaODominioAlterado() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Alinhamento", "100", true)));
			when(gateway.salvar(any())).thenAnswer(i -> i.getArgument(0));

			ServicoResponse resposta = atualizar.executar(1L, new ServicoRequest("Alinhamento", "3D", new BigDecimal("120")));

			assertThat(resposta.valor()).hasToString("120.00");
			assertThat(resposta.descricao()).isEqualTo("3D");
			verify(gateway).existeComNome("Alinhamento", 1L);
		}

		@Test
		void servicoInexistenteResponde404() {
			when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> atualizar.executar(9L, new ServicoRequest("X", null, BigDecimal.TEN)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Serviço não encontrado");
		}

		@Test
		void nomeDeOutroServicoNaoGrava() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Alinhamento", "100", true)));
			when(gateway.existeComNome(anyString(), any())).thenReturn(true);

			assertThatThrownBy(() -> atualizar.executar(1L, new ServicoRequest("Balanceamento", null, BigDecimal.TEN)))
					.isInstanceOf(RegraNegocioException.class);
			verify(gateway, never()).salvar(any());
		}
	}

	@Nested
	class Inativar {

		private final InativarServico inativar = new InativarServico(gateway);

		@Test
		void gravaOServicoInativo() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Alinhamento", "100", true)));

			inativar.executar(1L);

			ArgumentCaptor<Servico> gravadoAgora = ArgumentCaptor.forClass(Servico.class);
			verify(gateway).salvar(gravadoAgora.capture());
			assertThat(gravadoAgora.getValue().isAtivo()).isFalse();
		}

		@Test
		void servicoInexistenteResponde404() {
			when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> inativar.executar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		}
	}

	@Nested
	class Consultar {

		private final ConsultarServicos consultar = new ConsultarServicos(gateway);

		@Test
		void buscaPorId() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Alinhamento", "100", true)));

			assertThat(consultar.buscarPorId(1L).nome()).isEqualTo("Alinhamento");
		}

		@Test
		void idInexistenteResponde404() {
			when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> consultar.buscarPorId(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		}

		@Test
		void listaTodos() {
			when(gateway.listar()).thenReturn(List.of(gravado(1L, "A", "10", true), gravado(2L, "B", "20", false)));

			assertThat(consultar.listar()).extracting(ServicoResponse::id).containsExactly(1L, 2L);
		}
	}
}
