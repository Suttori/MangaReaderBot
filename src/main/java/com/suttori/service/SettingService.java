package com.suttori.service;

import com.suttori.dao.UserRepository;
import com.suttori.entity.User;
import com.suttori.telegram.TelegramSender;
import com.suttori.util.Util;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
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
    private UserRepository userRepository;

    @Autowired
    public SettingService(TelegramSender telegramSender, Util util, LocaleService localeService, UserService userService, ButtonService buttonService, UserRepository userRepository) {
        this.telegramSender = telegramSender;
        this.util = util;
        this.localeService = localeService;
        this.userService = userService;
        this.buttonService = buttonService;
        this.userRepository = userRepository;
    }

    public void clickSettings(Long userId, Integer messageId) {
        User user = userRepository.findByUserId(userId);
        boolean access = user.getIsPremiumBotUser() != null && user.getIsPremiumBotUser();
        userRepository.setPosition("DEFAULT_POSITION", userId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                //new InlineKeyboardRow(InlineKeyboardButton.builder().text(localeService.getBundle("settingService.changeLanguage")).callbackData("chooseLanguage").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Изменить формат выдачи манги")).callbackData("changeMangaFormat").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(access ? "Изменить количество выдачи глав" : "Изменить количество выдачи глав \uD83D\uDD12")).callbackData("changeNumberOfChaptersSent").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode("Поддержать донатом ☕")).callbackData("clickDonate").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(EmojiParser.parseToUnicode(localeService.getBundle("settingService.support"))).callbackData("clickTechSupport").build())
        )));

        if (messageId != null) {
            telegramSender.sendEditMessageText(EditMessageText.builder()
                    .text("В настройках вы можете найти мои контакты, изменить формат выдачи глав и многое другое")
                    .messageId(messageId)
                    .replyMarkup(inlineKeyboardMarkup)
                    .chatId(userId).build());
        } else {
            telegramSender.send(SendMessage.builder()
                    .text("В настройках вы можете найти мои контакты, изменить формат выдачи глав и многое другое")
                    .chatId(userId)
                    .replyMarkup(inlineKeyboardMarkup).build());
        }
    }

    public void clickChangeNumberOfChaptersSent(CallbackQuery callbackQuery) {
        User user = userRepository.findByUserId(callbackQuery.getFrom().getId());
        if (user.getIsPremiumBotUser() == null || !user.getIsPremiumBotUser()) {
            telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Прости, но это функция доступна только тем, кто поддержал бот донатом. Она необязательна и призвана отблагодарить людей, которые поддерживают разработку бота. Подробнее читай в \"Поддержать донатом ☕\"")
                    .showAlert(true).build());
            return;
        }
        String numberOfChaptersSent = userRepository.findByUserId(callbackQuery.getFrom().getId()).getNumberOfChaptersSent();
        String formatName = null;
        if (numberOfChaptersSent == null) {
            formatName = "Сейчас главы выдаются по 3 штуки за раз";
            userRepository.setNumberOfChaptersSent("3", callbackQuery.getFrom().getId());
        } else if (numberOfChaptersSent.equals("3")) {
            formatName = "Сейчас главы выдаются по 3 штуки за раз";
        } else if (numberOfChaptersSent.equals("5")) {
            formatName = "Сейчас главы выдаются по 5 штук за раз";
        } else if (numberOfChaptersSent.equals("10")) {
            formatName = "Сейчас главы выдаются по 10 штук за раз";
        }

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Для того чтобы загрузить несколько глав одним нажатием ты можешь нажать на соответствующую кнопку под главой. Здесь можешь выбрать сколько глав отправится после нажатия на эту кнопку.\n\n" + formatName)
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("3").callbackData("chooseNumberOfChaptersSent\n3").build(),
                                InlineKeyboardButton.builder().text("5").callbackData("chooseNumberOfChaptersSent\n5").build(),
                                InlineKeyboardButton.builder().text("10").callbackData("chooseNumberOfChaptersSent\n10").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("Назад").callbackData("backToSettings").build())
                ))))
                .parseMode("HTML")
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public void changeNumberOfChaptersSent(CallbackQuery callbackQuery) {
        String numberOfChaptersSent = util.parseValue(callbackQuery.getData())[1];
        String formatName = null;
        switch (numberOfChaptersSent) {
            case "3" -> {
                userRepository.setNumberOfChaptersSent("3", callbackQuery.getFrom().getId());
                formatName = "3";
            }
            case "5" -> {
                userRepository.setNumberOfChaptersSent("5", callbackQuery.getFrom().getId());
                formatName = "5";
            }
            case "10" -> {
                userRepository.setNumberOfChaptersSent("10", callbackQuery.getFrom().getId());
                formatName = "10";
            }
        }
        telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("Количество выдачи глав одним сообщением изменено на " + formatName + ". Если захочешь изменить его, то всегда можешь сделать это в настройках.")
                .showAlert(true).build());
        clickSettings(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
    }

    public void clickChangeMangaFormat(CallbackQuery callbackQuery) {
        String format = userRepository.findByUserId(callbackQuery.getFrom().getId()).getMangaFormatParameter();
        String formatName = null;
        if (format == null) {
            formatName = "Сейчас выбран \"Адаптивный\" формат выдачи манги.";
        } else if (format.equals("telegraph")) {
            formatName = "Сейчас выбран формат выдачи всей манги и манхвы в виде \"Telegraph статьи\"";
        } else if (format.equals("pdf")) {
            formatName = "Сейчас выбран формат выдачи всей манги и манхвы в виде \"PDF файла\"";
        }

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Здесь ты можешь выбрать формат выдачи глав.\n\n<b>Адаптивный</b> - обычная работа бота, когда манга и комиксы выдаются в виде Telegraph статьи, а манхва и всё что имеет длинные страницы в виде PDF файла. Подходит для большинства ситуаций." +
                        "\n\n<b>Telegraph статья</b> - ссылка на мангу в читалке Telegraph, преимуществом является то, что ее не нужно качать и все происходит почти моментально. В свою очередь не подходит для манхв, потому что страницы обрезаются или выглядят мелкими." +
                        "\n\n<b>PDF файл</b> - бот выдает PDF файл, который вы загружаете и читаете в любое время. Из плюсов: едва заметное лучшее качество изображений по сравнению с Telegraph статьей; если скачали главу, то можно читать без интернета. Минусы: приходиться качать главы; не всегда главы загружены, и иногда приходится ждать около минуты; кому-то может понадобится очистить кеш телеграма; бо́льшая нагрузка на сервер.\n\n" + formatName)
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(Arrays.asList(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("Адаптивный").callbackData("chooseMangaFormat\nadaptive").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("Telegraph статья").callbackData("chooseMangaFormat\ntelegraph").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("PDF файл").callbackData("chooseMangaFormat\npdf").build()),
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("Назад").callbackData("backToSettings").build())
                ))))
                .parseMode("HTML")
                .chatId(callbackQuery.getFrom().getId()).build());
    }

    public void changeMangaFormat(CallbackQuery callbackQuery) {
        String format = util.parseValue(callbackQuery.getData())[1];
        String formatName = null;
        switch (format) {
            case "adaptive" -> {
                userRepository.setMangaFormat(null, callbackQuery.getFrom().getId());
                formatName = "\"Адаптивный\"";
            }
            case "telegraph" -> {
                userRepository.setMangaFormat("telegraph", callbackQuery.getFrom().getId());
                formatName = "Telegraph статьи";
            }
            case "pdf" -> {
                userRepository.setMangaFormat("pdf", callbackQuery.getFrom().getId());
                formatName = "PDF файл";
            }
        }
        telegramSender.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("Формат выдачи манги изменен на " + formatName + ". Если захочешь изменить его, то всегда можешь сделать это в настройках.")
                .showAlert(true).build());
        clickSettings(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
    }

    public void clickTechSupport(CallbackQuery callbackQuery) {
        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("По техническим и всем другим вопросам пиши сюда @Suttori")
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(new InlineKeyboardMarkup(new ArrayList<>(List.of(
                        new InlineKeyboardRow(InlineKeyboardButton.builder().text("Назад").callbackData("backToSettings").build())
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
        userService.setPosition(callbackQuery.getFrom().getId(), "DEFAULT_POSITION");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(new ArrayList<>(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Crypto Pay (USDT)").callbackData("clickDonateCryptoPay").build()),
               // new InlineKeyboardRow(InlineKeyboardButton.builder().text("Monobank").callbackData("clickDonateMono").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Назад").callbackData("backToSettings").build())
        )));

        telegramSender.sendEditMessageText(EditMessageText.builder()
                .text("Если тебе нравится бот и ты хочешь поддержать его развитие и работу, то можешь сделать это по кнопкам ниже или лично @Suttori\n\nЗа донат от 3$ тебе станет доступна функция загрузки нескольких глав одновременно\n\nСпасибо за поддержку ❤\uFE0F")
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(inlineKeyboardMarkup)
                .disableWebPagePreview(true)
                .disableWebPagePreview(true)
                .chatId(callbackQuery.getMessage().getChatId()).build());
    }

}
