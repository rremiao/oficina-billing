package br.com.oficina.billing.mensageria.application;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Converte os {@code dados} do envelope no formato do comando; JSON fora do contrato é mensagem inválida. */
public final class LeitorDados {

	private LeitorDados() {
	}

	public static <T> T ler(JsonMapper jsonMapper, EnvelopeMensagem envelope, Class<T> formato) {
		JsonNode dados = envelope.dados();
		if (dados == null || dados.isNull() || dados.isMissingNode()) {
			throw new MensagemInvalidaException("Comando " + envelope.tipo() + " sem dados");
		}
		try {
			return jsonMapper.treeToValue(dados, formato);
		} catch (JacksonException e) {
			throw new MensagemInvalidaException("Dados do comando " + envelope.tipo() + " fora do contrato", e);
		}
	}

	/** Campo obrigatório ausente nos {@code dados}. */
	public static <T> T exigir(T valor, String campo, EnvelopeMensagem envelope) {
		if (valor == null) {
			throw new MensagemInvalidaException("Comando " + envelope.tipo() + " sem " + campo);
		}
		return valor;
	}
}
