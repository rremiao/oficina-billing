package br.com.oficina.billing.dto.order.find;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoFindOrderRefund {

    private String id;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("reference_id")
    private String referenceId;
    private String amount;
    private String status;
}
