package br.com.oficina.billing.mensageria.output.outbox;

import br.com.oficina.billing.mensageria.config.MensageriaProperties;
import br.com.oficina.billing.mensageria.output.outbox.Broker.FalhaPublicacaoException;
import br.com.oficina.billing.mensageria.output.outbox.Broker.MensagemSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * Envia os eventos pendentes do outbox. Cada evento só é marcado como enviado depois da confirmação
 * do broker; se o envio falhar, a tentativa é registrada e a rodada para ali, para não inverter a
 * ordem dos eventos.
 *
 * <p>A entrega é "pelo menos uma vez": se o processo cair entre a confirmação e o commit, o evento sai
 * de novo na próxima rodada, com o mesmo {@code messageId}, e o OS Service descarta a repetição.</p>
 */
@Component
public class OutboxEnvio {

	private static final int TAMANHO_MAXIMO_ERRO = 500;
	private static final Logger LOG = LoggerFactory.getLogger(OutboxEnvio.class);

	private final OutboxEventoRepository repository;
	private final Broker broker;
	private final TransactionTemplate transacao;
	private final MensageriaProperties propriedades;

	OutboxEnvio(OutboxEventoRepository repository, Broker broker, TransactionTemplate transacao,
			MensageriaProperties propriedades) {
		this.repository = repository;
		this.broker = broker;
		this.transacao = transacao;
		this.propriedades = propriedades;
	}

	/** Envia uma rodada de pendentes e devolve quantos o broker confirmou. */
	public int enviarPendentes() {
		Integer enviados = transacao.execute(status -> {
			int confirmados = 0;
			for (OutboxEvento evento : repository.travarPendentes(propriedades.outbox().lote())) {
				try {
					broker.enviar(new MensagemSaida(evento.getMessageId(), evento.getTipo(), evento.getExchange(),
							evento.getRoutingKey(), evento.getOrdemServicoId(), evento.getCorrelationId(),
							evento.getPayload(), evento.getCriadoEm()));
				} catch (FalhaPublicacaoException e) {
					registrarFalha(evento, e);
					break;
				}
				evento.setPublicadoEm(LocalDateTime.now());
				evento.setUltimoErro(null);
				confirmados++;
				LOG.info("outbox_enviado tipo={} messageId={} sagaId={}",
						evento.getTipo(), evento.getMessageId(), evento.getSagaId());
			}
			return confirmados;
		});
		return enviados == null ? 0 : enviados;
	}

	private static void registrarFalha(OutboxEvento evento, RuntimeException erro) {
		evento.setTentativas(evento.getTentativas() + 1);
		String mensagem = String.valueOf(erro.getMessage());
		evento.setUltimoErro(mensagem.length() > TAMANHO_MAXIMO_ERRO ? mensagem.substring(0, TAMANHO_MAXIMO_ERRO) : mensagem);
		LOG.warn("outbox_falha_envio tipo={} messageId={} tentativas={}",
				evento.getTipo(), evento.getMessageId(), evento.getTentativas());
	}
}
