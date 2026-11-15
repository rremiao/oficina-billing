package br.com.oficina.billing.orcamento.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrcamentoJpaRepository extends JpaRepository<OrcamentoEntity, Long> {

	Optional<OrcamentoEntity> findBySagaId(UUID sagaId);

	List<OrcamentoEntity> findByOrdemServicoIdOrderByIdDesc(Long ordemServicoId);
}
