package br.com.oficina.billing.pagamento.domain;

import br.com.oficina.billing.shared.domain.Dinheiro;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pagamento de um orçamento, um por orçamento. As transições devolvem {@code true} quando mudam o
 * status e {@code false} quando o pagamento já estava lá: o provedor avisa a mesma coisa mais de uma
 * vez, e repetir não pode ter efeito.
 */
public final class Pagamento {

	private final Long id;
	private final Long orcamentoId;
	private final Long clienteId;
	private final Origem origem;
	private final BigDecimal valor;
	private final String idCobranca;
	private final String linkPagamento;
	private StatusPagamento status;
	private String metodo;
	private String motivo;
	private String idEstorno;
	private BigDecimal valorEstornado;
	private String ultimaRespostaProvedor;
	private final LocalDateTime dataCriacao;
	private final LocalDateTime dataAtualizacao;

	private Pagamento(Dados dados) {
		this.id = dados.id();
		this.orcamentoId = Objects.requireNonNull(dados.orcamentoId());
		this.clienteId = Objects.requireNonNull(dados.clienteId());
		this.origem = Objects.requireNonNull(dados.origem());
		this.valor = Dinheiro.exigirPositivo(dados.valor(), "Valor do pagamento");
		this.idCobranca = Objects.requireNonNull(dados.idCobranca());
		this.linkPagamento = dados.linkPagamento();
		this.status = Objects.requireNonNull(dados.status());
		this.metodo = dados.metodo();
		this.motivo = dados.motivo();
		this.idEstorno = dados.idEstorno();
		this.valorEstornado = dados.valorEstornado();
		this.ultimaRespostaProvedor = dados.ultimaRespostaProvedor();
		this.dataCriacao = dados.dataCriacao();
		this.dataAtualizacao = dados.dataAtualizacao();
	}

	/** Pagamento recém-criado no provedor, esperando o cliente pagar. */
	public static Pagamento aguardando(Long orcamentoId, Long clienteId, Origem origem, BigDecimal valor,
			String idCobranca, String linkPagamento, String respostaProvedor) {
		return new Pagamento(new Dados(null, orcamentoId, clienteId, origem, valor, idCobranca, linkPagamento,
				StatusPagamento.AGUARDANDO_PAGAMENTO, null, null, null, null, respostaProvedor, null, null));
	}

	/** Reconstrói um pagamento já gravado. */
	public static Pagamento existente(Dados dados) {
		return new Pagamento(dados);
	}

	public boolean confirmar(String metodoPagamento, String respostaProvedor) {
		if (status != StatusPagamento.AGUARDANDO_PAGAMENTO) {
			return false;
		}
		this.status = StatusPagamento.CONFIRMADO;
		this.metodo = metodoPagamento;
		this.ultimaRespostaProvedor = respostaProvedor;
		return true;
	}

	public boolean recusar(String motivoRecusa, String respostaProvedor) {
		if (status != StatusPagamento.AGUARDANDO_PAGAMENTO) {
			return false;
		}
		this.status = StatusPagamento.RECUSADO;
		this.motivo = motivoRecusa;
		this.ultimaRespostaProvedor = respostaProvedor;
		return true;
	}

	/** Se precisa chamar o provedor para estornar. Já estornado não precisa; os outros status não podem. */
	public boolean precisaEstornar() {
		if (status == StatusPagamento.ESTORNADO) {
			return false;
		}
		if (status != StatusPagamento.CONFIRMADO) {
			throw new RegraNegocioException(ExceptionMessage.REFUND_NOT_ALLOWED.formatted(id, status));
		}
		return true;
	}

	/** Estorno total. Sem valor informado pelo provedor, vale o valor pago. */
	public void registrarEstorno(String idEstornoProvedor, BigDecimal valorDevolvido, String motivoEstorno,
			String respostaProvedor) {
		if (precisaEstornar()) {
			this.status = StatusPagamento.ESTORNADO;
			this.idEstorno = idEstornoProvedor;
			this.valorEstornado = valorDevolvido == null ? valor : valorDevolvido;
			this.motivo = motivoEstorno;
			this.ultimaRespostaProvedor = respostaProvedor;
		}
	}

	public boolean pertenceAoCliente(Long outroClienteId) {
		return clienteId.equals(outroClienteId);
	}

	public Long getId() {
		return id;
	}

	public Long getOrcamentoId() {
		return orcamentoId;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public Origem getOrigem() {
		return origem;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public String getIdCobranca() {
		return idCobranca;
	}

	public String getLinkPagamento() {
		return linkPagamento;
	}

	public StatusPagamento getStatus() {
		return status;
	}

	public String getMetodo() {
		return metodo;
	}

	public String getMotivo() {
		return motivo;
	}

	public String getIdEstorno() {
		return idEstorno;
	}

	public BigDecimal getValorEstornado() {
		return valorEstornado;
	}

	public String getUltimaRespostaProvedor() {
		return ultimaRespostaProvedor;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public LocalDateTime getDataAtualizacao() {
		return dataAtualizacao;
	}

	/**
	 * A que ciclo da Saga o pagamento responde: os eventos de pagamento voltam para o OS Service com
	 * estes dados, mesmo quando disparados depois pelo webhook do provedor.
	 *
	 * @param sagaId             ciclo da Saga do orçamento
	 * @param ordemServicoId     id da OS no OS Service
	 * @param codigoOrdemServico código legível da OS, opcional
	 * @param correlationId      id da requisição original, para seguir o fluxo nos logs
	 */
	public record Origem(UUID sagaId, Long ordemServicoId, String codigoOrdemServico, String correlationId) {

		public Origem {
			Objects.requireNonNull(sagaId);
			Objects.requireNonNull(ordemServicoId);
		}
	}

	/** Todos os campos de um pagamento já gravado. */
	public record Dados(Long id, Long orcamentoId, Long clienteId, Origem origem, BigDecimal valor, String idCobranca,
			String linkPagamento, StatusPagamento status, String metodo, String motivo, String idEstorno,
			BigDecimal valorEstornado, String ultimaRespostaProvedor, LocalDateTime dataCriacao,
			LocalDateTime dataAtualizacao) {
	}
}
