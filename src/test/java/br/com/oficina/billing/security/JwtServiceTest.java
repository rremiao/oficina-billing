package br.com.oficina.billing.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.apoio.TokensDeTeste;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;

class JwtServiceTest {

	private final JwtService jwtService = new JwtService(TokensDeTeste.SEGREDO);

	@Test
	void tokenDeAdminAutenticaComoOperadorComRoleAdmin() {
		UsernamePasswordAuthenticationToken autenticacao = jwtService.autenticar(TokensDeTeste.admin()).orElseThrow();

		assertThat(autenticacao.getPrincipal())
				.isEqualTo(new OperadorAutenticado("admin@oficina.com", PapelOperador.ADMIN));
		assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ADMIN");
	}

	@Test
	void tokenDeAtendenteAutenticaComRoleAtendente() {
		UsernamePasswordAuthenticationToken autenticacao =
				jwtService.autenticar(TokensDeTeste.atendente()).orElseThrow();

		assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ATENDENTE");
	}

	@Test
	void tokenDeFuncionarioSemClaimTipoContinuaValendoComoOperador() {
		Optional<UsernamePasswordAuthenticationToken> autenticacao =
				jwtService.autenticar(TokensDeTeste.operadorSemTipo("antigo@oficina.com", "ATENDENTE"));

		assertThat(autenticacao).get()
				.extracting(UsernamePasswordAuthenticationToken::getPrincipal)
				.isEqualTo(new OperadorAutenticado("antigo@oficina.com", PapelOperador.ATENDENTE));
	}

	@Test
	void tokenDeFuncionarioComPapelDesconhecidoNaoAutentica() {
		assertThat(jwtService.autenticar(TokensDeTeste.operador("x@oficina.com", "MECANICO"))).isEmpty();
	}

	@Test
	void tokenDeFuncionarioComRoleClienteNaoAutentica() {
		assertThat(jwtService.autenticar(TokensDeTeste.operador("x@oficina.com", "CLIENTE"))).isEmpty();
	}

	@Test
	void tokenDeFuncionarioSemRoleNaoAutentica() {
		String token = TokensDeTeste.comClaims(TokensDeTeste.base("x@oficina.com").claim("tipo", "OPERADOR"));

		assertThat(jwtService.autenticar(token)).isEmpty();
	}

	@Test
	void tokenDeFuncionarioSemSubjectNaoAutentica() {
		String token = TokensDeTeste.comClaims(TokensDeTeste.base(null).claim("tipo", "OPERADOR").claim("role", "ADMIN"));

		assertThat(jwtService.autenticar(token)).isEmpty();
	}

	@Test
	void tokenDeClienteAutenticaSempreComoClienteComOsDadosDasClaims() {
		UsernamePasswordAuthenticationToken autenticacao =
				jwtService.autenticar(TokensDeTeste.cliente(7L, "12345678909", "Maria")).orElseThrow();

		assertThat(autenticacao.getPrincipal()).isEqualTo(new ClienteAutenticado(7L, "12345678909", "Maria"));
		assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_CLIENTE");
	}

	@Test
	void tokenDeClienteComRoleAdminNaoGanhaPrivilegioDeFuncionario() {
		String token = TokensDeTeste.comClaims(TokensDeTeste.base("12345678909")
				.claim("tipo", "CLIENTE").claim("clienteId", 7).claim("role", "ADMIN"));

		assertThat(jwtService.autenticar(token)).get()
				.extracting(a -> a.getAuthorities().iterator().next().getAuthority())
				.isEqualTo("ROLE_CLIENTE");
	}

	@Test
	void tokenDeClienteSemClienteIdNaoAutentica() {
		String token = TokensDeTeste.comClaims(TokensDeTeste.base("12345678909").claim("tipo", "CLIENTE"));

		assertThat(jwtService.autenticar(token)).isEmpty();
	}

	@Test
	void tokenDeClienteSemSubjectNaoAutentica() {
		String token = TokensDeTeste.comClaims(TokensDeTeste.base(null).claim("tipo", "CLIENTE").claim("clienteId", 7));

		assertThat(jwtService.autenticar(token)).isEmpty();
	}

	@Test
	void tokenComTipoDesconhecidoNaoAutentica() {
		String token = TokensDeTeste.comClaims(TokensDeTeste.base("x").claim("tipo", "SISTEMA").claim("role", "ADMIN"));

		assertThat(jwtService.autenticar(token)).isEmpty();
	}

	@Test
	void tokenExpiradoNaoAutentica() {
		assertThat(jwtService.autenticar(TokensDeTeste.clienteExpirado(7L, "12345678909"))).isEmpty();
	}

	@Test
	void tokenAssinadoComOutroSegredoNaoAutentica() {
		assertThat(jwtService.autenticar(TokensDeTeste.clienteComOutroSegredo(7L, "12345678909"))).isEmpty();
	}

	@Test
	void textoQueNaoEJwtNaoAutentica() {
		assertThat(jwtService.autenticar("nao-e-um-jwt")).isEmpty();
	}

	@Test
	void segredoCurtoDemaisImpedeASubida() {
		assertThatThrownBy(() -> new JwtService("curto")).isInstanceOf(WeakKeyException.class);
	}
}
