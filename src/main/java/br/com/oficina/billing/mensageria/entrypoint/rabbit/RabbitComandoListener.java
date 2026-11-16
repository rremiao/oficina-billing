package br.com.oficina.billing.mensageria.entrypoint.rabbit;

import br.com.oficina.billing.mensageria.application.ConsumidorComandos;
import br.com.oficina.billing.mensageria.application.MensagemRecebida;
import br.com.oficina.billing.mensageria.application.MensagemRejeitadaException;
import br.com.oficina.billing.shared.observabilidade.CorrelationIdFilter;
import br.com.oficina.billing.shared.observabilidade.Rastreamento;
import com.newrelic.api.agent.Trace;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Escuta a fila de comandos e entrega ao {@link ConsumidorComandos}. Rejeição definitiva vira
 * {@link AmqpRejectAndDontRequeueException} (direto para a DLQ); qualquer outra exceção sobe e o
 * container tenta de novo antes de mandar para a DLQ.
 *
 * <p>O {@code @Trace} fica aqui, no ponto de entrada: o processamento do comando vira uma transação do
 * New Relic, ligada ao trace de quem o enviou pelos cabeçalhos da mensagem.</p>
 */
@Component
class RabbitComandoListener {

	private final ConsumidorComandos consumidor;

	RabbitComandoListener(ConsumidorComandos consumidor) {
		this.consumidor = consumidor;
	}

	@Trace(dispatcher = true)
	@RabbitListener(queues = "${oficina.mensageria.fila-entrada}")
	public void receber(Message mensagem) {
		try {
			consumidor.receber(converter(mensagem));
		} catch (MensagemRejeitadaException e) {
			throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
		}
	}

	static MensagemRecebida converter(Message mensagem) {
		MessageProperties propriedades = mensagem.getMessageProperties();
		Object correlationId = propriedades.getHeader(CorrelationIdFilter.CABECALHO);
		return new MensagemRecebida(new String(mensagem.getBody(), StandardCharsets.UTF_8), propriedades.getType(),
				correlationId != null ? correlationId.toString() : null, cabecalhosDeRastreamento(propriedades));
	}

	private static Map<String, String> cabecalhosDeRastreamento(MessageProperties propriedades) {
		Map<String, String> cabecalhos = new HashMap<>();
		for (String nome : Rastreamento.CABECALHOS) {
			Object valor = propriedades.getHeader(nome);
			if (valor != null) {
				cabecalhos.put(nome, valor.toString());
			}
		}
		return cabecalhos;
	}
}
