package br.com.oficina.billing.catalogo.item.application.usecase;

import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inativar não confere OS ativa, como fazia o monólito: o Billing não conhece o status da OS, e o
 * orçamento já gerado guarda uma cópia do nome e do preço. O efeito é o item não entrar em orçamento
 * novo.
 */
@Service
public class InativarItem {

	private final ItemGateway itemGateway;

	public InativarItem(ItemGateway itemGateway) {
		this.itemGateway = itemGateway;
	}

	@Transactional
	public void executar(Long id) {
		Item item = itemGateway.buscarPorId(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException(ExceptionMessage.ITEM_NOT_FOUND));

		item.inativar();
		itemGateway.salvar(item);
	}
}
