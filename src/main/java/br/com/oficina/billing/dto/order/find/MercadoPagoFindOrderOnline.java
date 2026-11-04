package br.com.oficina.billing.dto.order.find;

import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponseRetry;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MercadoPagoFindOrderOnline {

    @JsonProperty("callback_url")
    private String callbackUrl;
    @JsonProperty("success_url")
    private String successUrl;
    @JsonProperty("failure_url")
    private String failureUrl;
    @JsonProperty("pending_url")
    private String pendingUrl;
    @JsonProperty("available_from")
    private LocalDateTime availableFrom;
    private String autoReturn;
    private MercadoPagoOrderResponseRetry retries;
}
