package com.suttori.entity.CryptoPay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transfer {
    @JsonProperty("transfer_id")
    private Long transferId;

    @JsonProperty("spend_id")
    private String spendId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("asset")
    private String asset;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("status")
    private String status; // always "completed"

    @JsonProperty("completed_at")
    private String completedAt;

    @JsonProperty("comment")
    private String comment;
}
