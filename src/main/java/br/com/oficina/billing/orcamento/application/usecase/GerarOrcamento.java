package br.com.oficina.billing.orcamento.application.usecase;

import br.com.oficina.billing.mensageria.application.EventoSaga;
import br.com.oficina.billing.mensageria.application.PublicadorEventos;
import br.com.oficina.billing.mensageria.application.TipoEvento;
import br.com.oficina.billing.orcamento.application.dto.OrcamentoResponse;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento.ItemSolicitado;
import br.com.oficina.billing.orcamento.application.evento.OrcamentoGeradoDados;
import br.com.oficina.billing.orcamento.application.gateway.CatalogoPrecos;
import br.com.oficina.billing.orcamento.application.gateway.CatalogoPrecos.Preco;
import br.com.oficina.billing.orcamento.application.gateway.OrcamentoGateway;
import br.com.oficina.billing.orcamento.domain.LinhaItem;
import br.com.oficina.billing.orcamento.domain.LinhaServico;
import br.com.oficina.billing.orcamento.domain.Orcamento;
import br.com.oficina.billing.shared.exception.ExceptionMessage;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Monta o orçamento do pedido do OS Service com os preços do catálogo e responde
 * {@code OrcamentoGerado}. É idempotente por {@code sagaId}: o mesmo pedido de novo (reenvio do OS ou
 * entrega repetida da mensagem) devolve o orçamento já gerado e responde de novo, com o mesmo
 * {@code messageId}, sem recalcular nem duplicar.
 */
@Service
public class GerarOrcamento {

	private final OrcamentoGateway orcamentoGateway;
	private final CatalogoPrecos catalogoPrecos;
	private final PublicadorEventos publicador;

	public GerarOrcamento(OrcamentoGateway orcamentoGateway, CatalogoPrecos catalogoPrecos,
			PublicadorEventos publicador) {
		this.orcamentoGateway = orcamentoGateway;
		this.catalogoPrecos = catalogoPrecos;
		this.publicador = publicador;
	}

	@Transactional
	public OrcamentoResponse executar(SolicitacaoOrcamento solicitacao) {
		Orcamento.Cabecalho cabecalho = new Orcamento.Cabecalho(solicitacao.sagaId(), solicitacao.ordemServicoId(),
				solicitacao.codigoOrdemServico(), solicitacao.clienteId(), solicitacao.veiculoId());

		Optional<Orcamento> jaGerado = orcamentoGateway.buscarPorSagaId(cabecalho.sagaId());
		if (jaGerado.isPresent()) {
			exigirMesmaOs(jaGerado.get(), cabecalho);
			return responder(jaGerado.get());
		}

		Orcamento orcamento = Orcamento.gerar(cabecalho, linhasDeServico(solicitacao.servicoIds()),
				linhasDeItem(solicitacao.itens()));

		return responder(orcamentoGateway.salvar(orcamento));
	}

	/** O mesmo {@code sagaId} vindo de outra OS não é repetição, é pedido inconsistente. */
	private static void exigirMesmaOs(Orcamento jaGerado, Orcamento.Cabecalho pedido) {
		if (!jaGerado.getCabecalho().ordemServicoId().equals(pedido.ordemServicoId())) {
			throw new RegraNegocioException(ExceptionMessage.SAGA_OF_OTHER_ORDER.formatted(pedido.sagaId()));
		}
	}

	private OrcamentoResponse responder(Orcamento orcamento) {
		Orcamento.Cabecalho cabecalho = orcamento.getCabecalho();
		publicador.publicar(new EventoSaga(TipoEvento.ORCAMENTO_GERADO, cabecalho.sagaId().toString(),
				cabecalho.sagaId(), cabecalho.ordemServicoId(), cabecalho.codigoOrdemServico(), null,
				new OrcamentoGeradoDados(orcamento.getId(), orcamento.getValorServicos(), orcamento.getValorItens(),
						orcamento.getValorTotal())));
		return OrcamentoResponse.de(orcamento);
	}

	private List<LinhaServico> linhasDeServico(List<Long> ids) {
		exigirIds(ids);
		Map<Long, Preco> precos = catalogoPrecos.servicos(ids);
		return ids.stream().map(id -> {
			Preco preco = exigirAtivo(precos.get(id), ExceptionMessage.SERVICE_NOT_IN_CATALOG,
					ExceptionMessage.INACTIVE_SERVICE, id);
			return new LinhaServico(id, preco.nome(), preco.valor());
		}).toList();
	}

	private List<LinhaItem> linhasDeItem(List<ItemSolicitado> itens) {
		List<Long> ids = itens.stream().map(item -> item == null ? null : item.itemId()).toList();
		exigirIds(ids);
		Map<Long, Preco> precos = catalogoPrecos.itens(ids);
		return itens.stream().map(item -> {
			Preco preco = exigirAtivo(precos.get(item.itemId()), ExceptionMessage.ITEM_NOT_IN_CATALOG,
					ExceptionMessage.INACTIVE_ITEM, item.itemId());
			return new LinhaItem(item.itemId(), preco.nome(), item.quantidade(), preco.valor());
		}).toList();
	}

	private static void exigirIds(List<Long> ids) {
		if (ids.stream().anyMatch(Objects::isNull)) {
			throw new RegraNegocioException(ExceptionMessage.REQUIRED_CATALOG_ID);
		}
	}

	private static Preco exigirAtivo(Preco preco, String naoExiste, String inativo, Long id) {
		if (preco == null) {
			throw new RegraNegocioException(naoExiste.formatted(id));
		}
		if (!preco.ativo()) {
			throw new RegraNegocioException(inativo.formatted(id));
		}
		return preco;
	}
}
