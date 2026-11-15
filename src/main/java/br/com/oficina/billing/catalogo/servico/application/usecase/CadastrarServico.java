package br.com.oficina.billing.catalogo.servico.application.usecase;

import br.com.oficina.billing.catalogo.servico.application.dto.ServicoRequest;
import br.com.oficina.billing.catalogo.servico.application.dto.ServicoResponse;
import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.catalogo.servico.domain.Servico;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarServico {

	private final ServicoGateway servicoGateway;

	public CadastrarServico(ServicoGateway servicoGateway) {
		this.servicoGateway = servicoGateway;
	}

	@Transactional
	public ServicoResponse executar(ServicoRequest request) {
		Servico servico = Servico.novo(request.nome(), request.descricao(), request.valor());

		if (servicoGateway.existeComNome(servico.getNome(), null)) {
			throw new RegraNegocioException(ExceptionMessage.EXISTING_SERVICE);
		}

		return ServicoResponse.de(servicoGateway.salvar(servico));
	}
}
