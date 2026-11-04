package br.com.oficina.billing.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoAuthRequest {

    @JsonProperty("client_secret")
    private String clientSecret;
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("grant_type")
    private String grantType;
    private String code;

}
