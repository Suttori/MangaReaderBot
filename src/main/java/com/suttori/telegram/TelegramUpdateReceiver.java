package com.suttori.telegram;


import com.suttori.entity.CryptoPay.Invoice;
import com.suttori.processor.Processor;
import com.suttori.service.AwsServerService;
import com.suttori.service.CryptoPayPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.concurrent.*;


@Component
public class TelegramUpdateReceiver implements LongPollingUpdateConsumer {


    private Processor processor;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, List<Update>>> userUpdateMap = new ConcurrentHashMap<>();
    private CryptoPayPaymentService cryptoPayPaymentService;


    private TelegramSender telegramSender;

    @Autowired
    public TelegramUpdateReceiver(CryptoPayPaymentService cryptoPayPaymentService, TelegramSender telegramSender) {
        this.cryptoPayPaymentService = cryptoPayPaymentService;
        this.telegramSender = telegramSender;
    }

    @Autowired
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void consume(List<Update> updates) {

//
//        Invoice invoice = cryptoPayPaymentService.createInvoice("crypto", "USDT", null, "28", "userInfo");
//
//        telegramSender.send(SendMessage.builder()
//                .text(invoice.getMiniAppInvoiceUrl())
//                .chatId(updates.get(0).getMessage().getFrom().getId()).build());

        //processor.process(updates);


        Long userId = getUserId(updates.get(0));
        if (userId == null) {
            return;
        }

        long timeStamp = System.currentTimeMillis();

        if (userUpdateMap.get(userId) != null) {
            userUpdateMap.get(userId).put(timeStamp, updates);
        } else {
            ConcurrentHashMap<Long, List<Update>> linkedHashMap = new ConcurrentHashMap<>();
            linkedHashMap.put(timeStamp, updates);
            userUpdateMap.put(userId, linkedHashMap);
            try {
                executorService.submit(() ->
                        processUserQueue(updates, userId, timeStamp)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processUserQueue(List<Update> updates, Long userId, long timeStamp) {
        try {
            processor.process(updates);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            userUpdateMap.get(userId).remove(timeStamp);
            Map<Long, List<Update>> result = userUpdateMap.get(userId);
            if (result != null) {
                Long smallestKey = result.keySet()
                        .stream()
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                if (smallestKey == null) {
                    userUpdateMap.remove(userId);
                } else {
                    processUserQueue(userUpdateMap.get(userId).get(smallestKey), userId, smallestKey);
                }
            }
        }
    }

    public Long getUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        } else if (update.hasInlineQuery()) {
            return update.getInlineQuery().getFrom().getId();
        } else {
            return null;
        }
    }

}
