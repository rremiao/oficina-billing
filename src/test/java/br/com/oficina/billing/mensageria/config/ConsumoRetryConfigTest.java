package br.com.oficina.billing.mensageria.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class ConsumoRetryConfigTest {

	@Test
	void falhaTransitoriaETentadaDeNovo() {
		assertThat(ConsumoRetryConfig.deveTentarDeNovo(new IllegalStateException("banco fora"))).isTrue();
	}

	@Test
	void rejeicaoDefinitivaEmQualquerNivelNaoETentadaDeNovo() {
		AmqpRejectAndDontRequeueException rejeicao = new AmqpRejectAndDontRequeueException("inválida");

		assertThat(ConsumoRetryConfig.deveTentarDeNovo(rejeicao)).isFalse();
		assertThat(ConsumoRetryConfig.deveTentarDeNovo(new RuntimeException("embrulho", rejeicao))).isFalse();
	}

	@Test
	void customizadorUsaOPredicado() {
		assertThat(new ConsumoRetryConfig().naoRepetirRejeicaoDefinitiva()).isNotNull();
	}
}
