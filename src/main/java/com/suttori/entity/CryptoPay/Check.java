package com.suttori.entity.CryptoPay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Check {
    @JsonProperty("check_id")
    private Long checkId;

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("asset")
    private String asset;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("bot_check_url")
    private String botCheckUrl;

    @JsonProperty("status")
    private String status; // "active" or "activated"

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("activated_at")
    private String activatedAt;
}
