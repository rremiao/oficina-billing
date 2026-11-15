package br.com.oficina.billing.orcamento.application.gateway;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * O que o orçamento precisa do catálogo: nome, preço e se está ativo. Porta própria do orçamento,
 * para ele não depender do cadastro do catálogo.
 */
public interface CatalogoPrecos {

	/** Preços dos serviços pedidos, por id. Id que não existe no catálogo fica fora do mapa. */
	Map<Long, Preco> servicos(Collection<Long> ids);

	/** Preços dos itens pedidos, por id. Id que não existe no catálogo fica fora do mapa. */
	Map<Long, Preco> itens(Collection<Long> ids);

	record Preco(String nome, BigDecimal valor, boolean ativo) {
	}
}
