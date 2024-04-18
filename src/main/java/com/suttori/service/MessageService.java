package com.suttori.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class MessageService {

    private final Logger logger = LoggerFactory.getLogger(MessageService.class);

//    public List<SendMessage> createSendMessages(String text, Long chatId) {
//        final var messages = new ArrayList<SendMessage>();
//        for (int i = 0; i < text.length(); i += 4000) {
//            messages.add(
//                    createSendMessageWithMaxLength(
//                            text.substring(i, Math.min(text.length(), i + 4000)), chatId
//                    )
//            );
//        }
//        return messages;
//    }

    public List<SendMessage> createSendMessages(String text, Long chatId) {
        final var messages = new ArrayList<SendMessage>();

        // Разбиваем текст по знакам переноса строки
        String[] lines = text.split("\n\n");

        // Текущая длина сообщения
        int currentLength = 0;

        // Текущее сообщение
        StringBuilder currentMessage = new StringBuilder();

        // Итерируем по строкам
        for (String line : lines) {
            // Если добавление текущей строки не превысит лимит
            if (currentLength + line.length() <= 4000) {
                // Добавляем строку к текущему сообщению
                currentMessage.append(line).append("\n\n");
                currentLength += line.length() + 1; // +1 для символа переноса строки
            } else {
                // Если превышен лимит, создаем новое сообщение
                messages.add(createSendMessageWithMaxLength(currentMessage.toString(), chatId));

                // Сбрасываем текущее сообщение и длину
                currentMessage = new StringBuilder(line).append("\n\n");
                currentLength = line.length() + 1; // +1 для символа переноса строки
            }
        }

        // Добавляем последнее сообщение, если оно не пустое
        if (currentLength > 0) {
            messages.add(createSendMessageWithMaxLength(currentMessage.toString(), chatId));
        }

        return messages;
    }


    public SendMessage createSendMessageWithMaxLength(String text, Long chatId) {
        return SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .disableWebPagePreview(true)
                .parseMode("HTML")
                .build();
    }

    private String substringToTelegramLength(String s) {
        return s.substring(0, Math.min(s.length(), 4000));
    }

    public EditMessageText createEditMessageWithoutButton(String textMessage, Integer messageId, Long chatId) {
        logger.info("createEditMessageWithoutButton " + chatId);
        var editMessageText = new EditMessageText("");
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(textMessage);
        return editMessageText;
    }

    public EditMessageText createEditMessageWithButton(String textMessage, String textButton, Integer messageId, String callbackData, Long chatId) {
        logger.info("createEditMessageWithButton " + chatId);
        var editMessageText = new EditMessageText("");
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(textMessage);
        var buttonOne = new InlineKeyboardButton(textButton);
        buttonOne.setCallbackData(callbackData);
        editMessageText.setReplyMarkup(createButton(Collections.singletonList(buttonOne)));
        return editMessageText;
    }

    public SendMessage createSendMessageWithTwoButtons(String textMessage, String textButtonOne, String textButtonTwo, String callbackDataOne, String callbackDataTwo, Long chatId) {
        logger.info("createSendMessageWithTwoButtons " + chatId);
        var buttonOne = new InlineKeyboardButton(textButtonOne);
        var buttonTwo = new InlineKeyboardButton(textButtonTwo);
        buttonOne.setCallbackData(callbackDataOne);
        buttonTwo.setCallbackData(callbackDataTwo);
        List<InlineKeyboardButton> buttonList = new LinkedList<>();
        buttonList.add(buttonOne);
        buttonList.add(buttonTwo);
        return SendMessage.builder()
                .text(textMessage)
                .chatId(chatId)
                .replyMarkup(createButton(buttonList)).build();
    }

    public InlineKeyboardMarkup createButton(List<InlineKeyboardButton> buttonList) {
        logger.info("createSendMessageWithTwoButtons");
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow rowInLine = new InlineKeyboardRow(buttonList);
        keyboard.add(rowInLine);
        return new InlineKeyboardMarkup(keyboard);
    }
}
