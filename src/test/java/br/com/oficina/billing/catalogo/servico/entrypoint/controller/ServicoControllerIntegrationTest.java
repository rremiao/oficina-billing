package br.com.oficina.billing.catalogo.servico.entrypoint.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.billing.apoio.BancoDeTeste;
import br.com.oficina.billing.apoio.TokensDeTeste;
import br.com.oficina.billing.config.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ServicoControllerIntegrationTest {

	private static final String ADMIN = TokensDeTeste.bearer(TokensDeTeste.admin());

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limparBanco() {
		BancoDeTeste.limpar(jdbc);
	}

	@Test
	void cicloCompletoDoServicoNoCatalogo() throws Exception {
		cadastrar("  Alinhamento  ", "150")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/servicos/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nome").value("Alinhamento"))
				.andExpect(jsonPath("$.valor").value(150.00))
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.dataCriacao", notNullValue()));

		mockMvc.perform(comAdmin(put("/servicos/1"))
						.content("{\"nome\": \"Alinhamento 3D\", \"descricao\": \"Com laudo\", \"valor\": 180.50}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Alinhamento 3D"))
				.andExpect(jsonPath("$.valor").value(180.50))
				.andExpect(jsonPath("$.dataAtualizacao", notNullValue()));

		mockMvc.perform(comAdmin(delete("/servicos/1"))).andExpect(status().isNoContent());

		mockMvc.perform(comAdmin(get("/servicos/1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(false))
				.andExpect(jsonPath("$.descricao").value("Com laudo"));
	}

	@Test
	void listaAtivosEInativosEmOrdemDeId() throws Exception {
		cadastrar("Alinhamento", "150");
		cadastrar("Balanceamento", "80");
		mockMvc.perform(comAdmin(delete("/servicos/1")));

		mockMvc.perform(comAdmin(get("/servicos")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nome").value("Alinhamento"))
				.andExpect(jsonPath("$[0].ativo").value(false))
				.andExpect(jsonPath("$[1].nome").value("Balanceamento"));
	}

	@Test
	void nomeRepetidoSemDiferenciarMaiusculasResponde422() throws Exception {
		cadastrar("Alinhamento", "150");

		cadastrar("  ALINHAMENTO ", "99")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Já existe serviço cadastrado com este nome"));
	}

	@Test
	void atualizarParaONomeDeOutroServicoResponde422EManterOProprioNomeEAceito() throws Exception {
		cadastrar("Alinhamento", "150");
		cadastrar("Balanceamento", "80");

		mockMvc.perform(comAdmin(put("/servicos/2")).content("{\"nome\": \"alinhamento\", \"valor\": 80}"))
				.andExpect(status().isUnprocessableContent());
		mockMvc.perform(comAdmin(put("/servicos/2")).content("{\"nome\": \"Balanceamento\", \"valor\": 90}"))
				.andExpect(status().isOk());
	}

	@Test
	void inativarDuasVezesContinuaRespondendo204() throws Exception {
		cadastrar("Alinhamento", "150");

		mockMvc.perform(comAdmin(delete("/servicos/1"))).andExpect(status().isNoContent());
		mockMvc.perform(comAdmin(delete("/servicos/1"))).andExpect(status().isNoContent());
	}

	@Test
	void dadosInvalidosRespondem400() throws Exception {
		cadastrar("Alinhamento", "10.005")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("valor: Valor deve ter até 8 dígitos inteiros e 2 casas decimais"));
		cadastrar("Alinhamento", "0").andExpect(status().isBadRequest());
		cadastrar(" ", "10")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("nome: Nome é obrigatório"));
	}

	@Test
	void servicoInexistenteResponde404() throws Exception {
		mockMvc.perform(comAdmin(get("/servicos/99"))).andExpect(status().isNotFound());
		mockMvc.perform(comAdmin(put("/servicos/99")).content("{\"nome\": \"X\", \"valor\": 10}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(comAdmin(delete("/servicos/99")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Serviço não encontrado"));
	}

	@Test
	void atendenteCadastraEClienteNaoAcessaOCatalogo() throws Exception {
		mockMvc.perform(post("/servicos").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.atendente()))
						.content("{\"nome\": \"Alinhamento\", \"valor\": 150}"))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/servicos")
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(1L, "12345678909", "Maria"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/servicos")).andExpect(status().isUnauthorized());
	}

	private ResultActions cadastrar(String nome, String valor) throws Exception {
		return mockMvc.perform(comAdmin(post("/servicos"))
				.content("{\"nome\": \"%s\", \"valor\": %s}".formatted(nome, valor)));
	}

	private MockHttpServletRequestBuilder comAdmin(MockHttpServletRequestBuilder requisicao) {
		return requisicao.header("Authorization", ADMIN).contentType(MediaType.APPLICATION_JSON);
	}
}
