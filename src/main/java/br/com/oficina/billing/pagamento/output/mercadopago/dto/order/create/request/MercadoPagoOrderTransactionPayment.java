package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MercadoPagoOrderTransactionPayment {

    private String amount;
    @JsonProperty("payment_method")
    private MercadoPagoOrderTransactionPaymentMethod paymentMethod;
}
