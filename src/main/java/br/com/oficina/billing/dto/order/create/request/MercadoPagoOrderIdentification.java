package br.com.oficina.billing.dto.order.create.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoOrderIdentification {

    private String type;
    private String number;
}
