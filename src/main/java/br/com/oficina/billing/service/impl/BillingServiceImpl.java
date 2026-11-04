package br.com.oficina.billing.service.impl;

import br.com.oficina.billing.client.BillingClient;
import br.com.oficina.billing.dto.auth.MercadoPagoAuthRequest;
import br.com.oficina.billing.dto.order.create.request.MercadoPagoOrderRequest;
import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.dto.order.find.MercadoPagoFindOrderResponse;
import br.com.oficina.billing.service.BillingService;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceImpl implements BillingService {

    private final BillingClient client;

    public BillingServiceImpl(BillingClient client) {
        this.client = client;
    }


    @Override
    public MercadoPagoOrderResponse createOrder() {
        return client.createOrder(getToken(), "testKey", new MercadoPagoOrderRequest());
    }

    @Override
    public MercadoPagoFindOrderResponse findOrder(String id) {
        return client.getOrder(getToken(), id);
    }

    private String getToken() {
        return "Bearer " + client.getAccessToken(new MercadoPagoAuthRequest());
    }
}
