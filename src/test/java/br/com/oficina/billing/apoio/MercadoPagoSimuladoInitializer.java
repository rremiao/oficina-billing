package br.com.oficina.billing.apoio;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Aponta o Mercado Pago para o simulador antes de qualquer bean ser registrado. Precisa ser tão cedo
 * porque o Spring Cloud OpenFeign resolve o {@code url} do {@code @FeignClient} no registro do bean:
 * propriedade registrada depois (por {@code DynamicPropertyRegistrar}) fazia o teste chamar o Mercado
 * Pago de verdade.
 */
public class MercadoPagoSimuladoInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext contexto) {
		contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("mercadoPagoSimulado", Map.of(
				"mercadopago.url", MercadoPagoSimulado.url(),
				"mercadopago.access-token", MercadoPagoSimulado.ACCESS_TOKEN,
				"mercadopago.webhook-secret", MercadoPagoSimulado.SEGREDO_WEBHOOK)));
	}
}
