package br.com.oficina.billing.mensageria.output.rabbit;

import br.com.oficina.billing.mensageria.output.outbox.Broker;
import br.com.oficina.billing.shared.observabilidade.CorrelationIdFilter;
import com.rabbitmq.client.ShutdownSignalException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

/**
 * {@link Broker} no RabbitMQ: publica na exchange com a routing key e espera a confirmação do broker
 * (publisher confirm). Propriedades AMQP e cabeçalhos iguais aos do OS Service.
 */
@Component
class RabbitBroker implements Broker {

	static final String CABECALHO_ORDEM_SERVICO = "x-ordem-servico-id";

	private static final String APLICACAO = "oficina-billing";
	private static final long TIMEOUT_CONFIRMACAO_MS = 5_000;

	private final RabbitTemplate rabbitTemplate;

	RabbitBroker(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void enviar(MensagemSaida saida) {
		MessageBuilderSupport<Message> construtor = MessageBuilder.withBody(saida.corpo().getBytes(StandardCharsets.UTF_8))
				.setContentType(MessageProperties.CONTENT_TYPE_JSON)
				.setContentEncoding(StandardCharsets.UTF_8.name())
				.setMessageId(saida.messageId().toString())
				.setType(saida.tipo())
				.setCorrelationId(saida.correlationId())
				.setHeader(CorrelationIdFilter.CABECALHO, saida.correlationId())
				.setHeader(CABECALHO_ORDEM_SERVICO, saida.ordemServicoId())
				.setAppId(APLICACAO)
				.setTimestamp(Timestamp.valueOf(saida.criadoEm()))
				.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
		// Cabeçalhos do trace de quem gerou o evento: o OS Service continua o mesmo trace
		saida.rastreamento().forEach(construtor::setHeader);
		Message mensagem = construtor.build();

		try {
			rabbitTemplate.invoke(operacoes -> {
				operacoes.send(saida.destino(), saida.chaveRoteamento(), mensagem);
				operacoes.waitForConfirmsOrDie(TIMEOUT_CONFIRMACAO_MS);
				return null;
			});
		} catch (AmqpException | ShutdownSignalException e) {
			// ShutdownSignalException chega crua do cliente do RabbitMQ quando o broker fecha o canal
			// (exchange inexistente, por exemplo) durante a espera da confirmação
			throw new FalhaPublicacaoException(e.getMessage(), e);
		}
	}
}
