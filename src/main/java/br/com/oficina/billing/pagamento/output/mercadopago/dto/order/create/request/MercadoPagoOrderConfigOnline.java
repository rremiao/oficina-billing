package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderConfigOnline {

    @JsonProperty("available_from")
    private LocalDateTime availableFrom;
    @JsonProperty("allowed_user_type")
    private String allowedUserType;
    @JsonProperty("success_url")
    private String successUrl;
    @JsonProperty("failure_url")
    private String failureUrl;
    @JsonProperty("pending_url")
    private String pendingUrl;
    @JsonProperty("auto_return")
    private String autoReturn;
    private List<MercadoPagoOrderTrack> tracks;
}
