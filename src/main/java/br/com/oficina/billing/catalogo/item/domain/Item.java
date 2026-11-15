package br.com.oficina.billing.catalogo.item.domain;

import br.com.oficina.billing.shared.domain.Dinheiro;
import br.com.oficina.billing.shared.domain.Texto;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Peça ou insumo do catálogo, com o preço de venda. O saldo em estoque é do serviço de Execução: os
 * dois serviços falam do mesmo item pelo id.
 */
public final class Item {

	private static final String CAMPO_VALOR = "Valor unitário";

	private final Long id;
	private String nome;
	private String descricao;
	private TipoItem tipo;
	private BigDecimal valorUnitario;
	private String unidadeMedida;
	private boolean ativo;
	private final LocalDateTime dataCriacao;
	private final LocalDateTime dataAtualizacao;

	private Item(Dados dados, Long id, boolean ativo, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
		this.id = id;
		aplicar(dados);
		this.ativo = ativo;
		this.dataCriacao = dataCriacao;
		this.dataAtualizacao = dataAtualizacao;
	}

	public static Item novo(Dados dados) {
		return new Item(dados, null, true, null, null);
	}

	/** Reconstrói um item já gravado. */
	public static Item existente(Long id, Dados dados, boolean ativo, LocalDateTime dataCriacao,
			LocalDateTime dataAtualizacao) {
		return new Item(dados, id, ativo, dataCriacao, dataAtualizacao);
	}

	/** Mudar o preço não muda orçamento já gerado: o orçamento guarda o valor da época. */
	public void atualizar(Dados dados) {
		aplicar(dados);
	}

	/** Item inativo continua no histórico, mas não entra em orçamento novo. */
	public void inativar() {
		this.ativo = false;
	}

	private void aplicar(Dados dados) {
		if (dados.tipo() == null) {
			throw new RegraNegocioException(ExceptionMessage.REQUIRED_TYPE);
		}
		this.nome = Texto.obrigatorio(dados.nome(), ExceptionMessage.REQUIRED_NAME);
		this.descricao = Texto.opcional(dados.descricao());
		this.tipo = dados.tipo();
		this.valorUnitario = Dinheiro.exigirPositivo(dados.valorUnitario(), CAMPO_VALOR);
		this.unidadeMedida = Texto.obrigatorio(dados.unidadeMedida(), ExceptionMessage.REQUIRED_UNIT);
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

	public TipoItem getTipo() {
		return tipo;
	}

	public BigDecimal getValorUnitario() {
		return valorUnitario;
	}

	public String getUnidadeMedida() {
		return unidadeMedida;
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

	/** Campos editáveis do item, agrupados para não passar seis argumentos soltos. */
	public record Dados(String nome, String descricao, TipoItem tipo, BigDecimal valorUnitario, String unidadeMedida) {
	}
}
