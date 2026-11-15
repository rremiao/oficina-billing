package br.com.oficina.billing.config;

import br.com.oficina.billing.BillingApplication;
import br.com.oficina.billing.apoio.MercadoPagoSimuladoInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Teste de integração com a aplicação inteira, PostgreSQL e RabbitMQ reais (Testcontainers) e o
 * Mercado Pago simulado (WireMock). Todas as classes anotadas compartilham o mesmo contexto do Spring e
 * os mesmos containers. O envio agendado do outbox fica desligado: o teste chama
 * {@code OutboxEnvio.enviarPendentes()} quando quer que os eventos saiam.
 */
@SpringBootTest(classes = BillingApplication.class,
		properties = "oficina.mensageria.outbox.agendamento-habilitado=false")
@ContextConfiguration(initializers = MercadoPagoSimuladoInitializer.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IntegrationTest {
}
