package br.com.oficina.billing.pagamento.application.usecase;

import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoCobranca;
import br.com.oficina.billing.pagamento.application.evento.NotificadorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.OrcamentoParaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.OrcamentoParaCobranca.OrcamentoACobrar;
import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.CobrancaCriada;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.NovaCobranca;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.Situacao;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Cria a cobrança do orçamento aprovado no provedor. É idempotente por orçamento: o comando repetido
 * devolve o pagamento já criado, e a chave de idempotência enviada ao provedor é fixa por orçamento,
 * então nem uma falha entre a chamada e a gravação cobra duas vezes. O resultado (confirmado ou
 * recusado) normalmente chega depois, pelo webhook; se o pagamento já tem resultado, ele é respondido
 * de novo.
 */
@Service
public class CobrarPagamento {

	private final PagamentoGateway pagamentoGateway;
	private final OrcamentoParaCobranca orcamentos;
	private final ProvedorPagamento provedor;
	private final NotificadorPagamento notificador;

	public CobrarPagamento(PagamentoGateway pagamentoGateway, OrcamentoParaCobranca orcamentos,
			ProvedorPagamento provedor, NotificadorPagamento notificador) {
		this.pagamentoGateway = pagamentoGateway;
		this.orcamentos = orcamentos;
		this.provedor = provedor;
		this.notificador = notificador;
	}

	@Transactional
	public PagamentoResponse executar(SolicitacaoCobranca solicitacao) {
		OrcamentoACobrar orcamento = orcamentos.buscar(solicitacao.orcamentoId())
				.orElseThrow(() -> new RegraNegocioException(
						ExceptionMessage.QUOTE_TO_CHARGE_NOT_FOUND.formatted(solicitacao.orcamentoId())));

		if (!orcamento.sagaId().equals(solicitacao.sagaId())) {
			throw new RegraNegocioException(ExceptionMessage.QUOTE_OF_OTHER_SAGA.formatted(orcamento.id()));
		}

		Optional<Pagamento> jaCriado = pagamentoGateway.buscarPorOrcamentoId(orcamento.id());
		if (jaCriado.isPresent()) {
			notificador.notificarResultado(jaCriado.get());
			return PagamentoResponse.de(jaCriado.get());
		}

		CobrancaCriada cobranca = provedor.criarCobranca(new NovaCobranca(
				"orcamento-" + orcamento.id(),
				"Orçamento da " + (orcamento.codigoOrdemServico() != null
						? orcamento.codigoOrdemServico() : "OS " + orcamento.ordemServicoId()),
				orcamento.valorTotal(),
				orcamento.itens(),
				"cobranca-orcamento-" + orcamento.id()));

		Pagamento pagamento = Pagamento.aguardando(orcamento.id(), orcamento.clienteId(),
				new Pagamento.Origem(orcamento.sagaId(), orcamento.ordemServicoId(), orcamento.codigoOrdemServico(),
						solicitacao.correlationId()),
				orcamento.valorTotal(), cobranca.id(), cobranca.linkPagamento(), cobranca.resposta());

		// Raro, mas possível: o provedor já responde com a cobrança encerrada
		if (cobranca.situacao() == Situacao.APROVADA) {
			pagamento.confirmar(null, cobranca.resposta());
		} else if (cobranca.situacao() == Situacao.RECUSADA) {
			pagamento.recusar(null, cobranca.resposta());
		}

		Pagamento gravado = pagamentoGateway.salvar(pagamento);
		notificador.notificarResultado(gravado);
		return PagamentoResponse.de(gravado);
	}
}
