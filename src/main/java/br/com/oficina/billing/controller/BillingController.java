package br.com.oficina.billing.controller;

import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.dto.order.find.MercadoPagoFindOrderResponse;
import br.com.oficina.billing.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BillingController implements BillingApi {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }


    @Override
    public ResponseEntity<MercadoPagoOrderResponse> createOrder() {
        return ResponseEntity.ok(billingService.createOrder());
    }

    @Override
    public ResponseEntity<MercadoPagoFindOrderResponse> findOrder(String id) {
        return ResponseEntity.ok(billingService.findOrder(id));
    }
}
