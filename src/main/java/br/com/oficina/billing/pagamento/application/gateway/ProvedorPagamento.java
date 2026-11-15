package br.com.oficina.billing.pagamento.application.gateway;

import java.math.BigDecimal;
import java.util.List;

/**
 * Quem cobra de verdade. Hoje é o Mercado Pago, mas a aplicação só conhece esta porta: trocar de
 * provedor é escrever outro adaptador. Falha de comunicação lança
 * {@link FalhaProvedorPagamentoException}, que é transitória.
 */
public interface ProvedorPagamento {

	/**
	 * Cria a cobrança. A mesma {@code chaveIdempotencia} devolve a mesma cobrança, então repetir a
	 * chamada depois de uma falha não cobra duas vezes.
	 */
	CobrancaCriada criarCobranca(NovaCobranca cobranca);

	/** Situação atual da cobrança no provedor, fonte da verdade para confirmar ou recusar. */
	SituacaoCobranca consultar(String idCobranca);

	/** Estorna o valor inteiro; a mesma {@code chaveIdempotencia} não estorna duas vezes. */
	EstornoRealizado estornar(String idCobranca, String chaveIdempotencia);

	enum Situacao {
		PENDENTE,
		APROVADA,
		RECUSADA,
		ESTORNADA
	}

	record NovaCobranca(String referenciaExterna, String descricao, BigDecimal valor, List<ItemCobranca> itens,
			String chaveIdempotencia) {
	}

	record ItemCobranca(String codigo, String titulo, int quantidade, BigDecimal valorUnitario) {
	}

	/**
	 * @param id          id da cobrança no provedor
	 * @param linkPagamento onde o cliente paga (checkout do provedor)
	 * @param situacao    situação logo depois de criada, normalmente pendente
	 * @param resposta    resposta do provedor, guardada para auditoria
	 */
	record CobrancaCriada(String id, String linkPagamento, Situacao situacao, String resposta) {
	}

	/**
	 * @param situacao situação traduzida
	 * @param metodo   meio de pagamento usado (pix, cartão), quando já pago
	 * @param detalhe  motivo informado pelo provedor, usado na recusa
	 * @param resposta resposta do provedor, guardada para auditoria
	 */
	record SituacaoCobranca(Situacao situacao, String metodo, String detalhe, String resposta) {
	}

	record EstornoRealizado(String id, BigDecimal valor, String resposta) {
	}
}
