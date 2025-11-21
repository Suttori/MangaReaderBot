package com.suttori.entity.CryptoPay;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeRate {

    @JsonProperty("is_valid")
    private Boolean isValid;

    @JsonProperty("is_crypto")
    private Boolean isCrypto;

    @JsonProperty("is_fiat")
    private Boolean isFiat;

    @JsonProperty("source")
    private String source;

    @JsonProperty("target")
    private String target;

    @JsonProperty("rate")
    private String rate;
}
