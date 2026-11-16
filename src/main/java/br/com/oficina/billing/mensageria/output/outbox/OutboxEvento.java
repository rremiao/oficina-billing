package br.com.oficina.billing.mensageria.output.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evento gravado na mesma transação da mudança que o provocou, à espera de envio. */
@Getter
@Setter
@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", nullable = false, unique = true, updatable = false)
	private UUID messageId;

	@Column(name = "tipo", nullable = false, length = 60, updatable = false)
	private String tipo;

	@Column(name = "exchange", nullable = false, length = 100, updatable = false)
	private String exchange;

	@Column(name = "routing_key", nullable = false, length = 100, updatable = false)
	private String routingKey;

	@Column(name = "saga_id", nullable = false, updatable = false)
	private UUID sagaId;

	@Column(name = "ordem_servico_id", nullable = false, updatable = false)
	private Long ordemServicoId;

	@Column(name = "correlation_id", nullable = false, length = 128, updatable = false)
	private String correlationId;

	@Column(name = "payload", nullable = false, columnDefinition = "TEXT", updatable = false)
	private String payload;

	/** Cabeçalhos do trace da transação que gravou o evento, em JSON (nulo sem trace ativo). */
	@Column(name = "rastreamento", columnDefinition = "TEXT", updatable = false)
	private String rastreamento;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	@Column(name = "publicado_em")
	private LocalDateTime publicadoEm;

	@Column(name = "tentativas", nullable = false)
	private int tentativas;

	@Column(name = "ultimo_erro", length = 500)
	private String ultimoErro;

	@PrePersist
	void aoCriar() {
		this.criadoEm = LocalDateTime.now();
	}
}
