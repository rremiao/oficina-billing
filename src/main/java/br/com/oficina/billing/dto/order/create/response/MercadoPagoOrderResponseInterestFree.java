package br.com.oficina.billing.dto.order.create.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderResponseInterestFree {

    private String type;
    private List<Integer> values;
}
