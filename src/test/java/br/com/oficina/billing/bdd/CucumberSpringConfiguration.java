package br.com.oficina.billing.bdd;

import br.com.oficina.billing.config.IntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Contexto do Spring dos cenários BDD: a aplicação inteira com PostgreSQL e RabbitMQ reais e o Mercado
 * Pago simulado, a mesma configuração dos testes de integração (o contexto é reaproveitado).
 */
@CucumberContextConfiguration
@IntegrationTest
public class CucumberSpringConfiguration {
}
