package br.com.oficina.billing.dto.order.find;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoFindOrderTransaction {

    private List<MercadoPagoFindOrderPayment> payments;
    private List<MercadoPagoFindOrderChargeback> chargebacks;
    private List<MercadoPagoFindOrderRefund> refunds;
}
