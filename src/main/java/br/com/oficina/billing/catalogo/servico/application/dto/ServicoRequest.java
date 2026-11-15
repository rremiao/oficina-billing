package br.com.oficina.billing.catalogo.servico.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicoRequest(
		@NotBlank(message = "Nome é obrigatório")
		@Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
		String nome,

		@Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
		String descricao,

		@NotNull(message = "Valor é obrigatório")
		@DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
		@Digits(integer = 8, fraction = 2, message = "Valor deve ter até 8 dígitos inteiros e 2 casas decimais")
		BigDecimal valor
) {
}
