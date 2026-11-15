package br.com.oficina.billing.pagamento.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PagamentoJpaRepository extends JpaRepository<PagamentoEntity, Long> {

	Optional<PagamentoEntity> findByOrcamentoId(Long orcamentoId);

	Optional<PagamentoEntity> findByIdCobranca(String idCobranca);
}
