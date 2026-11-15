package br.com.oficina.billing.orcamento.application.usecase;

import br.com.oficina.billing.orcamento.application.dto.OrcamentoResponse;
import br.com.oficina.billing.orcamento.application.gateway.OrcamentoGateway;
import br.com.oficina.billing.orcamento.domain.Orcamento;
import br.com.oficina.billing.shared.application.Solicitante;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consulta de orçamentos. Funcionário vê qualquer um; cliente só os próprios, e o orçamento de outro
 * cliente responde 404, como se não existisse.
 */
@Service
@Transactional(readOnly = true)
public class ConsultarOrcamentos {

	private final OrcamentoGateway orcamentoGateway;

	public ConsultarOrcamentos(OrcamentoGateway orcamentoGateway) {
		this.orcamentoGateway = orcamentoGateway;
	}

	public OrcamentoResponse buscarPorId(Long id, Solicitante solicitante) {
		return orcamentoGateway.buscarPorId(id)
				.filter(orcamento -> podeVer(orcamento, solicitante))
				.map(OrcamentoResponse::de)
				.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.QUOTE_NOT_FOUND));
	}

	/** Do mais recente para o mais antigo. Para o cliente, só os orçamentos dele. */
	public List<OrcamentoResponse> listarPorOrdemServico(Long ordemServicoId, Solicitante solicitante) {
		return orcamentoGateway.listarPorOrdemServico(ordemServicoId).stream()
				.filter(orcamento -> podeVer(orcamento, solicitante))
				.map(OrcamentoResponse::de)
				.toList();
	}

	private static boolean podeVer(Orcamento orcamento, Solicitante solicitante) {
		return !solicitante.ehCliente() || orcamento.pertenceAoCliente(solicitante.clienteId());
	}
}
