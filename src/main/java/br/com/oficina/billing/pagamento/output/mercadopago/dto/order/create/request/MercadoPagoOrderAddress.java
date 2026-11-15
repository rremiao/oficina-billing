package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderAddress {

    @JsonProperty("zip_code")
    private String zipCode;
    @JsonProperty("street_name")
    private String streetName;
    @JsonProperty("street_number")
    private String streetNumber;
    private String neighborhood;
    private String city;
}
