package br.com.oficina.billing.orcamento.domain;

import br.com.oficina.billing.shared.domain.Dinheiro;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orçamento de uma OS em um ciclo da Saga. Os totais saem sempre das linhas: não existe orçamento
 * com total que não bate com a soma.
 */
public final class Orcamento {

	private final Long id;
	private final Cabecalho cabecalho;
	private final List<LinhaServico> servicos;
	private final List<LinhaItem> itens;
	private final BigDecimal valorServicos;
	private final BigDecimal valorItens;
	private final BigDecimal valorTotal;
	private final LocalDateTime dataCriacao;

	private Orcamento(Long id, Cabecalho cabecalho, List<LinhaServico> servicos, List<LinhaItem> itens,
			LocalDateTime dataCriacao) {
		this.id = id;
		this.cabecalho = Objects.requireNonNull(cabecalho);
		this.servicos = List.copyOf(servicos);
		this.itens = List.copyOf(itens);
		if (this.servicos.isEmpty() && this.itens.isEmpty()) {
			throw new RegraNegocioException(ExceptionMessage.QUOTE_WITHOUT_LINES);
		}
		this.valorServicos = somar(this.servicos.stream().map(LinhaServico::valorUnitario).toList(), "Valor dos serviços");
		this.valorItens = somar(this.itens.stream().map(LinhaItem::valorTotal).toList(), "Valor dos itens");
		this.valorTotal = Dinheiro.exigirDentroDoLimite(valorServicos.add(valorItens), "Valor total do orçamento");
		this.dataCriacao = dataCriacao;
	}

	public static Orcamento gerar(Cabecalho cabecalho, List<LinhaServico> servicos, List<LinhaItem> itens) {
		return new Orcamento(null, cabecalho, servicos, itens, null);
	}

	/** Reconstrói um orçamento já gravado. */
	public static Orcamento existente(Long id, Cabecalho cabecalho, List<LinhaServico> servicos, List<LinhaItem> itens,
			LocalDateTime dataCriacao) {
		return new Orcamento(id, cabecalho, servicos, itens, dataCriacao);
	}

	public boolean pertenceAoCliente(Long clienteId) {
		return cabecalho.clienteId().equals(clienteId);
	}

	private static BigDecimal somar(List<BigDecimal> valores, String campo) {
		BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.UNNECESSARY);
		return Dinheiro.exigirDentroDoLimite(soma, campo);
	}

	public Long getId() {
		return id;
	}

	public Cabecalho getCabecalho() {
		return cabecalho;
	}

	public List<LinhaServico> getServicos() {
		return servicos;
	}

	public List<LinhaItem> getItens() {
		return itens;
	}

	public BigDecimal getValorServicos() {
		return valorServicos;
	}

	public BigDecimal getValorItens() {
		return valorItens;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	/**
	 * De qual OS e de qual ciclo da Saga é o orçamento. OS, cliente e veículo são ids do OS Service.
	 *
	 * @param sagaId             ciclo da Saga que pediu o orçamento; um orçamento por ciclo
	 * @param ordemServicoId     id da OS no OS Service
	 * @param codigoOrdemServico código legível da OS, opcional
	 * @param clienteId          dono da OS, usado para o cliente consultar só os próprios orçamentos
	 * @param veiculoId          veículo da OS, opcional
	 */
	public record Cabecalho(UUID sagaId, Long ordemServicoId, String codigoOrdemServico, Long clienteId,
			Long veiculoId) {

		public Cabecalho {
			if (sagaId == null) {
				throw new RegraNegocioException(ExceptionMessage.REQUIRED_SAGA);
			}
			if (ordemServicoId == null) {
				throw new RegraNegocioException(ExceptionMessage.REQUIRED_SERVICE_ORDER);
			}
			if (clienteId == null) {
				throw new RegraNegocioException(ExceptionMessage.REQUIRED_CUSTOMER);
			}
		}
	}
}
