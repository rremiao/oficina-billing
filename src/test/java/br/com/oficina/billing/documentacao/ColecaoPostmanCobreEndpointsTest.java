package br.com.oficina.billing.documentacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.billing.config.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * A collection do Postman é a evidência de que todos os endpoints foram exercitados. Este teste
 * compara as rotas registradas no Spring MVC com os requests da collection e falha quando um
 * endpoint novo fica sem request.
 */
@IntegrationTest
class ColecaoPostmanCobreEndpointsTest {

	private static final Path COLECAO = Path.of("docs/postman/oficina-billing.postman_collection.json");
	private static final String PACOTE_DA_APLICACAO = "br.com.oficina.billing.";
	private static final Pattern VARIAVEL_POSTMAN = Pattern.compile("\\{\\{[^}]+}}");

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	@Test
	void todaRotaDaApiTemRequestNaColecao() throws IOException {
		List<String> requestsDaColecao = lerRequests();

		TreeSet<String> semRequest = new TreeSet<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> rota : handlerMapping.getHandlerMethods().entrySet()) {
			if (!rota.getValue().getBeanType().getName().startsWith(PACOTE_DA_APLICACAO)) {
				continue;
			}
			for (RequestMethod metodo : rota.getKey().getMethodsCondition().getMethods()) {
				for (String caminho : rota.getKey().getPathPatternsCondition().getPatternValues()) {
					Pattern esperado = Pattern.compile(metodo.name() + " " + caminho.replaceAll("\\{[^}]+}", "[^/]+"));
					if (requestsDaColecao.stream().noneMatch(r -> esperado.matcher(r).matches())) {
						semRequest.add(metodo.name() + " " + caminho);
					}
				}
			}
		}

		assertThat(semRequest)
				.as("endpoints sem request em %s", COLECAO)
				.isEmpty();
	}

	/** Requests no formato "METODO /caminho", com as variáveis do Postman trocadas por "X". */
	private List<String> lerRequests() throws IOException {
		JsonNode colecao = JsonMapper.builder().build().readTree(Files.readString(COLECAO));
		List<String> requests = new ArrayList<>();
		coletar(colecao.path("item"), requests);
		return requests;
	}

	private void coletar(JsonNode itens, List<String> requests) {
		for (JsonNode item : itens) {
			if (item.has("item")) {
				coletar(item.path("item"), requests);
				continue;
			}
			JsonNode request = item.path("request");
			String url = request.path("url").isString()
					? request.path("url").asString()
					: request.path("url").path("raw").asString();
			// A query string não faz parte da rota (o webhook do Mercado Pago leva data.id e type na URL)
			String caminho = url.replace("{{baseUrl}}", "").split("\\?", 2)[0];
			requests.add(request.path("method").asString() + " " + VARIAVEL_POSTMAN.matcher(caminho).replaceAll("X"));
		}
	}
}
