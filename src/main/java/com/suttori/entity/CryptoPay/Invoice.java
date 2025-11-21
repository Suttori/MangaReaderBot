package com.suttori.entity.CryptoPay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Invoice {
    @JsonProperty("invoice_id")
    private Long invoiceId;
    @JsonProperty("hash")
    private String hash;
    @JsonProperty("currency_type")
    private String currencyType;
    @JsonProperty("asset")
    private String asset;
    @JsonProperty("fiat")
    private String fiat;
    @JsonProperty("amount")
    private String amount;
    @JsonProperty("paid_asset")
    private String paidAsset;
    @JsonProperty("paid_amount")
    private String paidAmount;
    @JsonProperty("paid_fiat_rate")
    private String paidFiatRate;
    @JsonProperty("accepted_assets")
    private List<String> acceptedAssets;
    @JsonProperty("fee_asset")
    private String feeAsset;
    @JsonProperty("fee_amount")
    private Double feeAmount;
    @JsonProperty("fee")
    private String fee; // deprecated
    @JsonProperty("pay_url")
    private String payUrl; // deprecated
    @JsonProperty("bot_invoice_url")
    private String botInvoiceUrl;
    @JsonProperty("mini_app_invoice_url")
    private String miniAppInvoiceUrl;
    @JsonProperty("web_app_invoice_url")
    private String webAppInvoiceUrl;
    @JsonProperty("description")
    private String description;
    @JsonProperty("status")
    private String status;
    @JsonProperty("swap_to")
    private String swapTo;
    @JsonProperty("is_swapped")
    private Boolean isSwapped;
    @JsonProperty("swapped_uid")
    private String swappedUid;
    @JsonProperty("swapped_to")
    private String swappedTo;
    @JsonProperty("swapped_rate")
    private String swappedRate;
    @JsonProperty("swapped_output")
    private String swappedOutput;
    @JsonProperty("swapped_usd_amount")
    private String swappedUsdAmount;
    @JsonProperty("swapped_usd_rate")
    private String swappedUsdRate;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("paid_usd_rate")
    private String paidUsdRate;
    @JsonProperty("usd_rate")
    private String usdRate; // deprecated
    @JsonProperty("allow_comments")
    private Boolean allowComments;
    @JsonProperty("allow_anonymous")
    private Boolean allowAnonymous;
    @JsonProperty("expiration_date")
    private String expirationDate;
    @JsonProperty("paid_at")
    private String paidAt;
    @JsonProperty("paid_anonymously")
    private Boolean paidAnonymously;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("hidden_message")
    private String hiddenMessage;
    @JsonProperty("payload")
    private String payload;
    @JsonProperty("paid_btn_name")
    private String paidBtnName;
    @JsonProperty("paid_btn_url")
    private String paidBtnUrl;


}
