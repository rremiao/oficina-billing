package br.com.oficina.billing.client;

import br.com.oficina.billing.dto.auth.MercadoPagoAuthRequest;
import br.com.oficina.billing.dto.auth.MercadoPagoAuthResponse;
import br.com.oficina.billing.dto.order.create.request.MercadoPagoOrderRequest;
import br.com.oficina.billing.dto.order.create.response.MercadoPagoOrderResponse;
import br.com.oficina.billing.dto.order.find.MercadoPagoFindOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "billing-client", url = "${billing.url}")
public interface BillingClient {

    @PostMapping("/oauth/token")
    MercadoPagoAuthResponse getAccessToken(MercadoPagoAuthRequest request);

    @PostMapping("/v1/orders")
    MercadoPagoOrderResponse createOrder(@RequestHeader("Authorization") String token, @RequestHeader("X-Idempotency-Key") String key, MercadoPagoOrderRequest request);

    @GetMapping("/v1/orders/{orderId}")
    MercadoPagoFindOrderResponse getOrder(@RequestHeader("Authorization") String token, @PathVariable String orderId);

}
