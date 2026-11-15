package br.com.oficina.billing.mensageria.application;

import br.com.oficina.billing.shared.exception.RegraNegocioException;
import br.com.oficina.billing.shared.observabilidade.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Trata os comandos que chegam do OS Service, sem conhecer o broker.
 *
 * <ul>
 *   <li>Mensagem inválida, ou que o caso de uso recusa por regra de negócio, lança
 *   {@link MensagemRejeitadaException}: vai direto para a DLQ, porque tentar de novo não muda nada.</li>
 *   <li>Qualquer outra falha (banco fora, Mercado Pago fora) sobe como está e é tentada de novo.</li>
 * </ul>
 *
 * <p>O correlationId da mensagem vai para o MDC: os logs e os eventos publicados em consequência
 * continuam correlacionados com a requisição original.</p>
 */
@Component
public class ConsumidorComandos {

	private static final Logger LOG = LoggerFactory.getLogger(ConsumidorComandos.class);

	private final Map<String, ManipuladorComando> manipuladores;
	private final JsonMapper jsonMapper;

	public ConsumidorComandos(List<ManipuladorComando> manipuladores, JsonMapper jsonMapper) {
		// toMap falha na subida se dois manipuladores disputarem o mesmo comando
		this.manipuladores = manipuladores.stream()
				.collect(Collectors.toMap(manipulador -> manipulador.tipo().nome(), Function.identity()));
		this.jsonMapper = jsonMapper;
	}

	public void receber(MensagemRecebida mensagem) {
		MDC.put(CorrelationIdFilter.CHAVE_DE_LOG,
				mensagem.correlationId() != null ? mensagem.correlationId() : UUID.randomUUID().toString());
		EnvelopeMensagem envelope = null;
		try {
			envelope = ler(mensagem);
			if (envelope.correlationId() != null) {
				MDC.put(CorrelationIdFilter.CHAVE_DE_LOG, envelope.correlationId());
			}
			ManipuladorComando manipulador = manipuladores.get(envelope.tipo());
			if (manipulador == null) {
				throw new MensagemInvalidaException("Nenhum manipulador para o comando " + envelope.tipo());
			}
			manipulador.manipular(envelope);
			LOG.info("comando_processado tipo={} sagaId={} ordemServicoId={}",
					envelope.tipo(), envelope.sagaId(), envelope.ordemServicoId());
		} catch (MensagemInvalidaException | RegraNegocioException e) {
			LOG.warn("comando_rejeitado destino=dlq tipo={} sagaId={} motivo={}",
					envelope != null ? envelope.tipo() : mensagem.tipo(),
					envelope != null ? envelope.sagaId() : null, e.getMessage());
			throw new MensagemRejeitadaException(e.getMessage(), e);
		} finally {
			MDC.remove(CorrelationIdFilter.CHAVE_DE_LOG);
		}
	}

	private EnvelopeMensagem ler(MensagemRecebida mensagem) {
		EnvelopeMensagem envelope;
		try {
			envelope = jsonMapper.readValue(mensagem.corpo(), EnvelopeMensagem.class);
		} catch (JacksonException e) {
			throw new MensagemInvalidaException("Corpo da mensagem não é um envelope JSON válido", e);
		}
		if (envelope == null || envelope.tipo() == null || envelope.tipo().isBlank()) {
			throw new MensagemInvalidaException("Envelope sem tipo");
		}
		if (envelope.sagaId() == null) {
			throw new MensagemInvalidaException("Envelope sem sagaId");
		}
		if (envelope.ordemServicoId() == null) {
			throw new MensagemInvalidaException("Envelope sem ordemServicoId");
		}
		return envelope;
	}
}
