package br.com.oficina.billing.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Responde em {@code application/problem+json} as recusas da camada de segurança, que acontecem
 * antes do Spring MVC e por isso não passam pelo HandlerGeralExcecoes: 401 sem token válido e 403
 * quando o perfil do token não tem acesso à rota.
 *
 * <p>Os corpos são texto fixo, sem {@code instance} (opcional na RFC 9457): a resposta não ecoa nada
 * que veio do cliente.</p>
 */
@Component
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

	private static final String NAO_AUTENTICADO = "{\"type\":\"about:blank\",\"title\":\"Nao autenticado\","
			+ "\"status\":401,\"detail\":\"Token ausente, inválido ou expirado\"}";

	private static final String ACESSO_NEGADO = "{\"type\":\"about:blank\",\"title\":\"Acesso negado\","
			+ "\"status\":403,\"detail\":\"O perfil do token não tem acesso a este recurso\"}";

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException excecao) throws IOException {
		responder(response, HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException excecao) throws IOException {
		responder(response, HttpStatus.FORBIDDEN, ACESSO_NEGADO);
	}

	private void responder(HttpServletResponse response, HttpStatus status, String corpo) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(corpo);
	}
}
