package br.com.oficina.billing.mensageria.output.outbox;

import br.com.oficina.billing.mensageria.config.MensageriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Dispara o envio do outbox em intervalo fixo e, uma vez por dia, remove eventos já enviados mais
 * antigos que a retenção (pendentes nunca são removidos). Nos testes de integração fica desligado, e o
 * envio é chamado sob demanda.
 */
@Component
@ConditionalOnProperty(name = "oficina.mensageria.outbox.agendamento-habilitado", havingValue = "true",
		matchIfMissing = true)
class OutboxAgendador {

	private static final Logger LOG = LoggerFactory.getLogger(OutboxAgendador.class);

	private final OutboxEnvio envio;
	private final OutboxEventoRepository repository;
	private final MensageriaProperties propriedades;

	OutboxAgendador(OutboxEnvio envio, OutboxEventoRepository repository, MensageriaProperties propriedades) {
		this.envio = envio;
		this.repository = repository;
		this.propriedades = propriedades;
	}

	@Scheduled(fixedDelayString = "${oficina.mensageria.outbox.intervalo-ms:1000}")
	public void enviar() {
		envio.enviarPendentes();
	}

	@Scheduled(cron = "0 30 3 * * *")
	@Transactional
	public void limpar() {
		int removidos = repository.removerEnviadosAntesDe(
				LocalDateTime.now().minusDays(propriedades.outbox().retencaoDias()));
		LOG.info("outbox_limpeza removidos={}", removidos);
	}
}
