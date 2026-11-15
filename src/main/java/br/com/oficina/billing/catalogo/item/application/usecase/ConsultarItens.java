package br.com.oficina.billing.catalogo.item.application.usecase;

import br.com.oficina.billing.catalogo.item.application.dto.ItemResponse;
import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultarItens {

	private final ItemGateway itemGateway;

	public ConsultarItens(ItemGateway itemGateway) {
		this.itemGateway = itemGateway;
	}

	public ItemResponse buscarPorId(Long id) {
		return itemGateway.buscarPorId(id)
				.map(ItemResponse::de)
				.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.ITEM_NOT_FOUND));
	}

	public List<ItemResponse> listar() {
		return itemGateway.listar().stream().map(ItemResponse::de).toList();
	}

	public List<ItemResponse> listarPorTipo(TipoItem tipo) {
		return itemGateway.listarPorTipo(tipo).stream().map(ItemResponse::de).toList();
	}
}
