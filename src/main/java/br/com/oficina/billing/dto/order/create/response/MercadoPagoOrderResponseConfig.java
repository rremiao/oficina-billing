package br.com.oficina.billing.dto.order.create.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderResponseConfig {

    private MercadoPagoOrderResponseOnline online;
    @JsonProperty("payment_method")
    private MercadoPagoOrderResponsePaymentMethod paymentMethod;
}
