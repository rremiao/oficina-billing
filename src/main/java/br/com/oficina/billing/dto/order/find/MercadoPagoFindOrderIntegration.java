package br.com.oficina.billing.dto.order.find;

import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponseSponsor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoFindOrderIntegration {

    @JsonProperty("application_id")
    private String applicationId;
    @JsonProperty("integrator_id")
    private String integratorId;
    @JsonProperty("platform_id")
    private String platformId;
    private MercadoPagoOrderResponseSponsor sponsor;
}
