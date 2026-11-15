package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderAdditionalInfo {

    @JsonProperty("payer.registration_date")
    private LocalDateTime payerRegistrationDate;
    @JsonProperty("payer.authentication_type")
    private String payerAuthenticationType;
    @JsonProperty("payer.is_prime_user")
    private Boolean payerIsPrimeUser;
    @JsonProperty("payer.is_first_purchase_online")
    private Boolean payerIsFirstPurchaseOnline;
    @JsonProperty("payer.last_purchase")
    private LocalDateTime payerLastPurchase;
    @JsonProperty("travel.passengers")
    private List<MercadoPagoOrderTravelPassenger> travelPassengers;
    @JsonProperty("travel.routes")
    private List<MercadoPagoOrderTravelRoute> travelRoutes;
}
