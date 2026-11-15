package br.com.oficina.billing.pagamento.output.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.Situacao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

class MercadoPagoSituacaoTest {

	@ParameterizedTest
	@CsvSource({
		"processed, APROVADA",
		"failed, RECUSADA",
		"canceled, RECUSADA",
		"cancelled, RECUSADA",
		"expired, RECUSADA",
		"refunded, ESTORNADA",
		"created, PENDENTE",
		"processing, PENDENTE",
		"action_required, PENDENTE"
	})
	void traduzOStatusDaOrdem(String status, Situacao esperada) {
		assertThat(MercadoPagoProvedor.situacao(status)).isEqualTo(esperada);
	}

	@ParameterizedTest
	@NullSource
	void semStatusAOrdemContinuaPendente(String status) {
		assertThat(MercadoPagoProvedor.situacao(status)).isEqualTo(Situacao.PENDENTE);
	}
}
