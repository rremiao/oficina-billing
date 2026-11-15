package br.com.oficina.billing.apoio;

import org.springframework.jdbc.core.JdbcTemplate;

/** Os testes de integração dividem o mesmo banco: cada um começa com as tabelas vazias. */
public final class BancoDeTeste {

	private BancoDeTeste() {
	}

	public static void limpar(JdbcTemplate jdbc) {
		jdbc.execute("TRUNCATE TABLE outbox_evento, pagamento, orcamento_item, orcamento_servico, orcamento, item, "
				+ "servico RESTART IDENTITY CASCADE");
	}
}
