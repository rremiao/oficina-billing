package br.com.oficina.billing.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.billing.apoio.TokensDeTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

class JwtAuthenticationFilterTest {

	private final JwtAuthenticationFilter filtro =
			new JwtAuthenticationFilter(new JwtService(TokensDeTeste.SEGREDO));

	@AfterEach
	void limparContexto() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void bearerValidoAutenticaESegueACadeia() throws Exception {
		MockFilterChain cadeia = new MockFilterChain();

		filtro.doFilter(requisicaoCom("Bearer " + TokensDeTeste.admin()), new MockHttpServletResponse(), cadeia);

		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		assertThat(autenticacao.getPrincipal()).isInstanceOf(OperadorAutenticado.class);
		assertThat(autenticacao.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
		assertThat(cadeia.getRequest()).isNotNull();
	}

	@Test
	void semCabecalhoSegueSemAutenticacao() throws Exception {
		MockFilterChain cadeia = new MockFilterChain();

		filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), cadeia);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(cadeia.getRequest()).isNotNull();
	}

	@Test
	void cabecalhoQueNaoEBearerSegueSemAutenticacao() throws Exception {
		filtro.doFilter(requisicaoCom("Basic dXNlcjpzZW5oYQ=="), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void tokenInvalidoSegueSemAutenticacao() throws Exception {
		MockFilterChain cadeia = new MockFilterChain();

		filtro.doFilter(requisicaoCom("Bearer " + TokensDeTeste.clienteExpirado(1L, "12345678909")),
				new MockHttpServletResponse(), cadeia);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(cadeia.getRequest()).isNotNull();
	}

	@Test
	void naoSubstituiUmaAutenticacaoQueJaExiste() throws Exception {
		TestingAuthenticationToken existente = new TestingAuthenticationToken("ja-autenticado", null);
		SecurityContextHolder.getContext().setAuthentication(existente);

		filtro.doFilter(requisicaoCom("Bearer " + TokensDeTeste.admin()), new MockHttpServletResponse(),
				new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existente);
	}

	private MockHttpServletRequest requisicaoCom(String authorization) {
		MockHttpServletRequest requisicao = new MockHttpServletRequest();
		requisicao.addHeader("Authorization", authorization);
		return requisicao;
	}
}
