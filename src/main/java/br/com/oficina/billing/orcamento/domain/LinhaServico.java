package br.com.oficina.billing.orcamento.domain;

import br.com.oficina.billing.shared.domain.Dinheiro;
import br.com.oficina.billing.shared.domain.Texto;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;

import java.math.BigDecimal;

/**
 * Serviço orçado, com o nome e o preço copiados do catálogo no momento do orçamento.
 *
 * @param servicoId     id no catálogo
 * @param nome          nome na época do orçamento
 * @param valorUnitario preço na época do orçamento; o serviço entra uma vez
 */
public record LinhaServico(Long servicoId, String nome, BigDecimal valorUnitario) {

	public LinhaServico {
		if (servicoId == null) {
			throw new RegraNegocioException(ExceptionMessage.REQUIRED_CATALOG_ID);
		}
		nome = Texto.obrigatorio(nome, ExceptionMessage.REQUIRED_NAME);
		valorUnitario = Dinheiro.exigirPositivo(valorUnitario, "Valor do serviço");
	}
}
