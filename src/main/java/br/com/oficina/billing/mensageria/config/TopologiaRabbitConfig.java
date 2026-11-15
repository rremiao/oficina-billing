package br.com.oficina.billing.mensageria.config;

import br.com.oficina.billing.mensageria.application.TipoComando;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Declara no RabbitMQ o que o Billing usa. A declaração é idempotente e igual à do OS Service: quem
 * subir primeiro cria as exchanges.
 *
 * <pre>
 * os.eventos (topic)  ─→ oficina-billing-comandos (só SolicitarOrcamento, CobrarPagamento, EstornarPagamento)
 *                            └─ rejeitada → oficina-billing-comandos-dlx → oficina-billing-comandos-dlq
 * billing.eventos (topic) ← Billing publica (routing key = tipo do evento); o OS Service consome
 * </pre>
 */
@Configuration
public class TopologiaRabbitConfig {

	@Bean
	public Declarables topologiaMensageria(MensageriaProperties propriedades) {
		TopicExchange exchangeOs = new TopicExchange(propriedades.exchangeOs(), true, false);
		TopicExchange exchangeBilling = new TopicExchange(propriedades.exchangeBilling(), true, false);
		DirectExchange deadLetter = new DirectExchange(propriedades.exchangeDeadLetter(), true, false);

		Queue filaEntrada = QueueBuilder.durable(propriedades.filaEntrada())
				.deadLetterExchange(propriedades.exchangeDeadLetter())
				.deadLetterRoutingKey(propriedades.filaDeadLetter())
				.build();
		Queue filaDeadLetter = QueueBuilder.durable(propriedades.filaDeadLetter()).build();

		List<Declarable> declaraveis = new ArrayList<>(List.of(
				exchangeOs, exchangeBilling, deadLetter, filaEntrada, filaDeadLetter,
				BindingBuilder.bind(filaDeadLetter).to(deadLetter).with(propriedades.filaDeadLetter())));
		for (TipoComando comando : TipoComando.values()) {
			declaraveis.add(BindingBuilder.bind(filaEntrada).to(exchangeOs).with(comando.nome()));
		}
		return new Declarables(declaraveis);
	}
}
