package br.com.oficina.billing.shared.domain;

import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de valor monetário: positivo e com no máximo duas casas, como o {@code NUMERIC(10,2)} do banco. */
public final class Dinheiro {

	private static final int CASAS = 2;

	/** Maior valor que cabe no {@code NUMERIC(10,2)}. */
	private static final BigDecimal LIMITE = new BigDecimal("99999999.99");

	private Dinheiro() {
	}

	/** Soma que passa do que o banco guarda vira regra de negócio, não erro 500 na gravação. */
	public static BigDecimal exigirDentroDoLimite(BigDecimal valor, String campo) {
		if (valor.compareTo(LIMITE) > 0) {
			throw new RegraNegocioException(ExceptionMessage.AMOUNT_OVER_LIMIT.formatted(campo));
		}
		return valor;
	}

	/** Devolve o valor com exatamente duas casas, sem arredondar: casa a mais é erro, não centavo perdido. */
	public static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
		if (valor == null || valor.signum() <= 0) {
			throw new RegraNegocioException(ExceptionMessage.INVALID_AMOUNT.formatted(campo));
		}
		if (valor.stripTrailingZeros().scale() > CASAS) {
			throw new RegraNegocioException(ExceptionMessage.INVALID_AMOUNT_SCALE.formatted(campo));
		}
		return valor.setScale(CASAS, RoundingMode.UNNECESSARY);
	}
}
