package br.com.oficina.billing.config;

import br.com.oficina.billing.security.JwtAuthenticationFilter;
import br.com.oficina.billing.security.JwtService;
import br.com.oficina.billing.security.PapelOperador;
import br.com.oficina.billing.security.RespostaErroSeguranca;
import br.com.oficina.billing.security.TipoToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Toda rota nova nasce restrita a funcionários (ADMIN e ATENDENTE). Abrir uma rota ao token de
 * cliente exige conferir a posse do recurso no caso de uso e um teste de integração.
 */
@Configuration
public class SecurityConfig {

	private static final String ADMIN = PapelOperador.ADMIN.name();
	private static final String ATENDENTE = PapelOperador.ATENDENTE.name();
	private static final String CLIENTE = TipoToken.CLIENTE.name();

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			RespostaErroSeguranca respostaErro) {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(respostaErro)
						.accessDeniedHandler(respostaErro))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/api-docs/**",
								"/actuator/health",
								"/actuator/health/**",
								// O despacho de erro não passa pelo filtro JWT; sem liberar /error, todo erro
								// (404 de rota inexistente, 500) chegaria ao cliente como 401.
								"/error"
						).permitAll()
						// Aviso do Mercado Pago: sem JWT, a origem é conferida pela assinatura x-signature
						.requestMatchers(HttpMethod.POST, "/pagamentos/webhook/mercadopago").permitAll()
						// Rotas abertas ao token de cliente: a posse é conferida nos casos de uso de consulta
						.requestMatchers(HttpMethod.GET, "/orcamentos/**", "/pagamentos/**")
						.hasAnyRole(ADMIN, ATENDENTE, CLIENTE)
						.anyRequest().hasAnyRole(ADMIN, ATENDENTE))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
