package br.com.oficina.billing.mensageria.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Propriedades {@code oficina.mensageria.*} e o agendamento do envio e da limpeza do outbox. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MensageriaProperties.class)
public class MensageriaConfig {
}
