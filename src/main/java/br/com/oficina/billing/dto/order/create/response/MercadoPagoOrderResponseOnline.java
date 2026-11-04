package br.com.oficina.billing.dto.order.create.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MercadoPagoOrderResponseOnline {

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
    @JsonProperty("auto_return")
    private String autoReturn;
    private MercadoPagoOrderResponseRetry retries;
}
