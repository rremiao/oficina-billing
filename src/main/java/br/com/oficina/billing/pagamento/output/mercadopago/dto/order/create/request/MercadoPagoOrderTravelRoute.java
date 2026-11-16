package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderTravelRoute {

    private String departure;
    private String destination;
    @JsonProperty("departure_date_time")
    private OffsetDateTime departureDateTime;
    @JsonProperty("arrival_date_time")
    private OffsetDateTime arrivalDateTime;
    private String company;
    @JsonProperty("item_references")
    private List<String> itemReferences;
}
