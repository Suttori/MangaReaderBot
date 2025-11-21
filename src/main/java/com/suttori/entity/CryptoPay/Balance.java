package com.suttori.entity.CryptoPay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Balance {

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("available")
    private String available;

    @JsonProperty("onhold")
    private String onhold;
}
