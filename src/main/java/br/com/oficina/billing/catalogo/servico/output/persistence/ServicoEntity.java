package br.com.oficina.billing.catalogo.servico.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "servico")
public class ServicoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "descricao", length = 255)
	private String descricao;

	@Column(name = "valor", nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(name = "ativo", nullable = false)
	private boolean ativo;

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
