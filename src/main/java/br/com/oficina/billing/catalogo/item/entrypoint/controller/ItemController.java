package br.com.oficina.billing.catalogo.item.entrypoint.controller;

import br.com.oficina.billing.catalogo.item.application.dto.ItemRequest;
import br.com.oficina.billing.catalogo.item.application.dto.ItemResponse;
import br.com.oficina.billing.catalogo.item.application.usecase.AtualizarItem;
import br.com.oficina.billing.catalogo.item.application.usecase.CadastrarItem;
import br.com.oficina.billing.catalogo.item.application.usecase.ConsultarItens;
import br.com.oficina.billing.catalogo.item.application.usecase.InativarItem;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;
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
@RequestMapping("/itens")
@Tag(name = "Catálogo de itens", description = "Peças e insumos com o preço de venda. O saldo em estoque fica na Execução.")
public class ItemController {

	private final CadastrarItem cadastrarItem;
	private final AtualizarItem atualizarItem;
	private final InativarItem inativarItem;
	private final ConsultarItens consultarItens;

	public ItemController(CadastrarItem cadastrarItem, AtualizarItem atualizarItem, InativarItem inativarItem,
			ConsultarItens consultarItens) {
		this.cadastrarItem = cadastrarItem;
		this.atualizarItem = atualizarItem;
		this.inativarItem = inativarItem;
		this.consultarItens = consultarItens;
	}

	@PostMapping
	@Operation(summary = "Cadastrar item", description = "Tipo, nome e unidade de medida não se repetem.")
	public ResponseEntity<ItemResponse> cadastrar(@RequestBody @Valid ItemRequest request) {
		ItemResponse response = cadastrarItem.executar(request);
		return ResponseEntity.created(URI.create("/itens/" + response.id())).body(response);
	}

	@GetMapping
	@Operation(summary = "Listar itens", description = "Ativos e inativos, em ordem de id.")
	public List<ItemResponse> listar() {
		return consultarItens.listar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar item por id")
	public ItemResponse buscarPorId(@PathVariable Long id) {
		return consultarItens.buscarPorId(id);
	}

	@GetMapping("/tipo/{tipo}")
	@Operation(summary = "Listar itens por tipo", description = "PECA ou INSUMO.")
	public List<ItemResponse> listarPorTipo(@PathVariable TipoItem tipo) {
		return consultarItens.listarPorTipo(tipo);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualizar item",
			description = "O preço novo vale para orçamentos futuros; orçamento já gerado guarda o valor da época.")
	public ItemResponse atualizar(@PathVariable Long id, @RequestBody @Valid ItemRequest request) {
		return atualizarItem.executar(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Inativar item", description = "O item sai dos orçamentos novos e continua no histórico.")
	public ResponseEntity<Void> inativar(@PathVariable Long id) {
		inativarItem.executar(id);
		return ResponseEntity.noContent().build();
	}
}
