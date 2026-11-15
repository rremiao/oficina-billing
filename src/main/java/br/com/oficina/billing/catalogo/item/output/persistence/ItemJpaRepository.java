package br.com.oficina.billing.catalogo.item.output.persistence;

import br.com.oficina.billing.catalogo.item.domain.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface ItemJpaRepository extends JpaRepository<ItemEntity, Long> {

	List<ItemEntity> findByTipoOrderByIdAsc(TipoItem tipo);

	/** Mesma regra do índice único {@code ux_item_tipo_nome_unidade}; {@code idIgnorado} pode ser nulo. */
	@Query("""
			select count(i) > 0
			from ItemEntity i
			where i.tipo = :tipo
				and lower(i.nome) = lower(:nome)
				and lower(i.unidadeMedida) = lower(:unidadeMedida)
				and (:idIgnorado is null or i.id <> :idIgnorado)
			""")
	boolean existeDuplicado(TipoItem tipo, String nome, String unidadeMedida, Long idIgnorado);
}
