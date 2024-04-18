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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.*;

@Service
public class SettingService {

    private TelegramSender telegramSender;
    private Util util;
    private LocaleService localeService;
    private UserService userService;
    private ButtonService buttonService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public SettingService(TelegramSender telegramSender, Util util, LocaleService localeService, UserService userService, ButtonService buttonService) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.localeService = localeService;
        this.userService = userService;
        this.buttonService = buttonService;
    }

    public void clickSettings(Long userId, Integer messageId) {
        userRepository.setTemporaryMessageId(null, userId);
        userRepository.setPosition("DEFAULT_POSITION", userId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                //Collections.singletonList(InlineKeyboardButton.builder().text(localeService.getBundle("settingService.changeLanguage")).callbackData("chooseLanguage").build()),
                //Collections.singletonList(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(localeService.getBundle("settingService.supportMe"))).callbackData("clickDonate").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(localeService.getBundle("settingService.support"))).callbackData("clickTechSupport").build())
        )));

        if (messageId != null) {
            telegramSender.sendEditMessageText(EditMessageText.builder()
                    .text("В настройках вы можете найти мои контакты")
                    .messageId(messageId)
                    .replyMarkup(inlineKeyboardMarkup)
                    .chatId(userId).build());
        } else {
            telegramSender.send(SendMessage.builder()
                    .text("В настройках вы можете найти мои контакты и способы поддержки")
                    .chatId(userId)
                    .replyMarkup(inlineKeyboardMarkup).build());
        }
    }

    public void clickTechSupport(CallbackQuery callbackQuery) {
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("По техническим и всем другим вопросам пиши сюда @Suttori, также не буду против любой поддержки, патреон: patreon.com/MangaReaderBot\n")
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text(localeService.getBundle("cancel")).callbackData("backToSettings").build())
                ))))
                .chatId(callbackQuery.getMessage().getChatId()).build());
    }

    public void chooseLanguage(CallbackQuery callbackQuery) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("English").callbackData("set_language\nen").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Українська").callbackData("set_language\nuk").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Русский").callbackData("set_language\nru").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(localeService.getBundle("cancel")).callbackData("backToSettings").build())
        )));

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text(localeService.getBundle("settingService.chooseLanguage"))
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(inlineKeyboardMarkup)
                .chatId(callbackQuery.getMessage().getChatId()).build());
    }

    public void setLocale(CallbackQuery callbackQuery) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(localeService.getBundle("back")).callbackData("backToSettings").build())
        )));
        String locale = util.parseValue(callbackQuery.getData())[1];
        localeService.setLocale(Locale.forLanguageTag(locale));
        userRepository.setLocale(locale, callbackQuery.getFrom().getId());
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text(localeService.getBundle("settingService.languageChangeSuccessful"))
                .chatId(callbackQuery.getFrom().getId())
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("HTML")
                .messageId(callbackQuery.getMessage().getMessageId()).build());

        callbackQuery.getFrom().setLanguageCode(userService.getLocale(callbackQuery.getFrom().getId()));
        localeService.setLocale(Locale.forLanguageTag(callbackQuery.getFrom().getLanguageCode()));
        buttonService.generateMainButtonsWithGreetings(callbackQuery.getFrom().getId(), "");
    }

    public void clickDonate(CallbackQuery callbackQuery) {
        User user = userRepository.findByUserId(callbackQuery.getFrom().getId());
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(localeService.getBundle("settingService.supportWithHeart"))).callbackData("goToPay").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(localeService.getBundle("cancel")).callbackData("backToSettings").build())
        )));

        String balance = localeService.getBundle("settingService.currentBalance") + user.getBalance() + " \uD83C\uDF4C";

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text(EmojiParser.parseToUnicode(localeService.getBundle("settingService.describeDonate") + balance))
                .chatId(callbackQuery.getMessage().getChatId())
                .parseMode("HTML")
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(inlineKeyboardMarkup).build());
    }
}
