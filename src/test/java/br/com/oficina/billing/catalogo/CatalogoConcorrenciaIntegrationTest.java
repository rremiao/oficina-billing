package br.com.oficina.billing.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.oficina.billing.apoio.BancoDeTeste;
import br.com.oficina.billing.catalogo.item.application.gateway.ItemGateway;
import br.com.oficina.billing.catalogo.item.domain.Item;
import br.com.oficina.billing.catalogo.item.domain.TipoItem;
import br.com.oficina.billing.catalogo.servico.application.dto.ServicoRequest;
import br.com.oficina.billing.catalogo.servico.application.gateway.ServicoGateway;
import br.com.oficina.billing.catalogo.servico.application.usecase.CadastrarServico;
import br.com.oficina.billing.catalogo.servico.domain.Servico;
import br.com.oficina.billing.config.IntegrationTest;
import br.com.oficina.billing.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * O nome único não depende só da checagem da aplicação: dois cadastros iguais ao mesmo tempo, ou um
 * cadastro que passou pela checagem antes de outro gravar, esbarram no índice único do banco e viram
 * a mesma regra de negócio (422), não erro 500.
 */
@IntegrationTest
class CatalogoConcorrenciaIntegrationTest {

	@Autowired
	private CadastrarServico cadastrarServico;

	@Autowired
	private ServicoGateway servicoGateway;

	@Autowired
	private ItemGateway itemGateway;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limparBanco() {
		BancoDeTeste.limpar(jdbc);
	}

	@Test
	void doisCadastrosSimultaneosComOMesmoNomeGravamSoUm() throws Exception {
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<String>> resultados = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			for (String nome : List.of("Alinhamento", "ALINHAMENTO")) {
				resultados.add(executor.submit(() -> {
					largada.await();
					try {
						cadastrarServico.executar(new ServicoRequest(nome, null, BigDecimal.TEN));
						return "gravado";
					} catch (RegraNegocioException e) {
						return e.getMessage();
					}
				}));
			}
			largada.countDown();

			List<String> desfechos = new ArrayList<>();
			for (Future<String> resultado : resultados) {
				desfechos.add(resultado.get());
			}
			assertThat(desfechos).containsExactlyInAnyOrder("gravado", "Já existe serviço cadastrado com este nome");
		}
		assertThat(jdbc.queryForObject("SELECT count(*) FROM servico", Integer.class)).isEqualTo(1);
	}

	@Test
	void servicoQuePassouPelaChecagemEsbarraNoIndiceUnico() {
		jdbc.update("INSERT INTO servico (nome, valor) VALUES ('Alinhamento', 100)");

		assertThatThrownBy(() -> servicoGateway.salvar(Servico.novo("alinhamento", null, BigDecimal.TEN)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Já existe serviço cadastrado com este nome");
	}

	@Test
	void itemQuePassouPelaChecagemEsbarraNoIndiceUnico() {
		jdbc.update("INSERT INTO item (nome, tipo, valor_unitario, unidade_medida) VALUES ('Filtro', 'PECA', 30, 'UN')");

		Item duplicado = Item.novo(new Item.Dados("FILTRO", null, TipoItem.PECA, BigDecimal.TEN, "un"));

		assertThatThrownBy(() -> itemGateway.salvar(duplicado))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Já existe item cadastrado com o mesmo tipo, nome e unidade de medida");
	}

	@Test
	void bancoRecusaValorZeroETipoDesconhecidoMesmoForaDaAplicacao() {
		assertThatThrownBy(() -> jdbc.update("INSERT INTO servico (nome, valor) VALUES ('Grátis', 0)"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO item (nome, tipo, valor_unitario, unidade_medida) VALUES ('Pneu', 'PNEU', 10, 'UN')"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
