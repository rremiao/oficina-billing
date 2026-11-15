package br.com.oficina.billing.catalogo.item.application.usecase;

import br.com.oficina.billing.catalogo.item.application.dto.ItemRequest;
import br.com.oficina.billing.catalogo.item.application.dto.ItemResponse;
import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarItem {

	private final ItemGateway itemGateway;

	public CadastrarItem(ItemGateway itemGateway) {
		this.itemGateway = itemGateway;
	}

	@Transactional
	public ItemResponse executar(ItemRequest request) {
		Item item = Item.novo(request.paraDados());

		if (itemGateway.existeDuplicado(item.getTipo(), item.getNome(), item.getUnidadeMedida(), null)) {
			throw new RegraNegocioException(ExceptionMessage.EXISTING_ITEM);
		}

		return ItemResponse.de(itemGateway.salvar(item));
	}
}
