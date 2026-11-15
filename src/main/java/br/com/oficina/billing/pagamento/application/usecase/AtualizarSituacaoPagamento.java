package br.com.oficina.billing.pagamento.application.usecase;

import br.com.oficina.billing.pagamento.application.dto.PagamentoResponse;
import br.com.oficina.billing.pagamento.application.evento.NotificadorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.PagamentoGateway;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento;
import br.com.oficina.billing.pagamento.application.gateway.ProvedorPagamento.SituacaoCobranca;
import br.com.oficina.billing.pagamento.domain.Pagamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Reage ao aviso do provedor de que uma cobrança mudou. O aviso só diz qual cobrança: a situação vem
 * de uma consulta ao provedor, então um aviso forjado ou repetido nunca confirma pagamento sozinho.
 * Quando o pagamento muda, o resultado vai para o OS Service.
 */
@Service
public class AtualizarSituacaoPagamento {

	private final PagamentoGateway pagamentoGateway;
	private final ProvedorPagamento provedor;
	private final NotificadorPagamento notificador;

	public AtualizarSituacaoPagamento(PagamentoGateway pagamentoGateway, ProvedorPagamento provedor,
			NotificadorPagamento notificador) {
		this.pagamentoGateway = pagamentoGateway;
		this.provedor = provedor;
		this.notificador = notificador;
	}

	/** Vazio quando a cobrança não é de nenhum pagamento do Billing (aviso de outra integração da conta). */
	@Transactional
	public Optional<PagamentoResponse> executar(String idCobranca) {
		Optional<Pagamento> encontrado = pagamentoGateway.buscarPorIdCobranca(idCobranca);
		if (encontrado.isEmpty()) {
			return Optional.empty();
		}
		Pagamento pagamento = encontrado.get();

		SituacaoCobranca situacao = provedor.consultar(idCobranca);
		boolean mudou = switch (situacao.situacao()) {
			case APROVADA -> pagamento.confirmar(situacao.metodo(), situacao.resposta());
			case RECUSADA -> pagamento.recusar(situacao.detalhe(), situacao.resposta());
			// Estorno só acontece pelo comando da Saga, que já registra o resultado
			case PENDENTE, ESTORNADA -> false;
		};
		if (!mudou) {
			return Optional.of(PagamentoResponse.de(pagamento));
		}

		Pagamento gravado = pagamentoGateway.salvar(pagamento);
		notificador.notificarResultado(gravado);
		return Optional.of(PagamentoResponse.de(gravado));
	}
}
