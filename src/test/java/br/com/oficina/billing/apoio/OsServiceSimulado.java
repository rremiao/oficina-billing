package br.com.oficina.billing.apoio;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Faz o papel do OS Service no RabbitMQ: publica os comandos em {@code os.eventos}, como ele publica,
 * e captura o que o Billing publica em {@code billing.eventos} numa fila só do teste.
 */
public final class OsServiceSimulado {

	public static final String EXCHANGE_OS = "os.eventos";
	public static final String EXCHANGE_BILLING = "billing.eventos";
	public static final String FILA_DLQ = "oficina-billing-comandos-dlq";
	private static final String FILA_CAPTURA = "teste-captura-billing-eventos";
	private static final long ESPERA_MS = 10_000;

	private final RabbitTemplate rabbitTemplate;
	private final AmqpAdmin admin;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	public OsServiceSimulado(RabbitTemplate rabbitTemplate, AmqpAdmin admin) {
		this.rabbitTemplate = rabbitTemplate;
		this.admin = admin;
		Queue captura = new Queue(FILA_CAPTURA, false, false, false);
		admin.declareQueue(captura);
		admin.declareBinding(BindingBuilder.bind(captura).to(new TopicExchange(EXCHANGE_BILLING, true, false)).with("#"));
	}

	/** Esvazia a captura e a DLQ: cada teste começa sem mensagem de outro. */
	public void limpar() {
		admin.purgeQueue(FILA_CAPTURA, false);
		admin.purgeQueue(FILA_DLQ, false);
	}

	/** Publica o comando com o envelope do contrato e devolve o {@code correlationId} usado. */
	public String enviar(String tipo, UUID sagaId, Long ordemServicoId, Object dados) {
		String correlationId = UUID.randomUUID().toString();
		Map<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("sagaId", sagaId);
		envelope.put("messageId", UUID.nameUUIDFromBytes((tipo + sagaId).getBytes(StandardCharsets.UTF_8)));
		envelope.put("tipo", tipo);
		envelope.put("ordemServicoId", ordemServicoId);
		envelope.put("codigoOrdemServico", "OS-2026-0000" + ordemServicoId);
		envelope.put("correlationId", correlationId);
		envelope.put("ocorridoEm", "2026-11-14T13:45:10.123Z");
		envelope.put("versao", 1);
		envelope.put("dados", dados);
		enviarCorpo(tipo, jsonMapper.writeValueAsString(envelope), correlationId);
		return correlationId;
	}

	/** Publica um corpo qualquer, inclusive inválido, com a routing key do comando. */
	public void enviarCorpo(String tipo, String corpo, String correlationId) {
		MessageProperties propriedades = new MessageProperties();
		propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		propriedades.setType(tipo);
		propriedades.setHeader("x-correlation-id", correlationId);
		Message mensagem = MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
				.andProperties(propriedades).build();
		rabbitTemplate.send(EXCHANGE_OS, tipo, mensagem);
	}

	/** Próximo evento publicado pelo Billing, ou nulo se nada chegar no tempo. */
	public Message proximoEvento() {
		return rabbitTemplate.receive(FILA_CAPTURA, ESPERA_MS);
	}

	/** Evento publicado agora, sem esperar: nulo quando não há. */
	public Message eventoJaPublicado() {
		return rabbitTemplate.receive(FILA_CAPTURA);
	}

	public Message proximaNaDlq() {
		return rabbitTemplate.receive(FILA_DLQ, ESPERA_MS);
	}

	public JsonNode corpo(Message mensagem) {
		return jsonMapper.readTree(new String(mensagem.getBody(), StandardCharsets.UTF_8));
	}
}
