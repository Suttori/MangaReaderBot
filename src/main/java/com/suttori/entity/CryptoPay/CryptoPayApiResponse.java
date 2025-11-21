package com.suttori.entity.CryptoPay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CryptoPayApiResponse <T>{

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("result")
    private T result;

    @JsonProperty("error")
    private String error;
}
