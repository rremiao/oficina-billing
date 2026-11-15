package br.com.oficina.billing.security;

/**
 * Principal das requisições com token de cliente, montado só a partir das claims.
 *
 * @param id   id do cliente no OS Service
 * @param cpf  CPF sem pontuação ({@code subject} do token); nunca vai para log
 * @param nome nome do cliente, para exibição
 */
public record ClienteAutenticado(Long id, String cpf, String nome) {
}
