package br.com.oficina.billing.mensageria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes de exchanges e filas e parâmetros do outbox ({@code oficina.mensageria.*}).
 *
 * @param exchangeOs      topic exchange onde o OS Service publica os comandos
 * @param exchangeBilling topic exchange onde o Billing publica os eventos
 * @param filaEntrada     fila do Billing, ligada à exchange do OS Service pelos tipos de comando
 * @param outbox          envio dos eventos gravados no outbox
 */
@ConfigurationProperties("oficina.mensageria")
public record MensageriaProperties(String exchangeOs, String exchangeBilling, String filaEntrada, Outbox outbox) {

	/**
	 * @param agendamentoHabilitado liga o envio agendado (desligado nos testes, que enviam sob demanda)
	 * @param intervaloMs           intervalo entre rodadas de envio
	 * @param lote                  máximo de eventos enviados por rodada
	 * @param retencaoDias          por quantos dias um evento já enviado fica no outbox
	 */
	public record Outbox(boolean agendamentoHabilitado, long intervaloMs, int lote, int retencaoDias) {
	}

	/** Exchange que recebe o que a fila de entrada rejeita. */
	public String exchangeDeadLetter() {
		return filaEntrada + "-dlx";
	}

	/** Fila das mensagens rejeitadas: {@code oficina-billing-comandos-dlq}, como no documento do time. */
	public String filaDeadLetter() {
		return filaEntrada + "-dlq";
	}
}
