package br.com.oficina.billing.pagamento.entrypoint.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.billing.apoio.MercadoPagoSimulado;
import org.junit.jupiter.api.Test;

class AssinaturaMercadoPagoTest {

	private final AssinaturaMercadoPago assinatura = new AssinaturaMercadoPago(MercadoPagoSimulado.SEGREDO_WEBHOOK);

	@Test
	void aceitaAssinaturaFeitaComOSegredo() {
		String xSignature = MercadoPagoSimulado.assinatura("ORD01ABC", "req-1", "1704908010");

		assertThat(assinatura.valida(xSignature, "req-1", "ORD01ABC")).isTrue();
	}

	@Test
	void recusaAssinaturaDeOutraOrdemOuDeOutraRequisicao() {
		String xSignature = MercadoPagoSimulado.assinatura("ORD01ABC", "req-1", "1704908010");

		assertThat(assinatura.valida(xSignature, "req-1", "ORD99XYZ")).isFalse();
		assertThat(assinatura.valida(xSignature, "req-2", "ORD01ABC")).isFalse();
	}

	@Test
	void recusaAssinaturaFeitaComOutroSegredo() {
		AssinaturaMercadoPago comOutroSegredo = new AssinaturaMercadoPago("outro-segredo");
		String xSignature = MercadoPagoSimulado.assinatura("ORD01ABC", "req-1", "1704908010");

		assertThat(comOutroSegredo.valida(xSignature, "req-1", "ORD01ABC")).isFalse();
	}

	@Test
	void recusaCabecalhoAusenteOuMalformado() {
		assertThat(assinatura.valida(null, "req-1", "ORD01ABC")).isFalse();
		assertThat(assinatura.valida("v1=abc", "req-1", "ORD01ABC")).isFalse();
		assertThat(assinatura.valida("ts=1704908010", "req-1", "ORD01ABC")).isFalse();
		assertThat(assinatura.valida("lixo", "req-1", "ORD01ABC")).isFalse();
		assertThat(assinatura.valida("ts=1,v1=abc", "req-1", null)).isFalse();
	}

	@Test
	void semSegredoConfiguradoNenhumAvisoEAceito() {
		AssinaturaMercadoPago semSegredo = new AssinaturaMercadoPago("");
		String xSignature = MercadoPagoSimulado.assinatura("ORD01ABC", "req-1", "1704908010");

		assertThat(semSegredo.valida(xSignature, "req-1", "ORD01ABC")).isFalse();
	}

	@Test
	void manifestoUsaIdEmMinusculasEDeixaDeForaORequestIdAusente() {
		assertThat(AssinaturaMercadoPago.manifesto("ORD01ABC", "req-1", "10")).isEqualTo("id:ord01abc;request-id:req-1;ts:10;");
		assertThat(AssinaturaMercadoPago.manifesto("ORD01ABC", null, "10")).isEqualTo("id:ord01abc;ts:10;");
	}
}
