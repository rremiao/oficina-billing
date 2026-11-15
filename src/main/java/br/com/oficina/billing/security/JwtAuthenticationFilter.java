package br.com.oficina.billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o {@code Authorization: Bearer} e coloca a autenticação no contexto. Não é um bean: o
 * SecurityConfig o instancia, para o Spring Boot não registrá-lo também como filtro do servlet.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String PREFIXO_BEARER = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String cabecalho = request.getHeader("Authorization");

		if (cabecalho != null && cabecalho.startsWith(PREFIXO_BEARER)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			jwtService.autenticar(cabecalho.substring(PREFIXO_BEARER.length())).ifPresent(autenticacao -> {
				autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(autenticacao);
			});
		}

		filterChain.doFilter(request, response);
	}
}
