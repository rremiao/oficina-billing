package br.com.oficina.billing.pagamento.application.evento;

import br.com.oficina.billing.mensageria.application.EventoSaga;
import br.com.oficina.billing.mensageria.application.PublicadorEventos;
import br.com.oficina.billing.mensageria.application.TipoEvento;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Traduz o status final do pagamento no evento para o OS Service: confirmado, recusado ou estornado.
 * Pagamento aguardando não gera evento. A chave do evento é o id do pagamento, então notificar de novo
 * publica o mesmo {@code messageId}.
 */
@Component
public class NotificadorPagamento {

	private final PublicadorEventos publicador;

	public NotificadorPagamento(PublicadorEventos publicador) {
		this.publicador = publicador;
	}

	public void notificarResultado(Pagamento pagamento) {
		switch (pagamento.getStatus()) {
			case CONFIRMADO -> publicar(pagamento, TipoEvento.PAGAMENTO_CONFIRMADO, new PagamentoConfirmadoDados(
					pagamento.getId(), pagamento.getOrcamentoId(), pagamento.getValor(), pagamento.getMetodo()));
			case RECUSADO -> publicar(pagamento, TipoEvento.PAGAMENTO_RECUSADO,
					new PagamentoRecusadoDados(pagamento.getOrcamentoId(), pagamento.getMotivo()));
			case ESTORNADO -> publicar(pagamento, TipoEvento.PAGAMENTO_ESTORNADO, new PagamentoEstornadoDados(
					pagamento.getId(), pagamento.getIdEstorno(), pagamento.getValorEstornado()));
			default -> {
				// AGUARDANDO_PAGAMENTO: ainda sem resultado, o evento sai quando o provedor avisar
			}
		}
	}

	private void publicar(Pagamento pagamento, TipoEvento tipo, Object dados) {
		Pagamento.Origem origem = pagamento.getOrigem();
		publicador.publicar(new EventoSaga(tipo, pagamento.getId().toString(), origem.sagaId(), origem.ordemServicoId(),
				origem.codigoOrdemServico(), origem.correlationId(), dados));
	}

	public record PagamentoConfirmadoDados(Long pagamentoId, Long orcamentoId, BigDecimal valorPago, String metodo) {
	}

	public record PagamentoRecusadoDados(Long orcamentoId, String motivo) {
	}

	public record PagamentoEstornadoDados(Long pagamentoId, String refundId, BigDecimal valorEstornado) {
	}
}
