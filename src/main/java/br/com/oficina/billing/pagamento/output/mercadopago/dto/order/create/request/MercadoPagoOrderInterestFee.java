package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderInterestFee {

    private String type;
    private List<Integer> values;
}
