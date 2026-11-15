package br.com.oficina.billing.orcamento.output.catalogo;

import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.catalogo.servico.domain.Servico;
import br.com.oficina.billing.orcamento.application.gateway.CatalogoPrecos;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Liga o orçamento ao catálogo do próprio Billing, traduzindo para o que o orçamento precisa. */
@Component
class CatalogoPrecosAdapter implements CatalogoPrecos {

	private final ServicoGateway servicoGateway;
	private final ItemGateway itemGateway;

	CatalogoPrecosAdapter(ServicoGateway servicoGateway, ItemGateway itemGateway) {
		this.servicoGateway = servicoGateway;
		this.itemGateway = itemGateway;
	}

	@Override
	public Map<Long, Preco> servicos(Collection<Long> ids) {
		return servicoGateway.buscarPorIds(ids).stream()
				.collect(Collectors.toMap(Servico::getId,
						servico -> new Preco(servico.getNome(), servico.getValor(), servico.isAtivo())));
	}

	@Override
	public Map<Long, Preco> itens(Collection<Long> ids) {
		return itemGateway.buscarPorIds(ids).stream()
				.collect(Collectors.toMap(Item::getId,
						item -> new Preco(item.getNome(), item.getValorUnitario(), item.isAtivo())));
	}
}
