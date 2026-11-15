package br.com.oficina.billing.catalogo.servico.application.dto;

import br.com.oficina.billing.catalogo.servico.domain.Servico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicoResponse(
		Long id,
		String nome,
		String descricao,
		BigDecimal valor,
		boolean ativo,
		LocalDateTime dataCriacao,
		LocalDateTime dataAtualizacao
) {

	public static ServicoResponse de(Servico servico) {
		return new ServicoResponse(
				servico.getId(),
				servico.getNome(),
				servico.getDescricao(),
				servico.getValor(),
				servico.isAtivo(),
				servico.getDataCriacao(),
				servico.getDataAtualizacao());
	}
}
