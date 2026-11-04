package br.com.oficina.billing.dto.order.find;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoFindOrderChargeback {

    private String id;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("case_id")
    private String caseId;
    private String status;
    private List<String> references;
}
