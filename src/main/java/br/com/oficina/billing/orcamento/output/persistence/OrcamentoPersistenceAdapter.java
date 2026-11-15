package br.com.oficina.billing.orcamento.output.persistence;

import br.com.oficina.billing.orcamento.application.gateway.OrcamentoGateway;
import br.com.oficina.billing.orcamento.domain.LinhaItem;
import br.com.oficina.billing.orcamento.domain.LinhaServico;
import br.com.oficina.billing.orcamento.domain.Orcamento;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class OrcamentoPersistenceAdapter implements OrcamentoGateway {

	private final OrcamentoJpaRepository repository;

	OrcamentoPersistenceAdapter(OrcamentoJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Orcamento salvar(Orcamento orcamento) {
		return paraDominio(repository.saveAndFlush(paraEntidade(orcamento)));
	}

	@Override
	public Optional<Orcamento> buscarPorId(Long id) {
		return repository.findById(id).map(OrcamentoPersistenceAdapter::paraDominio);
	}

	@Override
	public Optional<Orcamento> buscarPorSagaId(UUID sagaId) {
		return repository.findBySagaId(sagaId).map(OrcamentoPersistenceAdapter::paraDominio);
	}

	@Override
	public List<Orcamento> listarPorOrdemServico(Long ordemServicoId) {
		return repository.findByOrdemServicoIdOrderByIdDesc(ordemServicoId).stream()
				.map(OrcamentoPersistenceAdapter::paraDominio)
				.toList();
	}

	private static OrcamentoEntity paraEntidade(Orcamento orcamento) {
		Orcamento.Cabecalho cabecalho = orcamento.getCabecalho();
		OrcamentoEntity entidade = new OrcamentoEntity();
		entidade.setSagaId(cabecalho.sagaId());
		entidade.setOrdemServicoId(cabecalho.ordemServicoId());
		entidade.setCodigoOrdemServico(cabecalho.codigoOrdemServico());
		entidade.setClienteId(cabecalho.clienteId());
		entidade.setVeiculoId(cabecalho.veiculoId());
		entidade.setValorServicos(orcamento.getValorServicos());
		entidade.setValorItens(orcamento.getValorItens());
		entidade.setValorTotal(orcamento.getValorTotal());
		orcamento.getServicos().forEach(linha -> entidade.getServicos().add(paraEntidade(linha)));
		orcamento.getItens().forEach(linha -> entidade.getItens().add(paraEntidade(linha)));
		return entidade;
	}

	private static OrcamentoServicoEntity paraEntidade(LinhaServico linha) {
		OrcamentoServicoEntity entidade = new OrcamentoServicoEntity();
		entidade.setServicoId(linha.servicoId());
		entidade.setNome(linha.nome());
		entidade.setValorUnitario(linha.valorUnitario());
		return entidade;
	}

	private static OrcamentoItemEntity paraEntidade(LinhaItem linha) {
		OrcamentoItemEntity entidade = new OrcamentoItemEntity();
		entidade.setItemId(linha.itemId());
		entidade.setNome(linha.nome());
		entidade.setQuantidade(linha.quantidade());
		entidade.setValorUnitario(linha.valorUnitario());
		entidade.setValorTotal(linha.valorTotal());
		return entidade;
	}

	/** Os totais são recalculados pelas linhas no domínio; os gravados existem para consulta direta no banco. */
	private static Orcamento paraDominio(OrcamentoEntity entidade) {
		Orcamento.Cabecalho cabecalho = new Orcamento.Cabecalho(entidade.getSagaId(), entidade.getOrdemServicoId(),
				entidade.getCodigoOrdemServico(), entidade.getClienteId(), entidade.getVeiculoId());
		List<LinhaServico> servicos = entidade.getServicos().stream()
				.map(linha -> new LinhaServico(linha.getServicoId(), linha.getNome(), linha.getValorUnitario()))
				.toList();
		List<LinhaItem> itens = entidade.getItens().stream()
				.map(linha -> new LinhaItem(linha.getItemId(), linha.getNome(), linha.getQuantidade(),
						linha.getValorUnitario()))
				.toList();
		return Orcamento.existente(entidade.getId(), cabecalho, servicos, itens, entidade.getDataCriacao());
	}
}
