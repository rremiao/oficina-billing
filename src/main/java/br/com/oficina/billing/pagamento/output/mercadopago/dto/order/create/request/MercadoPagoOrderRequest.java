package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MercadoPagoOrderRequest {

    private String type;
    @JsonProperty("total_amount")
    private String totalAmount;
    @JsonProperty("external_reference")
    private String externalReference;
    @JsonProperty("processing_mode")
    private String processingMode;
    @JsonProperty("capture_mode")
    private String captureMode;
    @JsonProperty("marketplace_fee")
    private String marketplaceFee;
    @JsonProperty("expiration_date")
    private String expirationDate;
    private MercadoPagoOrderPayer payer;
    private MercadoPagoOrderConfig config;
    private MercadoPagoOrderTransactions transactions;
    private List<MercadoPagoOrderItem> items;
    @JsonProperty("additional_info")
    private MercadoPagoOrderAdditionalInfo additionalInfo;
    private String description;



}
