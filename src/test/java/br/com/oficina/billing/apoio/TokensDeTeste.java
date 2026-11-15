package br.com.oficina.billing.apoio;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Reproduz os tokens emitidos fora do Billing: o login de funcionários do OS Service e a Lambda de
 * autenticação por CPF. As claims são escritas literalmente, e não a partir das constantes do
 * JwtService, porque os emissores são externos: se o contrato mudar de um lado só, o teste falha.
 */
public final class TokensDeTeste {

	/** Mesmo valor do default de {@code security.jwt.secret} no application.properties. */
	public static final String SEGREDO = "jwt-local-secret-123456789012345678901234567890";

	public static final String OUTRO_SEGREDO = "outro-segredo-123456789012345678901234567890";

	private static final long DUAS_HORAS = 7_200_000L;

	private TokensDeTeste() {
	}

	public static String operador(String email, String role) {
		return assinar(base(email).claim("tipo", "OPERADOR").claim("role", role), SEGREDO);
	}

	/** Token de funcionário emitido antes de a claim {@code tipo} existir. */
	public static String operadorSemTipo(String email, String role) {
		return assinar(base(email).claim("role", role), SEGREDO);
	}

	public static String admin() {
		return operador("admin@oficina.com", "ADMIN");
	}

	public static String atendente() {
		return operador("atendente@oficina.com", "ATENDENTE");
	}

	public static String cliente(Long clienteId, String cpf, String nome) {
		return assinar(clienteBase(clienteId, cpf, nome), SEGREDO);
	}

	public static String clienteComOutroSegredo(Long clienteId, String cpf) {
		return assinar(clienteBase(clienteId, cpf, "Cliente"), OUTRO_SEGREDO);
	}

	public static String clienteExpirado(Long clienteId, String cpf) {
		JwtBuilder builder = clienteBase(clienteId, cpf, "Cliente")
				.issuedAt(new Date(System.currentTimeMillis() - 2 * DUAS_HORAS))
				.expiration(new Date(System.currentTimeMillis() - DUAS_HORAS));
		return assinar(builder, SEGREDO);
	}

	/** Assina um token com as claims que o teste quiser, para cenários de claim ausente ou inválida. */
	public static String comClaims(JwtBuilder builder) {
		return assinar(builder, SEGREDO);
	}

	public static JwtBuilder base(String subject) {
		return Jwts.builder()
				.subject(subject)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + DUAS_HORAS));
	}

	public static String bearer(String token) {
		return "Bearer " + token;
	}

	private static JwtBuilder clienteBase(Long clienteId, String cpf, String nome) {
		return base(cpf)
				.claim("tipo", "CLIENTE")
				.claim("clienteId", clienteId)
				.claim("nome", nome)
				.claim("role", "CLIENTE");
	}

	private static String assinar(JwtBuilder builder, String segredo) {
		return builder
				.signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
				.compact();
	}
}
