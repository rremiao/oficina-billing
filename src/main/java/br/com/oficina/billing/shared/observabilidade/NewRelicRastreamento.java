package br.com.oficina.billing.shared.observabilidade;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rastreamento no New Relic. Sem o agent Java anexado ao processo (testes, docker-compose), as chamadas
 * viram no-op e {@link #capturar()} devolve vazio. No EKS o agent é injetado pelo k8s-agents-operator
 * (k8s/overlays/eks/newrelic-instrumentation.yaml).
 */
@Component
class NewRelicRastreamento implements Rastreamento {

	@Override
	public Map<String, String> capturar() {
		CabecalhosRastreamento cabecalhos = new CabecalhosRastreamento(Map.of());
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(cabecalhos);
		return cabecalhos.comoMapa();
	}

	@Override
	public void continuar(Map<String, String> cabecalhos) {
		if (cabecalhos != null && !cabecalhos.isEmpty()) {
			NewRelic.getAgent().getTransaction()
					.acceptDistributedTraceHeaders(TransportType.AMQP, new CabecalhosRastreamento(cabecalhos));
		}
	}

	@Override
	public void nomearTransacao(String categoria, String nome) {
		NewRelic.setTransactionName(categoria, nome);
	}
}
