package br.com.oficina.billing.dto.order.create.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MercadoPagoOrderInterestFee {

    private String type;
    private List<Integer> values;
}
