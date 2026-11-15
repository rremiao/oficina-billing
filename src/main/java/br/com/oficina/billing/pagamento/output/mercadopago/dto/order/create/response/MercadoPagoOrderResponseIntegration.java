package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderResponseIntegration {

    @JsonProperty("application_id")
    private String applicationId;
    @JsonProperty("integrator_id")
    private String integratorId;
    @JsonProperty("platform_id")
    private String platformId;
    private MercadoPagoOrderResponseSponsor sponsor;
}
