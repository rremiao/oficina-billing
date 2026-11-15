package br.com.oficina.billing.catalogo.item.application.gateway;

import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Porta de persistência do catálogo de itens. */
public interface ItemGateway {

	/**
	 * Grava e devolve o item com id e datas. Mesmo tipo, nome e unidade de outro item (sem diferenciar
	 * maiúsculas) lança {@code RegraNegocioException}, mesmo quando dois cadastros simultâneos passam
	 * pela checagem.
	 */
	Item salvar(Item item);

	Optional<Item> buscarPorId(Long id);

	/** Os itens encontrados entre os ids pedidos; id inexistente simplesmente não volta. */
	List<Item> buscarPorIds(Collection<Long> ids);

	/** Todos os itens, ativos e inativos, em ordem de id. */
	List<Item> listar();

	List<Item> listarPorTipo(TipoItem tipo);

	/** Se já existe outro item com o mesmo tipo, nome e unidade; {@code idIgnorado} pode ser nulo. */
	boolean existeDuplicado(TipoItem tipo, String nome, String unidadeMedida, Long idIgnorado);
}
