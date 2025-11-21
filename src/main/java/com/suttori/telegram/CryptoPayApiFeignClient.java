package com.suttori.telegram;

import com.suttori.config.FeignConfig;
import com.suttori.entity.CryptoPay.CryptoPayApiResponse;
import com.suttori.entity.CryptoPay.Invoice;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "cryptoPayApiFeignClient", url = "https://pay.crypt.bot/", configuration = FeignConfig.class)
public interface CryptoPayApiFeignClient {

    @GetMapping("/api/getMe")
    Response getMe(@RequestHeader("Crypto-Pay-API-Token") String token);

    @PostMapping("/api/createInvoice")
    CryptoPayApiResponse<Invoice> createInvoice(@RequestHeader("Crypto-Pay-API-Token") String token, @RequestBody Map<String, Object> params);
}
