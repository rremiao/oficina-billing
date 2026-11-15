package br.com.oficina.billing.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.rotasdeteste.RotasQueFalham;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Cada tipo de erro sai em problem+json, com status e mensagem em português. */
class HandlerGeralExcecoesTest {

	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RotasQueFalham())
			.setControllerAdvice(new HandlerGeralExcecoes())
			.build();

	@Test
	void recursoNaoEncontradoResponde404() throws Exception {
		mockMvc.perform(get("/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Recurso nao encontrado"))
				.andExpect(jsonPath("$.detail").value("Serviço não encontrado"));
	}

	@Test
	void regraDeNegocioResponde422() throws Exception {
		mockMvc.perform(get("/regra"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Regra de negocio violada"))
				.andExpect(jsonPath("$.detail").value("Já existe serviço cadastrado com este nome"));
	}

	@Test
	void campoInvalidoResponde400ComOCampoEAMensagem() throws Exception {
		mockMvc.perform(post("/validacao").contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Erro de validacao"))
				.andExpect(jsonPath("$.detail").value("nome: Nome é obrigatório"));
	}

	@Test
	void jsonMalformadoResponde400() throws Exception {
		mockMvc.perform(post("/validacao").contentType(MediaType.APPLICATION_JSON).content("{nome"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou com JSON inválido"));
	}

	@Test
	void tipoInvalidoNaUrlResponde400() throws Exception {
		mockMvc.perform(get("/numero/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Valor inválido para 'id': abc"));
	}

	@Test
	void enumInvalidoNaUrlListaOsValoresAceitos() throws Exception {
		mockMvc.perform(get("/tipo/PNEU"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString("Valores aceitos: PECA, INSUMO")));
	}

	@Test
	void metodoNaoSuportadoResponde405() throws Exception {
		mockMvc.perform(delete("/regra"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.detail").value("Método DELETE não suportado nesta rota"));
	}
}
