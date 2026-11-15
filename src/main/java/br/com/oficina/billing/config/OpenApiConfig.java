package br.com.oficina.billing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.addServersItem(new Server()
						.url("/billing/v1")
						.description("Contexto da API v1"))
				.info(new Info()
						.title("Oficina - Billing Service")
						.version("v1")
						.description("""
								Microsserviço de catálogo, orçamento e pagamento da oficina (Tech Challenge Fase 4).

								É dono de tudo que tem valor em reais: o catálogo de serviços e itens com preço, os
								orçamentos e, nas próximas etapas, o pagamento pelo Mercado Pago e o estorno. Os
								orçamentos nascem por evento da Saga, a partir do pedido do OS Service, e não por
								esta API.

								Todas as rotas exigem token JWT no header Authorization: Bearer <token>, emitido pelo
								login de funcionários do OS Service ou pela function de autenticação por CPF.
								Erros respondem em application/problem+json.
								""")
						.contact(new Contact()
								.name("Equipe Oficina - Billing Service")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
				.components(new Components()
						.addSecuritySchemes(BEARER_SCHEME,
								new SecurityScheme()
										.name(BEARER_SCHEME)
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")));
	}
}
