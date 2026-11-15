package br.com.oficina.billing.catalogo.servico.output.persistence;

import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.catalogo.servico.domain.Servico;
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
class ServicoPersistenceAdapter implements ServicoGateway {

	private static final String INDICE_NOME = "ux_servico_nome";

	private final ServicoJpaRepository repository;

	ServicoPersistenceAdapter(ServicoJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Servico salvar(Servico servico) {
		ServicoEntity entidade = servico.getId() == null
				? new ServicoEntity()
				: repository.findById(servico.getId())
						.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.SERVICE_NOT_FOUND));

		entidade.setNome(servico.getNome());
		entidade.setDescricao(servico.getDescricao());
		entidade.setValor(servico.getValor());
		entidade.setAtivo(servico.isAtivo());

		try {
			return paraDominio(repository.saveAndFlush(entidade));
		} catch (DataIntegrityViolationException e) {
			if (ViolacaoDeUnicidade.doIndice(e, INDICE_NOME)) {
				throw new RegraNegocioException(ExceptionMessage.EXISTING_SERVICE);
			}
			throw e;
		}
	}

	@Override
	public Optional<Servico> buscarPorId(Long id) {
		return repository.findById(id).map(ServicoPersistenceAdapter::paraDominio);
	}

	@Override
	public List<Servico> buscarPorIds(Collection<Long> ids) {
		return repository.findAllById(ids).stream().map(ServicoPersistenceAdapter::paraDominio).toList();
	}

	@Override
	public List<Servico> listar() {
		return repository.findAll(Sort.by("id")).stream().map(ServicoPersistenceAdapter::paraDominio).toList();
	}

	@Override
	public boolean existeComNome(String nome, Long idIgnorado) {
		return idIgnorado == null
				? repository.existsByNomeIgnoreCase(nome)
				: repository.existsByNomeIgnoreCaseAndIdNot(nome, idIgnorado);
	}

	static Servico paraDominio(ServicoEntity entidade) {
		return Servico.existente(entidade.getId(), entidade.getNome(), entidade.getDescricao(), entidade.getValor(),
				entidade.isAtivo(), entidade.getDataCriacao(), entidade.getDataAtualizacao());
	}
}
