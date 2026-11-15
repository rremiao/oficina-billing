package br.com.oficina.billing.orcamento.entrypoint.controller;

import br.com.oficina.billing.orcamento.application.dto.OrcamentoResponse;
import br.com.oficina.billing.orcamento.application.usecase.ConsultarOrcamentos;
import br.com.oficina.billing.security.SolicitanteAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Só consulta. O orçamento nasce do pedido do OS Service pela Saga (SolicitarOrcamento), não por
 * esta API: expor a criação por HTTP abriria uma porta que o contrato entre os serviços proíbe.
 */
@RestController
@RequestMapping("/orcamentos")
@Tag(name = "Orçamentos", description = "Orçamentos gerados para as ordens de serviço")
public class OrcamentoController {

	private final ConsultarOrcamentos consultarOrcamentos;

	public OrcamentoController(ConsultarOrcamentos consultarOrcamentos) {
		this.consultarOrcamentos = consultarOrcamentos;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar orçamento por id",
			description = "Funcionário vê qualquer orçamento; cliente só os próprios (outro cliente recebe 404).")
	public OrcamentoResponse buscarPorId(@PathVariable Long id, Authentication autenticacao) {
		return consultarOrcamentos.buscarPorId(id, SolicitanteAutenticado.de(autenticacao));
	}

	@GetMapping("/ordem-servico/{ordemServicoId}")
	@Operation(summary = "Listar orçamentos de uma ordem de serviço",
			description = "Do mais recente para o mais antigo, um por ciclo da Saga. Cliente vê só os próprios.")
	public List<OrcamentoResponse> listarPorOrdemServico(@PathVariable Long ordemServicoId,
			Authentication autenticacao) {
		return consultarOrcamentos.listarPorOrdemServico(ordemServicoId, SolicitanteAutenticado.de(autenticacao));
	}
}
