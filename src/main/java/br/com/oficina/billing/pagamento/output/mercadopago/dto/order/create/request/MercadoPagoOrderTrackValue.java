package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderTrackValue {

    @JsonProperty("conversion_id")
    private String conversionId;
    @JsonProperty("conversion_label")
    private String conversionLabel;
    @JsonProperty("pixel_id")
    private String pixelId;
}
