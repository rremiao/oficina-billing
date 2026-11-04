package br.com.oficina.billing.dto.order.find;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MercadoPagoFindOrderResponse {

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
    @JsonProperty("capture_mode")
    private String captureMode;
    @JsonProperty("country_code")
    private String countryCode;
    @JsonProperty("user_id")
    private String userId;
    private String description;
    @JsonProperty("expiration_date")
    private String expirationTime;
    @JsonProperty("client_token")
    private String clientToken;
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
    @JsonProperty("last_updated_date")
    private LocalDateTime lastUpdatedDate;
    @JsonProperty("checkout_available_at")
    private String checkoutAvailableAt;
    private String currency;
    private MercadoPagoFindOrderPayer payer;
    @JsonProperty("integration_data")
    private MercadoPagoFindOrderIntegration integrationData;
    private MercadoPagoFindOrderTransaction transactions;
    private List<MercadoPagoFindOrderItem> items;
    private MercadoPagoFindOrderConfig config;
}
