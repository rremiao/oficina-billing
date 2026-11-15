package br.com.oficina.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Sem o usuário em memória padrão do Spring Boot: toda autenticação vem do JWT, e esse usuário com
 * senha gerada não teria uso nenhum.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableFeignClients(basePackages = "br.com.oficina.billing.pagamento.output.mercadopago")
public class BillingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingApplication.class, args);
	}
}
