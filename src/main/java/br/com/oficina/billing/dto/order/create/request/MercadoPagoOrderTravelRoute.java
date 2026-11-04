package br.com.oficina.billing.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderTravelRoute {

    private String departure;
    private String destination;
    @JsonProperty("departure_date_time")
    private LocalDateTime departureDateTime;
    @JsonProperty("arrival_date_time")
    private LocalDateTime arrivalDateTime;
    private String company;
    @JsonProperty("item_references")
    private List<String> itemReferences;
}
