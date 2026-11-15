package br.com.oficina.billing.catalogo.item.application.dto;

import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cadastro e atualização de item. Saldo e estoque mínimo são do serviço de Execução. */
public record ItemRequest(
		@NotBlank(message = "Nome é obrigatório")
		@Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
		String nome,

		@Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
		String descricao,

		@NotNull(message = "Tipo é obrigatório")
		TipoItem tipo,

		@NotNull(message = "Valor unitário é obrigatório")
		@DecimalMin(value = "0.0", inclusive = false, message = "Valor unitário deve ser maior que zero")
		@Digits(integer = 8, fraction = 2, message = "Valor unitário deve ter até 8 dígitos inteiros e 2 casas decimais")
		BigDecimal valorUnitario,

		@NotBlank(message = "Unidade de medida é obrigatória")
		@Size(max = 10, message = "Unidade de medida deve ter no máximo 10 caracteres")
		String unidadeMedida
) {

	public Item.Dados paraDados() {
		return new Item.Dados(nome, descricao, tipo, valorUnitario, unidadeMedida);
	}
}
