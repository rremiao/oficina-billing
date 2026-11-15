package br.com.oficina.billing.catalogo.item.entrypoint.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
class ItemControllerIntegrationTest {

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
	void cicloCompletoDoItemNoCatalogo() throws Exception {
		cadastrar("Pastilha de freio", "PECA", "UN", "89.90")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/itens/1"))
				.andExpect(jsonPath("$.tipo").value("PECA"))
				.andExpect(jsonPath("$.valorUnitario").value(89.90))
				.andExpect(jsonPath("$.unidadeMedida").value("UN"))
				.andExpect(jsonPath("$.ativo").value(true));

		mockMvc.perform(comAdmin(put("/itens/1")).content("""
						{"nome": "Pastilha cerâmica", "descricao": "Dianteira", "tipo": "PECA",
						 "valorUnitario": 120, "unidadeMedida": "JG"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Pastilha cerâmica"))
				.andExpect(jsonPath("$.unidadeMedida").value("JG"));

		mockMvc.perform(comAdmin(delete("/itens/1"))).andExpect(status().isNoContent());

		mockMvc.perform(comAdmin(get("/itens/1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	void listaTodosEPorTipo() throws Exception {
		cadastrar("Pastilha de freio", "PECA", "UN", "89.90");
		cadastrar("Óleo 5W30", "INSUMO", "L", "45");
		cadastrar("Disco de freio", "PECA", "UN", "210");

		mockMvc.perform(comAdmin(get("/itens"))).andExpect(jsonPath("$", hasSize(3)));
		mockMvc.perform(comAdmin(get("/itens/tipo/PECA")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].nome").value("Pastilha de freio"))
				.andExpect(jsonPath("$[1].nome").value("Disco de freio"));
	}

	@Test
	void mesmoTipoNomeEUnidadeSemDiferenciarMaiusculasResponde422() throws Exception {
		cadastrar("Pastilha de freio", "PECA", "UN", "89.90");

		cadastrar("PASTILHA DE FREIO", "PECA", "un", "70")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Já existe item cadastrado com o mesmo tipo, nome e unidade de medida"));
	}

	@Test
	void mudarTipoOuUnidadeNaoEDuplicidade() throws Exception {
		cadastrar("Fluido de freio", "INSUMO", "L", "40").andExpect(status().isCreated());
		cadastrar("Fluido de freio", "INSUMO", "ML", "1").andExpect(status().isCreated());
		cadastrar("Fluido de freio", "PECA", "L", "40").andExpect(status().isCreated());
	}

	@Test
	void doisItensDiferentesSemDescricaoNaoColidem() throws Exception {
		cadastrar("Filtro de óleo", "PECA", "UN", "35").andExpect(status().isCreated());
		cadastrar("Pastilha de freio", "PECA", "UN", "89.90").andExpect(status().isCreated());
	}

	@Test
	void atualizarParaOsDadosDeOutroItemResponde422() throws Exception {
		cadastrar("Filtro de óleo", "PECA", "UN", "35");
		cadastrar("Pastilha de freio", "PECA", "UN", "89.90");

		mockMvc.perform(comAdmin(put("/itens/2")).content("""
						{"nome": "filtro de óleo", "tipo": "PECA", "valorUnitario": 35, "unidadeMedida": "UN"}
						"""))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void tipoInvalidoResponde400() throws Exception {
		mockMvc.perform(comAdmin(get("/itens/tipo/PNEU")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("Valores aceitos: PECA, INSUMO")));
		cadastrar("Pneu", "PNEU", "UN", "300").andExpect(status().isBadRequest());
	}

	@Test
	void dadosInvalidosRespondem400() throws Exception {
		cadastrar("Óleo", "INSUMO", " ", "45")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("unidadeMedida: Unidade de medida é obrigatória"));
		cadastrar("Óleo", "INSUMO", "L", "1.999").andExpect(status().isBadRequest());
	}

	@Test
	void itemInexistenteResponde404() throws Exception {
		mockMvc.perform(comAdmin(get("/itens/99"))).andExpect(status().isNotFound());
		mockMvc.perform(comAdmin(put("/itens/99")).content("""
						{"nome": "X", "tipo": "PECA", "valorUnitario": 10, "unidadeMedida": "UN"}
						"""))
				.andExpect(status().isNotFound());
		mockMvc.perform(comAdmin(delete("/itens/99")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Item não encontrado"));
	}

	@Test
	void clienteNaoAcessaOCatalogo() throws Exception {
		mockMvc.perform(get("/itens")
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(1L, "12345678909", "Maria"))))
				.andExpect(status().isForbidden());
	}

	private ResultActions cadastrar(String nome, String tipo, String unidade, String valor) throws Exception {
		return mockMvc.perform(comAdmin(post("/itens")).content("""
				{"nome": "%s", "tipo": "%s", "valorUnitario": %s, "unidadeMedida": "%s"}
				""".formatted(nome, tipo, valor, unidade)));
	}

	private MockHttpServletRequestBuilder comAdmin(MockHttpServletRequestBuilder requisicao) {
		return requisicao.header("Authorization", ADMIN).contentType(MediaType.APPLICATION_JSON);
	}
}
