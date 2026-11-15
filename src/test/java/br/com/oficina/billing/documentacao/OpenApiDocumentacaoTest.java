package br.com.oficina.billing.documentacao;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.billing.config.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Todo endpoint aparece no Swagger, agrupado e com resumo (docs/REGRAS_EXECUCAO_E_QUALIDADE.md, seção 7). */
@IntegrationTest
class OpenApiDocumentacaoTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void grupoDoCatalogoDocumentaServicosEItens() throws Exception {
		mockMvc.perform(get("/api-docs/catalogo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Oficina - Billing Service"))
				.andExpect(jsonPath("$.paths", hasKey("/servicos/{id}")))
				.andExpect(jsonPath("$.paths", hasKey("/itens/tipo/{tipo}")))
				.andExpect(jsonPath("$.paths['/servicos'].post.summary").value("Cadastrar serviço"))
				.andExpect(jsonPath("$.paths['/itens/{id}'].delete.summary").value("Inativar item"))
				.andExpect(jsonPath("$.tags[*].name").isNotEmpty());
	}

	@Test
	void grupoDoOrcamentoSoTemConsultas() throws Exception {
		mockMvc.perform(get("/api-docs/orcamento"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/orcamentos/{id}'].get.summary").value("Buscar orçamento por id"))
				.andExpect(jsonPath("$.paths", hasKey("/orcamentos/ordem-servico/{ordemServicoId}")))
				.andExpect(jsonPath("$.paths['/orcamentos/{id}'].post").doesNotExist());
	}

	@Test
	void grupoDoPagamentoTemConsultasEOWebhookSemJwt() throws Exception {
		mockMvc.perform(get("/api-docs/pagamento"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/pagamentos/orcamento/{orcamentoId}'].get.summary")
						.value("Buscar o pagamento de um orçamento"))
				.andExpect(jsonPath("$.paths['/pagamentos/webhook/mercadopago'].post.summary")
						.value("Receber aviso do Mercado Pago"))
				.andExpect(jsonPath("$.paths['/pagamentos/webhook/mercadopago'].post.security").isEmpty());
	}
}
