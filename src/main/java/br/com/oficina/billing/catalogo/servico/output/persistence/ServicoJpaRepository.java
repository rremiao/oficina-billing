package br.com.oficina.billing.catalogo.servico.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServicoJpaRepository extends JpaRepository<ServicoEntity, Long> {

	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
