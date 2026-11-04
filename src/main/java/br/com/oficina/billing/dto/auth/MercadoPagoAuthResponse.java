package br.com.oficina.billing.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MercadoPagoAuthResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private String expiresIn;
    private String scope;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("public_key")
    private String publicKey;
    @JsonProperty("live_mode")
    private Boolean liveMode;
}
