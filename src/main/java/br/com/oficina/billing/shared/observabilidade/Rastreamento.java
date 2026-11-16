package br.com.oficina.billing.shared.observabilidade;

import java.util.Map;
import java.util.Set;

/**
 * Rastreamento distribuído entre os serviços. Isola a ferramenta (New Relic, em
 * {@link NewRelicRastreamento}) do código de mensageria e permite verificar nos testes o que é propagado.
 */
public interface Rastreamento {

	/**
	 * Cabeçalhos propagados com as mensagens: W3C Trace Context ({@code traceparent},
	 * {@code tracestate}) e o formato próprio do New Relic ({@code newrelic}). Os mesmos do OS Service.
	 */
	Set<String> CABECALHOS = Set.of("traceparent", "tracestate", "newrelic");

	/** Cabeçalhos do trace em andamento. Vazio quando não há trace (sem o agente, por exemplo). */
	Map<String, String> capturar();

	/** Liga a transação atual ao trace de quem enviou a mensagem. */
	void continuar(Map<String, String> cabecalhos);

	/** Nome da transação atual (no New Relic, aparece em APM → Transactions). */
	void nomearTransacao(String categoria, String nome);
}
