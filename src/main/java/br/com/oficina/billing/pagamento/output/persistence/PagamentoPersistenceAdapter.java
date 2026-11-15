package br.com.oficina.billing.pagamento.output.persistence;

import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class PagamentoPersistenceAdapter implements PagamentoGateway {

	private final PagamentoJpaRepository repository;

	PagamentoPersistenceAdapter(PagamentoJpaRepository repository) {
		this.repository = repository;
	}

	/**
	 * Atualização carrega a entidade na transação atual: a {@code versao} lida é a que o banco confere
	 * no {@code UPDATE}, e quem gravou antes faz a outra transação falhar.
	 */
	@Override
	public Pagamento salvar(Pagamento pagamento) {
		PagamentoEntity entidade = pagamento.getId() == null
				? novaEntidade(pagamento)
				: repository.findById(pagamento.getId())
						.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.PAYMENT_NOT_FOUND));

		entidade.setStatus(pagamento.getStatus());
		entidade.setMetodo(pagamento.getMetodo());
		entidade.setMotivo(pagamento.getMotivo());
		entidade.setIdEstorno(pagamento.getIdEstorno());
		entidade.setValorEstornado(pagamento.getValorEstornado());
		entidade.setRespostaProvedor(pagamento.getUltimaRespostaProvedor());

		return paraDominio(repository.saveAndFlush(entidade));
	}

	@Override
	public Optional<Pagamento> buscarPorId(Long id) {
		return repository.findById(id).map(PagamentoPersistenceAdapter::paraDominio);
	}

	@Override
	public Optional<Pagamento> buscarPorOrcamentoId(Long orcamentoId) {
		return repository.findByOrcamentoId(orcamentoId).map(PagamentoPersistenceAdapter::paraDominio);
	}

	@Override
	public Optional<Pagamento> buscarPorIdCobranca(String idCobranca) {
		return repository.findByIdCobranca(idCobranca).map(PagamentoPersistenceAdapter::paraDominio);
	}

	private static PagamentoEntity novaEntidade(Pagamento pagamento) {
		PagamentoEntity entidade = new PagamentoEntity();
		entidade.setOrcamentoId(pagamento.getOrcamentoId());
		entidade.setClienteId(pagamento.getClienteId());
		entidade.setSagaId(pagamento.getOrigem().sagaId());
		entidade.setOrdemServicoId(pagamento.getOrigem().ordemServicoId());
		entidade.setCodigoOrdemServico(pagamento.getOrigem().codigoOrdemServico());
		entidade.setCorrelationId(pagamento.getOrigem().correlationId());
		entidade.setValor(pagamento.getValor());
		entidade.setIdCobranca(pagamento.getIdCobranca());
		entidade.setLinkPagamento(pagamento.getLinkPagamento());
		return entidade;
	}

	private static Pagamento paraDominio(PagamentoEntity entidade) {
		Pagamento.Origem origem = new Pagamento.Origem(entidade.getSagaId(), entidade.getOrdemServicoId(),
				entidade.getCodigoOrdemServico(), entidade.getCorrelationId());
		return Pagamento.existente(new Pagamento.Dados(entidade.getId(), entidade.getOrcamentoId(),
				entidade.getClienteId(), origem, entidade.getValor(), entidade.getIdCobranca(),
				entidade.getLinkPagamento(), entidade.getStatus(), entidade.getMetodo(), entidade.getMotivo(),
				entidade.getIdEstorno(), entidade.getValorEstornado(), entidade.getRespostaProvedor(),
				entidade.getDataCriacao(), entidade.getDataAtualizacao()));
	}
}
