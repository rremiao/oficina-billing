package br.com.oficina.billing.catalogo.item.application.dto;

import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(
		Long id,
		String nome,
		String descricao,
		TipoItem tipo,
		BigDecimal valorUnitario,
		String unidadeMedida,
		boolean ativo,
		LocalDateTime dataCriacao,
		LocalDateTime dataAtualizacao
) {

	public static ItemResponse de(Item item) {
		return new ItemResponse(
				item.getId(),
				item.getNome(),
				item.getDescricao(),
				item.getTipo(),
				item.getValorUnitario(),
				item.getUnidadeMedida(),
				item.isAtivo(),
				item.getDataCriacao(),
				item.getDataAtualizacao());
	}
}
