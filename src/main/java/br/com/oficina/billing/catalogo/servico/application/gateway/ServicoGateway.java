package br.com.oficina.billing.catalogo.servico.application.gateway;

import br.com.oficina.billing.catalogo.servico.domain.Servico;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Porta de persistência do catálogo de serviços. */
public interface ServicoGateway {

	/**
	 * Grava e devolve o serviço com id e datas. Nome repetido (sem diferenciar maiúsculas) lança
	 * {@code RegraNegocioException}, mesmo quando dois cadastros simultâneos passam pela checagem.
	 */
	Servico salvar(Servico servico);

	Optional<Servico> buscarPorId(Long id);

	/** Os serviços encontrados entre os ids pedidos; id inexistente simplesmente não volta. */
	List<Servico> buscarPorIds(Collection<Long> ids);

	/** Todos os serviços, ativos e inativos, em ordem de id. */
	List<Servico> listar();

	/** Se já existe outro serviço com o nome, sem diferenciar maiúsculas; {@code idIgnorado} pode ser nulo. */
	boolean existeComNome(String nome, Long idIgnorado);
}
