package br.com.oficina.billing.catalogo.servico.domain;

import br.com.oficina.billing.shared.domain.Dinheiro;
import br.com.oficina.billing.shared.domain.Texto;
import br.com.oficina.billing.shared.exception.ExceptionMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Serviço do catálogo, com o preço cobrado no orçamento. */
public final class Servico {

	private static final String CAMPO_VALOR = "Valor";

	private final Long id;
	private String nome;
	private String descricao;
	private BigDecimal valor;
	private boolean ativo;
	private final LocalDateTime dataCriacao;
	private final LocalDateTime dataAtualizacao;

	private Servico(Long id, String nome, String descricao, BigDecimal valor, boolean ativo,
			LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
		this.id = id;
		this.nome = Texto.obrigatorio(nome, ExceptionMessage.REQUIRED_NAME);
		this.descricao = Texto.opcional(descricao);
		this.valor = Dinheiro.exigirPositivo(valor, CAMPO_VALOR);
		this.ativo = ativo;
		this.dataCriacao = dataCriacao;
		this.dataAtualizacao = dataAtualizacao;
	}

	public static Servico novo(String nome, String descricao, BigDecimal valor) {
		return new Servico(null, nome, descricao, valor, true, null, null);
	}

	/** Reconstrói um serviço já gravado. */
	public static Servico existente(Long id, String nome, String descricao, BigDecimal valor, boolean ativo,
			LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
		return new Servico(id, nome, descricao, valor, ativo, dataCriacao, dataAtualizacao);
	}

	/** Mudar o preço não muda orçamento já gerado: o orçamento guarda o valor da época. */
	public void atualizar(String nome, String descricao, BigDecimal valor) {
		this.nome = Texto.obrigatorio(nome, ExceptionMessage.REQUIRED_NAME);
		this.descricao = Texto.opcional(descricao);
		this.valor = Dinheiro.exigirPositivo(valor, CAMPO_VALOR);
	}

	/** Serviço inativo continua no histórico, mas não entra em orçamento novo. */
	public void inativar() {
		this.ativo = false;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public LocalDateTime getDataAtualizacao() {
		return dataAtualizacao;
	}
}
