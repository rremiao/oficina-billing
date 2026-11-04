package br.com.oficina.billing.dto.order.create.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderResponsePaymentMethod {

    @JsonProperty("max_installments")
    private Integer maxInstallments;
    @JsonProperty("not_allowed_ids")
    private List<String> notAllowedIds;
    @JsonProperty("not_allowed_types")
    private List<String> notAllowedTypes;
    @JsonProperty("default_type")
    private String defaultType;
    @JsonProperty("installment_cost")
    private String installmentCost;
    private MercadoPagoOrderResponseInstallment installments;
}
