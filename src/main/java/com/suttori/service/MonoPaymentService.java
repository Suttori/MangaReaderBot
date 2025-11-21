package com.suttori.service;

import com.suttori.dao.UserRepository;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MonoPaymentService {

    private TelegramSender telegramSender;
    private Util util;
    private AdminService adminService;
    private LocaleService localeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public MonoPaymentService(TelegramSender telegramSender, Util util, AdminService adminService, LocaleService localeService) {
        this.telegramSender = telegramSender;
        this.adminService = adminService;
        this.util = util;
        this.localeService = localeService;
    }

    public void clickDonateByMono () {

    }

    public void getRequest(Message message) {
        User user = userRepository.findByUserId(message.getFrom().getId());

        telegramSender.send(SendMessage.builder()
                .text("Спасибо за донат! ❤\uFE0F \n\nПодключение функций обычно занимает не больше часа, если у тебя есть вопросы, то напиши в поддержку.")
                .chatId(message.getFrom().getId()).build());

        StringBuilder text = new StringBuilder();

        if (user.getUserName() != null) {
            text.append("\uD83E\uDD11 Оплата юзером @").append(user.getUserName()).append(" id <code>").append(user.getUserId()).append("</code>");
        } else if (user.getFirstName() != null && user.getLastName() != null) {
            text.append("\uD83E\uDD11 Оплата юзером ").append(user.getFirstName()).append(" ").append(user.getLastName()).append(" id <code>").append(user.getUserId()).append("</code>");
        } else {
            text.append("\uD83E\uDD11 Оплата юзером ").append(user.getFirstName()).append(" id <code>").append(user.getUserId()).append("</code>");
        }

        if (user.getSubscriptionEndDate() != null) {
            text.append("\nТекущая подписка до: ").append(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(user.getSubscriptionEndDate().toLocalDateTime()));
        } else {
            text.append("\nТекущая подписка отсутствует");
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Подписка на месяц").callbackData("subscription_activation\nmonth\n" + user.getUserId()).build()),
            new InlineKeyboardRow(InlineKeyboardButton.builder().text("Подписка на 3 месяца").callbackData("subscription_activation\n3month\n" + user.getUserId()).build()),
            new InlineKeyboardRow(InlineKeyboardButton.builder().text("Подписка на 6 месяцев").callbackData("subscription_activation\n6month\n" + user.getUserId()).build()),
            new InlineKeyboardRow(InlineKeyboardButton.builder().text("Подписка на год").callbackData("subscription_activation\nyear\n" + user.getUserId()).build())
        )));

        telegramSender.send(SendMessage.builder()
                .text(EmojiParser.parseToUnicode(text.toString()))
                .chatId(6298804214L)
                .parseMode("HTML")
                .replyMarkup(inlineKeyboardMarkup).build());
    }

    public void clickActivationSubscribe(CallbackQuery callbackQuery) {
        if (!adminService.isAdmin(callbackQuery.getFrom().getId())) {
            return;
        }

        User user = userRepository.findByUserId(Long.valueOf(util.parseValue(callbackQuery.getData())[2]));
        int subscriptionDays =0;
        if (util.parseValue(callbackQuery.getData())[1].equals("month")) {
            subscriptionDays = 30;
        } else if (util.parseValue(callbackQuery.getData())[1].equals("3month")) {
            subscriptionDays = 90;
        } else if (util.parseValue(callbackQuery.getData())[1].equals("6month")) {
            subscriptionDays = 180;
        } else if (util.parseValue(callbackQuery.getData())[1].equals("year")) {
            subscriptionDays = 365;
        }

        if (user.getSubscriptionEndDate() == null) {
            user.setSubscriptionEndDate(new Timestamp(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(subscriptionDays)));
        } else {
            user.setSubscriptionEndDate(new Timestamp(user.getSubscriptionEndDate().getTime() + TimeUnit.DAYS.toMillis(subscriptionDays)));
        }
        user.setIsPremiumBotUser(true);
        userRepository.save(user);
        user = userRepository.findByUserId(user.getUserId());
        String formattedTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(user.getSubscriptionEndDate().toLocalDateTime());
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Подписка успешно активирована\n\n" + "Дата окончания подписки юзера <code>" + user.getUserId() + "</code>: " + formattedTime)
                .parseMode("HTML")
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getFrom().getId()).build());
        telegramSender.send(SendMessage.builder()
                .text(EmojiParser.parseToUnicode("Дополнительные функции уже доступны!\nДоступ дан до: " + formattedTime + "\n\nСпасибо за донат! ❤\uFE0F"))
                .chatId(user.getUserId())
                .parseMode("HTML").build());
    }

}
