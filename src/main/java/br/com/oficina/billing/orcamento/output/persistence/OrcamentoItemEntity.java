package br.com.oficina.billing.orcamento.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "orcamento_item")
public class OrcamentoItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "item_id", nullable = false, updatable = false)
	private Long itemId;

	@Column(name = "nome", nullable = false, length = 120, updatable = false)
	private String nome;

	@Column(name = "quantidade", nullable = false, updatable = false)
	private int quantidade;

	@Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valorUnitario;

	@Column(name = "valor_total", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valorTotal;
}
