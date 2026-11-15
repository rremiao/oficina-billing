package br.com.oficina.billing.pagamento.output.persistence;

import br.com.oficina.billing.pagamento.domain.StatusPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pagamento")
public class PagamentoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "orcamento_id", nullable = false, updatable = false)
	private Long orcamentoId;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private Long clienteId;

	@Column(name = "saga_id", nullable = false, updatable = false)
	private UUID sagaId;

	@Column(name = "ordem_servico_id", nullable = false, updatable = false)
	private Long ordemServicoId;

	@Column(name = "codigo_ordem_servico", length = 20, updatable = false)
	private String codigoOrdemServico;

	@Column(name = "correlation_id", length = 128, updatable = false)
	private String correlationId;

	@Column(name = "valor", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valor;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 25)
	private StatusPagamento status;

	@Column(name = "id_cobranca", nullable = false, length = 64, updatable = false)
	private String idCobranca;

	@Column(name = "link_pagamento", length = 500, updatable = false)
	private String linkPagamento;

	@Column(name = "metodo", length = 40)
	private String metodo;

	@Column(name = "motivo", length = 255)
	private String motivo;

	@Column(name = "id_estorno", length = 64)
	private String idEstorno;

	@Column(name = "valor_estornado", precision = 10, scale = 2)
	private BigDecimal valorEstornado;

	/** JSON cru do provedor; o cast grava como {@code jsonb} sem passar por mapeador de JSON do Hibernate. */
	@ColumnTransformer(write = "?::jsonb")
	@Column(name = "resposta_provedor", columnDefinition = "jsonb")
	private String respostaProvedor;

	@Version
	@Column(name = "versao", nullable = false)
	private long versao;

	@Column(name = "data_criacao", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	@Column(name = "data_atualizacao")
	private LocalDateTime dataAtualizacao;

	@PrePersist
	void aoCriar() {
		this.dataCriacao = LocalDateTime.now();
	}

	@PreUpdate
	void aoAtualizar() {
		this.dataAtualizacao = LocalDateTime.now();
	}
}
