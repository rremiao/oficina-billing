package br.com.oficina.billing.security;

/** Quem emitiu o token: o login de funcionários do OS Service ou a Lambda de autenticação por CPF. */
public enum TipoToken {

	OPERADOR,

	CLIENTE
}
