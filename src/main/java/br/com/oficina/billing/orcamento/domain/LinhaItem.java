package br.com.oficina.billing.orcamento.domain;

import br.com.oficina.billing.shared.domain.Dinheiro;
import br.com.oficina.billing.shared.domain.Texto;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;

import java.math.BigDecimal;

/**
 * Peça ou insumo orçado, com o nome e o preço copiados do catálogo no momento do orçamento.
 *
 * @param itemId        id no catálogo
 * @param nome          nome na época do orçamento
 * @param quantidade    quantidade pedida pelo OS Service
 * @param valorUnitario preço unitário na época do orçamento
 */
public record LinhaItem(Long itemId, String nome, int quantidade, BigDecimal valorUnitario) {

	public LinhaItem {
		if (itemId == null) {
			throw new RegraNegocioException(ExceptionMessage.REQUIRED_CATALOG_ID);
		}
		if (quantidade <= 0) {
			throw new RegraNegocioException(ExceptionMessage.INVALID_QUANTITY.formatted(itemId));
		}
		nome = Texto.obrigatorio(nome, ExceptionMessage.REQUIRED_NAME);
		valorUnitario = Dinheiro.exigirPositivo(valorUnitario, "Valor do item");
		Dinheiro.exigirDentroDoLimite(valorUnitario.multiply(BigDecimal.valueOf(quantidade)), "Valor do item");
	}

	public BigDecimal valorTotal() {
		return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
	}
}
