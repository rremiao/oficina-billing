package br.com.oficina.billing.pagamento.application.usecase;

import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.dto.SolicitacaoEstorno;
import br.com.oficina.billing.pagamento.application.evento.NotificadorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.EstornoRealizado;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensação da Saga: devolve o valor pago. Idempotente: pagamento já estornado só devolve o
 * resultado, e a chave de idempotência fixa por pagamento impede o provedor de estornar duas vezes.
 * Nos dois casos responde {@code PagamentoEstornado} (o mesmo {@code messageId} na repetição).
 */
@Service
public class EstornarPagamento {

	private final PagamentoGateway pagamentoGateway;
	private final ProvedorPagamento provedor;
	private final NotificadorPagamento notificador;

	public EstornarPagamento(PagamentoGateway pagamentoGateway, ProvedorPagamento provedor,
			NotificadorPagamento notificador) {
		this.pagamentoGateway = pagamentoGateway;
		this.provedor = provedor;
		this.notificador = notificador;
	}

	@Transactional
	public PagamentoResponse executar(SolicitacaoEstorno solicitacao) {
		Pagamento pagamento = pagamentoGateway.buscarPorId(solicitacao.pagamentoId())
				.orElseThrow(() -> new RegraNegocioException(
						ExceptionMessage.PAYMENT_TO_REFUND_NOT_FOUND.formatted(solicitacao.pagamentoId())));

		if (!pagamento.getOrigem().sagaId().equals(solicitacao.sagaId())) {
			throw new RegraNegocioException(ExceptionMessage.PAYMENT_OF_OTHER_SAGA.formatted(pagamento.getId()));
		}

		if (!pagamento.precisaEstornar()) {
			notificador.notificarResultado(pagamento);
			return PagamentoResponse.de(pagamento);
		}

		EstornoRealizado estorno = provedor.estornar(pagamento.getIdCobranca(), "estorno-pagamento-" + pagamento.getId());
		pagamento.registrarEstorno(estorno.id(), estorno.valor(), solicitacao.motivo(), estorno.resposta());

		Pagamento gravado = pagamentoGateway.salvar(pagamento);
		notificador.notificarResultado(gravado);
		return PagamentoResponse.de(gravado);
	}
}
