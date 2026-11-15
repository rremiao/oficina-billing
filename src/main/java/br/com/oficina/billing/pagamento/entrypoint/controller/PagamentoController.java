package br.com.oficina.billing.pagamento.entrypoint.controller;

import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.usecase.ConsultarPagamentos;
import br.com.oficina.billing.security.SolicitanteAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Só consulta. A cobrança nasce do comando {@code CobrarPagamento} da Saga, e a confirmação, do aviso
 * do Mercado Pago.
 */
@RestController
@RequestMapping("/pagamentos")
@Tag(name = "Pagamentos", description = "Pagamento dos orçamentos aprovados, pelo Mercado Pago")
public class PagamentoController {

	private final ConsultarPagamentos consultarPagamentos;

	public PagamentoController(ConsultarPagamentos consultarPagamentos) {
		this.consultarPagamentos = consultarPagamentos;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar pagamento por id",
			description = "Funcionário vê qualquer pagamento; cliente só os próprios (outro cliente recebe 404).")
	public PagamentoResponse buscarPorId(@PathVariable Long id, Authentication autenticacao) {
		return consultarPagamentos.buscarPorId(id, SolicitanteAutenticado.de(autenticacao));
	}

	@GetMapping("/orcamento/{orcamentoId}")
	@Operation(summary = "Buscar o pagamento de um orçamento",
			description = "Traz o link do checkout do Mercado Pago, onde o cliente paga depois de aprovar o orçamento.")
	public PagamentoResponse buscarPorOrcamento(@PathVariable Long orcamentoId, Authentication autenticacao) {
		return consultarPagamentos.buscarPorOrcamento(orcamentoId, SolicitanteAutenticado.de(autenticacao));
	}
}
