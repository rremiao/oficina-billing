package br.com.oficina.billing.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderPayer {

    private String email;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    private MercadoPagoOrderPhone phone;
    private MercadoPagoOrderIdentification identification;
    private MercadoPagoOrderAddress address;
}
