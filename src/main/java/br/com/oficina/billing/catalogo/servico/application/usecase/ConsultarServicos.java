package br.com.oficina.billing.catalogo.servico.application.usecase;

import br.com.oficina.billing.catalogo.servico.application.dto.ServicoResponse;
import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultarServicos {

	private final ServicoGateway servicoGateway;

	public ConsultarServicos(ServicoGateway servicoGateway) {
		this.servicoGateway = servicoGateway;
	}

	public ServicoResponse buscarPorId(Long id) {
		return servicoGateway.buscarPorId(id)
				.map(ServicoResponse::de)
				.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.SERVICE_NOT_FOUND));
	}

	public List<ServicoResponse> listar() {
		return servicoGateway.listar().stream().map(ServicoResponse::de).toList();
	}
}
