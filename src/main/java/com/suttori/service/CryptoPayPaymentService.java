package com.suttori.service;


import com.suttori.dao.ActivationTokenRepository;
import com.suttori.entity.ActivationToken;
import com.suttori.entity.CryptoPay.CryptoPayApiResponse;
import com.suttori.entity.CryptoPay.Invoice;
import com.suttori.entity.User;
import com.suttori.telegram.CryptoPayApiFeignClient;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;


import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;

@Service
public class CryptoPayPaymentService {

    @Value("${cryptoPayApiToken}")
    private String cryptoPayApiToken;

    private final Logger logger = LoggerFactory.getLogger(CryptoPayPaymentService.class);

    private CryptoPayApiFeignClient cryptoPayApiFeignClient;

    private Util util;
    private TelegramSender telegramSender;
    private UserService userService;
    private ActivationTokenService activationTokenService;

    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    public CryptoPayPaymentService(CryptoPayApiFeignClient cryptoPayApiFeignClient, Util util, TelegramSender telegramSender, UserService userService, ActivationTokenService activationTokenService, ActivationTokenRepository activationTokenRepository) {
        this.cryptoPayApiFeignClient = cryptoPayApiFeignClient;
        this.util = util;
        this.telegramSender = telegramSender;
        this.userService = userService;
        this.activationTokenService = activationTokenService;
        this.activationTokenRepository = activationTokenRepository;
    }

    public void clickDonateCryptoPay(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Выбери сумму, которую ты хочешь пожертвовать на развитие бота")
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("3$")).callbackData("payViaCryptoPay\n3\n" + userId).build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("5$")).callbackData("payViaCryptoPay\n5\n" + userId).build(),
                                InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("10$")).callbackData("payViaCryptoPay\n10\n" + userId).build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Другая сумма")).callbackData("payViaCryptoPay\nanother\n" + userId).build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backToSettings").build())
                )))).build());
    }

    public void chooseSumForPay(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        String string = callbackQuery.getData();
        String[] parts = string.split("\n");
        Invoice invoice;
        if (parts[1].equals("3")) {
            invoice = createInvoice("crypto", "USDT", null, "3", userId.toString());
        } else if (parts[1].equals("5")) {
            invoice = createInvoice("crypto", "USDT", null, "5", userId.toString());
        } else if (parts[1].equals("10")) {
            invoice = createInvoice("crypto", "USDT", null, "10", userId.toString());
        } else if (parts[1].equals("another")) {
            chooseAnotherSum(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            return;
        } else {
            util.sendErrorMessage("Ошибка при выборе суммы доната, попробуй еще раз и, если ошибка повторится, то обратись в поддержку", userId);
            logger.error("Ошибка при выборе суммы оплаты");
            return;
        }

        if (invoice == null) {
            util.sendErrorMessage("Ошибка при создании счета на оплату, попробуй еще раз и, если ошибка повторится, то обратись в поддержку", userId);
            logger.error("Ошибка при создании счета на оплату");
            return;
        }

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(userId)
                .text("Нажми на кнопку ниже, чтобы произвести оплату")
                .replyMarkup(new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Оплатить")).url(invoice.getBotInvoiceUrl()).build()),
                                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backToSettings").build())
                ))).build());
    }

    private void chooseAnotherSum(Long userId, Integer messageId) {
        userService.setPosition(userId, "WAIT_FOR_DONATE_SUM");
        telegramSender.sendEditMessageText(EditMessageText.builder().
                text("Введи сумму, которую хочешь задонатить в $ (USDT)")
                .chatId(userId)
                .messageId(messageId)
                .replyMarkup(new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backToSettings").build())
                ))).build());
    }

    public void getSumForDonate(Message message) {
        Long userId = message.getFrom().getId();
        if (message.hasText() && message.getText().matches("^[1-9]\\d*$")) {
            Invoice invoice = createInvoice("crypto", "USDT", null, message.getText(), String.valueOf(userId));
            telegramSender.send(SendMessage.builder()
                    .chatId(userId)
                    .text("Нажми на кнопку ниже, чтобы произвести оплату")
                    .replyMarkup(new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Оплатить")).url(invoice.getBotInvoiceUrl()).build()),
                            new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Назад")).callbackData("backToSettings").build())
                    ))).build());
        } else {
            util.sendErrorMessage("Сумма должна быть целым положительным числом больше 0, попробуй еще раз", message.getFrom().getId());
        }
    }

    private Invoice createInvoice(String currencyType, String asset, String fiat, String amount, String payload) {
        User user = userService.getUser(Long.valueOf(payload));
        Map<String, Object> param = new HashMap<>();
        if (currencyType.equals("crypto")) {
            param.put("currency_type", currencyType);
            param.put("asset", asset);
            param.put("amount", amount);
        } else if (currencyType.equals("fiat")) {
            param.put("currency_type", currencyType);
            param.put("fiat", fiat);
            param.put("amount", amount);
        } else {
            logger.error("Ошибка при получении currencyType");
            return null;
        }

        ActivationToken activationToken = activationTokenService.generateToken(Long.valueOf(payload), new Timestamp(System.currentTimeMillis() + 72 * 60 * 60 * 1000));

        if (Integer.parseInt(amount) >= 3 && (user.getIsPremiumBotUser() == null || !user.getIsPremiumBotUser())) {
            param.put("hidden_message", "Нажми на \"Перейти в бот\", чтобы активировать бонусы за донат");
            param.put("paid_btn_name", "openBot");

            param.put("paid_btn_url", "https://t.me/MangaManhwa_bot?start=activatePass_" + activationToken.getId() + "_" + payload + "_" + activationToken.getToken());
        } else {
            param.put("hidden_message", "Большое спасибо за поддержку! ♥");
        }

        param.put("payload", payload);
        param.put("expires_in", Duration.ofMinutes(15).getSeconds());

        try {
            CryptoPayApiResponse<Invoice> result = cryptoPayApiFeignClient.createInvoice(cryptoPayApiToken, param);
            activationToken.setInvoiceId(result.getResult().getInvoiceId());
            activationTokenRepository.save(activationToken);
            return result.getResult();
        } catch (Exception e) {
            logger.error("Ошибка при получении при создании createInvoice");
            e.printStackTrace();
            return null;
        }

    }


}
