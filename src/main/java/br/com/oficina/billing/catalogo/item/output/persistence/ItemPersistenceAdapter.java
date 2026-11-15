package br.com.oficina.billing.catalogo.item.output.persistence;

import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import br.com.oficina.billing.shared.persistence.ViolacaoDeUnicidade;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
class ItemPersistenceAdapter implements ItemGateway {

	private static final String INDICE_DUPLICIDADE = "ux_item_tipo_nome_unidade";

	private final ItemJpaRepository repository;

	ItemPersistenceAdapter(ItemJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Item salvar(Item item) {
		ItemEntity entidade = item.getId() == null
				? new ItemEntity()
				: repository.findById(item.getId())
						.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.ITEM_NOT_FOUND));

		entidade.setNome(item.getNome());
		entidade.setDescricao(item.getDescricao());
		entidade.setTipo(item.getTipo());
		entidade.setValorUnitario(item.getValorUnitario());
		entidade.setUnidadeMedida(item.getUnidadeMedida());
		entidade.setAtivo(item.isAtivo());

		try {
			return paraDominio(repository.saveAndFlush(entidade));
		} catch (DataIntegrityViolationException e) {
			if (ViolacaoDeUnicidade.doIndice(e, INDICE_DUPLICIDADE)) {
				throw new RegraNegocioException(ExceptionMessage.EXISTING_ITEM);
			}
			throw e;
		}
	}

	@Override
	public Optional<Item> buscarPorId(Long id) {
		return repository.findById(id).map(ItemPersistenceAdapter::paraDominio);
	}

	@Override
	public List<Item> buscarPorIds(Collection<Long> ids) {
		return repository.findAllById(ids).stream().map(ItemPersistenceAdapter::paraDominio).toList();
	}

	@Override
	public List<Item> listar() {
		return repository.findAll(Sort.by("id")).stream().map(ItemPersistenceAdapter::paraDominio).toList();
	}

	@Override
	public List<Item> listarPorTipo(TipoItem tipo) {
		return repository.findByTipoOrderByIdAsc(tipo).stream().map(ItemPersistenceAdapter::paraDominio).toList();
	}

	@Override
	public boolean existeDuplicado(TipoItem tipo, String nome, String unidadeMedida, Long idIgnorado) {
		return repository.existeDuplicado(tipo, nome, unidadeMedida, idIgnorado);
	}

	static Item paraDominio(ItemEntity entidade) {
		Item.Dados dados = new Item.Dados(entidade.getNome(), entidade.getDescricao(), entidade.getTipo(),
				entidade.getValorUnitario(), entidade.getUnidadeMedida());
		return Item.existente(entidade.getId(), dados, entidade.isAtivo(), entidade.getDataCriacao(),
				entidade.getDataAtualizacao());
	}
}
