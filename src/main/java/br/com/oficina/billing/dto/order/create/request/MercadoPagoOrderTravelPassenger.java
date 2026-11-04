package br.com.oficina.billing.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderTravelPassenger {

    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("identification_type")
    private String identificationType;
    @JsonProperty("identification_number")
    private String identificationNumber;
    @JsonProperty("item_references")
    private List<String> itemReferences;
}
