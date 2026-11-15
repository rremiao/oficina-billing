package br.com.oficina.billing.catalogo.servico.application.usecase;

import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.catalogo.servico.domain.Servico;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inativar não confere OS ativa, como fazia o monólito: o Billing não conhece o status da OS, e o
 * orçamento já gerado guarda uma cópia do nome e do preço. O efeito é o serviço não entrar em
 * orçamento novo.
 */
@Service
public class InativarServico {

	private final ServicoGateway servicoGateway;

	public InativarServico(ServicoGateway servicoGateway) {
		this.servicoGateway = servicoGateway;
	}

	@Transactional
	public void executar(Long id) {
		Servico servico = servicoGateway.buscarPorId(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.SERVICE_NOT_FOUND));

		servico.inativar();
		servicoGateway.salvar(servico);
	}
}
