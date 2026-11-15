package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.find;

import br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.response.MercadoPagoOrderResponsePaymentMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoFindOrderConfig {

    private MercadoPagoFindOrderOnline online;
    private MercadoPagoOrderResponsePaymentMethod paymentMethod;
}
