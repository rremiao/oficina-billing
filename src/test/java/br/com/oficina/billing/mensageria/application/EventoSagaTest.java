package br.com.oficina.billing.mensageria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class EventoSagaTest {

	private static final UUID SAGA = UUID.randomUUID();

	private static EventoSaga evento(TipoEvento tipo, String chave) {
		return new EventoSaga(tipo, chave, SAGA, 42L, null, null, null);
	}

	@Test
	void oMesmoFatoTemSempreOMesmoMessageId() {
		assertThat(evento(TipoEvento.PAGAMENTO_CONFIRMADO, "5").messageId())
				.isEqualTo(evento(TipoEvento.PAGAMENTO_CONFIRMADO, "5").messageId());
	}

	@Test
	void outroTipoOuOutraChaveEOutroFato() {
		UUID confirmado = evento(TipoEvento.PAGAMENTO_CONFIRMADO, "5").messageId();

		assertThat(evento(TipoEvento.PAGAMENTO_ESTORNADO, "5").messageId()).isNotEqualTo(confirmado);
		assertThat(evento(TipoEvento.PAGAMENTO_CONFIRMADO, "6").messageId()).isNotEqualTo(confirmado);
	}

	@Test
	void eventoSemTipoChaveSagaOuOsNaoExiste() {
		assertThatThrownBy(() -> new EventoSaga(null, "5", SAGA, 42L, null, null, null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new EventoSaga(TipoEvento.ORCAMENTO_GERADO, null, SAGA, 42L, null, null, null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new EventoSaga(TipoEvento.ORCAMENTO_GERADO, "5", null, 42L, null, null, null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new EventoSaga(TipoEvento.ORCAMENTO_GERADO, "5", SAGA, null, null, null, null))
				.isInstanceOf(NullPointerException.class);
	}
}
