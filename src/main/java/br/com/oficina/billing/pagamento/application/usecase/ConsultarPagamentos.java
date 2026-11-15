package br.com.oficina.billing.pagamento.application.usecase;

import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import br.com.oficina.billing.shared.application.Solicitante;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consulta de pagamentos. É por aqui que o cliente pega o link do checkout depois de aprovar o
 * orçamento. Funcionário vê qualquer um; cliente só os próprios (o de outro cliente responde 404).
 */
@Service
@Transactional(readOnly = true)
public class ConsultarPagamentos {

	private final PagamentoGateway pagamentoGateway;

	public ConsultarPagamentos(PagamentoGateway pagamentoGateway) {
		this.pagamentoGateway = pagamentoGateway;
	}

	public PagamentoResponse buscarPorId(Long id, Solicitante solicitante) {
		return visivel(pagamentoGateway.buscarPorId(id), solicitante);
	}

	public PagamentoResponse buscarPorOrcamento(Long orcamentoId, Solicitante solicitante) {
		return visivel(pagamentoGateway.buscarPorOrcamentoId(orcamentoId), solicitante);
	}

	private static PagamentoResponse visivel(Optional<Pagamento> pagamento, Solicitante solicitante) {
		return pagamento
				.filter(p -> !solicitante.ehCliente() || p.pertenceAoCliente(solicitante.clienteId()))
				.map(PagamentoResponse::de)
				.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.PAYMENT_NOT_FOUND));
	}
}
