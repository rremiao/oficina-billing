package br.com.oficina.billing.orcamento.application.dto;

import br.com.oficina.billing.orcamento.domain.LinhaItem;
import br.com.oficina.billing.orcamento.domain.LinhaServico;
import br.com.oficina.billing.orcamento.domain.Orcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrcamentoResponse(
		Long id,
		UUID sagaId,
		Long ordemServicoId,
		String codigoOrdemServico,
		Long clienteId,
		Long veiculoId,
		List<Servico> servicos,
		List<Item> itens,
		BigDecimal valorServicos,
		BigDecimal valorItens,
		BigDecimal valorTotal,
		LocalDateTime dataCriacao
) {

	public static OrcamentoResponse de(Orcamento orcamento) {
		Orcamento.Cabecalho cabecalho = orcamento.getCabecalho();
		return new OrcamentoResponse(
				orcamento.getId(),
				cabecalho.sagaId(),
				cabecalho.ordemServicoId(),
				cabecalho.codigoOrdemServico(),
				cabecalho.clienteId(),
				cabecalho.veiculoId(),
				orcamento.getServicos().stream().map(Servico::de).toList(),
				orcamento.getItens().stream().map(Item::de).toList(),
				orcamento.getValorServicos(),
				orcamento.getValorItens(),
				orcamento.getValorTotal(),
				orcamento.getDataCriacao());
	}

	public record Servico(Long servicoId, String nome, BigDecimal valorUnitario) {

		static Servico de(LinhaServico linha) {
			return new Servico(linha.servicoId(), linha.nome(), linha.valorUnitario());
		}
	}

	public record Item(Long itemId, String nome, int quantidade, BigDecimal valorUnitario, BigDecimal valorTotal) {

		static Item de(LinhaItem linha) {
			return new Item(linha.itemId(), linha.nome(), linha.quantidade(), linha.valorUnitario(), linha.valorTotal());
		}
	}
}
