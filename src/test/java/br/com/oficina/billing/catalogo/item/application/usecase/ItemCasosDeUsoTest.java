package br.com.oficina.billing.catalogo.item.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.billing.catalogo.item.application.dto.ItemRequest;
import br.com.oficina.billing.catalogo.item.application.dto.ItemResponse;
import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

class ItemCasosDeUsoTest {

	private final ItemGateway gateway = mock(ItemGateway.class);

	private static Item gravado(Long id, String nome, TipoItem tipo) {
		return Item.existente(id, new Item.Dados(nome, null, tipo, BigDecimal.TEN, "UN"), true, null, null);
	}

	private static ItemRequest pedido(String nome, TipoItem tipo) {
		return new ItemRequest(nome, null, tipo, new BigDecimal("89.90"), "UN");
	}

	@Nested
	class Cadastrar {

		private final CadastrarItem cadastrar = new CadastrarItem(gateway);

		@Test
		void gravaOItemComONomeNormalizado() {
			when(gateway.salvar(any())).thenAnswer(i -> i.getArgument(0));

			ItemResponse resposta = cadastrar.executar(pedido(" Pastilha ", TipoItem.PECA));

			assertThat(resposta.nome()).isEqualTo("Pastilha");
			verify(gateway).existeDuplicado(TipoItem.PECA, "Pastilha", "UN", null);
		}

		@Test
		void duplicadoNaoGrava() {
			when(gateway.existeDuplicado(TipoItem.PECA, "Pastilha", "UN", null)).thenReturn(true);

			assertThatThrownBy(() -> cadastrar.executar(pedido("Pastilha", TipoItem.PECA)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Já existe item cadastrado com o mesmo tipo, nome e unidade de medida");
			verify(gateway, never()).salvar(any());
		}
	}

	@Nested
	class Atualizar {

		private final AtualizarItem atualizar = new AtualizarItem(gateway);

		@Test
		void atualizaEGravaODominioAlterado() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Pastilha", TipoItem.PECA)));
			when(gateway.salvar(any())).thenAnswer(i -> i.getArgument(0));

			ItemResponse resposta = atualizar.executar(1L, pedido("Pastilha cerâmica", TipoItem.PECA));

			assertThat(resposta.nome()).isEqualTo("Pastilha cerâmica");
			assertThat(resposta.valorUnitario()).hasToString("89.90");
			verify(gateway).existeDuplicado(TipoItem.PECA, "Pastilha cerâmica", "UN", 1L);
		}

		@Test
		void itemInexistenteResponde404() {
			when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> atualizar.executar(9L, pedido("X", TipoItem.PECA)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Item não encontrado");
		}

		@Test
		void duplicadoDeOutroItemNaoGrava() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Pastilha", TipoItem.PECA)));
			when(gateway.existeDuplicado(TipoItem.PECA, "Disco", "UN", 1L)).thenReturn(true);

			assertThatThrownBy(() -> atualizar.executar(1L, pedido("Disco", TipoItem.PECA)))
					.isInstanceOf(RegraNegocioException.class);
			verify(gateway, never()).salvar(any());
		}
	}

	@Nested
	class Inativar {

		private final InativarItem inativar = new InativarItem(gateway);

		@Test
		void gravaOItemInativo() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Pastilha", TipoItem.PECA)));

			inativar.executar(1L);

			ArgumentCaptor<Item> gravadoAgora = ArgumentCaptor.forClass(Item.class);
			verify(gateway).salvar(gravadoAgora.capture());
			assertThat(gravadoAgora.getValue().isAtivo()).isFalse();
		}

		@Test
		void itemInexistenteResponde404() {
			when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> inativar.executar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		}
	}

	@Nested
	class Consultar {

		private final ConsultarItens consultar = new ConsultarItens(gateway);

		@Test
		void buscaPorId() {
			when(gateway.buscarPorId(1L)).thenReturn(Optional.of(gravado(1L, "Pastilha", TipoItem.PECA)));

			assertThat(consultar.buscarPorId(1L).tipo()).isEqualTo(TipoItem.PECA);
		}

		@Test
		void idInexistenteResponde404() {
			when(gateway.buscarPorId(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> consultar.buscarPorId(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
		}

		@Test
		void listaTodosEPorTipo() {
			when(gateway.listar()).thenReturn(List.of(gravado(1L, "Pastilha", TipoItem.PECA),
					gravado(2L, "Óleo", TipoItem.INSUMO)));
			when(gateway.listarPorTipo(TipoItem.INSUMO)).thenReturn(List.of(gravado(2L, "Óleo", TipoItem.INSUMO)));

			assertThat(consultar.listar()).extracting(ItemResponse::id).containsExactly(1L, 2L);
			assertThat(consultar.listarPorTipo(TipoItem.INSUMO)).extracting(ItemResponse::nome).containsExactly("Óleo");
		}
	}
}
