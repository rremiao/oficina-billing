package br.com.oficina.billing.orcamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.billing.apoio.BancoDeTeste;
import br.com.oficina.billing.apoio.TokensDeTeste;
import br.com.oficina.billing.config.IntegrationTest;
import br.com.oficina.billing.orcamento.application.dto.OrcamentoResponse;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento;
import br.com.oficina.billing.orcamento.application.dto.SolicitacaoOrcamento.ItemSolicitado;
import br.com.oficina.billing.orcamento.application.usecase.GerarOrcamento;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Orçamento de ponta a ponta com PostgreSQL real: gerado a partir do pedido do OS Service (como a
 * Saga vai fazer na etapa 5) e consultado pela API.
 */
@IntegrationTest
class OrcamentoIntegrationTest {

	private static final String ADMIN = TokensDeTeste.bearer(TokensDeTeste.admin());

	@Autowired
	private GerarOrcamento gerarOrcamento;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void catalogoComOsIdsDoMonolito() {
		BancoDeTeste.limpar(jdbc);
		jdbc.update("INSERT INTO servico (id, nome, valor) VALUES (30, 'Troca de pastilhas', 150.00)");
		jdbc.update("INSERT INTO servico (id, nome, valor) VALUES (31, 'Alinhamento', 80.50)");
		jdbc.update("INSERT INTO item (id, nome, tipo, valor_unitario, unidade_medida) "
				+ "VALUES (20, 'Pastilha de freio', 'PECA', 89.90, 'JG')");
		jdbc.update("INSERT INTO item (id, nome, tipo, valor_unitario, unidade_medida, ativo) "
				+ "VALUES (21, 'Pastilha antiga', 'PECA', 50.00, 'JG', false)");
	}

	private static SolicitacaoOrcamento pedido(UUID saga, Long ordemServicoId, Long clienteId) {
		return new SolicitacaoOrcamento(saga, ordemServicoId, "OS-2026-0000" + ordemServicoId, clienteId, 3L,
				List.of(30L, 31L), List.of(new ItemSolicitado(20L, 2)));
	}

	@Test
	void orcamentoGeradoGuardaOPrecoDaEpocaEApareceNaApi() throws Exception {
		OrcamentoResponse gerado = gerarOrcamento.executar(pedido(UUID.randomUUID(), 42L, 7L));

		jdbc.update("UPDATE servico SET valor = 999.00 WHERE id = 30");

		mockMvc.perform(get("/orcamentos/" + gerado.id()).header("Authorization", ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ordemServicoId").value(42))
				.andExpect(jsonPath("$.codigoOrdemServico").value("OS-2026-000042"))
				.andExpect(jsonPath("$.servicos", hasSize(2)))
				.andExpect(jsonPath("$.servicos[0].nome").value("Troca de pastilhas"))
				.andExpect(jsonPath("$.servicos[0].valorUnitario").value(150.00))
				.andExpect(jsonPath("$.itens[0].quantidade").value(2))
				.andExpect(jsonPath("$.itens[0].valorTotal").value(179.80))
				.andExpect(jsonPath("$.valorServicos").value(230.50))
				.andExpect(jsonPath("$.valorItens").value(179.80))
				.andExpect(jsonPath("$.valorTotal").value(410.30));

		assertThat(jdbc.queryForObject("SELECT valor_total FROM orcamento WHERE id = ?", String.class, gerado.id()))
				.isEqualTo("410.30");
	}

	@Test
	void pedidoRepetidoDaMesmaSagaNaoDuplicaOrcamento() {
		UUID saga = UUID.randomUUID();

		OrcamentoResponse primeiro = gerarOrcamento.executar(pedido(saga, 42L, 7L));
		OrcamentoResponse repetido = gerarOrcamento.executar(pedido(saga, 42L, 7L));

		assertThat(repetido.id()).isEqualTo(primeiro.id());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento", Integer.class)).isEqualTo(1);
	}

	@Test
	void duasEntregasSimultaneasDoMesmoPedidoGravamUmOrcamentoSo() throws Exception {
		UUID saga = UUID.randomUUID();
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<OrcamentoResponse>> entregas = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			for (int i = 0; i < 2; i++) {
				entregas.add(executor.submit(() -> {
					largada.await();
					return gerarOrcamento.executar(pedido(saga, 42L, 7L));
				}));
			}
			largada.countDown();

			for (Future<OrcamentoResponse> entrega : entregas) {
				try {
					assertThat(entrega.get().sagaId()).isEqualTo(saga);
				} catch (ExecutionException e) {
					// A perdedora da corrida esbarra no índice único; na Saga, a nova tentativa devolve o já gerado
					assertThat(e.getCause()).isInstanceOf(DataIntegrityViolationException.class);
				}
			}
		}

		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento WHERE saga_id = ?", Integer.class, saga))
				.isEqualTo(1);
		assertThat(gerarOrcamento.executar(pedido(saga, 42L, 7L)).sagaId()).isEqualTo(saga);
	}

	@Test
	void itemInativoNaoEntraEmOrcamentoNovo() {
		SolicitacaoOrcamento comItemInativo = new SolicitacaoOrcamento(UUID.randomUUID(), 42L, null, 7L, null,
				List.of(), List.of(new ItemSolicitado(21L, 1)));

		assertThatThrownBy(() -> gerarOrcamento.executar(comItemInativo))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Item 21 está inativo e não entra em orçamento");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento", Integer.class)).isZero();
	}

	@Test
	void clienteVeSoOProprioOrcamento() throws Exception {
		OrcamentoResponse gerado = gerarOrcamento.executar(pedido(UUID.randomUUID(), 42L, 7L));
		String rota = "/orcamentos/" + gerado.id();

		mockMvc.perform(get(rota).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(7L, "12345678909", "Maria"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clienteId").value(7));
		mockMvc.perform(get(rota).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(8L, "98765432100", "João"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Orçamento não encontrado"));
		mockMvc.perform(get(rota).header("Authorization", TokensDeTeste.bearer(TokensDeTeste.atendente())))
				.andExpect(status().isOk());
		mockMvc.perform(get(rota)).andExpect(status().isUnauthorized());
	}

	@Test
	void listaOsOrcamentosDaOsDoMaisRecenteParaOMaisAntigo() throws Exception {
		OrcamentoResponse primeiroCiclo = gerarOrcamento.executar(pedido(UUID.randomUUID(), 42L, 7L));
		OrcamentoResponse segundoCiclo = gerarOrcamento.executar(pedido(UUID.randomUUID(), 42L, 7L));
		gerarOrcamento.executar(pedido(UUID.randomUUID(), 43L, 8L));

		mockMvc.perform(get("/orcamentos/ordem-servico/42").header("Authorization", ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(segundoCiclo.id()))
				.andExpect(jsonPath("$[1].id").value(primeiroCiclo.id()));
		mockMvc.perform(get("/orcamentos/ordem-servico/42")
						.header("Authorization", TokensDeTeste.bearer(TokensDeTeste.cliente(8L, "98765432100", "João"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get("/orcamentos/ordem-servico/999").header("Authorization", ADMIN))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void orcamentoInexistenteResponde404() throws Exception {
		mockMvc.perform(get("/orcamentos/999").header("Authorization", ADMIN)).andExpect(status().isNotFound());
	}
}
