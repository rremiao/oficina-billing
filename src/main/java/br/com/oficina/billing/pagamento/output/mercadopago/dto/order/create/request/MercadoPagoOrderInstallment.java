package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderInstallment {

    @JsonProperty("interest_fee")
    private MercadoPagoOrderInterestFee interestFee;
}
