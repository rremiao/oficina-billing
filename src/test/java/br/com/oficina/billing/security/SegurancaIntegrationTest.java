package br.com.oficina.billing.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.billing.apoio.TokensDeTeste;
import br.com.oficina.billing.config.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A regra de acesso, com a aplicação inteira. O {@code /actuator/info} serve de rota protegida
 * comum: fica fora da lista pública, então segue a regra geral (só funcionários).
 */
@IntegrationTest
class SegurancaIntegrationTest {

	private static final String ROTA_PROTEGIDA = "/actuator/info";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthCheckEPublico() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
	}

	@Test
	void swaggerEContratoOpenApiSaoPublicos() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mockMvc.perform(get("/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Oficina - Billing Service"))
				.andExpect(jsonPath("$.servers[0].url").value("/billing/v1"));
	}

	@Test
	void semTokenResponde401EmProblemJson() throws Exception {
		mockMvc.perform(get(ROTA_PROTEGIDA))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Nao autenticado"))
				.andExpect(jsonPath("$.instance").doesNotExist());
	}

	@Test
	void tokenExpiradoOuDeOutraChaveResponde401() throws Exception {
		mockMvc.perform(get(ROTA_PROTEGIDA)
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.clienteExpirado(1L, "12345678909"))))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(ROTA_PROTEGIDA)
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.clienteComOutroSegredo(1L, "12345678909"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenDeClienteEmRotaDeFuncionarioResponde403EmProblemJson() throws Exception {
		mockMvc.perform(get(ROTA_PROTEGIDA)
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(1L, "12345678909", "Maria"))))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.title").value("Acesso negado"));
	}

	@Test
	void adminEAtendenteAcessamRotaDeFuncionario() throws Exception {
		mockMvc.perform(get(ROTA_PROTEGIDA).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.admin())))
				.andExpect(status().isOk());
		mockMvc.perform(get(ROTA_PROTEGIDA).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.atendente())))
				.andExpect(status().isOk());
	}

	@Test
	void rotaInexistenteResponde404EmProblemJson() throws Exception {
		mockMvc.perform(get("/nao-existe").header("Authorization", TokensDeTeste.bearer(TokensDeTeste.admin())))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void respostaDevolveOCorrelationIdRecebido() throws Exception {
		mockMvc.perform(get("/actuator/health").header("x-correlation-id", "abc-123"))
				.andExpect(status().isOk())
				.andExpect(header().string("x-correlation-id", "abc-123"));
	}
}
