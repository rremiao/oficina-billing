package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class MercadoPagoFindOrderPayment {

    private String id;
    private String amount;
    @JsonProperty("paid_amount")
    private String paidAmount;
    private String status;
    @JsonProperty("status_detail")
    private String statusDetail;
    @JsonProperty("confirmed_amount")
    private String confirmedAmount;
    @JsonProperty("expiration_time")
    private String expirationTime;
    @JsonProperty("date_of_expiration")
    private OffsetDateTime dateOfExpiration;
    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("installment_amount")
    private String installmentMethod;
    @JsonProperty("payment_method")
    private MercadoPagoFindOrderPaymentMethod paymentMethod;


}
