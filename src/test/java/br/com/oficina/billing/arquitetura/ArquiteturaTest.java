package br.com.oficina.billing.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regra de dependência entre camadas (docs/REGRAS_EXECUCAO_E_QUALIDADE.md, seção 11). Violação
 * derruba o build.
 */
@AnalyzeClasses(packages = "br.com.oficina.billing", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArquiteturaTest {

	private static final String[] INFRAESTRUTURA = {
		"org.springframework..",
		"jakarta.persistence..",
		"jakarta.servlet..",
		"feign..",
		"lombok..",
		"tools.jackson..",
		"com.fasterxml.jackson.."
	};

	@ArchTest
	static final ArchRule DOMINIO_NAO_CONHECE_FRAMEWORK_NEM_OUTRAS_CAMADAS = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(INFRAESTRUTURA)
			.orShould().dependOnClassesThat().resideInAnyPackage("..application..", "..entrypoint..", "..output..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule APLICACAO_NAO_CONHECE_WEB_PERSISTENCIA_NEM_INTEGRACOES = noClasses()
			.that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework.web..",
					"org.springframework.data..",
					"org.springframework.cloud..",
					"org.springframework.amqp..",
					"jakarta.persistence..",
					"jakarta.servlet..",
					"feign..")
			.orShould().dependOnClassesThat().resideInAnyPackage("..entrypoint..", "..output..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule ADAPTADOR_DE_ENTRADA_NAO_USA_ADAPTADOR_DE_SAIDA = noClasses()
			.that().resideInAPackage("..entrypoint..")
			.should().dependOnClassesThat().resideInAPackage("..output..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule DTO_DO_MERCADO_PAGO_NAO_SAI_DO_ADAPTADOR = classes()
			.that().resideInAPackage("..output.mercadopago..")
			.should().onlyBeAccessed().byClassesThat().resideInAPackage("..output.mercadopago..");

	@ArchTest
	static final ArchRule INJECAO_SO_POR_CONSTRUTOR = noClasses()
			.should().dependOnClassesThat().haveFullyQualifiedName(
					"org.springframework.beans.factory.annotation.Autowired")
			.because("injeção de dependência é só por construtor (seção 11)");

	private ArquiteturaTest() {
	}
}
