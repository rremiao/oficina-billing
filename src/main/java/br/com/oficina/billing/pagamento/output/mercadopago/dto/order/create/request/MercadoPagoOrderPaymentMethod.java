package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderPaymentMethod {

    @JsonProperty("max_installments")
    private Integer maxInstallments;
    @JsonProperty("not_allowed_ids")
    private List<String> notAllowedIds;
    @JsonProperty("not_allowed_types")
    private List<String> notAllowedTypes;
    private MercadoPagoOrderInstallment installments;
}
