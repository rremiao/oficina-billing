package br.com.oficina.rotasdeteste;

import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas que só existem para provocar cada tipo de erro no HandlerGeralExcecoesTest. Fica fora de
 * {@code br.com.oficina.billing} de propósito: assim não entra no component scan dos testes de
 * integração.
 */
@RestController
public class RotasQueFalham {

	public enum Tipo { PECA, INSUMO }

	public record Corpo(@NotBlank(message = "Nome é obrigatório") String nome) {
	}

	@GetMapping("/nao-encontrado")
	public String naoEncontrado() {
		throw new RecursoNaoEncontradoException("Serviço não encontrado");
	}

	@GetMapping("/regra")
	public String regra() {
		throw new RegraNegocioException("Já existe serviço cadastrado com este nome");
	}

	@PostMapping("/validacao")
	public String validacao(@RequestBody @Valid Corpo corpo) {
		return corpo.nome();
	}

	@GetMapping("/numero/{id}")
	public String numero(@PathVariable Long id) {
		return id.toString();
	}

	@GetMapping("/tipo/{tipo}")
	public String tipo(@PathVariable Tipo tipo) {
		return tipo.name();
	}
}
