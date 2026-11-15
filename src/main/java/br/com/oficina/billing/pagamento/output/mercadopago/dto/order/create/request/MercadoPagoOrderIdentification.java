package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderIdentification {

    private String type;
    private String number;
}
