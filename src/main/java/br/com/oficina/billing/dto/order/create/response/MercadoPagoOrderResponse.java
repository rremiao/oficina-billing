package br.com.oficina.billing.dto.order.create.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderResponse {

    private String id;
    private String type;
    @JsonProperty("processing_mode")
    private String processingMode;
    private String status;
    @JsonProperty("status_detail")
    private String statusDetail;
    @JsonProperty("external_reference")
    private String externalReference;
    @JsonProperty("total_amount")
    private String totalAmount;
    @JsonProperty("total_paid_amount")
    private String totalPaidAmount;
    @JsonProperty("marketplace_fee")
    private String marketplaceFee;
    @JsonProperty("checkout_url")
    private String checkoutUrl;
    @JsonProperty("expiration_time")
    private String expirationTime;
    @JsonProperty("country_code")
    private String countryCode;
    @JsonProperty("user_id")
    private String userId;
    private String currency;
    @JsonProperty("capture_mode")
    private String captureMode;
    @JsonProperty("client_token")
    private String clientToken;
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
    @JsonProperty("last_updated_date")
    private LocalDateTime lastUpdatedDate;
    @JsonProperty("integration_data")
    private MercadoPagoOrderResponseIntegration integrationData;
    private MercadoPagoOrderResponseConfig config;
    private List<MercadoPagoOrderResponseItem> items;
    private String description;
}
