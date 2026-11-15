package br.com.oficina.billing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Valida o JWT emitido pelo OS Service (funcionários) ou pela Lambda de autenticação por CPF
 * (clientes). O Billing só valida, não emite token, e não tem tabela de usuários: a autenticação
 * sai inteira das claims (ver docs/adr/0001-jwt-validado-sem-consulta-ao-banco.md).
 */
@Component
public class JwtService {

	static final String CLAIM_TIPO = "tipo";
	static final String CLAIM_ROLE = "role";
	static final String CLAIM_CLIENTE_ID = "clienteId";
	static final String CLAIM_NOME = "nome";

	private static final String PREFIXO_ROLE = "ROLE_";

	private final SecretKey chave;

	public JwtService(@Value("${security.jwt.secret}") String segredo) {
		this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Confere assinatura e expiração e monta a autenticação. Token inválido, expirado, assinado com
	 * outra chave ou com claims inesperadas devolve vazio, e a requisição segue sem autenticação.
	 */
	public Optional<UsernamePasswordAuthenticationToken> autenticar(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
			return switch (tipo(claims)) {
				case CLIENTE -> cliente(claims);
				case OPERADOR -> operador(claims);
			};
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/** Tokens emitidos antes da claim {@code tipo} existir só podiam ser de funcionário. */
	private TipoToken tipo(Claims claims) {
		String tipo = claims.get(CLAIM_TIPO, String.class);
		return tipo == null ? TipoToken.OPERADOR : TipoToken.valueOf(tipo);
	}

	private Optional<UsernamePasswordAuthenticationToken> operador(Claims claims) {
		String role = claims.get(CLAIM_ROLE, String.class);
		if (claims.getSubject() == null || role == null) {
			return Optional.empty();
		}
		PapelOperador papel = PapelOperador.valueOf(role);
		OperadorAutenticado operador = new OperadorAutenticado(claims.getSubject(), papel);
		return Optional.of(new UsernamePasswordAuthenticationToken(
				operador, null, List.of(new SimpleGrantedAuthority(PREFIXO_ROLE + papel.name()))));
	}

	/**
	 * A authority do cliente é sempre {@code ROLE_CLIENTE}, qualquer que seja a claim {@code role}:
	 * um token de cliente nunca concede privilégio de funcionário.
	 */
	private Optional<UsernamePasswordAuthenticationToken> cliente(Claims claims) {
		Number clienteId = claims.get(CLAIM_CLIENTE_ID, Number.class);
		if (clienteId == null || claims.getSubject() == null) {
			return Optional.empty();
		}
		ClienteAutenticado cliente = new ClienteAutenticado(
				clienteId.longValue(), claims.getSubject(), claims.get(CLAIM_NOME, String.class));
		return Optional.of(new UsernamePasswordAuthenticationToken(
				cliente, null, List.of(new SimpleGrantedAuthority(PREFIXO_ROLE + TipoToken.CLIENTE.name()))));
	}
}
