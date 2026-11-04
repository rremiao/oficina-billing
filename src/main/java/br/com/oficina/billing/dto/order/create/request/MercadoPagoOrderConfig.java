package br.com.oficina.billing.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderConfig {

    @JsonProperty("statement_descriptor")
    private String statementDescriptor;
    @JsonProperty("default_payment_due_date")
    private String defaultPaymentDueDate;
    private MercadoPagoOrderConfigOnline online;
    @JsonProperty("payment_method")
    private MercadoPagoOrderPaymentMethod paymentMethod;
}
