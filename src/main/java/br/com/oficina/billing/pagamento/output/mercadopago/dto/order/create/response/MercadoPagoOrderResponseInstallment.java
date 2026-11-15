package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderResponseInstallment {

    @JsonProperty("interest_free")
    private MercadoPagoOrderResponseInterestFree interestFree;
    private MercadoPagoOrderResponseAvailable available;
}
