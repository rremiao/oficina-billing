package br.com.oficina.billing.orcamento.entrypoint.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.oficina.billing.mensageria.application.EnvelopeMensagem;
import br.com.oficina.billing.mensageria.application.MensagemInvalidaException;
import br.com.oficina.billing.mensageria.application.TipoComando;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento.ItemSolicitado;
import br.com.oficina.billing.orcamento.application.usecase.GerarOrcamento;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.UUID;

class SolicitarOrcamentoManipuladorTest {

	private static final UUID SAGA = UUID.randomUUID();
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GerarOrcamento gerarOrcamento = mock(GerarOrcamento.class);
	private final SolicitarOrcamentoManipulador manipulador = new SolicitarOrcamentoManipulador(gerarOrcamento, JSON);

	private static EnvelopeMensagem envelope(String dados) {
		return new EnvelopeMensagem(UUID.randomUUID(), "SolicitarOrcamento", SAGA, 42L, "OS-2026-000042", "corr", null, 1,
				dados == null ? null : JSON.readTree(dados));
	}

	private SolicitacaoOrcamento solicitacaoEnviada() {
		ArgumentCaptor<SolicitacaoOrcamento> solicitacao = ArgumentCaptor.forClass(SolicitacaoOrcamento.class);
		verify(gerarOrcamento).executar(solicitacao.capture());
		return solicitacao.getValue();
	}

	@Test
	void traduzOsDadosDoContratoNaSolicitacao() {
		manipulador.manipular(envelope("""
				{"clienteId": 7, "veiculoId": 3, "servicos": [{"servicoId": 30}], "itens": [{"itemId": 20, "quantidade": 2}]}
				"""));

		SolicitacaoOrcamento solicitacao = solicitacaoEnviada();
		assertThat(manipulador.tipo()).isEqualTo(TipoComando.SOLICITAR_ORCAMENTO);
		assertThat(solicitacao.sagaId()).isEqualTo(SAGA);
		assertThat(solicitacao.ordemServicoId()).isEqualTo(42L);
		assertThat(solicitacao.codigoOrdemServico()).isEqualTo("OS-2026-000042");
		assertThat(solicitacao.clienteId()).isEqualTo(7L);
		assertThat(solicitacao.servicoIds()).containsExactly(30L);
		assertThat(solicitacao.itens()).containsExactly(new ItemSolicitado(20L, 2));
	}

	@Test
	void listasAusentesViramVaziasEQuantidadeAusenteViraZero() {
		manipulador.manipular(envelope("{\"clienteId\": 7, \"itens\": [{\"itemId\": 20}, null], \"servicos\": [null]}"));

		SolicitacaoOrcamento solicitacao = solicitacaoEnviada();
		assertThat(solicitacao.itens()).containsExactly(new ItemSolicitado(20L, 0), null);
		assertThat(solicitacao.servicoIds()).isEqualTo(Arrays.asList((Long) null));
	}

	@Test
	void semListasNenhumas() {
		manipulador.manipular(envelope("{\"clienteId\": 7}"));

		assertThat(solicitacaoEnviada().servicoIds()).isEmpty();
	}

	@Test
	void dadosForaDoContratoSaoMensagemInvalida() {
		assertThatThrownBy(() -> manipulador.manipular(envelope("{\"veiculoId\": 3}")))
				.isInstanceOf(MensagemInvalidaException.class)
				.hasMessage("Comando SolicitarOrcamento sem clienteId");
		assertThatThrownBy(() -> manipulador.manipular(envelope(null)))
				.hasMessage("Comando SolicitarOrcamento sem dados");
		assertThatThrownBy(() -> manipulador.manipular(envelope("null")))
				.hasMessage("Comando SolicitarOrcamento sem dados");
		assertThatThrownBy(() -> manipulador.manipular(envelope("{\"clienteId\": \"sete\"}")))
				.hasMessage("Dados do comando SolicitarOrcamento fora do contrato");
	}
}
