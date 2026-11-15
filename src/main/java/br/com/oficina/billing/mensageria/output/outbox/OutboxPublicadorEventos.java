package br.com.oficina.billing.mensageria.output.outbox;

import br.com.oficina.billing.mensageria.application.EnvelopeMensagem;
import br.com.oficina.billing.mensageria.application.EventoSaga;
import br.com.oficina.billing.mensageria.application.PublicadorEventos;
import br.com.oficina.billing.mensageria.config.MensageriaProperties;
import br.com.oficina.billing.shared.observabilidade.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link PublicadorEventos} pelo Transactional Outbox. Exige transação aberta ({@code MANDATORY}): o
 * evento só existe se a mudança for gravada, e a mudança só é gravada junto com o evento.
 */
@Component
class OutboxPublicadorEventos implements PublicadorEventos {

	private final OutboxEventoRepository repository;
	private final JsonMapper jsonMapper;
	private final MensageriaProperties propriedades;

	OutboxPublicadorEventos(OutboxEventoRepository repository, JsonMapper jsonMapper,
			MensageriaProperties propriedades) {
		this.repository = repository;
		this.jsonMapper = jsonMapper;
		this.propriedades = propriedades;
	}

	/** O mesmo fato de novo (comando repetido) só agenda o reenvio do evento já registrado, com o mesmo id. */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void publicar(EventoSaga evento) {
		UUID messageId = evento.messageId();
		Optional<OutboxEvento> jaRegistrado = repository.findByMessageId(messageId);
		if (jaRegistrado.isPresent()) {
			jaRegistrado.get().setPublicadoEm(null);
			return;
		}

		String correlationId = correlationId(evento);
		EnvelopeMensagem envelope = new EnvelopeMensagem(messageId, evento.tipo().nome(), evento.sagaId(),
				evento.ordemServicoId(), evento.codigoOrdemServico(), correlationId, Instant.now(),
				EnvelopeMensagem.VERSAO_ATUAL, jsonMapper.valueToTree(evento.dados()));

		OutboxEvento registro = new OutboxEvento();
		registro.setMessageId(messageId);
		registro.setTipo(evento.tipo().nome());
		registro.setExchange(propriedades.exchangeBilling());
		registro.setRoutingKey(evento.tipo().nome());
		registro.setSagaId(evento.sagaId());
		registro.setOrdemServicoId(evento.ordemServicoId());
		registro.setCorrelationId(correlationId);
		registro.setPayload(jsonMapper.writeValueAsString(envelope));
		repository.save(registro);
	}

	/** O informado no evento; senão, o da mensagem ou requisição atual; senão, um novo. */
	private static String correlationId(EventoSaga evento) {
		if (evento.correlationId() != null) {
			return evento.correlationId();
		}
		String atual = MDC.get(CorrelationIdFilter.CHAVE_DE_LOG);
		return atual != null ? atual : UUID.randomUUID().toString();
	}
}
