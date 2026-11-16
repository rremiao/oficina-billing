package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/** Meio de pagamento escolhido para a transação, por exemplo {@code pix} do tipo {@code bank_transfer}. */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MercadoPagoOrderTransactionPaymentMethod {

    private String id;
    private String type;
}
