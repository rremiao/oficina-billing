package br.com.oficina.billing.controller;

import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.dto.order.find.MercadoPagoFindOrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/billing")
public interface BillingApi {

    @PostMapping
    ResponseEntity<MercadoPagoOrderResponse> createOrder();

    @GetMapping
    ResponseEntity<MercadoPagoFindOrderResponse> findOrder(String id);
}
