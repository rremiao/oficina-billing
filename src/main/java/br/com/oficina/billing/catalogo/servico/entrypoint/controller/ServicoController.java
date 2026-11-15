package br.com.oficina.billing.catalogo.servico.entrypoint.controller;

import br.com.oficina.billing.catalogo.servico.application.dto.ServicoRequest;
import br.com.oficina.billing.catalogo.servico.application.dto.ServicoResponse;
import br.com.oficina.billing.catalogo.servico.application.usecase.AtualizarServico;
import br.com.oficina.billing.catalogo.servico.application.usecase.CadastrarServico;
import br.com.oficina.billing.catalogo.servico.application.usecase.ConsultarServicos;
import br.com.oficina.billing.catalogo.servico.application.usecase.InativarServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/servicos")
@Tag(name = "Catálogo de serviços", description = "Serviços da oficina com o preço cobrado no orçamento")
public class ServicoController {

	private final CadastrarServico cadastrarServico;
	private final AtualizarServico atualizarServico;
	private final InativarServico inativarServico;
	private final ConsultarServicos consultarServicos;

	public ServicoController(CadastrarServico cadastrarServico, AtualizarServico atualizarServico,
			InativarServico inativarServico, ConsultarServicos consultarServicos) {
		this.cadastrarServico = cadastrarServico;
		this.atualizarServico = atualizarServico;
		this.inativarServico = inativarServico;
		this.consultarServicos = consultarServicos;
	}

	@PostMapping
	@Operation(summary = "Cadastrar serviço", description = "Nome único, sem diferenciar maiúsculas.")
	public ResponseEntity<ServicoResponse> cadastrar(@RequestBody @Valid ServicoRequest request) {
		ServicoResponse response = cadastrarServico.executar(request);
		return ResponseEntity.created(URI.create("/servicos/" + response.id())).body(response);
	}

	@GetMapping
	@Operation(summary = "Listar serviços", description = "Ativos e inativos, em ordem de id.")
	public List<ServicoResponse> listar() {
		return consultarServicos.listar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar serviço por id")
	public ServicoResponse buscarPorId(@PathVariable Long id) {
		return consultarServicos.buscarPorId(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualizar serviço",
			description = "O preço novo vale para orçamentos futuros; orçamento já gerado guarda o valor da época.")
	public ServicoResponse atualizar(@PathVariable Long id, @RequestBody @Valid ServicoRequest request) {
		return atualizarServico.executar(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Inativar serviço", description = "O serviço sai dos orçamentos novos e continua no histórico.")
	public ResponseEntity<Void> inativar(@PathVariable Long id) {
		inativarServico.executar(id);
		return ResponseEntity.noContent().build();
	}
}
