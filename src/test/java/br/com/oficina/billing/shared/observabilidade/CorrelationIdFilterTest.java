package br.com.oficina.billing.shared.observabilidade;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filtro = new CorrelationIdFilter();

	@Test
	void herdaOIdentificadorRecebidoDoGateway() throws Exception {
		MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/servicos");
		requisicao.addHeader(CorrelationIdFilter.CABECALHO, "  id-do-gateway  ");
		MockHttpServletResponse resposta = new MockHttpServletResponse();
		AtomicReference<String> noLog = new AtomicReference<>();

		filtro.doFilter(requisicao, resposta, guardarMdc(noLog));

		assertThat(noLog.get()).isEqualTo("id-do-gateway");
		assertThat(resposta.getHeader(CorrelationIdFilter.CABECALHO)).isEqualTo("id-do-gateway");
	}

	@Test
	void geraUmIdentificadorQuandoARequisicaoNaoTraz() throws Exception {
		MockHttpServletResponse resposta = new MockHttpServletResponse();
		AtomicReference<String> noLog = new AtomicReference<>();

		filtro.doFilter(new MockHttpServletRequest("GET", "/servicos"), resposta, guardarMdc(noLog));

		assertThat(UUID.fromString(noLog.get())).isNotNull();
		assertThat(resposta.getHeader(CorrelationIdFilter.CABECALHO)).isEqualTo(noLog.get());
	}

	@Test
	void cabecalhoEmBrancoGanhaIdentificadorNovo() throws Exception {
		MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/servicos");
		requisicao.addHeader(CorrelationIdFilter.CABECALHO, "   ");
		AtomicReference<String> noLog = new AtomicReference<>();

		filtro.doFilter(requisicao, new MockHttpServletResponse(), guardarMdc(noLog));

		assertThat(UUID.fromString(noLog.get())).isNotNull();
	}

	@Test
	void cortaCabecalhoLongoDemais() throws Exception {
		MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/servicos");
		requisicao.addHeader(CorrelationIdFilter.CABECALHO, "x".repeat(500));
		AtomicReference<String> noLog = new AtomicReference<>();

		filtro.doFilter(requisicao, new MockHttpServletResponse(), guardarMdc(noLog));

		assertThat(noLog.get()).hasSize(128);
	}

	@Test
	void limpaOMdcAoFinalDaRequisicao() throws Exception {
		filtro.doFilter(new MockHttpServletRequest("GET", "/orcamentos/12345678909"),
				new MockHttpServletResponse(), (req, res) -> { });

		assertThat(MDC.get(CorrelationIdFilter.CHAVE_DE_LOG)).isNull();
	}

	private FilterChain guardarMdc(AtomicReference<String> destino) {
		return (requisicao, resposta) -> destino.set(MDC.get(CorrelationIdFilter.CHAVE_DE_LOG));
	}
}
