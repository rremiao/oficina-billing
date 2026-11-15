package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoFindOrderPaymentMethod {

    private String id;
    private String type;
    private String token;
    private Integer installments;
    @JsonProperty("statement_descriptor")
    private String statementDescriptor;
    @JsonProperty("ticket_url")
    private String ticketUrl;
    @JsonProperty("barcode_content")
    private String barcodeContent;
    private String reference;
    @JsonProperty("verification_code")
    private String verificationCode;
    @JsonProperty("financial_institution")
    private String financialInstitution;
    @JsonProperty("digitable_line")
    private String digitableLine;
    @JsonProperty("qr_code")
    private String qrCode;
    @JsonProperty("qr_code_base64")
    private String qrCodeBase64;
    @JsonProperty("e2e_id")
    private String e2eId;
}
