package br.com.oficina.billing.dto.order.find;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MercadoPagoFindOrderItem {

    @JsonProperty("external_code")
    private String externalCode;
    private String title;
    private String description;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("picture_url")
    private String pictureUrl;
    private Integer quantity;
    @JsonProperty("unit_price")
    private String unitPrice;
    private String type;
    private Boolean warranty;
    @JsonProperty("event_date")
    private LocalDateTime eventDate;
}
