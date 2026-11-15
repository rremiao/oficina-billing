package br.com.oficina.billing.orcamento.output.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Orçamento gravado de uma vez e nunca alterado: sem data de atualização. */
@Getter
@Setter
@Entity
@Table(name = "orcamento")
public class OrcamentoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "saga_id", nullable = false, updatable = false)
	private UUID sagaId;

	@Column(name = "ordem_servico_id", nullable = false, updatable = false)
	private Long ordemServicoId;

	@Column(name = "codigo_ordem_servico", length = 20, updatable = false)
	private String codigoOrdemServico;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private Long clienteId;

	@Column(name = "veiculo_id", updatable = false)
	private Long veiculoId;

	@Column(name = "valor_servicos", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valorServicos;

	@Column(name = "valor_itens", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valorItens;

	@Column(name = "valor_total", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valorTotal;

	@Column(name = "data_criacao", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "orcamento_id", nullable = false, updatable = false)
	@OrderBy("id")
	private List<OrcamentoServicoEntity> servicos = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "orcamento_id", nullable = false, updatable = false)
	@OrderBy("id")
	private List<OrcamentoItemEntity> itens = new ArrayList<>();

	@PrePersist
	void aoCriar() {
		this.dataCriacao = LocalDateTime.now();
	}
}
