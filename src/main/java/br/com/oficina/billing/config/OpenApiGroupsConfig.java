package br.com.oficina.billing.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

	@Bean
	public GroupedOpenApi catalogoOpenApi() {
		return GroupedOpenApi.builder()
				.group("catalogo")
				.pathsToMatch("/servicos/**", "/itens/**")
				.build();
	}

	@Bean
	public GroupedOpenApi orcamentoOpenApi() {
		return GroupedOpenApi.builder()
				.group("orcamento")
				.pathsToMatch("/orcamentos/**")
				.build();
	}

	@Bean
	public GroupedOpenApi pagamentoOpenApi() {
		return GroupedOpenApi.builder()
				.group("pagamento")
				.pathsToMatch("/pagamentos/**")
				.build();
	}
}
