package com.suttori.service;

import com.suttori.telegram.TelegramSender;
import com.vdurmont.emoji.EmojiParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;

@Service
public class ButtonService {
    private final Logger logger = LoggerFactory.getLogger(ButtonService.class);

    private TelegramSender telegramSender;
    private LocaleService localeService;
    private AdminService adminService;

    @Autowired
    public ButtonService(TelegramSender telegramSender, LocaleService localeService, AdminService adminService) {
        this.telegramSender = telegramSender;
        this.localeService = localeService;
        this.adminService = adminService;
    }

    public void generateMainButtonsWithGreetings(Long userId, String message) {
        KeyboardRow customRow = new KeyboardRow();
        if (adminService.isAdmin(userId)) {
            customRow.add(KeyboardButton.builder().text(EmojiParser.parseToUnicode(localeService.getBundle("buttonService.adminPanel"))).build());
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(new ArrayList<>(Arrays.asList(
                new KeyboardRow(Arrays.asList(KeyboardButton.builder().text(EmojiParser.parseToUnicode("Поиск")).build(),
                        KeyboardButton.builder().text(EmojiParser.parseToUnicode("Случайная манга")).build())),
                new KeyboardRow(Arrays.asList(KeyboardButton.builder().text(EmojiParser.parseToUnicode("Настройки ⚙")).build(),
                        KeyboardButton.builder().text(EmojiParser.parseToUnicode("Профиль")).build())),
                customRow
        )));

        markup.setResizeKeyboard(true);
        markup.setIsPersistent(false);

        if (message.equals(localeService.getBundle("buttonService.mainMenu"))) {
            telegramSender.send(SendMessage.builder()
                    .chatId(userId)
                    .text(localeService.getBundle("buttonService.mainMenu"))
                    .replyMarkup(markup)
                    .parseMode("HTML").build());
        } else {
            telegramSender.sendPhoto(SendPhoto.builder()
                    .photo(new InputFile("AgACAgIAAxkBAAIH8GXP4cRo_m4c4rZWPAsClhd7_GFTAAJ7zzEbxHOASuJDgxuv7s41AQADAgADcwADNAQ"))
                    .chatId(userId)
                    .caption(EmojiParser.parseToUnicode("Приветствую в боте для чтения манги, манхвы и других литератур! С его помощью можно загружать главы и читать прямо в телеграм, получать уведомления о новых главах, добавлять мангу в закладки, вести статистику и следить за друзьями! Приятного чтения ❤\uFE0F"))
                    .replyMarkup(markup)
                    .parseMode("HTML").build());
//            telegramSender.send(SendMessage.builder()
//                    .text("test")
//                    .replyMarkup(markup)
//                    .chatId(userId).build());

        }
    }
}
