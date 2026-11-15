package br.com.oficina.billing.mensageria.output.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OutboxEventoRepository extends JpaRepository<OutboxEvento, Long> {

	/**
	 * Pendentes em ordem de criação, travados para esta transação. O {@code SKIP LOCKED} deixa várias
	 * réplicas enviarem ao mesmo tempo sem mandar o mesmo evento duas vezes.
	 */
	@Query(value = """
			SELECT * FROM outbox_evento
			WHERE publicado_em IS NULL
			ORDER BY id
			LIMIT :lote
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEvento> travarPendentes(@Param("lote") int lote);

	Optional<OutboxEvento> findByMessageId(UUID messageId);

	@Modifying
	@Query("DELETE FROM OutboxEvento e WHERE e.publicadoEm < :limite")
	int removerEnviadosAntesDe(@Param("limite") LocalDateTime limite);
}
