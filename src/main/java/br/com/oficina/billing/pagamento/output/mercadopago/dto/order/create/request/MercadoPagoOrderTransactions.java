package br.com.oficina.billing.pagamento.output.mercadopago.dto.order.create.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Transações da ordem: no checkout online a Orders API exige o meio de pagamento já na criação. */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MercadoPagoOrderTransactions {

    private List<MercadoPagoOrderTransactionPayment> payments;
}
