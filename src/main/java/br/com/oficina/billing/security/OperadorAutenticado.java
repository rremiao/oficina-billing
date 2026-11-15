package br.com.oficina.billing.security;

/**
 * Principal das requisições com token de funcionário, montado só a partir das claims.
 *
 * @param email e-mail do funcionário ({@code subject} do token)
 * @param papel papel na oficina
 */
public record OperadorAutenticado(String email, PapelOperador papel) {
}
