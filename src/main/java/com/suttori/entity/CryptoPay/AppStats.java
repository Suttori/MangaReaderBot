package com.suttori.entity.CryptoPay;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppStats {

    @JsonProperty("volume")
    private Double volume;

    @JsonProperty("conversion")
    private Double conversion;

    @JsonProperty("unique_users_count")
    private Long uniqueUsersCount;

    @JsonProperty("created_invoice_count")
    private Long createdInvoiceCount;

    @JsonProperty("paid_invoice_count")
    private Long paidInvoiceCount;

    @JsonProperty("start_at")
    private String startAt;

    @JsonProperty("end_at")
    private String endAt;


}
