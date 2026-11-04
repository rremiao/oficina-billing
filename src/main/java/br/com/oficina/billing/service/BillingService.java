package br.com.oficina.billing.service;

import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.dto.order.find.MercadoPagoFindOrderResponse;

public interface BillingService {

    MercadoPagoOrderResponse createOrder();

    MercadoPagoFindOrderResponse findOrder(String id);
}
